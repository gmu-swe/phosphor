package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/* Ensures that calls methods in Unsafe that set or retrieve the value of a field of a Java heap object set and
 * retrieve both the original field and its associated taint field if it has one. */
public class RuntimeUnsafePropagator {

    private RuntimeUnsafePropagator() {
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
                        Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                        if(taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                            tagOffset = (isStatic ? unsafe.staticFieldOffset(taintField) : unsafe.objectFieldOffset(taintField));
                        }
                    } catch(Exception e) {
                        //
                    }
                    if(fieldClazz.isArray()) {
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if(taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
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
                if(cl.$$PHOSPHOR_OFFSET_CACHE == null) {
                    cl.$$PHOSPHOR_OFFSET_CACHE = getOffsetPairs(unsafe, cl);
                }
                for(OffsetPair pair : cl.$$PHOSPHOR_OFFSET_CACHE) {
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
    private static void getTag(Unsafe unsafe, Object obj, long originalOffset, TaintedPrimitiveWithObjTag prealloc, SpecialAccessPolicy policy) {
        prealloc.taint = Taint.emptyTaint();
        OffsetPair pair = getOffsetPair(unsafe, obj, originalOffset);
        if(pair != null && pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
            Object result = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, pair.tagFieldOffset) : unsafe.getObject(obj, pair.tagFieldOffset);
            if(result instanceof Taint) {
                prealloc.taint = (Taint) result;
            }
        }
    }

    /* If prealloc is a wrapped primitive type, sets it's value to be the primitive value that is at the specified offset
     * for the specified object and return it. Otherwise returns the object at the specified offset for the specified
     * object and returns it. */
    private static Object getValue(Unsafe unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if(prealloc instanceof TaintedByteWithObjTag) {
            byte val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getByteVolatile(obj, offset);
            } else {
                val = unsafe.getByte(obj, offset);
            }
            ((TaintedByteWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedBooleanWithObjTag) {
            boolean val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getBooleanVolatile(obj, offset);
            } else {
                val = unsafe.getBoolean(obj, offset);
            }
            ((TaintedBooleanWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedCharWithObjTag) {
            char val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getCharVolatile(obj, offset);
            } else {
                val = unsafe.getChar(obj, offset);
            }
            ((TaintedCharWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedDoubleWithObjTag) {
            double val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getDoubleVolatile(obj, offset);
            } else {
                val = unsafe.getDouble(obj, offset);
            }
            ((TaintedDoubleWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedFloatWithObjTag) {
            float val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getFloatVolatile(obj, offset);
            } else {
                val = unsafe.getFloat(obj, offset);
            }
            ((TaintedFloatWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedIntWithObjTag) {
            int val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getIntVolatile(obj, offset);
            } else {
                val = unsafe.getInt(obj, offset);
            }
            ((TaintedIntWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedLongWithObjTag) {
            long val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getLongVolatile(obj, offset);
            } else {
                val = unsafe.getLong(obj, offset);
            }
            ((TaintedLongWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedShortWithObjTag) {
            short val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getShortVolatile(obj, offset);
            } else {
                val = unsafe.getShort(obj, offset);
            }
            ((TaintedShortWithObjTag) prealloc).val = val;
            return prealloc;
        } else if(prealloc instanceof TaintedReferenceWithObjTag) {
            Object val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getObjectVolatile(obj, offset);
            } else {
                val = unsafe.getObject(obj, offset);
            }
            ((TaintedReferenceWithObjTag) prealloc).val = val;
            return MultiDTaintedArray.boxOnly1D(prealloc);
            // return prealloc;
        } else {
            prealloc = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, offset) : unsafe.getObject(obj, offset);
            return MultiDTaintedArray.boxOnly1D(prealloc);
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

    /* If the specified Object value is a wrapped type, puts it's val the field at the specified offset in the other
     * specified object. Otherwise, puts the specified Object value into the field at the specified offset in the other
     * specified object. */
    private static void putValue(Unsafe unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if(value instanceof TaintedByteWithObjTag) {
            byte val = ((TaintedByteWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putByteVolatile(obj, offset, val);
            } else {
                unsafe.putByte(obj, offset, val);
            }
        } else if(value instanceof TaintedBooleanWithObjTag) {
            boolean val = ((TaintedBooleanWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putBooleanVolatile(obj, offset, val);
            } else {
                unsafe.putBoolean(obj, offset, val);
            }
        } else if(value instanceof TaintedCharWithObjTag) {
            char val = ((TaintedCharWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putCharVolatile(obj, offset, val);
            } else {
                unsafe.putChar(obj, offset, val);
            }
        } else if(value instanceof TaintedDoubleWithObjTag) {
            double val = ((TaintedDoubleWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putDoubleVolatile(obj, offset, val);
            } else {
                unsafe.putDouble(obj, offset, val);
            }
        } else if(value instanceof TaintedFloatWithObjTag) {
            float val = ((TaintedFloatWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putFloatVolatile(obj, offset, val);
            } else {
                unsafe.putFloat(obj, offset, val);
            }
        } else if(value instanceof TaintedIntWithObjTag) {
            int val = ((TaintedIntWithObjTag) value).val;
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedInt(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putIntVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putInt(obj, offset, val);
            }
        } else if(value instanceof TaintedLongWithObjTag) {
            long val = ((TaintedLongWithObjTag) value).val;
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedLong(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putLongVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putLong(obj, offset, val);
            }
        } else if(value instanceof TaintedShortWithObjTag) {
            short val = ((TaintedShortWithObjTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putShortVolatile(obj, offset, val);
            } else {
                unsafe.putShort(obj, offset, val);
            }
        } else if(value instanceof LazyArrayObjTags) {
            Object val = ((LazyArrayObjTags) value).getVal();
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedObject(obj, offset, val);
                    break;
                case VOLATILE:
                    unsafe.putObjectVolatile(obj, offset, val);
                    break;
                default:
                    unsafe.putObject(obj, offset, val);
            }
        } else {
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedObject(obj, offset, value);
                    break;
                case VOLATILE:
                    unsafe.putObjectVolatile(obj, offset, value);
                    break;
                default:
                    unsafe.putObject(obj, offset, value);
            }
        }
    }

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets an element and tag
     * in the specified LazyArrayObjTags. returns whether the TaintedPrimitiveWithObjTag and LazyArrayObjTags's component
     * type's match. */
    private static boolean putArrayElement(Unsafe unsafe, LazyArrayObjTags tags, long offset, TaintedPrimitiveWithObjTag value) {
        if(tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if(tags instanceof LazyBooleanArrayObjTags && value instanceof TaintedBooleanWithObjTag) {
                ((LazyBooleanArrayObjTags) tags).set(null, index, null, ((TaintedBooleanWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyByteArrayObjTags && value instanceof TaintedByteWithObjTag) {
                ((LazyByteArrayObjTags) tags).set(null, index, null, ((TaintedByteWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyCharArrayObjTags && value instanceof TaintedCharWithObjTag) {
                ((LazyCharArrayObjTags) tags).set(null, index, null, ((TaintedCharWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyDoubleArrayObjTags && value instanceof TaintedDoubleWithObjTag) {
                ((LazyDoubleArrayObjTags) tags).set(null, index, null, ((TaintedDoubleWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyFloatArrayObjTags && value instanceof TaintedFloatWithObjTag) {
                ((LazyFloatArrayObjTags) tags).set(null, index, null, ((TaintedFloatWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyIntArrayObjTags && value instanceof TaintedIntWithObjTag) {
                ((LazyIntArrayObjTags) tags).set(null, index, null, ((TaintedIntWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyLongArrayObjTags && value instanceof TaintedLongWithObjTag) {
                ((LazyLongArrayObjTags) tags).set(null, index, null, ((TaintedLongWithObjTag) value).val, value.taint);
            } else if(tags instanceof LazyShortArrayObjTags && value instanceof TaintedShortWithObjTag) {
                ((LazyShortArrayObjTags) tags).set(null, index, null, ((TaintedShortWithObjTag) value).val, value.taint);
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets a tag
     * in the specified LazyArrayObjTags at a calculated index.
     * type's match. */
    private static void swapArrayElementTag(Unsafe unsafe, LazyArrayObjTags tags, long offset, Taint valueTaint) {
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

    public static void copyMemory$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object src, Taint srcTaint, long srcAddress, Taint srcAddressTaint, Object dest, Taint destTaint, long destAddress, Taint destAddressTaint, long length, Taint lengthTaint) {
        if(src instanceof LazyArrayObjTags) {
            src = ((LazyArrayObjTags) src).getVal();
        }
        if(dest instanceof LazyArrayObjTags) {
            dest = ((LazyArrayObjTags) dest).getVal();
        }
        unsafe.copyMemory(src, srcAddress, dest, destAddress, length);
    }

    public static void copyMemory$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, long srcAddress, Taint srcAddressTaint, long destAddress, Taint destAddressTaint, long length, Taint lengthTaint) {
        unsafe.copyMemory(srcAddress, destAddress, length);
    }

    public static TaintedBooleanWithObjTag compareAndSwapObject$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, Object expected, Taint expectedTaint, Object value, Taint valueTaint, TaintedBooleanWithObjTag ret) {
        ret.taint = Taint.emptyTaint();
        if(obj instanceof LazyReferenceArrayObjTags) {
            ret.val = unsafe.compareAndSwapObject(((LazyReferenceArrayObjTags) obj).val, offset, expected, value);
            if(ret.val) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            OffsetPair pair = null;
            boolean didCAS = false;
            if(value instanceof LazyArrayObjTags || expected instanceof LazyArrayObjTags) {
                //Need to be careful - maybe we are hitting a 1D primitive array field
                if(obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    //We are doing a CAS on a 1d primitive array field
                    ret.val = unsafe.compareAndSwapObject(obj, offset, MultiDTaintedArray.unbox1DOrNull(expected), MultiDTaintedArray.unbox1DOrNull(value));
                    didCAS = true;
                }
            }
            if(!didCAS) {
                //Either this is not a wrapped array, or we are storing it to the place where it should be stored without unwrapping
                ret.val = unsafe.compareAndSwapObject(obj, offset, expected, value);
                if(pair == null && obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
            }

            if(pair != null && ret.val) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, value);
                }
            }
        }
        return ret;
    }

    public static TaintedBooleanWithObjTag compareAndSwapInt$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, int expected, Taint expectedTaint, int value, Taint valueTaint, TaintedBooleanWithObjTag ret) {
        ret.taint = Taint.emptyTaint();
        if(obj instanceof LazyIntArrayObjTags) {
            ret.val = unsafe.compareAndSwapInt(((LazyIntArrayObjTags) obj).val, offset, expected, value);
            if(ret.val) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            ret.val = unsafe.compareAndSwapInt(obj, offset, expected, value);
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null && ret.val) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    public static TaintedBooleanWithObjTag compareAndSwapLong$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, long expected, Taint expectedTaint, long value, Taint valueTaint, TaintedBooleanWithObjTag ret) {
        ret.taint = Taint.emptyTaint();
        if(obj instanceof LazyLongArrayObjTags) {
            ret.val = unsafe.compareAndSwapLong(((LazyLongArrayObjTags) obj).val, offset, expected, value);
            if(ret.val) {
                swapArrayElementTag(unsafe, (LazyArrayObjTags) obj, offset, valueTaint);
            }
        } else {
            ret.val = unsafe.compareAndSwapLong(obj, offset, expected, value);
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null && ret.val) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, Object val, Taint valTaint) {
        if(obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(((LazyReferenceArrayObjTags) obj).unsafeIndexFor(unsafe, offset), val, valTaint);
        } else {
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.tagFieldOffset, valTaint);
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
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

    public static void putOrderedObject$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, Object val, Taint valTaint) {
        if(obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(((LazyReferenceArrayObjTags) obj).unsafeIndexFor(unsafe, offset), val, valTaint);
        } else {
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.tagFieldOffset, valTaint);
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putOrderedObject(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
                } else {
                    unsafe.putOrderedObject(obj, offset, val);
                }
            } else {
                unsafe.putOrderedObject(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            }
        }
    }

    public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, Object val, Taint valTaint) {
        if(obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).set(((LazyReferenceArrayObjTags) obj).unsafeIndexFor(unsafe, offset), val, valTaint);
        } else {
            unsafe.putObjectVolatile(obj, offset, MultiDTaintedArray.unbox1DOrNull(val));
            OffsetPair pair = null;
            if(obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if(pair != null) {
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, valTaint);
                }
                if(pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
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

    public static TaintedReferenceWithObjTag getObject$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedReferenceWithObjTag ret, Object e) {
        if(obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).get(((LazyReferenceArrayObjTags) obj).unsafeIndexFor(unsafe, offset), ret);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            ret.val = unsafe.getObject(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedReferenceWithObjTag getObjectVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedReferenceWithObjTag ret, Object e) {
        if(obj instanceof LazyReferenceArrayObjTags) {
            ((LazyReferenceArrayObjTags) obj).get(((LazyReferenceArrayObjTags) obj).unsafeIndexFor(unsafe, offset), ret);
        } else {
            //Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair != null && pair.wrappedFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            ret.val = unsafe.getObjectVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, byte val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putByte(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putByteVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, byte val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putByteVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedByteWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getByte(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getByte(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedByteWithObjTag getByteVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedByteWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getByteVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getByteVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, boolean val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putBoolean(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putBooleanVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, boolean val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putBooleanVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedBooleanWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getBoolean(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getBoolean(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedBooleanWithObjTag getBooleanVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedBooleanWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getBooleanVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getBooleanVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, char val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putChar(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putCharVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, char val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putCharVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedCharWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getChar(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getChar(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedCharWithObjTag getCharVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedCharWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getCharVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getCharVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, float val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putFloat(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putFloatVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, float val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putFloatVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedFloatWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getFloat(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getFloat(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedFloatWithObjTag getFloatVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedFloatWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getFloatVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getFloatVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putOrderedInt$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, int val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putOrderedInt(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putOrderedInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.ORDERED);
        }
    }

    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, int val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putInt(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putIntVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, int val, Taint valTaint) {

        if(obj instanceof LazyArrayObjTags) {
            unsafe.putIntVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedIntWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getInt(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getInt(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedIntWithObjTag getIntVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedIntWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getIntVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getIntVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, double val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putDouble(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putDoubleVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, double val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putDoubleVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedDoubleWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getDouble(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getDouble(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedDoubleWithObjTag getDoubleVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedDoubleWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getDoubleVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getDoubleVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, short val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putShort(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putShortVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, short val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putShortVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedShortWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getShort(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getShort(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    public static TaintedShortWithObjTag getShortVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedShortWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getShortVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getShortVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
    }

    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, long val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putLong(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static void putOrderedLong$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, long val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putOrderedLong(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putOrderedLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.ORDERED);
        }
    }

    public static void putLongVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, long val, Taint valTaint) {
        if(obj instanceof LazyArrayObjTags) {
            unsafe.putLongVolatile(((LazyArrayObjTags) obj).getVal(), offset, val);
            if((valTaint != null && !valTaint.isEmpty()) || ((LazyArrayObjTags) obj).taints != null) {
                ((LazyArrayObjTags) obj).setTaint(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedLongWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getLong(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getLong(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.NONE);
        }
        return ret;
    }

    /* for static fields, obj is a class. for instance fields of a class object, obj is also a class. if we want static fields, we need
     * offsets from *this* class's declared fields. for instance fields, we */
    public static TaintedLongWithObjTag getLongVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Taint unsafeTaint, Object obj, Taint objTaint, long offset, Taint offsetTaint, TaintedLongWithObjTag ret) {
        if(obj instanceof LazyArrayObjTags) {
            ret.val = unsafe.getLongVolatile(((LazyArrayObjTags) obj).getVal(), offset);
            ret.taint = ((LazyArrayObjTags) obj).getTaintOrEmpty(((LazyArrayObjTags) obj).unsafeIndexFor(unsafe, offset));
        } else {
            ret.val = unsafe.getLongVolatile(obj, offset);
            getTag(unsafe, obj, offset, ret, SpecialAccessPolicy.VOLATILE);
        }
        return ret;
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
}
