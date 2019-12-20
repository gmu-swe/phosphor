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

    /* Stores pairs containing the offset of an original, non-static primitive or primitive array field for the specified
     * class and the offset of the tag field associated with that original field. */
    private static SinglyLinkedList<OffsetPair> getOffsetPairs(Unsafe unsafe, Class<?> targetClazz) {
        SinglyLinkedList<OffsetPair> list = new SinglyLinkedList<>();
        for(Class<?> clazz = targetClazz; clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            for(Field field : clazz.getDeclaredFields()) {
                try {
                    Class<?> fieldClazz = field.getType();
                    if(!Modifier.isStatic(fieldClazz.getModifiers()) && fieldClazz.isPrimitive()) {
                        // The field is a primitive
                        long fieldOffset = unsafe.objectFieldOffset(field);
                        // Find the associated taint field's offset
                        long tagOffset = Unsafe.INVALID_FIELD_OFFSET;
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                            if(taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            }
                        } catch(Exception e) {
                            //
                        }
                        list.enqueue(new OffsetPair(fieldOffset, tagOffset));
                    } else if(!Modifier.isStatic(fieldClazz.getModifiers()) && fieldClazz.isArray() && fieldClazz.getComponentType().isPrimitive()) {
                        // The fields is a 1D primitive array
                        long fieldOffset = unsafe.objectFieldOffset(field);
                        // Find the associated taint field's offset
                        long tagOffset = Unsafe.INVALID_FIELD_OFFSET;
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if(taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            }
                        } catch(Exception e) {
                            //
                        }
                        list.enqueue(new OffsetPair(fieldOffset, tagOffset));
                    }
                } catch(Exception e) {
                    //
                }
            }
        }
        return list;
    }

    /* Returns an offset pair for the specified object's class where either the original field offset or the tag field
     * offset matches the specified offset or null if such an offset pair could not be found. */
    public static OffsetPair getOffsetPair(Unsafe unsafe, Object o, long offset) {
        try {
            if(o != null && o.getClass() != null) {
                if(o.getClass().$$PHOSPHOR_OFFSET_CACHE == null) {
                    o.getClass().$$PHOSPHOR_OFFSET_CACHE = getOffsetPairs(unsafe, o.getClass());
                }
                for(OffsetPair pair : o.getClass().$$PHOSPHOR_OFFSET_CACHE) {
                    if(pair.origFieldOffset == offset) {
                        return pair;
                    }
                }
            }
            return null;
        } catch(Exception e) {
            return null;
        }
    }

    /* If prealloc is a wrapped primitive type, set it's taint to be the value of the field at the specified offset in the
     * other specified object. Otherwise returns the value of the field at the specified offset in the specified object. */
    private static Object getTag(Unsafe unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if(prealloc instanceof TaintedPrimitiveWithObjTag) {
            Object result = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, offset) : unsafe.getObject(obj, offset);
            if(result instanceof Taint) {
                ((TaintedPrimitiveWithObjTag) prealloc).taint = (Taint) result;
            }
            return prealloc;
        } else {
            return (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, offset) : unsafe.getObject(obj, offset);
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
        } else {
            prealloc = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, offset) : unsafe.getObject(obj, offset);
            return MultiDTaintedArray.boxOnly1D(prealloc);
        }
    }

    /* If the specified Object value is a wrapped primitive type, puts it's taint into the field at the specified offset in the
     * other specified object. Otherwise if the specified Object value is null or a lazy array wrapper put the specified Object
     * value into the field at the specified offset in the other specified object. */
    private static void putTag(Unsafe unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if(value instanceof TaintedPrimitiveWithObjTag || value instanceof LazyArrayObjTags || value == null) {
            Object tag = (value instanceof TaintedPrimitiveWithObjTag) ? ((TaintedPrimitiveWithObjTag) value).taint : value;
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedObject(obj, offset, tag);
                    break;
                case VOLATILE:
                    unsafe.putObjectVolatile(obj, offset, tag);
                    break;
                default:
                    unsafe.putObject(obj, offset, tag);
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
     * in the specified LazyArrayObjTags. Returns whether the TaintedPrimitiveWithObjTag and LazyArrayObjTags's component
     * type's match. */
    private static boolean putArrayElement(Unsafe unsafe, LazyArrayObjTags tags, long offset, TaintedPrimitiveWithObjTag value) {
        if(tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if(tags instanceof LazyBooleanArrayObjTags && value instanceof TaintedBooleanWithObjTag) {
                ((LazyBooleanArrayObjTags) tags).set(null, index, value.taint, ((TaintedBooleanWithObjTag) value).val);
            } else if(tags instanceof LazyByteArrayObjTags && value instanceof TaintedByteWithObjTag) {
                ((LazyByteArrayObjTags) tags).set(null, index, value.taint, ((TaintedByteWithObjTag) value).val);
            } else if(tags instanceof LazyCharArrayObjTags && value instanceof TaintedCharWithObjTag) {
                ((LazyCharArrayObjTags) tags).set(null, index, value.taint, ((TaintedCharWithObjTag) value).val);
            } else if(tags instanceof LazyDoubleArrayObjTags && value instanceof TaintedDoubleWithObjTag) {
                ((LazyDoubleArrayObjTags) tags).set(null, index, value.taint, ((TaintedDoubleWithObjTag) value).val);
            } else if(tags instanceof LazyFloatArrayObjTags && value instanceof TaintedFloatWithObjTag) {
                ((LazyFloatArrayObjTags) tags).set(null, index, value.taint, ((TaintedFloatWithObjTag) value).val);
            } else if(tags instanceof LazyIntArrayObjTags && value instanceof TaintedIntWithObjTag) {
                ((LazyIntArrayObjTags) tags).set(null, index, value.taint, ((TaintedIntWithObjTag) value).val);
            } else if(tags instanceof LazyLongArrayObjTags && value instanceof TaintedLongWithObjTag) {
                ((LazyLongArrayObjTags) tags).set(null, index, value.taint, ((TaintedLongWithObjTag) value).val);
            } else if(tags instanceof LazyShortArrayObjTags && value instanceof TaintedShortWithObjTag) {
                ((LazyShortArrayObjTags) tags).set(null, index, value.taint, ((TaintedShortWithObjTag) value).val);
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets an element and tag
     * in the specified LazyArrayObjTags. Returns whether the TaintedPrimitiveWithObjTag and LazyArrayObjTags's component
     * type's match. */
    private static boolean getArrayElement(Unsafe unsafe, LazyArrayObjTags tags, long offset, TaintedPrimitiveWithObjTag prealloc) {
        if(tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if(tags instanceof LazyBooleanArrayObjTags && prealloc instanceof TaintedBooleanWithObjTag) {
                ((LazyBooleanArrayObjTags) tags).get(null, index, (TaintedBooleanWithObjTag) prealloc);
            } else if(tags instanceof LazyByteArrayObjTags && prealloc instanceof TaintedByteWithObjTag) {
                ((LazyByteArrayObjTags) tags).get(null, index, (TaintedByteWithObjTag) prealloc);
            } else if(tags instanceof LazyCharArrayObjTags && prealloc instanceof TaintedCharWithObjTag) {
                ((LazyCharArrayObjTags) tags).get(null, index, (TaintedCharWithObjTag) prealloc);
            } else if(tags instanceof LazyDoubleArrayObjTags && prealloc instanceof TaintedDoubleWithObjTag) {
                ((LazyDoubleArrayObjTags) tags).get(null, index, (TaintedDoubleWithObjTag) prealloc);
            } else if(tags instanceof LazyFloatArrayObjTags && prealloc instanceof TaintedFloatWithObjTag) {
                ((LazyFloatArrayObjTags) tags).get(null, index, (TaintedFloatWithObjTag) prealloc);
            } else if(tags instanceof LazyIntArrayObjTags && prealloc instanceof TaintedIntWithObjTag) {
                ((LazyIntArrayObjTags) tags).get(null, index, (TaintedIntWithObjTag) prealloc);
            } else if(tags instanceof LazyLongArrayObjTags && prealloc instanceof TaintedLongWithObjTag) {
                ((LazyLongArrayObjTags) tags).get(null, index, (TaintedLongWithObjTag) prealloc);
            } else if(tags instanceof LazyShortArrayObjTags && prealloc instanceof TaintedShortWithObjTag) {
                ((LazyShortArrayObjTags) tags).get(null, index, (TaintedShortWithObjTag) prealloc);
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private static void put(Unsafe unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if(obj instanceof LazyArrayObjTags && value instanceof TaintedPrimitiveWithObjTag) {
            // Offset is into a primitive array, set the taint tag and value for the element
            if(putArrayElement(unsafe, (LazyArrayObjTags) obj, offset, (TaintedPrimitiveWithObjTag) value)) {
                return;
            }
        }
        if(value instanceof TaintedPrimitiveWithObjTag) {
            obj = MultiDTaintedArray.unbox1D(obj);
        }
        if(obj != null && (value == null || value instanceof TaintedPrimitiveWithObjTag || value instanceof LazyArrayObjTags)) {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair == null && value instanceof LazyArrayObjTags) {
                // Don't unwrap the primitive array
                putTag(unsafe, obj, offset, value, policy);
            } else if(pair == null) {
                putValue(unsafe, obj, offset, value, policy);
            } else {
                if(pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET && offset != pair.tagFieldOffset) {
                    putValue(unsafe, obj, pair.origFieldOffset, value, policy);
                }
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    putTag(unsafe, obj, pair.tagFieldOffset, value, policy);
                }
            }
        } else {
            putValue(unsafe, obj, offset, value, policy);
        }
    }

    private static Object get(Unsafe unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if(obj instanceof LazyArrayObjTags && prealloc instanceof TaintedPrimitiveWithObjTag) {
            // Offset is into a primitive array, get the taint tag and value for the element
            if(getArrayElement(unsafe, (LazyArrayObjTags) obj, offset, (TaintedPrimitiveWithObjTag) prealloc)) {
                return prealloc;
            }
        }
        if(prealloc instanceof TaintedPrimitiveWithObjTag) {
            obj = MultiDTaintedArray.unbox1D(obj);
        }
        if(obj == null) {
            return getValue(unsafe, null, offset, prealloc, policy);
        } else {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair == null) {
                return getValue(unsafe, obj, offset, prealloc, policy);
            } else {
                if(pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    prealloc = getValue(unsafe, obj, pair.origFieldOffset, prealloc, policy);
                }
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    prealloc = getTag(unsafe, obj, pair.tagFieldOffset, prealloc, policy);
                }
                return prealloc;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void put(Unsafe unsafe, Object obj, long offset, Object value) {
        put(unsafe, obj, offset, value, SpecialAccessPolicy.NONE);
    }

    @SuppressWarnings("unused")
    public static void putVolatile(Unsafe unsafe, Object obj, long offset, Object value) {
        put(unsafe, obj, offset, value, SpecialAccessPolicy.VOLATILE);
    }

    @SuppressWarnings("unused")
    public static void putOrdered(Unsafe unsafe, Object obj, long offset, Object value) {
        put(unsafe, obj, offset, value, SpecialAccessPolicy.ORDERED);
    }

    @SuppressWarnings("unused")
    public static Object get(Unsafe unsafe, Object obj, long offset, Object prealloc) {
        return get(unsafe, obj, offset, prealloc, SpecialAccessPolicy.NONE);
    }

    @SuppressWarnings("unused")
    public static Object getVolatile(Unsafe unsafe, Object obj, long offset, Object prealloc) {
        return get(unsafe, obj, offset, prealloc, SpecialAccessPolicy.VOLATILE);
    }

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets a tag
     * in the specified LazyArrayObjTags at a calculated index.
     * type's match. */
    private static void swapArrayElementTag(Unsafe unsafe, LazyArrayObjTags tags, long offset, TaintedPrimitiveWithObjTag value) {
        if(tags.getVal() != null && tags.getVal().getClass().isArray()) {
            Class<?> clazz = tags.getVal().getClass();
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            long scale = unsafe.arrayIndexScale(clazz);
            // Calculate the index based off the offset
            int index = (int) ((offset - baseOffset) / scale);
            if(tags instanceof LazyIntArrayObjTags && value instanceof TaintedIntWithObjTag) {
                LazyIntArrayObjTags intTags = (LazyIntArrayObjTags) tags;
                if(intTags.taints == null && value.taint != null) {
                    intTags.taints = new Taint[intTags.getLength()];
                }
                if(intTags.taints != null) {
                    intTags.taints[index] = value.taint;
                }
            } else if(tags instanceof LazyLongArrayObjTags && value instanceof TaintedLongWithObjTag) {
                LazyLongArrayObjTags longTags = (LazyLongArrayObjTags) tags;
                if(longTags.taints == null && value.taint != null) {
                    longTags.taints = new Taint[longTags.getLength()];
                }
                if(longTags.taints != null) {
                    longTags.taints[index] = value.taint;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean compareAndSwap(Unsafe unsafe, Object obj, long offset, Object expected, Object value) {
        boolean result;
        LazyArrayObjTags lazyArr = null;
        if(obj instanceof LazyArrayObjTags && value instanceof TaintedPrimitiveWithObjTag) {
            lazyArr = (LazyArrayObjTags) obj;
        }
        if(value instanceof TaintedPrimitiveWithObjTag) {
            obj = MultiDTaintedArray.unbox1D(obj);
        }
        OffsetPair pair = null;
        if(obj != null && (value == null || value instanceof TaintedPrimitiveWithObjTag || value instanceof LazyArrayObjTags)) {
            pair = getOffsetPair(unsafe, obj, offset);
        }
        if(pair == null && (value instanceof LazyArrayObjTags)) {
            // Don't unwrap the primitive array
            result = unsafe.compareAndSwapObject(obj, offset, expected, value);
        } else if(expected instanceof TaintedIntWithObjTag && value instanceof TaintedIntWithObjTag) {
            result = unsafe.compareAndSwapInt(MultiDTaintedArray.unbox1D(obj), offset, ((TaintedIntWithObjTag) expected).val, ((TaintedIntWithObjTag) value).val);
        } else if(expected instanceof TaintedLongWithObjTag && value instanceof TaintedLongWithObjTag) {
            result = unsafe.compareAndSwapLong(MultiDTaintedArray.unbox1D(obj), offset, ((TaintedLongWithObjTag) expected).val, ((TaintedLongWithObjTag) value).val);
        } else {
            result = unsafe.compareAndSwapObject(obj, offset, MultiDTaintedArray.unbox1D(expected), MultiDTaintedArray.unbox1D(value));
        }
        if(result && pair != null && pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
            // If an associated tag was found and the swap was successful, set the tag
            putTag(unsafe, obj, pair.tagFieldOffset, value, SpecialAccessPolicy.VOLATILE);
        } else if(result && lazyArr != null) {
            // Offset is into a primitive array, set the taint tag for the element if swap was successful
            swapArrayElementTag(unsafe, lazyArr, offset, (TaintedPrimitiveWithObjTag) value);
        }
        return result;
    }

    private enum SpecialAccessPolicy {
        VOLATILE,
        ORDERED,
        NONE
    }

    public static class OffsetPair {

        public final long origFieldOffset;
        public final long tagFieldOffset;

        public OffsetPair(long origFieldOffset, long tagFieldOffset) {
            this.origFieldOffset = origFieldOffset;
            this.tagFieldOffset = tagFieldOffset;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OffsetPair && this.origFieldOffset == ((OffsetPair) other).origFieldOffset;
        }

        @Override
        public int hashCode() {
            return (int) (origFieldOffset ^ (origFieldOffset >>> 32));
        }

        @Override
        public String toString() {
            return String.format("{field @ %d -> tag @ %d}", origFieldOffset, tagFieldOffset);
        }
    }
}
