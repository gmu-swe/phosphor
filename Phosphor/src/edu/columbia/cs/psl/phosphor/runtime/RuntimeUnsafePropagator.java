package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class RuntimeUnsafePropagator {

    private static Object unwrap(Object o) {
        if(o instanceof LazyArrayObjTags[] | o instanceof LazyArrayObjTags) {
            return MultiDTaintedArrayWithObjTag.unboxRawOnly1D(o);
        } else {
            return o;
        }
    }

    private static long getTagFieldOffset(Unsafe unsafe, Object o, long offset) {
        for(Class<?> clazz = o.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for(Field field : declaredFields) {
                try {
                    Class<?> fieldClazz = field.getType();
                    if(fieldClazz.isPrimitive()) {
                        // The field is a primitive
                        long fieldOffset = unsafe.objectFieldOffset(field);
                        if(fieldOffset == offset) {
                            // Get the associated taint field
                            try {
                                Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                                if (Configuration.MULTI_TAINTING && taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                    return unsafe.objectFieldOffset(taintField);
                                } else if (!Configuration.MULTI_TAINTING && taintField.getType().equals(int.class)) {
                                    return unsafe.objectFieldOffset(taintField);
                                }
                            } catch(Exception e) {
                                return Unsafe.INVALID_FIELD_OFFSET;
                            }
                        }
                    } else if(fieldClazz.isArray() && fieldClazz.getComponentType().isPrimitive()) {
                        // The fields is a 1D primitive array
                        long fieldOffset = unsafe.objectFieldOffset(field);
                        if(fieldOffset == offset) {
                            // Get the associated taint field
                            try {
                                Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                                Class<?> taintClazz = taintField.getType();
                                if (Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayObjTags.class.isAssignableFrom(taintClazz)) {
                                    return unsafe.objectFieldOffset(taintField);
                                } else if (!Configuration.MULTI_TAINTING && taintClazz != null && LazyArrayIntTags.class.isAssignableFrom(taintClazz)) {
                                    return unsafe.objectFieldOffset(taintField);
                                }
                            } catch(Exception e) {
                                return Unsafe.INVALID_FIELD_OFFSET;
                            }
                        }
                    }
                } catch (Exception e) {
                    //
                }
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
}
