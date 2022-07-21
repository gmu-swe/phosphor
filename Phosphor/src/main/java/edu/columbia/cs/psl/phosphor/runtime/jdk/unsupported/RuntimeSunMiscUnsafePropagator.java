package edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator.OffsetPair;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/* Ensures that calls methods in Unsafe that set or retrieve the value of a field of a Java heap object set and
 * retrieve both the original field and its associated taint field if it has one. */
public class RuntimeSunMiscUnsafePropagator {

    private RuntimeSunMiscUnsafePropagator() {
        // Prevents this class from being instantiated
    }

    /* Used to disambiguate between a static field of a given type and an instance field of java.lang.Class */
    static long LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = Unsafe.INVALID_FIELD_OFFSET;

    /* Stores pairs containing the offset of an original, non-static primitive or primitive array field for the specified
     * class and the offset of the tag field associated with that original field. */
    private static SinglyLinkedList<OffsetPair> getOffsetPairs(Unsafe unsafe, Class<?> targetClazz) {
        SinglyLinkedList<OffsetPair> list = new SinglyLinkedList<>();
        for(Class<?> clazz = targetClazz; clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            for(Field field : clazz.getDeclaredFields()) {
                try {
                    Class<?> fieldClazz = field.getType();
                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    long fieldOffset = (isStatic ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field));
                    long tagOffset = Unsafe.INVALID_FIELD_OFFSET;
                    long wrapperOffset = Unsafe.INVALID_FIELD_OFFSET;
                    try {
                        if(!field.getName().equals("SPECIES_DATA")) {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                            if (taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = (isStatic ? unsafe.staticFieldOffset(taintField) : unsafe.objectFieldOffset(taintField));
                            }
                        }
                    } catch(Exception e) {
                        //
                    }
                    if(fieldClazz.isArray()) {
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if(taintClazz != null && TaggedArray.class.isAssignableFrom(taintClazz)) {
                                wrapperOffset = (isStatic ? unsafe.staticFieldOffset(taintField) : unsafe.objectFieldOffset(taintField));
                            }
                        } catch(Exception e) {
                            //
                        }
                    }
                    list.enqueue(new OffsetPair(isStatic, fieldOffset, wrapperOffset, tagOffset));
                } catch(Exception e) {
                    //
                }
            }
        }
        return list;
    }

    /* returns an offset pair for the specified object's class where either the original field offset or the tag field
     * offset matches the specified offset or null if such an offset pair could not be found. */
    public static OffsetPair getOffsetPair(Unsafe unsafe, Object o, long offset) {
        try {
            Class<?> cl = null;
            boolean isStatic = false;
            if(o instanceof Class) {
                /* We MIGHT be accessing a static field of this class, in which case we should take
                   the offset from *this* class instance (o). But, we might also be accessing an instance
                   field of the type Class, in which case we want to use the classes's class.
                 */
                if(LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS == Unsafe.INVALID_FIELD_OFFSET) {
                    findLastInstanceFieldOnJavaLangClass(unsafe);
                }
                if(offset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    /* We are not accessing an instance field of java.lang.Class, hence, we must be accessing
                     * a static field of type (Class) o */
                    cl = (Class) o;
                    isStatic = true;
                }
                /* Otherwise, we are accessing an instance field of java.lang.Class */
            }
            if(cl == null && o != null && o.getClass() != null) {
                cl = o.getClass();
            }
            if(cl != null) {
                if(InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl) == null) {
                    InstrumentedJREFieldHelper.set$$PHOSPHOR_OFFSET_CACHE(cl, getOffsetPairs(unsafe, cl));
                }
                for(OffsetPair pair : InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl)) {
                    if(pair.origFieldOffset == offset && pair.isStatic == isStatic) {
                        return pair;
                    }
                }
            }
            return null;
        } catch(Exception e) {
            return null;
        }
    }

    private static void findLastInstanceFieldOnJavaLangClass(Unsafe unsafe) {
        for(Field field : Class.class.getDeclaredFields()) {
            try {
                Class<?> fieldClazz = field.getType();
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if(isStatic) {
                    continue;
                }
                long fieldOffset = unsafe.objectFieldOffset(field);
                if(fieldOffset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = fieldOffset;
                }
            } catch(Exception e) {
                //
            }
        }
    }

    /* If prealloc is a wrapped primitive type, set it's taint to be the value of the field at the specified offset in the
     * other specified object. Otherwise returns the value of the field at the specified offset in the specified object. */
    private static void getTag(Unsafe unsafe, Object obj, long originalOffset, PhosphorStackFrame stackFrame, SpecialAccessPolicy policy) {
        stackFrame.returnTaint = Taint.emptyTaint();
        OffsetPair pair = getOffsetPair(unsafe, obj, originalOffset);
        if(pair != null && pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
            Object result = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, pair.tagFieldOffset) : unsafe.getObject(obj, pair.tagFieldOffset);
            if(result instanceof Taint) {
                stackFrame.returnTaint = (Taint) result;
            }
        }
    }

    /* If the specified Object value is a wrapped primitive type, puts it's taint into the field at the specified offset in the
     * other specified object. Otherwise if the specified Object value is null or a lazy array wrapper put the specified Object
     * value into the field at the specified offset in the other specified object. */
    private static void putTag(Unsafe unsafe, Object obj, long offset, Taint tag, SpecialAccessPolicy policy) {
        OffsetPair pair = null;
        if(obj != null) {
            pair = getOffsetPair(unsafe, obj, offset);
        }
        if(pair != null) {
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedObject(obj, pair.tagFieldOffset, tag);
                    break;
                case VOLATILE:
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, tag);
                    break;
                default:
                    unsafe.putObject(obj, pair.tagFieldOffset, tag);
            }
        }
    }

    /* If the specified TaintedPrimitiveWithObjTag and TaggedArray's component types match sets a tag
     * in the specified TaggedArray at a calculated index.
     * type's match. */
    private static void swapArrayElementTag(Unsafe unsafe, TaggedArray tags, long offset, Taint valueTaint) {
        if(tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if(tags.taints == null && valueTaint != null && !valueTaint.isEmpty()) {
                tags.taints = new Taint[tags.getLength()];
            }
            if(tags.taints != null) {
                tags.taints[index] = valueTaint;
            }
        }
    }

    public static void copyMemory(Unsafe unsafe, Object src, long srcAddress, Object dest, long destAddress, long length, PhosphorStackFrame stackFrame) {
        if(src instanceof TaggedArray) {
            src = ((TaggedArray) src).getVal();
        }
        if(dest instanceof TaggedArray) {
            dest = ((TaggedArray) dest).getVal();
        }
        unsafe.copyMemory(src, srcAddress, dest, destAddress, length);
    }

    public static void copyMemory(Unsafe unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame stackFrame) {
        unsafe.copyMemory(srcAddress, destAddress, length);
    }

    public static boolean compareAndSwapObject(Unsafe unsafe, Object obj, long offset, Object expected, Object value, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if(obj instanceof TaggedReferenceArray) {
            Taint valueTaint = stackFrame.getArgTaint(4);
            ret = unsafe.compareAndSwapObject(((TaggedReferenceArray) obj).val, offset, expected, value);
            if(ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            OffsetPair pair = null;
            boolean didCAS = false;
            if(value instanceof TaggedArray || expected instanceof TaggedArray) {
                //Need to be careful - maybe we are hitting a 1D primitive array field
                if(obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    //We are doing a CAS on a 1d primitive array field
                    ret = unsafe.compareAndSwapObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(expected), MultiDArrayUtils.unbox1DOrNull(value));
                    didCAS = true;
                }
            }
            if(!didCAS) {
                //Either this is not a wrapped array, or we are storing it to the place where it should be stored without unwrapping
                ret = unsafe.compareAndSwapObject(obj, offset, expected, value);
                if(pair == null && obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
            }

            if(pair != null && ret) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, stackFrame.getArgTaint(4));
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, value);
                }
            }
        }
        return ret;
    }

    public static boolean compareAndSwapInt(Unsafe unsafe, Object obj, long offset, int expected, int value, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if(obj instanceof TaggedIntArray) {
            ret = unsafe.compareAndSwapInt(((TaggedIntArray) obj).val, offset, expected, value);
            if(ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, phosphorStackFrame.getArgTaint(4));
            }
        } else {
            ret = unsafe.compareAndSwapInt(obj, offset, expected, value);
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null && ret) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(4));
                }
            }
        }
        return ret;
    }

    public static boolean compareAndSwapLong(Unsafe unsafe, Object obj, long offset, long expected, long value, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if(obj instanceof TaggedLongArray) {
            ret = unsafe.compareAndSwapLong(((TaggedLongArray) obj).val, offset, expected, value);
            if(ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, phosphorStackFrame.getArgTaint(4));
            }
        } else {
            ret = unsafe.compareAndSwapLong(obj, offset, expected, value);
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null && ret) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(4));
                }
            }
        }
        return ret;
    }

    private static int unsafeIndexFor(Unsafe unsafe, TaggedArray array, long offset) {
        Class<?> clazz = array.getVal().getClass();
        long baseOffset = unsafe.arrayBaseOffset(clazz);
        long scale = unsafe.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        int index = (int) ((offset - baseOffset) / scale);
        return index;
    }

    public static void putObject(Unsafe unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if(obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj).set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putObject(obj, offset, val);
                }
            } else {
                unsafe.putObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    public static void putOrderedObject(Unsafe unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if(obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj).set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putOrderedObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putOrderedObject(obj, offset, val);
                }
            } else {
                unsafe.putOrderedObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    public static void putObjectVolatile(Unsafe unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if(obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj).set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putObjectVolatile(obj, offset, val);
                }
            } else {
                unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    public static Object getObject(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedReferenceArray) {
            //Push the taint from the `offset` argument to the `idx` argument for get
            return ((TaggedReferenceArray) obj).get(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), stackFrame.getArgTaint(1), stackFrame);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getObject(obj, offset);
        }
    }

    public static Object getObjectVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedReferenceArray) {
            //Push the taint from the `offset` argument to the `idx` argument for get
            return ((TaggedReferenceArray) obj).get(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), stackFrame.getArgTaint(1), stackFrame);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getObjectVolatile(obj, offset);
        }
    }

    public static void putByte(Unsafe unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putByte(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putByteVolatile(Unsafe unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putByteVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static byte getByte(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByte(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getByte(obj, offset);
        }
    }

    public static byte getByteVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByteVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getByteVolatile(obj, offset);
        }
    }

    public static void putBoolean(Unsafe unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putBoolean(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putBooleanVolatile(Unsafe unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putBooleanVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static boolean getBoolean(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBoolean(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getBoolean(obj, offset);
        }
    }

    public static boolean getBooleanVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBooleanVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getBooleanVolatile(obj, offset);
        }
    }

    public static void putChar(Unsafe unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putChar(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putCharVolatile(Unsafe unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putCharVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static char getChar(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getChar(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getChar(obj, offset);
        }
    }

    public static char getCharVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getCharVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getCharVolatile(obj, offset);
        }
    }

    public static void putFloat(Unsafe unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putFloat(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putFloatVolatile(Unsafe unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);

        if(obj instanceof TaggedArray) {
            unsafe.putFloatVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static float getFloat(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloat(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getFloat(obj, offset);
        }
    }

    public static float getFloatVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloatVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getFloatVolatile(obj, offset);
        }
    }

    public static void putOrderedInt(Unsafe unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putOrderedInt(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putOrderedInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.ORDERED);
        }
    }

    public static void putInt(Unsafe unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putInt(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putIntVolatile(Unsafe unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putIntVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static int getInt(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getInt(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getInt(obj, offset);
        }
    }

    public static int getIntVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getIntVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getIntVolatile(obj, offset);
        }
    }

    public static void putDouble(Unsafe unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putDouble(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putDoubleVolatile(Unsafe unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putDoubleVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static double getDouble(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDouble(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getDouble(obj, offset);
        }
    }

    public static double getDoubleVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDoubleVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getDoubleVolatile(obj, offset);
        }
    }

    public static void putShort(Unsafe unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putShort(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putShortVolatile(Unsafe unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putShortVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static short getShort(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShort(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getShort(obj, offset);
        }
    }

    public static short getShortVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShortVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getShortVolatile(obj, offset);
        }
    }

    public static void putLong(Unsafe unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putLong(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putOrderedLong(Unsafe unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putOrderedLong(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putOrderedLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.ORDERED);
        }
    }

    public static void putLongVolatile(Unsafe unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if(obj instanceof TaggedArray) {
            unsafe.putLongVolatile(((TaggedArray) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static long getLong(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLong(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getLong(obj, offset);
        }
    }

    /* for static fields, obj is a class. for instance fields of a class object, obj is also a class. if we want static fields, we need
     * offsets from *this* class's declared fields. for instance fields, we */
    public static long getLongVolatile(Unsafe unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if(obj instanceof TaggedArray) {
            stackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLongVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getLongVolatile(obj, offset);
        }
    }

    private enum SpecialAccessPolicy {
        VOLATILE,
        ORDERED,
        NONE
    }

}
