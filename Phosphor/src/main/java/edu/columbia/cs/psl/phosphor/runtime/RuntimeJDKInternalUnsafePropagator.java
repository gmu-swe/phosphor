package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.UnsafeProxy;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/* Ensures that calls methods in UnsafeProxy that set or retrieve the value of a field of a Java heap object set and
 * retrieve both the original field and its associated taint field if it has one. */
public class RuntimeJDKInternalUnsafePropagator {

    private RuntimeJDKInternalUnsafePropagator() {
        // Prevents this class from being instantiated
    }

    /* Used to disambiguate between a static field of a given type and an instance field of java.lang.Class */
    static long LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = UnsafeProxy.INVALID_FIELD_OFFSET;

    /* Stores pairs containing the offset of an original, non-static primitive or primitive array field for the specified
     * class and the offset of the tag field associated with that original field. */
    private static SinglyLinkedList<OffsetPair> getOffsetPairs(UnsafeProxy unsafe, Class<?> targetClazz) {
        SinglyLinkedList<OffsetPair> list = new SinglyLinkedList<>();
        for (Class<?> clazz = targetClazz; clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    Class<?> fieldClazz = field.getType();
                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    long fieldOffset = (isStatic ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field));
                    long tagOffset = UnsafeProxy.INVALID_FIELD_OFFSET;
                    long wrapperOffset = UnsafeProxy.INVALID_FIELD_OFFSET;
                    if (fieldClazz != Taint.class) {
                        try {
                            // Yes, we need to use our own string builder here...
                            // because java 16 will compile a lambda in here otherwise, and we can't trigger
                            // that class loading before Properties finishes getting initialized, but Properties' clinit
                            // will get us here first :'( :'(
                            StringBuilder taintFieldName = new StringBuilder(field.getName());
                            taintFieldName.append(TaintUtils.TAINT_FIELD);
                            Field taintField = clazz.getField(taintFieldName.toString());
                            if (taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = (isStatic ? unsafe.staticFieldOffset(taintField) : unsafe.objectFieldOffset(taintField));
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (fieldClazz.isArray()) {
                        try {
                            StringBuilder taintFieldName = new StringBuilder(field.getName());
                            taintFieldName.append(TaintUtils.TAINT_WRAPPER_FIELD);
                            Field taintField = clazz.getField(taintFieldName.toString());
                            Class<?> taintClazz = taintField.getType();
                            if (taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
                                wrapperOffset = (isStatic ? unsafe.staticFieldOffset(taintField) : unsafe.objectFieldOffset(taintField));
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    list.enqueue(new OffsetPair(isStatic, fieldOffset, wrapperOffset, tagOffset));
                } catch (Exception e) {
                    //
                }
            }
        }
        return list;
    }

    /* returns an offset pair for the specified object's class where either the original field offset or the tag field
     * offset matches the specified offset or null if such an offset pair could not be found. */
    public static OffsetPair getOffsetPair(UnsafeProxy unsafe, Object o, long offset) {
        try {
            Class<?> cl = null;
            boolean isStatic = false;
            if (o instanceof Class) {
                /* We MIGHT be accessing a static field of this class, in which case we should take
                   the offset from *this* class instance (o). But, we might also be accessing an instance
                   field of the type Class, in which case we want to use the classes's class.
                 */
                if (LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS == UnsafeProxy.INVALID_FIELD_OFFSET) {
                    findLastInstanceFieldOnJavaLangClass(unsafe);
                }
                if (offset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    /* We are not accessing an instance field of java.lang.Class, hence, we must be accessing
                     * a static field of type (Class) o */
                    cl = (Class) o;
                    isStatic = true;
                }
                /* Otherwise, we are accessing an instance field of java.lang.Class */
            }
            if (cl == null && o != null && o.getClass() != null) {
                cl = o.getClass();
            }
            if (cl != null) {
                if (InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl) == null) {
                    InstrumentedJREFieldHelper.set$$PHOSPHOR_OFFSET_CACHE(cl, getOffsetPairs(unsafe, cl));
                }
                for (OffsetPair pair : InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl)) {
                    if (pair.origFieldOffset == offset && pair.isStatic == isStatic) {
                        return pair;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void findLastInstanceFieldOnJavaLangClass(UnsafeProxy unsafe) {
        for (Field field : Class.class.getDeclaredFields()) {
            try {
                Class<?> fieldClazz = field.getType();
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if (isStatic) {
                    continue;
                }
                long fieldOffset = unsafe.objectFieldOffset(field);
                if (fieldOffset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = fieldOffset;
                }
            } catch (Exception e) {
                //
            }
        }
    }

    /* If prealloc is a wrapped primitive type, set it's taint to be the value of the field at the specified offset in the
     * other specified object. Otherwise returns the value of the field at the specified offset in the specified object. */
    private static void getTag(UnsafeProxy unsafe, Object obj, long originalOffset, PhosphorStackFrame phosphorStackFrame, SpecialAccessPolicy policy) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        OffsetPair pair = getOffsetPair(unsafe, obj, originalOffset);
        if (pair != null && pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
            Object result = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getReferenceVolatile(obj, pair.tagFieldOffset) : unsafe.getReference(obj, pair.tagFieldOffset);
            if (result instanceof Taint) {
                phosphorStackFrame.returnTaint = (Taint) result;
            }
        }
    }

    /* If prealloc is a wrapped primitive type, sets it's value to be the primitive value that is at the specified offset
     * for the specified object and return it. Otherwise returns the object at the specified offset for the specified
     * object and returns it. */
    private static Object getValue(UnsafeProxy unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if (prealloc instanceof TaintedByteWithObjTag) {
            byte val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getByteVolatile(obj, offset);
            } else {
                val = unsafe.getByte(obj, offset);
            }
            ((TaintedByteWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedBooleanWithObjTag) {
            boolean val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getBooleanVolatile(obj, offset);
            } else {
                val = unsafe.getBoolean(obj, offset);
            }
            ((TaintedBooleanWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedCharWithObjTag) {
            char val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getCharVolatile(obj, offset);
            } else {
                val = unsafe.getChar(obj, offset);
            }
            ((TaintedCharWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedDoubleWithObjTag) {
            double val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getDoubleVolatile(obj, offset);
            } else {
                val = unsafe.getDouble(obj, offset);
            }
            ((TaintedDoubleWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedFloatWithObjTag) {
            float val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getFloatVolatile(obj, offset);
            } else {
                val = unsafe.getFloat(obj, offset);
            }
            ((TaintedFloatWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedIntWithObjTag) {
            int val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getIntVolatile(obj, offset);
            } else {
                val = unsafe.getInt(obj, offset);
            }
            ((TaintedIntWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedLongWithObjTag) {
            long val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getLongVolatile(obj, offset);
            } else {
                val = unsafe.getLong(obj, offset);
            }
            ((TaintedLongWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedShortWithObjTag) {
            short val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getShortVolatile(obj, offset);
            } else {
                val = unsafe.getShort(obj, offset);
            }
            ((TaintedShortWithObjTag) prealloc).val = val;
            return prealloc;
        } else if (prealloc instanceof TaintedReferenceWithObjTag) {
            Object val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getReferenceVolatile(obj, offset);
            } else {
                val = unsafe.getReference(obj, offset);
            }
            ((TaintedReferenceWithObjTag) prealloc).val = val;
            return MultiDTaintedArray.boxOnly1D(prealloc);
            // return prealloc;
        } else {
            prealloc = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getReferenceVolatile(obj, offset) : unsafe.getReference(obj, offset);
            return MultiDTaintedArray.boxOnly1D(prealloc);
        }
    }

    /* If the specified Object value is a wrapped primitive type, puts it's taint into the field at the specified offset in the
     * other specified object. Otherwise if the specified Object value is null or a lazy array wrapper put the specified Object
     * value into the field at the specified offset in the other specified object. */
    private static void putTag(UnsafeProxy unsafe, Object obj, long offset, Taint tag, SpecialAccessPolicy policy) {
        OffsetPair pair = null;
        if (obj != null) {
            pair = getOffsetPair(unsafe, obj, offset);
        }
        if (pair != null) {
            switch (policy) {
                case ORDERED:
                    unsafe.putReferenceRelease(obj, pair.tagFieldOffset, tag);
                    break;
                case VOLATILE:
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, tag);
                    break;
                default:
                    unsafe.putReference(obj, pair.tagFieldOffset, tag);
            }
        }
    }

    /* If the specified Object value is a wrapped type, puts it's val the field at the specified offset in the other
     * specified object. Otherwise, puts the specified Object value into the field at the specified offset in the other
     * specified object. */
    private static void putValue(UnsafeProxy unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if (value instanceof TaintedByteWithObjTag) {
            byte val = ((TaintedByteWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putByteVolatile(obj, offset, val);
            } else {
                unsafe.putByte(obj, offset, val);
            }
        } else if (value instanceof TaintedBooleanWithObjTag) {
            boolean val = ((TaintedBooleanWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putBooleanVolatile(obj, offset, val);
            } else {
                unsafe.putBoolean(obj, offset, val);
            }
        } else if (value instanceof TaintedCharWithObjTag) {
            char val = ((TaintedCharWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putCharVolatile(obj, offset, val);
            } else {
                unsafe.putChar(obj, offset, val);
            }
        } else if (value instanceof TaintedDoubleWithObjTag) {
            double val = ((TaintedDoubleWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putDoubleVolatile(obj, offset, val);
            } else {
                unsafe.putDouble(obj, offset, val);
            }
        } else if (value instanceof TaintedFloatWithObjTag) {
            float val = ((TaintedFloatWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putFloatVolatile(obj, offset, val);
            } else {
                unsafe.putFloat(obj, offset, val);
            }
        } else if (value instanceof TaintedIntWithObjTag) {
            int val = ((TaintedIntWithObjTag) value).val;
            switch (policy) {
                case ORDERED:
                    unsafe.putIntRelease(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putIntVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putInt(obj, offset, val);
            }
        } else if (value instanceof TaintedLongWithObjTag) {
            long val = ((TaintedLongWithObjTag) value).val;
            switch (policy) {
                case ORDERED:
                    unsafe.putLongRelease(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putLongVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putLong(obj, offset, val);
            }
        } else if (value instanceof TaintedShortWithObjTag) {
            short val = ((TaintedShortWithObjTag) value).val;
            if (policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putShortVolatile(obj, offset, val);
            } else {
                unsafe.putShort(obj, offset, val);
            }
        } else if (value instanceof LazyArrayObjTags) {
            Object val = ((LazyArrayObjTags) value).getVal();
            switch (policy) {
                case ORDERED:
                    unsafe.putReferenceRelease(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putReferenceVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putReference(obj, offset, val);
            }
        } else {
            switch (policy) {
                case ORDERED:
                    unsafe.putReferenceRelease(obj, offset, value);
                    break;
                case VOLATILE:
                    unsafe.putReferenceVolatile(obj, offset, value);
                    break;
                default:
                    unsafe.putReference(obj, offset, value);
            }
        }
    }

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets a tag
     * in the specified LazyArrayObjTags at a calculated index.
     * type's match. */
    private static void swapArrayElementTag(UnsafeProxy unsafe, LazyArrayObjTags tags, long offset, Taint valueTaint) {
        if (tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if (tags.taints == null && valueTaint != null && !valueTaint.isEmpty()) {
                tags.taints = new Taint[tags.getLength()];
            }
            if (tags.taints != null) {
                tags.taints[index] = valueTaint;
            }
        }
    }

    public static void copyMemory(UnsafeProxy unsafe, Object src, long srcAddress, Object dest, long destAddress, long length, PhosphorStackFrame phosphorStackFrame) {
        //Object srcWrapper = phosphorStackFrame.wrappedArgs[1];
        //if(srcWrapper != null){
        //    src = srcWrapper;
        //}
        //Object destWrapper = phosphorStackFrame.wrappedArgs[3];
        //if(destWrapper != null){
        //    dest = destWrapper;
        //}
        if (src instanceof LazyArrayObjTags) {
            src = ((LazyArrayObjTags) src).getVal();
        }
        if (dest instanceof LazyArrayObjTags) {
            dest = ((LazyArrayObjTags) dest).getVal();
        }
        unsafe.copyMemory(src, srcAddress, dest, destAddress, length);
    }

    public static void copyMemory(UnsafeProxy unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame phosphorStackFrame) {
        unsafe.copyMemory(srcAddress, destAddress, length);
    }

    public static boolean compareAndSetReference(UnsafeProxy unsafe, Object obj, long offset, Object expected, Object value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof LazyReferenceArrayObjTags) {
            ret = unsafe.compareAndSetReference(((LazyReferenceArrayObjTags) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            OffsetPair pair = null;
            boolean didCAS = false;
            if (value instanceof LazyArrayObjTags || expected instanceof LazyArrayObjTags) {
                //Need to be careful - maybe we are hitting a 1D primitive array field
                if (obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    //We are doing a CAS on a 1d primitive array field
                    ret = unsafe.compareAndSetReference(obj, offset, MultiDTaintedArray.unbox1DOrNull(expected), MultiDTaintedArray.unbox1DOrNull(value));
                    didCAS = true;
                }
            }
            if (!didCAS) {
                //Either this is not a wrapped array, or we are storing it to the place where it should be stored without unwrapping
                ret = unsafe.compareAndSetReference(obj, offset, expected, value);
                if (pair == null && obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
            }

            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.wrappedFieldOffset, value);
                }
            }
        }
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static boolean compareAndSetInt(UnsafeProxy unsafe, Object obj, long offset, int expected, int value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof LazyIntArrayObjTags) {
            ret = unsafe.compareAndSetInt(((LazyIntArrayObjTags) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndSetInt(obj, offset, expected, value);
            boolean success = false;
            if (ret) {
                success = true;
            }
            //TODO fix? or delete?

            // was finding that we might have triggered bad class loading with getOffsetPair
            //OffsetPair pair = null;
            //if(obj != null) {
            //    pair = getOffsetPair(unsafe, obj, offset);
            //}
            //if(pair != null && ret.val) {
            //    if(pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
            //        unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
            //    }
            //}
        }
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static void putObject(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObject(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putObject(obj, offset, val);
                }
            } else {
                unsafe.putObject(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }


    public static void putObjectVolatile(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            unsafe.putObjectVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObjectVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putObjectVolatile(obj, offset, val);
                }
            } else {
                unsafe.putObjectVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }

    public static Object getObject(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            //Push the taint from the `offset` argument to the `idx` argument for get
            return ((LazyReferenceArrayObjTags) obj).get(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), stackFrame.getArgTaint(1), stackFrame);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getObject(obj, offset);
        }
    }

    public static Object getObjectVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            //Push the taint from the `offset` argument to the `idx` argument for get
            return ((LazyReferenceArrayObjTags) obj).get(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), stackFrame.getArgTaint(1), stackFrame);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getObjectVolatile(obj, offset);
        }
    }

    public static void putByte(UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putByte(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putByteVolatile(UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putByteVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static byte getByte(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getByte(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getByte(obj, offset);
        }
    }

    public static byte getByteVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getByteVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getByteVolatile(obj, offset);
        }
    }

    public static void putBoolean(UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putBoolean(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putBooleanVolatile(UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putBooleanVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static boolean getBoolean(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getBoolean(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getBoolean(obj, offset);
        }
    }

    public static boolean getBooleanVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getBooleanVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getBooleanVolatile(obj, offset);
        }
    }

    public static void putChar(UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putChar(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putCharVolatile(UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putCharVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static char getChar(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getChar(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getChar(obj, offset);
        }
    }

    public static char getCharVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getCharVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getCharVolatile(obj, offset);
        }
    }

    public static void putFloat(UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putFloat(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putFloatVolatile(UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);

        if (obj instanceof LazyArrayObjTags) {
            unsafe.putFloatVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static float getFloat(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getFloat(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getFloat(obj, offset);
        }
    }

    public static float getFloatVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getFloatVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getFloatVolatile(obj, offset);
        }
    }


    public static void putInt(UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putInt(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putIntVolatile(UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putIntVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static int getInt(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getInt(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getInt(obj, offset);
        }
    }

    public static int getIntVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getIntVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getIntVolatile(obj, offset);
        }
    }

    public static void putDouble(UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putDouble(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putDoubleVolatile(UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putDoubleVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static double getDouble(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getDouble(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getDouble(obj, offset);
        }
    }

    public static double getDoubleVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getDoubleVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getDoubleVolatile(obj, offset);
        }
    }

    public static void putShort(UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putShort(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putShortVolatile(UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putShortVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static short getShort(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getShort(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getShort(obj, offset);
        }
    }

    public static short getShortVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getShortVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getShortVolatile(obj, offset);
        }
    }

    public static void putLong(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putLong(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putLongVolatile(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putLongVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static long getLong(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getLong(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getLong(obj, offset);
        }
    }

    public static long getLongVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyArrayObjTags) {
            stackFrame.returnTaint = ((LazyArrayObjTags) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset));
            return unsafe.getLongVolatile(((LazyArrayObjTags) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getLongVolatile(obj, offset);
        }
    }

    public static boolean compareAndSetLong(UnsafeProxy unsafe, Object obj, long offset, long expected, long value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof LazyLongArrayObjTags) {
            ret = unsafe.compareAndSetLong(((LazyLongArrayObjTags) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndSetLong(obj, offset, expected, value);
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    public static void putReference(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), val, valTaint);
        } else {
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReference(obj, pair.tagFieldOffset, valTaint);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReference(obj, pair.wrappedFieldOffset, val);
                    unsafe.putReference(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putReference(obj, offset, val);
                }
            } else {
                unsafe.putReference(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }

    public static void putReferenceRelease(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), val, valTaint);
        } else {
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceRelease(obj, pair.tagFieldOffset, valTaint);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceRelease(obj, pair.wrappedFieldOffset, val);
                    unsafe.putReferenceRelease(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putReferenceRelease(obj, offset, val);
                }
            } else {
                unsafe.putReferenceRelease(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }

    public static void putReferenceVolatile(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof LazyReferenceArrayObjTags) {
            unsafe.putReferenceVolatile(((LazyReferenceArrayObjTags) obj).getVal(), offset, val);
        } else {
            unsafe.putReferenceVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valTaint);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.wrappedFieldOffset, val);
                    unsafe.putReferenceVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putReferenceVolatile(obj, offset, val);
                }
            } else {
                unsafe.putReferenceVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }

    public static Object getReference(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            int idx = unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset);
            Object ret = unsafe.getReference(((LazyReferenceArrayObjTags) obj).getVal(), offset);
            phosphorStackFrame.setReturnTaint(((LazyReferenceArrayObjTags) obj).getTaintOrEmpty(idx));
            return ret;
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, phosphorStackFrame, RuntimeJDKInternalUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getReference(obj, offset);
        }
    }

    public static Object getReferenceVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyReferenceArrayObjTags) {
            return unsafe.getReferenceVolatile(((LazyReferenceArrayObjTags) obj).getVal(), offset);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            Object ret = unsafe.getReferenceVolatile(obj, offset);
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return ret;
        }
    }

    public static Object getReferenceAcquire(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getReferenceVolatile(unsafe, obj, offset, phosphorStackFrame);
    }

    public static void putLongRelease(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof LazyArrayObjTags) {
            unsafe.putLongRelease(((LazyArrayObjTags) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(unsafeIndexFor(unsafe, (LazyArrayObjTags) obj, offset), valTaint);
            }
        } else {
            unsafe.putLongRelease(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.ORDERED);
        }
    }

    public static Object allocateUninitializedArray(UnsafeProxy unsafe, Class c, int len, PhosphorStackFrame phosphorStackFrame) {
        Object ret = unsafe.allocateUninitializedArray(c, len);
        if (ret instanceof LazyArrayObjTags) {
            return ret;
        }
        return MultiDTaintedArray.boxIfNecessary(ret);
    }

    private enum SpecialAccessPolicy {
        VOLATILE,
        ORDERED,
        NONE
    }

    public static class OffsetPair {

        public final long origFieldOffset;
        public final long wrappedFieldOffset;
        public final long tagFieldOffset;
        public final boolean isStatic;

        public OffsetPair(boolean isStatic, long origFieldOffset, long wrappedFieldOffset, long tagFieldOffset) {
            this.isStatic = isStatic;
            this.origFieldOffset = origFieldOffset;
            this.tagFieldOffset = tagFieldOffset;
            this.wrappedFieldOffset = wrappedFieldOffset;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OffsetPair && this.origFieldOffset == ((OffsetPair) other).origFieldOffset && this.isStatic == ((OffsetPair) other).isStatic;
        }

        @Override
        public int hashCode() {
            return (int) (origFieldOffset ^ (origFieldOffset >>> 32));
        }

        @Override
        public String toString() {
            return String.format("{field @ %d -> tag @ %d, wrapper @ %d}", origFieldOffset, tagFieldOffset, wrappedFieldOffset);
        }
    }

    private static int unsafeIndexFor(UnsafeProxy unsafe, LazyArrayObjTags array, long offset) {
        Class<?> clazz = array.getVal().getClass();
        long baseOffset = unsafe.arrayBaseOffset(clazz);
        long scale = unsafe.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        int index = (int) ((offset - baseOffset) / scale);
        return index;
    }
}
