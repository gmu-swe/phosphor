package edu.columbia.cs.psl.phosphor.runtime.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator.OffsetPair;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.TaggedArray;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *  Ensures that calls methods in Unsafe that set or retrieve the value of a field of a Java heap object set and
 * retrieve both the original field and its associated taint field if it has one.
 */
public class UnsafeMaskerHelper {
    private UnsafeMaskerHelper() {
        // Prevents this class from being instantiated
    }

    public static final UnsafeAdapter ADAPTER =
            Configuration.IS_JAVA_8 ? new SunUnsafeAdapter() : new JdkUnsafeAdapter();
    /* Used to disambiguate between a static field of a given type and an instance field of java.lang.Class */
    static long LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = ADAPTER.getInvalidFieldOffset();

    /* Stores pairs containing the offset of an original, non-static primitive or primitive array field for the specified
     * class and the offset of the tag field associated with that original field. */
    private static SinglyLinkedList<OffsetPair> getOffsetPairs(UnsafeAdapter unsafe, Class<?> targetClazz) {
        SinglyLinkedList<OffsetPair> list = new SinglyLinkedList<>();
        for (Class<?> clazz = targetClazz;
                clazz != null && !Object.class.equals(clazz);
                clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    Class<?> fieldClazz = field.getType();
                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    long fieldOffset = (isStatic ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field));
                    long tagOffset = ADAPTER.getInvalidFieldOffset();
                    long wrapperOffset = ADAPTER.getInvalidFieldOffset();
                    try {
                        if (!field.getName().equals("SPECIES_DATA")) {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                            if (taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                                tagOffset = (isStatic
                                        ? unsafe.staticFieldOffset(taintField)
                                        : unsafe.objectFieldOffset(taintField));
                            }
                        }
                    } catch (Exception e) {
                        //
                    }
                    if (fieldClazz.isArray()) {
                        try {
                            Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                            Class<?> taintClazz = taintField.getType();
                            if (TaggedArray.class.isAssignableFrom(taintClazz)) {
                                wrapperOffset = (isStatic
                                        ? unsafe.staticFieldOffset(taintField)
                                        : unsafe.objectFieldOffset(taintField));
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
    public static OffsetPair getOffsetPair(UnsafeAdapter unsafe, Object o, long offset) {
        try {
            Class<?> cl = null;
            boolean isStatic = false;
            if (o instanceof Class) {
                // We MIGHT be accessing a static field of this class, in which case we should take
                // the offset from *this* class instance (o).
                // But we might also be accessing an instance field of the type Class, in which case we want to use
                // the classes's class.
                if (LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS == ADAPTER.getInvalidFieldOffset()) {
                    findLastInstanceFieldOnJavaLangClass(unsafe);
                }
                if (offset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    /* We are not accessing an instance field of java.lang.Class, hence, we must be accessing
                     * a static field of type (Class) o */
                    cl = (Class<?>) o;
                    isStatic = true;
                }
                /* Otherwise, we are accessing an instance field of java.lang.Class */
            }
            if (cl == null && o != null) {
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

    private static void findLastInstanceFieldOnJavaLangClass(UnsafeAdapter adapter) {
        for (Field field : Class.class.getDeclaredFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                long fieldOffset = adapter.objectFieldOffset(field);
                if (fieldOffset > LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS) {
                    LAST_INSTANCE_OFFSET_JAVA_LANG_CLASS = fieldOffset;
                }
            } catch (Exception e) {
                //
            }
        }
    }

    /**
     *  If prealloc is a wrapped primitive type, set it's taint to be the value of the field at the specified offset
     *  in the other specified object.
     *  Otherwise returns the value of the field at the specified offset in the specified object.
     */
    public static void getTag(
            UnsafeAdapter unsafe,
            Object obj,
            long originalOffset,
            PhosphorStackFrame stackFrame,
            SpecialAccessPolicy policy) {
        stackFrame.returnTaint = Taint.emptyTaint();
        OffsetPair pair = getOffsetPair(unsafe, obj, originalOffset);
        if (pair != null && pair.tagFieldOffset != ADAPTER.getInvalidFieldOffset()) {
            Object result = (policy == SpecialAccessPolicy.VOLATILE)
                    ? unsafe.getObjectVolatile(obj, pair.tagFieldOffset)
                    : unsafe.getObject(obj, pair.tagFieldOffset);
            if (result instanceof Taint) {
                stackFrame.returnTaint = (Taint<?>) result;
            }
        }
    }

    /**
     * If the specified Object value is a wrapped primitive type, puts it's taint into the field at the specified offset in the
     * other specified object. Otherwise if the specified Object value is null or a lazy array wrapper put the specified Object
     * value into the field at the specified offset in the other specified object.
     */
    public static void putTag(UnsafeAdapter unsafe, Object obj, long offset, Taint<?> tag, SpecialAccessPolicy policy) {
        OffsetPair pair = null;
        if (obj != null) {
            pair = getOffsetPair(unsafe, obj, offset);
        }
        if (pair != null) {
            switch (policy) {
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

    public static int getArrayIndex(UnsafeAdapter unsafe, TaggedArray array, long offset) {
        Class<?> clazz = array.getVal().getClass();
        long baseOffset = unsafe.arrayBaseOffset(clazz);
        long scale = unsafe.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        return (int) ((offset - baseOffset) / scale);
    }

}
