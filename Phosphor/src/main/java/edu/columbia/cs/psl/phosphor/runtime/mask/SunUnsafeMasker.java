package edu.columbia.cs.psl.phosphor.runtime.mask;

import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaggedArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;
import sun.misc.Unsafe;

import java.security.ProtectionDomain;

import static edu.columbia.cs.psl.phosphor.runtime.mask.UnsafeMaskerHelper.*;

@SuppressWarnings("unused")
public final class SunUnsafeMasker {
    @Mask(owner = Unsafe.class)
    public static Class<?> defineAnonymousClass(Object unsafe, Class<?> hostClass, byte[] data, Object[] cpPatches,
                                                PhosphorStackFrame frame) {
        byte[] instrumented = Phosphor.instrumentClassBytesAnonymous(data);
        return ADAPTER.defineAnonymousClass(hostClass, instrumented, cpPatches);
    }

    @Mask(owner = Unsafe.class)
    public static Class<?> defineClass(
            Object unsafe,
            String name,
            byte[] b,
            int off,
            int len,
            ClassLoader loader,
            ProtectionDomain protectionDomain,
            PhosphorStackFrame frame) {
        if (b != null && off >= 0 && len >= 0 && off + len <= b.length) {
            byte[] buffer = new byte[len];
            System.arraycopy(b, off, buffer, 0, len);
            byte[] instrumented = Phosphor.instrumentClassBytes(buffer);
            return ADAPTER.defineClass(name, instrumented, 0, instrumented.length, loader, protectionDomain);
        }
        return ADAPTER.defineClass(name, b, off, len, loader, protectionDomain);
    }

    @Mask(owner = Unsafe.class)
    public static Object getObject(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return getObjectAndTag(o, offset, frame, SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static Object getObjectVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return getObjectAndTag(o, offset, frame, SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapInt(
            Object unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame frame) {
        boolean result = ADAPTER.compareAndSwapInt(unwrap(o), offset, expected, x);
        if (result) {
            putTag(o, offset, frame.getArgTaint(4), SpecialAccessPolicy.VOLATILE);
        }
        frame.returnTaint = Taint.emptyTaint();
        return result;
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapLong(
            Object unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame frame) {
        boolean result = ADAPTER.compareAndSwapLong(unwrap(o), offset, expected, x);
        if (result) {
            putTag(o, offset, frame.getArgTaint(4), SpecialAccessPolicy.VOLATILE);
        }
        frame.returnTaint = Taint.emptyTaint();
        return result;
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapObject(
            Object unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame frame) {
        frame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if (o instanceof TaggedReferenceArray) {
            Taint<?> valueTaint = frame.getArgTaint(4);
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            ret = ADAPTER.compareAndSwapObject(array.val, offset, expected, x);
            if (ret) {
                if (array.getVal() != null && array.getVal().getClass().isArray()) {
                    int index = getTaggedArrayIndex((TaggedArray) o, offset);
                    array.setTaint(index, valueTaint);
                }
            }
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            boolean didCAS = false;
            if (x instanceof TaggedArray || expected instanceof TaggedArray) {
                // Need to be careful - maybe we are hitting a 1D primitive array field
                if (o != null) {
                    pair = getOffsetPair(o, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    // We are doing a CAS on a 1d primitive array field
                    ret = ADAPTER.compareAndSwapObject(
                            o, offset, MultiDArrayUtils.unbox1DOrNull(expected), MultiDArrayUtils.unbox1DOrNull(x));
                    didCAS = true;
                }
            }
            if (!didCAS) {
                // Either this is not a wrapped array, or we are storing it to the place where it should be stored
                // without unwrapping
                ret = ADAPTER.compareAndSwapObject(o, offset, expected, x);
                if (pair == null && o != null) {
                    pair = getOffsetPair(o, offset);
                }
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    ADAPTER.putObjectVolatile(o, pair.tagFieldOffset, frame.getArgTaint(4));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    ADAPTER.putObjectVolatile(o, pair.wrappedFieldOffset, x);
                }
            }
        }
        return ret;
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBoolean(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getBoolean(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBooleanVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getBooleanVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static byte getByte(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getByte(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static byte getByteVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getByteVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static char getChar(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getChar(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static char getCharVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getCharVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static double getDouble(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getDouble(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static double getDoubleVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getDoubleVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static float getFloat(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getFloat(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static float getFloatVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getFloatVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static int getInt(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getInt(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static int getIntVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getIntVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static long getLong(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getLong(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static long getLongVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getLongVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static short getShort(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.NONE);
        return ADAPTER.getShort(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static short getShortVolatile(Object unsafe, Object o, long offset, PhosphorStackFrame frame) {
        frame.returnTaint = getTagPrimitive(o, offset, SpecialAccessPolicy.VOLATILE);
        return ADAPTER.getShortVolatile(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static void copyMemory(
            Object unsafe,
            Object srcBase,
            long srcOffset,
            Object destBase,
            long destOffset,
            long bytes,
            PhosphorStackFrame frame) {
        ADAPTER.copyMemory(unwrap(srcBase), srcOffset, unwrap(destBase), destOffset, bytes);
    }

    @Mask(owner = Unsafe.class)
    public static void copyMemory(
            Object unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame frame) {
        ADAPTER.copyMemory(srcAddress, destAddress, length);
    }

    @Mask(owner = Unsafe.class)
    public static void putBoolean(Object unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        ADAPTER.putBoolean(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putBooleanVolatile(Object unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        ADAPTER.putBooleanVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putByte(Object unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        ADAPTER.putByte(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putByteVolatile(Object unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        ADAPTER.putByteVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putChar(Object unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        ADAPTER.putChar(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putCharVolatile(Object unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        ADAPTER.putCharVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putDouble(Object unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        ADAPTER.putDouble(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putDoubleVolatile(Object unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        ADAPTER.putDoubleVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putFloat(Object unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        ADAPTER.putFloat(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putFloatVolatile(Object unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        ADAPTER.putFloatVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putInt(Object unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        ADAPTER.putInt(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putIntVolatile(Object unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        ADAPTER.putIntVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putLong(Object unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        ADAPTER.putLong(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putLongVolatile(Object unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        ADAPTER.putLongVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putObject(Object unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObjectAndTag(o, offset, x, frame, SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putObjectVolatile(Object unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObjectAndTag(o, offset, x, frame, SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedInt(Object unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        ADAPTER.putOrderedInt(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedLong(Object unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        ADAPTER.putOrderedLong(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedObject(Object unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObjectAndTag(o, offset, x, frame, SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putShort(Object unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        ADAPTER.putShort(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putShortVolatile(Object unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        ADAPTER.putShortVolatile(unwrap(o), offset, x);
        UnsafeMaskerHelper.putTag(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }
}