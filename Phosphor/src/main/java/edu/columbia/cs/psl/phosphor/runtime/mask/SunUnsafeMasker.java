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
    private static UnsafeAdapter adapter;

    @Mask(owner = Unsafe.class)
    public static Class<?> defineClass(
            Unsafe unsafe,
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
            return adapter.defineClass(name, instrumented, 0, instrumented.length, loader, protectionDomain);
        }
        return adapter.defineClass(name, b, off, len, loader, protectionDomain);
    }

    @Mask(owner = Unsafe.class)
    public static Object getObject(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return getObjectPolicy(o, offset, frame, SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static Object getObjectVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return getObjectPolicy(o, offset, frame, SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapInt(
            Unsafe unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame frame) {
        frame.returnTaint = Taint.emptyTaint();
        return swapTag(o, offset, frame, adapter.compareAndSwapInt(unwrap(o), offset, expected, x));
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapLong(
            Unsafe unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame frame) {
        frame.returnTaint = Taint.emptyTaint();
        return swapTag(o, offset, frame, adapter.compareAndSwapLong(unwrap(o), offset, expected, x));
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapObject(
            Unsafe unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame frame) {
        frame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if (o instanceof TaggedReferenceArray) {
            Taint<?> valueTaint = frame.getArgTaint(4);
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            ret = adapter.compareAndSwapObject(array.val, offset, expected, x);
            if (ret) {
                if (array.getVal() != null && array.getVal().getClass().isArray()) {
                    int index = getArrayIndex(adapter, (TaggedArray) o, offset);
                    array.setTaint(index, valueTaint);
                }
            }
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            boolean didCAS = false;
            if (x instanceof TaggedArray || expected instanceof TaggedArray) {
                // Need to be careful - maybe we are hitting a 1D primitive array field
                if (o != null) {
                    pair = getOffsetPair(adapter, o, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    // We are doing a CAS on a 1d primitive array field
                    ret = adapter.compareAndSwapObject(
                            o, offset, MultiDArrayUtils.unbox1DOrNull(expected), MultiDArrayUtils.unbox1DOrNull(x));
                    didCAS = true;
                }
            }
            if (!didCAS) {
                // Either this is not a wrapped array, or we are storing it to the place where it should be stored
                // without unwrapping
                ret = adapter.compareAndSwapObject(o, offset, expected, x);
                if (pair == null && o != null) {
                    pair = getOffsetPair(adapter, o, offset);
                }
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    adapter.putObjectVolatile(o, pair.tagFieldOffset, frame.getArgTaint(4));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    adapter.putObjectVolatile(o, pair.wrappedFieldOffset, x);
                }
            }
        }
        return ret;
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBoolean(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getBoolean(unwrap(o), offset);
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBooleanVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getBooleanVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static byte getByte(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getByte(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static byte getByteVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getByteVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static char getChar(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getChar(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static char getCharVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getCharVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static double getDouble(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getDouble(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static double getDoubleVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getDoubleVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static float getFloat(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getFloat(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static float getFloatVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getFloatVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static int getInt(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getInt(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static int getIntVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getIntVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static long getLong(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getLong(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static long getLongVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getLongVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static short getShort(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.NONE);
        return adapter.getShort(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static short getShortVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        getTagPrimitive(o, offset, frame, SpecialAccessPolicy.VOLATILE);
        return adapter.getShortVolatile(unwrap(0), offset);
    }

    @Mask(owner = Unsafe.class)
    public static void copyMemory(
            Unsafe unsafe,
            Object srcBase,
            long srcOffset,
            Object destBase,
            long destOffset,
            long bytes,
            PhosphorStackFrame frame) {
        adapter.copyMemory(unwrap(srcBase), srcOffset, unwrap(destBase), destOffset, bytes);
    }

    @Mask(owner = Unsafe.class)
    public static void copyMemory(
            Unsafe unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame frame) {
        adapter.copyMemory(srcAddress, destAddress, length);
    }

    @Mask(owner = Unsafe.class)
    public static void putBoolean(Unsafe unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        adapter.putBoolean(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putBooleanVolatile(Unsafe unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        adapter.putBooleanVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putByte(Unsafe unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        adapter.putByte(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putByteVolatile(Unsafe unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        adapter.putByteVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putChar(Unsafe unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        adapter.putChar(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putCharVolatile(Unsafe unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        adapter.putCharVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putDouble(Unsafe unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        adapter.putDouble(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putDoubleVolatile(Unsafe unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        adapter.putDoubleVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putFloat(Unsafe unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        adapter.putFloat(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putFloatVolatile(Unsafe unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        adapter.putFloatVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putInt(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        adapter.putInt(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putIntVolatile(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        adapter.putIntVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putLong(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        adapter.putLong(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putLongVolatile(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        adapter.putLongVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putObject(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObject(o, offset, x, frame, SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putObjectVolatile(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObject(o, offset, x, frame, SpecialAccessPolicy.VOLATILE);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedInt(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        adapter.putOrderedInt(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedLong(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        adapter.putOrderedLong(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedObject(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        putObject(o, offset, x, frame, SpecialAccessPolicy.ORDERED);
    }

    @Mask(owner = Unsafe.class)
    public static void putShort(Unsafe unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        adapter.putShort(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.NONE);
    }

    @Mask(owner = Unsafe.class)
    public static void putShortVolatile(Unsafe unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        adapter.putShortVolatile(unwrap(o), offset, x);
        putTagPrimitive(o, offset, frame.getArgTaint(3), SpecialAccessPolicy.VOLATILE);
    }

    private static void getTagPrimitive(Object o, long offset, PhosphorStackFrame frame, SpecialAccessPolicy policy) {
        if (o instanceof TaggedArray) {
            TaggedArray array = (TaggedArray) o;
            int index = getArrayIndex(adapter, array, offset);
            frame.returnTaint = array.getTaintOrEmpty(index);
        } else {
            getTag(adapter, o, offset, frame, policy);
        }
    }

    private static void putTagPrimitive(Object o, long offset, Taint<?> valTaint, SpecialAccessPolicy policy) {
        if (o instanceof TaggedArray) {
            TaggedArray array = (TaggedArray) o;
            int index = getArrayIndex(adapter, array, offset);
            array.setTaint(index, valTaint);
        } else {
            putTag(adapter, o, offset, valTaint, policy);
        }
    }

    private static void putObject(Object o, long offset, Object x, SpecialAccessPolicy policy) {
        switch (policy) {
            case NONE:
                adapter.putObject(o, offset, x);
                return;
            case VOLATILE:
                adapter.putObjectVolatile(o, offset, x);
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static Object getObjectPolicy(Object o, long offset, PhosphorStackFrame frame, SpecialAccessPolicy policy) {
        if (o instanceof TaggedReferenceArray) {
            // Push the taint from the `offset` argument to the `idx` argument for get
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            int index = getArrayIndex(adapter, array, offset);
            return array.get(index, frame.getArgTaint(1), frame);
        } else {
            // Is this trying to return a field that is wrapped?
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = getOffsetPair(adapter, o, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(adapter, o, offset, frame, policy);
            switch (policy) {
                case NONE:
                    return adapter.getObject(o, offset);
                case VOLATILE:
                    return adapter.getObjectVolatile(o, offset);
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private static void putObject(
            Object o, long offset, Object x, PhosphorStackFrame frame, SpecialAccessPolicy policy) {
        if (o instanceof TaggedReferenceArray) {
            TaggedReferenceArray array = (TaggedReferenceArray) o;
            int index = getArrayIndex(adapter, array, offset);
            array.set(index, x, frame.getArgTaint(3));
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (o != null) {
                pair = getOffsetPair(adapter, o, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    putObject(o, pair.tagFieldOffset, frame.getArgTaint(3), policy);
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    putObject(o, pair.wrappedFieldOffset, x, policy);
                    putObject(o, offset, MultiDArrayUtils.unbox1DOrNull(x), policy);
                } else {
                    putObject(o, offset, x, policy);
                }
            } else {
                putObject(o, offset, MultiDArrayUtils.unbox1DOrNull(x), policy);
            }
        }
    }

    private static boolean swapTag(Object o, long offset, PhosphorStackFrame frame, boolean swap) {
        if (swap) {
            if (o instanceof TaggedArray) {
                Taint<?> valueTaint = frame.getArgTaint(4);
                TaggedArray array = (TaggedArray) o;
                if (array.getVal() != null && array.getVal().getClass().isArray()) {
                    int index = getArrayIndex(adapter, array, offset);
                    array.setTaint(index, valueTaint);
                }
            } else if (o != null) {
                RuntimeJDKInternalUnsafePropagator.OffsetPair pair = getOffsetPair(adapter, o, offset);
                if (pair != null) {
                    if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                        adapter.putObjectVolatile(o, pair.tagFieldOffset, frame.getArgTaint(4));
                    }
                }
            }
        }
        return swap;
    }

    private static Object unwrap(Object object) {
        return object instanceof TaggedArray ? ((TaggedArray) object).getVal() : object;
    }
}