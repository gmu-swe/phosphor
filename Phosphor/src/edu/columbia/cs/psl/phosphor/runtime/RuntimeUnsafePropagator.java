package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class RuntimeUnsafePropagator {

    private static Object unwrap(Object o) {
        if(o instanceof LazyArrayObjTags[] | o instanceof LazyArrayObjTags) {
            return MultiDTaintedArrayWithObjTag.unboxRawOnly1D(o);
        } else {
            return o;
        }
    }

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

    /* Returns the offset of the taint field associated with the field at the specified offset for the specified object or
     * Unsafe.INVALID_FIELD_OFFSET if an associated taint field could not be found. */
    private static long getTagFieldOffset(Unsafe unsafe, Object o, long offset) {
        if(o == null || o.getClass() == null) {
            return Unsafe.INVALID_FIELD_OFFSET;
        }
        if(o.getClass().$$PHOSPHOR_OFFSET_CACHE == null) {
            o.getClass().$$PHOSPHOR_OFFSET_CACHE = getOffsetPairs(unsafe, o.getClass());
        }
        for(OffsetPair pair : o.getClass().$$PHOSPHOR_OFFSET_CACHE) {
            if(pair.origFieldOffset == offset) {
                return pair.tagFieldOffset;
            }
        }
        return Unsafe.INVALID_FIELD_OFFSET;
    }

    @SuppressWarnings("unused")
    public static void putTag(Unsafe unsafe, Object o, long offset, Taint tag) {
        try {
            o = unwrap(o);
            long tagOffset = getTagFieldOffset(unsafe, o, offset);
            if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                unsafe.putObject(o, tagOffset, tag);
            }
        } catch(Exception e) {
            //
        }
    }

    @SuppressWarnings("unused")
    public static void putTag(Unsafe unsafe, Object o, long offset, int tag) {
        try {
            o = unwrap(o);
            long tagOffset = getTagFieldOffset(unsafe, o, offset);
            if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                unsafe.putInt(o, tagOffset, tag);
            }
        } catch(Exception e) {
            //
        }
    }

    @SuppressWarnings("unused")
    public static void putTag(Unsafe unsafe, Object o, long offset, Object tags) {
        if(tags == null || tags instanceof LazyArrayObjTags || tags instanceof LazyArrayIntTags) {
            try {
                o = unwrap(o);
                long tagOffset = getTagFieldOffset(unsafe, o, offset);
                if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(o, tagOffset, tags);
                }
            } catch(Exception e) {
                //
            }
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
