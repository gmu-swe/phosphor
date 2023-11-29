package edu.columbia.cs.psl.phosphor.runtime.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator.OffsetPair;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.TaggedArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * Utility method for ensuring that method calls from {@link jdk.internal.misc.Unsafe} and
 * {@link sun.misc.Unsafe} that set or retrieve values from fields or array operate on both the
 * original value and its associated taint tag.
 */
public class UnsafeMaskerHelper {
    private UnsafeMaskerHelper() {
        // Prevents this class from being instantiated
    }

    public static final UnsafeAdapter ADAPTER =
            Configuration.IS_JAVA_8 ? new SunUnsafeAdapter() : new JdkUnsafeAdapter();
    /**
     *  Used to disambiguate between a static field of a given type and an instance field of java.lang.Class
     */
    private static long LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = ADAPTER.getInvalidFieldOffset();

    private static SinglyLinkedList<OffsetPair> getOffsetPairs(Class<?> targetClazz) {
        SinglyLinkedList<OffsetPair> list = new SinglyLinkedList<>();
        for (Class<?> clazz = targetClazz;
                clazz != null && !Object.class.equals(clazz);
                clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    long fieldOffset = (isStatic ? ADAPTER.staticFieldOffset(field) : ADAPTER.objectFieldOffset(field));
                    long tagOffset = getTagOffset(clazz, field, isStatic);
                    long wrapperOffset = getWrapperOffset(clazz, field, isStatic);
                    list.enqueue(new OffsetPair(isStatic, fieldOffset, wrapperOffset, tagOffset));
                } catch (Exception e) {
                    //
                }
            }
        }
        return list;
    }

    private static long getTagOffset(Class<?> clazz, Field field, boolean isStatic) {
        try {
            if (!field.getName().equals("SPECIES_DATA")) {
                Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                if (Configuration.TAINT_TAG_OBJ_CLASS.equals(taintField.getType())) {
                    return (isStatic ? ADAPTER.staticFieldOffset(taintField) : ADAPTER.objectFieldOffset(taintField));
                }
            }
        } catch (Exception e) {
            //
        }
        return ADAPTER.getInvalidFieldOffset();
    }

    private static long getWrapperOffset(Class<?> clazz, Field field, boolean isStatic) {
        if (field.getType().isArray()) {
            try {
                Field wrapperField = clazz.getField(field.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                if (TaggedArray.class.isAssignableFrom(wrapperField.getType())) {
                    return (isStatic
                            ? ADAPTER.staticFieldOffset(wrapperField)
                            : ADAPTER.objectFieldOffset(wrapperField));
                }
            } catch (Exception e) {
                //
            }
        }
        return ADAPTER.getInvalidFieldOffset();
    }

    public static OffsetPair getOffsetPair(Object o, long offset) {
        try {
            Class<?> cl = null;
            boolean isStatic = false;
            if (o == null) {
                return null;
            } else if (o instanceof Class) {
                // We MIGHT be accessing a static field of this class, in which case we should take
                // the offset from *this* class instance (o).
                // But we might also be accessing an instance field of the type Class, in which case we want to use
                // the class' class.
                if (LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS == ADAPTER.getInvalidFieldOffset()) {
                    findLastInstanceFieldOnJavaLangClass();
                }
                if (offset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    /* We are not accessing an instance field of java.lang.Class, hence, we must be accessing
                     * a static field of type (Class) o */
                    cl = (Class<?>) o;
                    isStatic = true;
                }
                /* Otherwise, we are accessing an instance field of java.lang.Class */
            }
            if (cl == null) {
                cl = o.getClass();
            }
            if (InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl) == null) {
                InstrumentedJREFieldHelper.set$$PHOSPHOR_OFFSET_CACHE(cl, getOffsetPairs(cl));
            }
            for (OffsetPair pair : InstrumentedJREFieldHelper.get$$PHOSPHOR_OFFSET_CACHE(cl)) {
                if (pair.origFieldOffset == offset && pair.isStatic == isStatic) {
                    return pair;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void findLastInstanceFieldOnJavaLangClass() {
        for (Field field : Class.class.getDeclaredFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                long fieldOffset = ADAPTER.objectFieldOffset(field);
                if (fieldOffset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = fieldOffset;
                }
            } catch (Exception e) {
                //
            }
        }
    }

    public static int getTaggedArrayIndex(TaggedArray array, long offset) {
        Class<?> clazz = array.getVal().getClass();
        long baseOffset = ADAPTER.arrayBaseOffset(clazz);
        long scale = ADAPTER.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        return (int) ((offset - baseOffset) / scale);
    }

    public static void putTag(Object o, long offset, Taint<?> valTaint, SpecialAccessPolicy policy) {
        if (o instanceof TaggedArray) {
            TaggedArray array = (TaggedArray) o;
            int index = getTaggedArrayIndex(array, offset);
            array.setTaint(index, valTaint);
        } else {
            OffsetPair pair = getOffsetPair(o, offset);
            if (pair != null && pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                policy.putObject(ADAPTER, o, pair.tagFieldOffset, valTaint);
            }
        }
    }

    public static Taint<?> getTag(Object o, long offset, SpecialAccessPolicy policy) {
        OffsetPair pair = getOffsetPair(o, offset);
        if (pair != null && pair.tagFieldOffset != ADAPTER.getInvalidFieldOffset()) {
            return (Taint<?>) policy.getObject(ADAPTER, o, pair.tagFieldOffset);
        }
        return Taint.emptyTaint();
    }

    public static Taint<?> getTagPrimitive(Object o, long offset, SpecialAccessPolicy policy) {
        if (o instanceof TaggedArray) {
            TaggedArray array = (TaggedArray) o;
            int index = getTaggedArrayIndex(array, offset);
            return array.getTaintOrEmpty(index);
        } else {
            return getTag(o, offset, policy);
        }
    }

    public static Object getObjectAndTag(Object o, long offset, PhosphorStackFrame frame, SpecialAccessPolicy policy) {
        if (o instanceof TaggedReferenceArray) {
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            int index = getTaggedArrayIndex(array, offset);
            // Propagate the offset taint tag to the index
            return array.get(index, frame.getArgTaint(1), frame);
        } else {
            // Is this trying to return a field that is wrapped?
            OffsetPair pair = getOffsetPair(o, offset);
            // TODO check
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            frame.returnTaint = getTag(o, offset, policy);
            return policy.getObject(ADAPTER, o, offset);
        }
    }

    public static Object unwrap(Object object) {
        return object instanceof TaggedArray ? ((TaggedArray) object).getVal() : object;
    }

    public static void putObjectAndTag(
            Object o, long offset, Object x, PhosphorStackFrame frame, SpecialAccessPolicy policy) {
        if (o instanceof TaggedReferenceArray) {
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            int index = getTaggedArrayIndex(array, offset);
            array.set(index, x, frame.getArgTaint(3));
        } else {
            OffsetPair pair = getOffsetPair(o, offset);
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    Object x1 = frame.getArgTaint(3);
                    policy.putObject(ADAPTER, o, pair.tagFieldOffset, x1);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    policy.putObject(ADAPTER, o, pair.wrappedFieldOffset, x);
                    Object x1 = MultiDArrayUtils.unbox1DOrNull(x);
                    policy.putObject(ADAPTER, o, offset, x1);
                } else {
                    policy.putObject(ADAPTER, o, offset, x);
                }
            } else {
                Object x1 = MultiDArrayUtils.unbox1DOrNull(x);
                policy.putObject(ADAPTER, o, offset, x1);
            }
        }
    }
}
