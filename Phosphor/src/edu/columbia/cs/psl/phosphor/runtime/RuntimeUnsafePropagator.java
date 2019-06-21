package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class RuntimeUnsafePropagator {

    public static Object unwrap(Object o) {
        if(o instanceof LazyArrayObjTags[] | o instanceof LazyArrayObjTags) {
            return MultiDTaintedArrayWithObjTag.unboxRawOnly1D(o);
        } else if(o instanceof LazyArrayIntTags[] | o instanceof LazyArrayIntTags) {
            return MultiDTaintedArrayWithIntTag.unboxRawOnly1D(o);
        } else {
            return o;
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
                            if (Configuration.MULTI_TAINTING && taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            } else if (!Configuration.MULTI_TAINTING && taintField.getType().equals(int.class)) {
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
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if (Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            } else if (!Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayIntTags.class.isAssignableFrom(taintClazz)) {
                                tagOffset = unsafe.objectFieldOffset(taintField);
                            }
                        } catch(Exception e) {
                            //
                        }
                        list.enqueue(new OffsetPair(fieldOffset, tagOffset));
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
        if(o == null || o.getClass() == null) {
            return null;
        }
        if(o.getClass().$$PHOSPHOR_OFFSET_CACHE == null) {
            o.getClass().$$PHOSPHOR_OFFSET_CACHE = getOffsetPairs(unsafe, o.getClass());
        }
        for(OffsetPair pair : o.getClass().$$PHOSPHOR_OFFSET_CACHE) {
            if(pair.origFieldOffset == offset || pair.tagFieldOffset == offset) {
                return pair;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void putTagOrOriginalField(Unsafe unsafe, Object o, long offset, Taint tag) {
        try {
            OffsetPair pair = getOffsetPair(unsafe, o, offset);
            if(pair != null) {
                if(pair.origFieldOffset == offset &&  pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    // Given offset is for an original field
                    unsafe.putObject(o, pair.tagFieldOffset, tag);
                }
            }
        } catch(Exception e) {
            //
        }
    }

    @SuppressWarnings("unused")
    public static void putTagOrOriginalField(Unsafe unsafe, Object o, long offset, int tag) {
        // Not fully supported
        try {
            OffsetPair pair = getOffsetPair(unsafe, o, offset);
            if(pair != null) {
                if(pair.origFieldOffset == offset &&  pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    // Given offset is for an original field
                    unsafe.putInt(o, pair.tagFieldOffset, tag);
                }
            }
        } catch(Exception e) {
            //
        }
    }

    @SuppressWarnings("unused")
    public static void putTagOrOriginalField(Unsafe unsafe, Object o, long offset, Object tags) {
        if(tags == null || tags instanceof LazyArrayObjTags || tags instanceof LazyArrayIntTags) {
            try {
                OffsetPair pair = getOffsetPair(unsafe, o, offset);
                if(pair != null) {
                    if(pair.tagFieldOffset == offset && pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                        // Given offset is for a tag field
                        unsafe.putObject(o, pair.origFieldOffset, unwrap(tags));
                    } else if(pair.origFieldOffset == offset &&  pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                        // Given offset is for an original field
                        unsafe.putObject(o, pair.tagFieldOffset, tags);
                    }
                }
            } catch(Exception e) {
                //
            }
        }
    }

    @SuppressWarnings("unused")
    public static Object getTagAndOriginalField(Unsafe unsafe, Object o, Object prealloc, long offset) {
        if(prealloc instanceof LazyArrayObjTags || prealloc instanceof TaintedPrimitiveWithObjTag) {
            try {
                OffsetPair pair = getOffsetPair(unsafe, o, offset);
                if(pair != null) {
                    if(pair.origFieldOffset == offset && pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                        // Given offset is for an original field
                        Object tag = unsafe.getObject(o, pair.tagFieldOffset);
                        if(tag instanceof Taint && prealloc instanceof TaintedPrimitiveWithObjTag) {
                            ((TaintedPrimitiveWithObjTag) prealloc).taint = (Taint)tag;
                        } else if(tag instanceof LazyArrayObjTags && prealloc instanceof LazyArrayObjTags) {
                            ((LazyArrayObjTags) prealloc).taints = ((LazyArrayObjTags) tag).taints;
                        }
                    }
                }
            } catch(Exception e) {
                //
            }
        }
        return prealloc;
    }

    /* Puts the primitive stored in the specified tag into the field at the specified offset in the specified object. */
    private static void putPrimitive(Unsafe unsafe, Object o, long offset, TaintedPrimitiveWithObjTag tag) {
        if(tag instanceof TaintedByteWithObjTag) {
            unsafe.putByte(o, offset, ((TaintedByteWithObjTag) tag).val);
        } else if(tag instanceof TaintedBooleanWithObjTag) {
            unsafe.putBoolean(o, offset, ((TaintedBooleanWithObjTag) tag).val);
        } else if(tag instanceof TaintedCharWithObjTag) {
            unsafe.putChar(o, offset, ((TaintedCharWithObjTag) tag).val);
        } else if(tag instanceof TaintedDoubleWithObjTag) {
            unsafe.putDouble(o, offset, ((TaintedDoubleWithObjTag) tag).val);
        } else if(tag instanceof TaintedFloatWithObjTag) {
            unsafe.putFloat(o, offset, ((TaintedFloatWithObjTag) tag).val);
        } else if(tag instanceof TaintedIntWithObjTag) {
            unsafe.putInt(o, offset, ((TaintedIntWithObjTag) tag).val);
        } else if(tag instanceof TaintedLongWithObjTag) {
            unsafe.putLong(o, offset, ((TaintedLongWithObjTag) tag).val);
        } else if(tag instanceof TaintedShortWithObjTag) {
            unsafe.putShort(o, offset, ((TaintedShortWithObjTag) tag).val);
        }
    }

    public static class OffsetPair {
        public final long origFieldOffset;
        public final long tagFieldOffset;

        public OffsetPair(long origFieldOffset, long tagFieldOffset) {
            this.origFieldOffset = origFieldOffset;
            this.tagFieldOffset = tagFieldOffset;
        }

        @Override
        public String toString() {
            return String.format("{field @ %d -> tag @ %d}", origFieldOffset, tagFieldOffset);
        }
    }
}
