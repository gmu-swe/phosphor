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

    /* Returns a new primitive wrapper of the specified type if the specified type is a primitive type. Otherwise
     * returns nul. */
    private static Object makePrimitiveWrapper(Class<?> clazz) {
        if(byte.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedByteWithObjTag() : new TaintedByteWithIntTag();
        } else if(boolean.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedBooleanWithObjTag() : new TaintedBooleanWithIntTag();
        } else if(char.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedCharWithObjTag() : new TaintedCharWithIntTag();
        } else if(double.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedDoubleWithObjTag() : new TaintedDoubleWithIntTag();
        } else if(float.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedFloatWithObjTag() : new TaintedFloatWithIntTag();
        } else if(int.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedIntWithObjTag() : new TaintedIntWithIntTag();
        } else if(long.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedLongWithObjTag() : new TaintedLongWithIntTag();
        } else if(short.class.equals(clazz)) {
            return Configuration.MULTI_TAINTING ? new TaintedShortWithObjTag() : new TaintedShortWithIntTag();
        } else {
            return null;
        }
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
                            if(Configuration.MULTI_TAINTING && taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            } else if(!Configuration.MULTI_TAINTING && taintField.getType().equals(int.class)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            }
                        } catch(Exception e) {
                            //
                        }
                        list.enqueue(new OffsetPair(fieldClazz,fieldOffset, tagOffset));
                    } else if(!Modifier.isStatic(fieldClazz.getModifiers()) && fieldClazz.isArray() && fieldClazz.getComponentType().isPrimitive()) {
                        // The fields is a 1D primitive array
                        long fieldOffset = unsafe.objectFieldOffset(field);
                        // Find the associated taint field's offset
                        long tagOffset = Unsafe.INVALID_FIELD_OFFSET;
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if(Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            } else if(!Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayIntTags.class.isAssignableFrom(taintClazz)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            }
                        } catch(Exception e) {
                            //
                        }
                        list.enqueue(new OffsetPair(fieldClazz, fieldOffset, tagOffset));
                    } else if(!Modifier.isStatic(fieldClazz.getModifiers()) && (LazyArrayIntTags.class.isAssignableFrom(fieldClazz)
                        || LazyArrayObjTags.class.isAssignableFrom(fieldClazz) || Taint.class.isAssignableFrom(fieldClazz))) {
                        if(field.getName().equals(TaintUtils.TAINT_FIELD) || !field.getName().endsWith(TaintUtils.TAINT_FIELD)) {
                            Class<?> placeholderClass = Taint.class.isAssignableFrom(fieldClazz) ? TaintedIntWithObjTag.class : null;
                            list.enqueue(new OffsetPair(placeholderClass, Unsafe.INVALID_FIELD_OFFSET, unsafe.objectFieldOffset(field)));
                        }
                    }
                } catch (Exception e) {
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
                    if(pair.origFieldOffset == offset || pair.tagFieldOffset == offset) {
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
                ((TaintedPrimitiveWithObjTag) prealloc).taint = (Taint)result;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedPrimitiveWithIntTag) {
            ((TaintedPrimitiveWithIntTag) prealloc).taint = (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getIntVolatile(obj, offset) : unsafe.getInt(obj, offset);
            return prealloc;
        } else {
            return (policy == SpecialAccessPolicy.VOLATILE) ? unsafe.getObjectVolatile(obj, offset) : unsafe.getObject(obj, offset);
        }
    }

    /* If prealloc is a wrapped primitive type, sets it's value to be the primitive value that is at the specified offset
     * for the specified object and return it. Otherwise returns the object at the specified offset for the specified
     * object and returns it. */
    private static Object getValue(Unsafe unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if(prealloc instanceof TaintedByteWithObjTag || prealloc instanceof TaintedByteWithIntTag) {
            byte val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getByteVolatile(obj, offset);
            } else {
                val = unsafe.getByte(obj, offset);
            }
            if(prealloc instanceof TaintedByteWithObjTag) {
                ((TaintedByteWithObjTag) prealloc).val = val;
            } else {
                ((TaintedByteWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedBooleanWithObjTag || prealloc instanceof TaintedBooleanWithIntTag) {
            boolean val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getBooleanVolatile(obj, offset);
            } else {
                val = unsafe.getBoolean(obj, offset);
            }
            if(prealloc instanceof TaintedBooleanWithObjTag) {
                ((TaintedBooleanWithObjTag) prealloc).val = val;
            } else {
                ((TaintedBooleanWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedCharWithObjTag || prealloc instanceof TaintedCharWithIntTag) {
            char val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getCharVolatile(obj, offset);
            } else {
                val = unsafe.getChar(obj, offset);
            }
            if(prealloc instanceof TaintedCharWithObjTag) {
                ((TaintedCharWithObjTag) prealloc).val = val;
            } else {
                ((TaintedCharWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedDoubleWithObjTag || prealloc instanceof TaintedDoubleWithIntTag) {
            double val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getDoubleVolatile(obj, offset);
            } else {
                val = unsafe.getDouble(obj, offset);
            }
            if(prealloc instanceof TaintedDoubleWithObjTag) {
                ((TaintedDoubleWithObjTag) prealloc).val = val;
            } else {
                ((TaintedDoubleWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedFloatWithObjTag || prealloc instanceof TaintedFloatWithIntTag) {
            float val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getFloatVolatile(obj, offset);
            } else {
                val = unsafe.getFloat(obj, offset);
            }
            if(prealloc instanceof TaintedFloatWithObjTag) {
                ((TaintedFloatWithObjTag) prealloc).val = val;
            } else {
                ((TaintedFloatWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedIntWithObjTag || prealloc instanceof TaintedIntWithIntTag) {
            int val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getIntVolatile(obj, offset);
            } else {
                val = unsafe.getInt(obj, offset);
            }
            if(prealloc instanceof TaintedIntWithObjTag) {
                ((TaintedIntWithObjTag) prealloc).val = val;
            } else {
                ((TaintedIntWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedLongWithObjTag || prealloc instanceof TaintedLongWithIntTag) {
            long val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getLongVolatile(obj, offset);
            } else {
                val = unsafe.getLong(obj, offset);
            }
            if(prealloc instanceof TaintedLongWithObjTag) {
                ((TaintedLongWithObjTag) prealloc).val = val;
            } else {
                ((TaintedLongWithIntTag) prealloc).val = val;
            }
            return prealloc;
        } else if(prealloc instanceof TaintedShortWithObjTag || prealloc instanceof TaintedShortWithIntTag) {
            short val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                val = unsafe.getShortVolatile(obj, offset);
            } else {
                val = unsafe.getShort(obj, offset);
            }
            if(prealloc instanceof TaintedShortWithObjTag) {
                ((TaintedShortWithObjTag) prealloc).val = val;
            } else {
                ((TaintedShortWithIntTag) prealloc).val = val;
            }
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
        if(value instanceof TaintedPrimitiveWithObjTag || value instanceof LazyArrayIntTags || value instanceof LazyArrayObjTags || value == null) {
            Object tag = (value instanceof TaintedPrimitiveWithObjTag) ?((TaintedPrimitiveWithObjTag) value).taint : value;
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
        } else if(value instanceof TaintedPrimitiveWithIntTag) {
            int tag = ((TaintedPrimitiveWithIntTag) value).taint;
            switch(policy) {
                case ORDERED:
                    unsafe.putOrderedInt(obj, offset, tag);
                    break;
                case VOLATILE:
                    unsafe.putIntVolatile(obj, offset, tag);
                    break;
                default:
                    unsafe.putInt(obj, offset, tag);
            }
        }
    }

    /* If the specified Object value is a wrapped type, puts it's val the field at the specified offset in the other
     * specified object. Otherwise, puts the specified Object value into the field at the specified offset in the other
     * specified object. */
    private static void putValue(Unsafe unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if(value instanceof TaintedByteWithObjTag || value instanceof TaintedByteWithIntTag) {
            byte val = (value instanceof TaintedByteWithObjTag) ? ((TaintedByteWithObjTag) value).val : ((TaintedByteWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putByteVolatile(obj, offset, val);
            } else {
                unsafe.putByte(obj, offset, val);
            }
        } else if(value instanceof TaintedBooleanWithObjTag || value instanceof TaintedBooleanWithIntTag) {
            boolean val = (value instanceof TaintedBooleanWithObjTag) ? ((TaintedBooleanWithObjTag) value).val : ((TaintedBooleanWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putBooleanVolatile(obj, offset, val);
            } else {
                unsafe.putBoolean(obj, offset, val);
            }
        } else if(value instanceof TaintedCharWithObjTag || value instanceof TaintedCharWithIntTag) {
            char val = (value instanceof TaintedCharWithObjTag) ? ((TaintedCharWithObjTag) value).val : ((TaintedCharWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putCharVolatile(obj, offset, val);
            } else {
                unsafe.putChar(obj, offset, val);
            }
        } else if(value instanceof TaintedDoubleWithObjTag || value instanceof TaintedDoubleWithIntTag) {
            double val = (value instanceof TaintedDoubleWithObjTag) ? ((TaintedDoubleWithObjTag) value).val : ((TaintedDoubleWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putDoubleVolatile(obj, offset, val);
            } else {
                unsafe.putDouble(obj, offset, val);
            }
        } else if(value instanceof TaintedFloatWithObjTag || value instanceof TaintedFloatWithIntTag) {
            float val = (value instanceof TaintedFloatWithObjTag) ? ((TaintedFloatWithObjTag) value).val : ((TaintedFloatWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putFloatVolatile(obj, offset, val);
            } else {
                unsafe.putFloat(obj, offset, val);
            }
        } else if(value instanceof TaintedIntWithObjTag || value instanceof TaintedIntWithIntTag) {
            int val = (value instanceof TaintedIntWithObjTag) ? ((TaintedIntWithObjTag) value).val : ((TaintedIntWithIntTag) value).val;
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
        } else if(value instanceof TaintedLongWithObjTag || value instanceof TaintedLongWithIntTag) {
            long val = (value instanceof TaintedLongWithObjTag) ? ((TaintedLongWithObjTag) value).val : ((TaintedLongWithIntTag) value).val;
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
        } else if(value instanceof TaintedShortWithObjTag || value instanceof TaintedShortWithIntTag) {
            short val = (value instanceof TaintedShortWithObjTag) ? ((TaintedShortWithObjTag) value).val : ((TaintedShortWithIntTag) value).val;
            if(policy == SpecialAccessPolicy.VOLATILE) {
                unsafe.putShortVolatile(obj, offset, val);
            } else {
                unsafe.putShort(obj, offset, val);
            }
        } else if(value instanceof LazyArrayObjTags || value instanceof LazyArrayIntTags) {
            Object val = (value instanceof LazyArrayObjTags) ? ((LazyArrayObjTags) value).getVal() : (( LazyArrayIntTags) value).getVal();
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

    private static void put(Unsafe unsafe, Object obj, long offset, Object value, SpecialAccessPolicy policy) {
        if(obj != null && (value == null || value instanceof TaintedPrimitiveWithObjTag || value instanceof TaintedPrimitiveWithIntTag ||
                value instanceof LazyArrayObjTags || value instanceof LazyArrayIntTags)) {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair == null) {
                obj = MultiDTaintedArray.unbox1D(obj);
                putValue(unsafe, obj, offset, value, policy);
            } else {
                if(pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET && offset != pair.tagFieldOffset) {
                    putValue(unsafe, obj, pair.origFieldOffset, value, policy);
                }
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    putTag(unsafe, obj, pair.tagFieldOffset, value, policy);
                }
            }
        } else  {
            putValue(unsafe, obj, offset, value, policy);
        }
    }

    private static Object get(Unsafe unsafe, Object obj, long offset, Object prealloc, SpecialAccessPolicy policy) {
        if(prealloc != null) {
            obj = MultiDTaintedArray.unbox1D(obj);
        }
        if(obj == null) {
            return getValue(unsafe, null, offset, prealloc, policy);
        } else  {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if(pair == null) {
                return getValue(unsafe, obj, offset, prealloc, policy);
            } else {
                if(prealloc == null && pair.origFieldType != null && pair.origFieldType.isPrimitive()) {
                    prealloc = makePrimitiveWrapper(pair.origFieldType);
                }
                if(pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    prealloc = getValue(unsafe, obj, pair.origFieldOffset, prealloc, policy);
                }
                if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    prealloc = getTag(unsafe, obj, pair.tagFieldOffset, prealloc, policy);
                }
                if(offset == pair.tagFieldOffset && (prealloc instanceof TaintedPrimitiveWithObjTag ||
                        prealloc instanceof TaintedPrimitiveWithIntTag)) {
                    return (prealloc instanceof TaintedPrimitiveWithObjTag) ? ((TaintedPrimitiveWithObjTag) prealloc).taint :
                            ((TaintedPrimitiveWithIntTag) prealloc).taint;
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

    public static class OffsetPair {

        public final Class<?> origFieldType;
        public final long origFieldOffset;
        public final long tagFieldOffset;

        public OffsetPair(Class<?> origFieldType, long origFieldOffset, long tagFieldOffset) {
            this.origFieldType = origFieldType;
            this.origFieldOffset = origFieldOffset;
            this.tagFieldOffset = tagFieldOffset;
        }

        @Override
        public String toString() {
            return String.format("{field @ %d -> tag @ %d}", origFieldOffset, tagFieldOffset);
        }
    }

    private enum SpecialAccessPolicy {
        VOLATILE,
        ORDERED,
        NONE
    }
}
