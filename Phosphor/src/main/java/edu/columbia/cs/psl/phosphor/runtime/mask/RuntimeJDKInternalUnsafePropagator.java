package edu.columbia.cs.psl.phosphor.runtime.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;

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
                            if (taintClazz != null && TaggedArray.class.isAssignableFrom(taintClazz)) {
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
                    try {
                        InstrumentedJREFieldHelper.set$$PHOSPHOR_OFFSET_CACHE(cl, getOffsetPairs(unsafe, cl));
                    } catch(ClassCircularityError err) {
                        //See issue 190
                    }
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

    /* If the specified TaintedPrimitiveWithObjTag and LazyArrayObjTags's component types match sets a tag
     * in the specified TaggedArray at a calculated index.
     * type's match. */
    private static void swapArrayElementTag(UnsafeProxy unsafe, TaggedArray tags, long offset, Taint valueTaint) {
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

    @SuppressWarnings("unused")
    public static void copyMemory(UnsafeProxy unsafe, Object src, long srcAddress, Object dest, long destAddress, long length, PhosphorStackFrame phosphorStackFrame) {
        if (src instanceof TaggedArray) {
            src = ((TaggedArray) src).getVal();
        }
        if (dest instanceof TaggedArray) {
            dest = ((TaggedArray) dest).getVal();
        }
        unsafe.copyMemory(src, srcAddress, dest, destAddress, length);
    }

    @SuppressWarnings("unused")
    public static void copyMemory(UnsafeProxy unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame phosphorStackFrame) {
        unsafe.copyMemory(srcAddress, destAddress, length);
    }

    public static void copySwapMemory(UnsafeProxy unsafe, Object srcBase, long srcOffset, Object destBase,
                                      long destOffset, long bytes, long elemSize, PhosphorStackFrame stackFrame) {
        if (srcBase instanceof TaggedArray) {
            srcBase = ((TaggedArray) srcBase).getVal();
        }
        if (destBase instanceof TaggedArray) {
            destBase = ((TaggedArray) destBase).getVal();
        }
        unsafe.copySwapMemory(srcBase, srcOffset, destBase, destOffset, bytes, elemSize);
    }

    public static void copySwapMemory(UnsafeProxy unsafe, long srcAddress, long destAddress, long bytes, long elemSize, PhosphorStackFrame stackFrame) {
        unsafe.copySwapMemory(srcAddress, destAddress, bytes, elemSize);
    }

    @SuppressWarnings("unused")
    public static Object allocateUninitializedArray(UnsafeProxy unsafe, Class c, int len, PhosphorStackFrame phosphorStackFrame) {
        Object ret = unsafe.allocateUninitializedArray(c, len);
        if (ret instanceof TaggedArray) {
            return ret;
        }
        return MultiDArrayUtils.boxIfNecessary(ret);
    }


    private enum SpecialAccessPolicy {
        VOLATILE,
        ORDERED,
        NONE
    }

    private static int unsafeIndexFor(UnsafeProxy unsafe, TaggedArray array, long offset) {
        Class<?> clazz = array.getVal().getClass();
        long baseOffset = unsafe.arrayBaseOffset(clazz);
        long scale = unsafe.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        return (int) ((offset - baseOffset) / scale);
    }

    @SuppressWarnings("unused")
    public static int getIntUnaligned(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 3) == 0) {
            return getInt(unsafe, o, offset, phosphorStackFrame);
        } else if ((offset & 1) == 0) {
            return UnsafeProxy.makeInt(getShort(unsafe, o, offset, phosphorStackFrame),
                    getShort(unsafe, o, offset + 2, phosphorStackFrame));
        } else {
            return UnsafeProxy.makeInt(getByte(unsafe, o, offset, phosphorStackFrame),
                    getByte(unsafe, o, offset + 1, phosphorStackFrame),
                    getByte(unsafe, o, offset + 2, phosphorStackFrame),
                    getByte(unsafe, o, offset + 3, phosphorStackFrame));
        }
    }

    @SuppressWarnings("unused")
    public static long getLongUnaligned(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 7) == 0) {
            return getLong(unsafe, o, offset, phosphorStackFrame);
        } else if ((offset & 3) == 0) {
            return UnsafeProxy.makeLong(getInt(unsafe, o, offset, phosphorStackFrame),
                    getInt(unsafe, o, offset + 4, phosphorStackFrame));
        } else if ((offset & 1) == 0) {
            return UnsafeProxy.makeLong(getShort(unsafe, o, offset, phosphorStackFrame),
                    getShort(unsafe, o, offset + 2, phosphorStackFrame),
                    getShort(unsafe, o, offset + 4, phosphorStackFrame),
                    getShort(unsafe, o, offset + 6, phosphorStackFrame));
        } else {
            return UnsafeProxy.makeLong(getByte(unsafe, o, offset, phosphorStackFrame),
                    getByte(unsafe, o, offset + 1, phosphorStackFrame),
                    getByte(unsafe, o, offset + 2, phosphorStackFrame),
                    getByte(unsafe, o, offset + 3, phosphorStackFrame),
                    getByte(unsafe, o, offset + 4, phosphorStackFrame),
                    getByte(unsafe, o, offset + 5, phosphorStackFrame),
                    getByte(unsafe, o, offset + 6, phosphorStackFrame),
                    getByte(unsafe, o, offset + 7, phosphorStackFrame));
        }
    }

    @SuppressWarnings("unused")
    public static char getCharUnaligned(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 1) == 0) {
            return getChar(unsafe, o, offset, phosphorStackFrame);
        } else {
            return (char) UnsafeProxy.makeShort(getByte(unsafe, o, offset, phosphorStackFrame),
                    getByte(unsafe, o, offset + 1, phosphorStackFrame));
        }
    }

    @SuppressWarnings("unused")
    public static short getShortUnaligned(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 1) == 0) {
            return getShort(unsafe, o, offset, phosphorStackFrame);
        } else {
            return UnsafeProxy.makeShort(getByte(unsafe, o, offset, phosphorStackFrame),
                    getByte(unsafe, o, offset + 1, phosphorStackFrame));
        }
    }

    @SuppressWarnings("unused")
    public static void putIntUnaligned(UnsafeProxy unsafe, Object o, long offset, int x, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 3) == 0) {
            putInt(unsafe, o, offset, x, phosphorStackFrame);
        } else if ((offset & 1) == 0) {
            unsafe.putIntParts(o, offset,
                    (short) (x >> 0),
                    (short) (x >>> 16));
        } else {
            unsafe.putIntParts(o, offset,
                    (byte) (x >>> 0),
                    (byte) (x >>> 8),
                    (byte) (x >>> 16),
                    (byte) (x >>> 24));
        }
    }

    @SuppressWarnings("unused")
    public static void putLongUnaligned(UnsafeProxy unsafe, Object o, long offset, long x, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 7) == 0) {
            putLong(unsafe, o, offset, x, phosphorStackFrame);
        } else if ((offset & 3) == 0) {
            unsafe.putLongParts(o, offset,
                    (int) (x >> 0),
                    (int) (x >>> 32));
        } else if ((offset & 1) == 0) {
            unsafe.putLongParts(o, offset,
                    (short) (x >>> 0),
                    (short) (x >>> 16),
                    (short) (x >>> 32),
                    (short) (x >>> 48));
        } else {
            unsafe.putLongParts(o, offset,
                    (byte) (x >>> 0),
                    (byte) (x >>> 8),
                    (byte) (x >>> 16),
                    (byte) (x >>> 24),
                    (byte) (x >>> 32),
                    (byte) (x >>> 40),
                    (byte) (x >>> 48),
                    (byte) (x >>> 56));
        }
    }

    @SuppressWarnings("unused")
    public static void putCharUnaligned(UnsafeProxy unsafe, Object o, long offset, char x, PhosphorStackFrame phosphorStackFrame) {
        putShortUnaligned(unsafe, o, offset, (short) x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putShortUnaligned(UnsafeProxy unsafe, Object o, long offset, short x, PhosphorStackFrame phosphorStackFrame) {
        if ((offset & 1) == 0) {
            putShort(unsafe, o, offset, x, phosphorStackFrame);
        } else {
            unsafe.putShortParts(o, offset,
                    (byte) (x >>> 0),
                    (byte) (x >>> 8));
        }
    }

    //Generated from Phosphor template for get$methodTypeOpaque(Ljava/lang/Object;J)LPlaceHolder;
    @SuppressWarnings("unused")
    public static byte getByteOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getByteVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static int getIntOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getIntVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static long getLongOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getLongVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static float getFloatOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getFloatVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static Object getReferenceOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getReferenceVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static char getCharOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getCharVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static short getShortOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getShortVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static double getDoubleOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getDoubleVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean getBooleanOpaque(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getBooleanVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    //Generated from Phosphor template for compareAndExchange$methodTypeAcquire(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)LPlaceHolder;
    @SuppressWarnings("unused")
    public static int compareAndExchangeIntAcquire(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static byte compareAndExchangeByteAcquire(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static long compareAndExchangeLongAcquire(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static Object compareAndExchangeReferenceAcquire(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static short compareAndExchangeShortAcquire(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for get$methodTypeAcquire(Ljava/lang/Object;J)LPlaceHolder;
    @SuppressWarnings("unused")
    public static byte getByteAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getByteVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static int getIntAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getIntVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static long getLongAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getLongVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static float getFloatAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getFloatVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static Object getReferenceAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getReferenceVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static char getCharAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getCharVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static short getShortAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getShortVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static double getDoubleAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getDoubleVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean getBooleanAcquire(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame phosphorStackFrame) {
        return getBooleanVolatile(unsafe, o, offset, phosphorStackFrame);
    }

    //Generated from Phosphor template for getAndAdd$methodType(Ljava/lang/Object;JLPlaceHolder;)LPlaceHolder;
    @SuppressWarnings("unused")
    public static int getAndAddInt(UnsafeProxy unsafe, Object o, long offset, int delta, PhosphorStackFrame phosphorStackFrame) {
        int v;
        do {
            v = getIntVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetInt(unsafe, o, offset, v, v + delta, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static byte getAndAddByte(UnsafeProxy unsafe, Object o, long offset, byte delta, PhosphorStackFrame phosphorStackFrame) {
        byte v;
        do {
            v = getByteVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetByte(unsafe, o, offset, v, (byte) (v + delta), phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static long getAndAddLong(UnsafeProxy unsafe, Object o, long offset, long delta, PhosphorStackFrame phosphorStackFrame) {
        long v;
        do {
            v = getLongVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetLong(unsafe, o, offset, v, v + delta, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static short getAndAddShort(UnsafeProxy unsafe, Object o, long offset, short delta, PhosphorStackFrame phosphorStackFrame) {
        short v;
        do {
            v = getShortVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetShort(unsafe, o, offset, v, (short) (v + delta), phosphorStackFrame));
        return v;
    }

    //Generated from Phosphor template for get$methodTypeVolatile(Ljava/lang/Object;J)LPlaceHolder;
    @SuppressWarnings("unused")
    public static byte getByteVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByteVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getByteVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static int getIntVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getIntVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getIntVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static long getLongVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLongVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getLongVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static float getFloatVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloatVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getFloatVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static Object getReferenceVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getReferenceVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getReferenceVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static char getCharVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getCharVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getCharVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static short getShortVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShortVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getShortVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static double getDoubleVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDoubleVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getDoubleVolatile(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static boolean getBooleanVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBooleanVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.VOLATILE);
            return unsafe.getBooleanVolatile(obj, offset);
        }
    }

    //Generated from Phosphor template for compareAndExchange$methodTypeRelease(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)LPlaceHolder;
    @SuppressWarnings("unused")
    public static int compareAndExchangeIntRelease(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static byte compareAndExchangeByteRelease(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static long compareAndExchangeLongRelease(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static Object compareAndExchangeReferenceRelease(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static short compareAndExchangeShortRelease(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndExchangeShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for weakCompareAndSet$methodTypeAcquire(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)Z
    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetIntAcquire(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetByteAcquire(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetLongAcquire(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetReferenceAcquire(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetShortAcquire(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for put$methodTypeOpaque(Ljava/lang/Object;JLPlaceHolder;)V
    @SuppressWarnings("unused")
    public static void putByteOpaque(UnsafeProxy unsafe, Object o, long offset, byte x, PhosphorStackFrame phosphorStackFrame) {
        putByteVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putIntOpaque(UnsafeProxy unsafe, Object o, long offset, int x, PhosphorStackFrame phosphorStackFrame) {
        putIntVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putLongOpaque(UnsafeProxy unsafe, Object o, long offset, long x, PhosphorStackFrame phosphorStackFrame) {
        putLongVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putFloatOpaque(UnsafeProxy unsafe, Object o, long offset, float x, PhosphorStackFrame phosphorStackFrame) {
        putFloatVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putReferenceOpaque(UnsafeProxy unsafe, Object o, long offset, Object x, PhosphorStackFrame phosphorStackFrame) {
        putReferenceVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putCharOpaque(UnsafeProxy unsafe, Object o, long offset, char x, PhosphorStackFrame phosphorStackFrame) {
        putCharVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putShortOpaque(UnsafeProxy unsafe, Object o, long offset, short x, PhosphorStackFrame phosphorStackFrame) {
        putShortVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putDoubleOpaque(UnsafeProxy unsafe, Object o, long offset, double x, PhosphorStackFrame phosphorStackFrame) {
        putDoubleVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putBooleanOpaque(UnsafeProxy unsafe, Object o, long offset, boolean x, PhosphorStackFrame phosphorStackFrame) {
        putBooleanVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for compareAndSet$methodType(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)Z
    @SuppressWarnings("unused")
    public static boolean compareAndSetInt(UnsafeProxy unsafe, Object obj, long offset, int expected, int value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedIntArray) {
            ret = unsafe.compareAndSetInt(((TaggedArray) obj).getVal(), offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndSetInt(obj, offset, expected, value);
            //TODO debugging and disabled this its probably ok though
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

    @SuppressWarnings("unused")
    public static boolean compareAndSetByte(UnsafeProxy unsafe, Object obj, long offset, byte expected, byte value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedByteArray) {
            ret = unsafe.compareAndSetByte(((TaggedArray) obj).getVal(), offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndSetByte(obj, offset, expected, value);
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

    @SuppressWarnings("unused")
    public static boolean compareAndSetLong(UnsafeProxy unsafe, Object obj, long offset, long expected, long value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedLongArray) {
            ret = unsafe.compareAndSetLong(((TaggedArray) obj).getVal(), offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
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

    @SuppressWarnings("unused")
    public static boolean compareAndSetReference(UnsafeProxy unsafe, Object obj, long offset, Object expected, Object value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedReferenceArray) {
            ret = unsafe.compareAndSetReference(((TaggedReferenceArray) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            OffsetPair pair = null;
            boolean didCAS = false;
            if (value instanceof TaggedArray || expected instanceof TaggedArray) {
                //Need to be careful - maybe we are hitting a 1D primitive array field
                if (obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    //We are doing a CAS on a 1d primitive array field
                    ret = unsafe.compareAndSetReference(obj, offset, MultiDArrayUtils.unbox1DOrNull(expected), MultiDArrayUtils.unbox1DOrNull(value));
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
                if(pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, value);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static boolean compareAndSetShort(UnsafeProxy unsafe, Object obj, long offset, short expected, short value, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedShortArray) {
            ret = unsafe.compareAndSetShort(((TaggedArray) obj).getVal(), offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndSetShort(obj, offset, expected, value);
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

    //Generated from Phosphor template for put$methodType(Ljava/lang/Object;JLPlaceHolder;)V
    @SuppressWarnings("unused")
    public static void putByte(UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putByte(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putInt(UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putInt(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putLong(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putLong(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putFloat(UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putFloat(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putReference(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putReference(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
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
                    unsafe.putReference(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putReference(obj, offset, val);
                }
            } else {
                unsafe.putReference(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    @SuppressWarnings("unused")
    public static void putChar(UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putChar(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putShort(UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putShort(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putDouble(UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putDouble(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putBoolean(UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putBoolean(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    //Generated from Phosphor template for put$methodTypeVolatile(Ljava/lang/Object;JLPlaceHolder;)V
    @SuppressWarnings("unused")
    public static void putByteVolatile(UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putByteVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByteVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putIntVolatile(UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putIntVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putIntVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putLongVolatile(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putLongVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLongVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putFloatVolatile(UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putFloatVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloatVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putReferenceVolatile(UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putReferenceVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
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
                    unsafe.putReferenceVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putReferenceVolatile(obj, offset, val);
                }
            } else {
                unsafe.putReferenceVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    @SuppressWarnings("unused")
    public static void putCharVolatile(UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putCharVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putCharVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putShortVolatile(UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putShortVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShortVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putDoubleVolatile(UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putDoubleVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDoubleVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    @SuppressWarnings("unused")
    public static void putBooleanVolatile(UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame phosphorStackFrame) {
        Taint valTaint = phosphorStackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putBooleanVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBooleanVolatile(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, SpecialAccessPolicy.NONE);
        }
    }

    //Generated from Phosphor template for compareAndExchange$methodType(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)LPlaceHolder;
    @SuppressWarnings("unused")
    public static int compareAndExchangeInt(UnsafeProxy unsafe, Object obj, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        int ret;
        if (obj instanceof TaggedIntArray) {
            ret = unsafe.compareAndExchangeInt(((TaggedArray) obj).getVal(), offset, expected, x);
            if (ret == expected) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndExchangeInt(obj, offset, expected, x);
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret == expected) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static byte compareAndExchangeByte(UnsafeProxy unsafe, Object obj, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        byte ret;
        if (obj instanceof TaggedByteArray) {
            ret = unsafe.compareAndExchangeByte(((TaggedArray) obj).getVal(), offset, expected, x);
            if (ret == expected) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndExchangeByte(obj, offset, expected, x);
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret == expected) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static long compareAndExchangeLong(UnsafeProxy unsafe, Object obj, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        long ret;
        if (obj instanceof TaggedLongArray) {
            ret = unsafe.compareAndExchangeLong(((TaggedArray) obj).getVal(), offset, expected, x);
            if (ret == expected) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndExchangeLong(obj, offset, expected, x);
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret == expected) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static Object compareAndExchangeReference(UnsafeProxy unsafe, Object obj, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        Object ret = null;
        if (obj instanceof TaggedReferenceArray) {
            ret = unsafe.compareAndExchangeReference(((TaggedArray) obj).getVal(), offset, expected, x);
            if (ret == expected) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            boolean didCAS = false;
            OffsetPair pair = null;
            if (x instanceof TaggedArray || expected instanceof TaggedArray) {
                //Need to be careful - maybe we are hitting a 1D primitive array field
                if (obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    //We are doing a CAS on a 1d primitive array field
                    ret = unsafe.compareAndExchangeReference(obj, offset, MultiDArrayUtils.unbox1DOrNull(expected), MultiDArrayUtils.unbox1DOrNull(x));
                    didCAS = true;
                }
            }
            if (!didCAS) {
                //Either this is not a wrapped array, or we are storing it to the place where it should be stored without unwrapping
                ret = unsafe.compareAndExchangeReference(obj, offset, expected, x);
                if (pair == null && obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
            }
            if (pair != null && ret == expected) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static short compareAndExchangeShort(UnsafeProxy unsafe, Object obj, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        Taint valueTaint = phosphorStackFrame.getArgTaint(4);
        Taint retTaint = Taint.emptyTaint();
        short ret;
        if (obj instanceof TaggedShortArray) {
            ret = unsafe.compareAndExchangeShort(((TaggedArray) obj).getVal(), offset, expected, x);
            if (ret == expected) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            ret = unsafe.compareAndExchangeShort(obj, offset, expected, x);
            OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret == expected) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putReferenceVolatile(obj, pair.tagFieldOffset, valueTaint);
                }
            }
        }
        return ret;
    }

    //Generated from Phosphor template for getAndSet$methodType(Ljava/lang/Object;JLPlaceHolder;)LPlaceHolder;
    @SuppressWarnings("unused")
    public static int getAndSetInt(UnsafeProxy unsafe, Object o, long offset, int newValue, PhosphorStackFrame phosphorStackFrame) {
        int v;
        do {
            v = getIntVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetInt(unsafe, o, offset, v, newValue, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static byte getAndSetByte(UnsafeProxy unsafe, Object o, long offset, byte newValue, PhosphorStackFrame phosphorStackFrame) {
        byte v;
        do {
            v = getByteVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetByte(unsafe, o, offset, v, newValue, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static long getAndSetLong(UnsafeProxy unsafe, Object o, long offset, long newValue, PhosphorStackFrame phosphorStackFrame) {
        long v;
        do {
            v = getLongVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetLong(unsafe, o, offset, v, newValue, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static Object getAndSetReference(UnsafeProxy unsafe, Object o, long offset, Object newValue, PhosphorStackFrame phosphorStackFrame) {
        Object v;
        do {
            v = getReferenceVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetReference(unsafe, o, offset, v, newValue, phosphorStackFrame));
        return v;
    }

    @SuppressWarnings("unused")
    public static short getAndSetShort(UnsafeProxy unsafe, Object o, long offset, short newValue, PhosphorStackFrame phosphorStackFrame) {
        short v;
        do {
            v = getShortVolatile(unsafe, o, offset, phosphorStackFrame);
        } while (!weakCompareAndSetShort(unsafe, o, offset, v, newValue, phosphorStackFrame));
        return v;
    }

    //Generated from Phosphor template for get$methodType(Ljava/lang/Object;J)LPlaceHolder;
    @SuppressWarnings("unused")
    public static byte getByte(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByte(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getByte(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static int getInt(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getInt(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getInt(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static long getLong(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLong(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getLong(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static float getFloat(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloat(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getFloat(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static Object getReference(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getReference(((TaggedArray) obj).getVal(), offset);
        } else {
            OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, phosphorStackFrame, RuntimeJDKInternalUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getReference(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static char getChar(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getChar(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getChar(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static short getShort(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShort(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getShort(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static double getDouble(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDouble(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getDouble(obj, offset);
        }
    }

    @SuppressWarnings("unused")
    public static boolean getBoolean(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedArray) {
            phosphorStackFrame.returnTaint = ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBoolean(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, phosphorStackFrame, SpecialAccessPolicy.NONE);
            return unsafe.getBoolean(obj, offset);
        }
    }

    //Generated from Phosphor template for weakCompareAndSet$methodTypePlain(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)Z
    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetIntPlain(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetBytePlain(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetLongPlain(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetReferencePlain(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetShortPlain(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for weakCompareAndSet$methodTypeRelease(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)Z
    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetIntRelease(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetByteRelease(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetLongRelease(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetReferenceRelease(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetShortRelease(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for weakCompareAndSet$methodType(Ljava/lang/Object;JLPlaceHolder;LPlaceHolder;)Z
    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetInt(UnsafeProxy unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetInt(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetByte(UnsafeProxy unsafe, Object o, long offset, byte expected, byte x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetByte(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetLong(UnsafeProxy unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetLong(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetReference(UnsafeProxy unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetReference(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static boolean weakCompareAndSetShort(UnsafeProxy unsafe, Object o, long offset, short expected, short x, PhosphorStackFrame phosphorStackFrame) {
        return compareAndSetShort(unsafe, o, offset, expected, x, phosphorStackFrame);
    }

    //Generated from Phosphor template for put$methodTypeRelease(Ljava/lang/Object;JLPlaceHolder;)V
    @SuppressWarnings("unused")
    public static void putByteRelease(UnsafeProxy unsafe, Object o, long offset, byte x, PhosphorStackFrame phosphorStackFrame) {
        putByteVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putIntRelease(UnsafeProxy unsafe, Object o, long offset, int x, PhosphorStackFrame phosphorStackFrame) {
        putIntVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putLongRelease(UnsafeProxy unsafe, Object o, long offset, long x, PhosphorStackFrame phosphorStackFrame) {
        putLongVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putFloatRelease(UnsafeProxy unsafe, Object o, long offset, float x, PhosphorStackFrame phosphorStackFrame) {
        putFloatVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putReferenceRelease(UnsafeProxy unsafe, Object o, long offset, Object x, PhosphorStackFrame phosphorStackFrame) {
        putReferenceVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putCharRelease(UnsafeProxy unsafe, Object o, long offset, char x, PhosphorStackFrame phosphorStackFrame) {
        putCharVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putShortRelease(UnsafeProxy unsafe, Object o, long offset, short x, PhosphorStackFrame phosphorStackFrame) {
        putShortVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putDoubleRelease(UnsafeProxy unsafe, Object o, long offset, double x, PhosphorStackFrame phosphorStackFrame) {
        putDoubleVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

    @SuppressWarnings("unused")
    public static void putBooleanRelease(UnsafeProxy unsafe, Object o, long offset, boolean x, PhosphorStackFrame phosphorStackFrame) {
        putBooleanVolatile(unsafe, o, offset, x, phosphorStackFrame);
    }

}
