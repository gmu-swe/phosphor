package edu.columbia.cs.psl.phosphor.runtime.mask;

import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.RuntimeSunMiscUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.UnsafeProxy;
import edu.columbia.cs.psl.phosphor.struct.TaggedArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedIntArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedLongArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;
import sun.misc.Unsafe;

import java.security.ProtectionDomain;

import static edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.RuntimeSunMiscUnsafePropagator.*;

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
        return null;
    }

    public static Object getObject(UnsafeProxy unsafe, Object o, long offset, PhosphorStackFrame frame) {
        if (o instanceof TaggedReferenceArray) {
            // Push the taint from the `offset` argument to the `idx` argument for get
            return ((TaggedReferenceArray) o)
                    .get(unsafeIndexFor(unsafe, (TaggedArray) o, offset), frame.getArgTaint(1), frame);
        } else {
            // Is this trying to return a field that is wrapped?
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = getOffsetPair(unsafe, o, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, o, offset, frame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getObject(o, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static Object getObjectVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return null;
    }

    public static Object getObjectVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedReferenceArray) {
            // Push the taint from the `offset` argument to the `idx` argument for get
            return ((TaggedReferenceArray) obj)
                    .get(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), stackFrame.getArgTaint(1), stackFrame);
        } else {
            // Is this trying to return a field that is wrapped?
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = getOffsetPair(unsafe, obj, offset);
            if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                offset = pair.wrappedFieldOffset;
            }
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getObjectVolatile(obj, offset);
        }
    }

    public static boolean compareAndSwapInt(
            UnsafeProxy unsafe,
            Object obj,
            long offset,
            int expected,
            int value,
            PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedIntArray) {
            ret = unsafe.compareAndSwapInt(((TaggedIntArray) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, phosphorStackFrame.getArgTaint(4));
            }
        } else {
            ret = unsafe.compareAndSwapInt(obj, offset, expected, value);
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(4));
                }
            }
        }
        return ret;
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapInt(
            Unsafe unsafe, Object o, long offset, int expected, int x, PhosphorStackFrame frame) {
        return false;
    }

    public static boolean compareAndSwapLong(
            UnsafeProxy unsafe,
            Object obj,
            long offset,
            long expected,
            long value,
            PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        boolean ret;
        if (obj instanceof TaggedLongArray) {
            ret = unsafe.compareAndSwapLong(((TaggedLongArray) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, phosphorStackFrame.getArgTaint(4));
            }
        } else {
            ret = unsafe.compareAndSwapLong(obj, offset, expected, value);
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(4));
                }
            }
        }
        return ret;
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapLong(
            Unsafe unsafe, Object o, long offset, long expected, long x, PhosphorStackFrame frame) {
        return false;
    }

    public static boolean compareAndSwapObject(
            UnsafeProxy unsafe, Object obj, long offset, Object expected, Object value, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        boolean ret = false;
        if (obj instanceof TaggedReferenceArray) {
            Taint<?> valueTaint = stackFrame.getArgTaint(4);
            ret = unsafe.compareAndSwapObject(((TaggedReferenceArray) obj).val, offset, expected, value);
            if (ret) {
                swapArrayElementTag(unsafe, (TaggedArray) obj, offset, valueTaint);
            }
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            boolean didCAS = false;
            if (value instanceof TaggedArray || expected instanceof TaggedArray) {
                // Need to be careful - maybe we are hitting a 1D primitive array field
                if (obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
                if (pair != null && pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    // We are doing a CAS on a 1d primitive array field
                    ret = unsafe.compareAndSwapObject(
                            obj,
                            offset,
                            MultiDArrayUtils.unbox1DOrNull(expected),
                            MultiDArrayUtils.unbox1DOrNull(value));
                    didCAS = true;
                }
            }
            if (!didCAS) {
                // Either this is not a wrapped array, or we are storing it to the place where it should be stored
                // without unwrapping
                ret = unsafe.compareAndSwapObject(obj, offset, expected, value);
                if (pair == null && obj != null) {
                    pair = getOffsetPair(unsafe, obj, offset);
                }
            }
            if (pair != null && ret) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, stackFrame.getArgTaint(4));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, value);
                }
            }
        }
        return ret;
    }

    @Mask(owner = Unsafe.class)
    public static boolean compareAndSwapObject(
            Unsafe unsafe, Object o, long offset, Object expected, Object x, PhosphorStackFrame frame) {
        return false;
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBoolean(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return false;
    }

    public static boolean getBoolean(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBoolean(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getBoolean(obj, offset);
        }
    }

    public static boolean getBooleanVolatile(
            UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getBooleanVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getBooleanVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static boolean getBooleanVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return false;
    }

    @Mask(owner = Unsafe.class)
    public static byte getByte(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static byte getByte(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByte(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getByte(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static byte getByte(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static byte getByteVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static byte getByteVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getByteVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getByteVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static char getChar(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static char getChar(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getChar(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getChar(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static char getChar(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static char getCharVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static char getCharVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getCharVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getCharVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static double getDouble(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static double getDouble(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDouble(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getDouble(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static double getDouble(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static double getDoubleVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static double getDoubleVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getDoubleVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getDoubleVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static float getFloat(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static float getFloat(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloat(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getFloat(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static float getFloat(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static float getFloatVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static float getFloatVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getFloatVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getFloatVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static int getInt(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static int getInt(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getInt(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getInt(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static int getInt(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static int getIntVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static int getIntVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getIntVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getIntVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static int getLoadAverage(Unsafe unsafe, double[] loadavg, int nelems, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static long getLong(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static long getLong(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLong(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getLong(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static long getLong(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static long getLongVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static long getLongVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getLongVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getLongVolatile(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static short getShort(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static short getShort(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShort(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
            return unsafe.getShort(obj, offset);
        }
    }

    @Mask(owner = Unsafe.class)
    public static short getShort(Unsafe unsafe, long address, PhosphorStackFrame frame) {
        return 0;
    }

    @Mask(owner = Unsafe.class)
    public static short getShortVolatile(Unsafe unsafe, Object o, long offset, PhosphorStackFrame frame) {
        return 0;
    }

    public static short getShortVolatile(UnsafeProxy unsafe, Object obj, long offset, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedArray) {
            stackFrame.returnTaint =
                    ((TaggedArray) obj).getTaintOrEmpty(unsafeIndexFor(unsafe, (TaggedArray) obj, offset));
            return unsafe.getShortVolatile(((TaggedArray) obj).getVal(), offset);
        } else {
            getTag(unsafe, obj, offset, stackFrame, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.VOLATILE);
            return unsafe.getShortVolatile(obj, offset);
        }
    }

    public static void copyMemory(
            UnsafeProxy unsafe,
            Object src,
            long srcAddress,
            Object dest,
            long destAddress,
            long length,
            PhosphorStackFrame stackFrame) {
        if (src instanceof TaggedArray) {
            src = ((TaggedArray) src).getVal();
        }
        if (dest instanceof TaggedArray) {
            dest = ((TaggedArray) dest).getVal();
        }
        unsafe.copyMemory(src, srcAddress, dest, destAddress, length);
    }

    public static void copyMemory(
            UnsafeProxy unsafe, long srcAddress, long destAddress, long length, PhosphorStackFrame stackFrame) {
        unsafe.copyMemory(srcAddress, destAddress, length);
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
        //
    }

    public void copyMemory(Unsafe unsafe, long var1, long var3, long var5, PhosphorStackFrame frame) {
        copyMemory(unsafe, null, var1, null, var3, var5, frame);
    }

    public static void putBoolean(
            UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putBoolean(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putBoolean(Unsafe unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        //
    }

    public static void putBooleanVolatile(
            UnsafeProxy unsafe, Object obj, long offset, boolean val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putBooleanVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putBoolean(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putBooleanVolatile(Unsafe unsafe, Object o, long offset, boolean x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putByte(Unsafe unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        //
    }

    public static void putByte(UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putByte(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putByte(Unsafe unsafe, long address, byte x, PhosphorStackFrame frame) {
        //
    }

    public static void putByteVolatile(
            UnsafeProxy unsafe, Object obj, long offset, byte val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putByteVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putByte(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putByteVolatile(Unsafe unsafe, Object o, long offset, byte x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putChar(Unsafe unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        //
    }

    public static void putChar(UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putChar(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putChar(Unsafe unsafe, long address, char x, PhosphorStackFrame frame) {
        //
    }

    public static void putCharVolatile(
            UnsafeProxy unsafe, Object obj, long offset, char val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putCharVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putChar(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putCharVolatile(Unsafe unsafe, Object o, long offset, char x, PhosphorStackFrame frame) {
        //
    }

    public static void putDouble(
            UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putDouble(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putDouble(Unsafe unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putDouble(Unsafe unsafe, long address, double x, PhosphorStackFrame frame) {
        //
    }

    public static void putDoubleVolatile(
            UnsafeProxy unsafe, Object obj, long offset, double val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putDoubleVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putDouble(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putDoubleVolatile(Unsafe unsafe, Object o, long offset, double x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putFloat(Unsafe unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        //
    }

    public static void putFloat(UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putFloat(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putFloat(Unsafe unsafe, long address, float x, PhosphorStackFrame frame) {
        //
    }

    public static void putFloatVolatile(
            UnsafeProxy unsafe, Object obj, long offset, float val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putFloatVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putFloat(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putFloatVolatile(Unsafe unsafe, Object o, long offset, float x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putInt(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        //
    }

    public static void putInt(UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putInt(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putInt(Unsafe unsafe, long address, int x, PhosphorStackFrame frame) {
        //
    }

    public static void putIntVolatile(
            UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putIntVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putIntVolatile(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putLong(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        //
    }

    public static void putLong(UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putLong(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putLong(Unsafe unsafe, long address, long x, PhosphorStackFrame frame) {
        //
    }

    public static void putLongVolatile(
            UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putLongVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putLongVolatile(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        //
    }

    public static void putObject(
            UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj)
                    .set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putObject(obj, offset, val);
                }
            } else {
                unsafe.putObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putObject(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        //
    }

    public static void putObjectVolatile(
            UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj)
                    .set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putObjectVolatile(obj, pair.wrappedFieldOffset, val);
                    unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putObjectVolatile(obj, offset, val);
                }
            } else {
                unsafe.putObjectVolatile(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putObjectVolatile(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        //
    }

    public static void putOrderedInt(
            UnsafeProxy unsafe, Object obj, long offset, int val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putOrderedInt(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putOrderedInt(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.ORDERED);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedInt(Unsafe unsafe, Object o, long offset, int x, PhosphorStackFrame frame) {
        //
    }

    public static void putOrderedLong(
            UnsafeProxy unsafe, Object obj, long offset, long val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putOrderedLong(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putOrderedLong(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.ORDERED);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedLong(Unsafe unsafe, Object o, long offset, long x, PhosphorStackFrame frame) {
        //
    }

    public static void putOrderedObject(
            UnsafeProxy unsafe, Object obj, long offset, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedReferenceArray) {
            ((TaggedReferenceArray) obj)
                    .set(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), val, phosphorStackFrame.getArgTaint(3));
        } else {
            RuntimeJDKInternalUnsafePropagator.OffsetPair pair = null;
            if (obj != null) {
                pair = getOffsetPair(unsafe, obj, offset);
            }
            if (pair != null) {
                if (pair.tagFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.tagFieldOffset, phosphorStackFrame.getArgTaint(3));
                }
                if (pair.wrappedFieldOffset != UnsafeProxy.INVALID_FIELD_OFFSET) {
                    unsafe.putOrderedObject(obj, pair.wrappedFieldOffset, val);
                    unsafe.putOrderedObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
                } else {
                    unsafe.putOrderedObject(obj, offset, val);
                }
            } else {
                unsafe.putOrderedObject(obj, offset, MultiDArrayUtils.unbox1DOrNull(val));
            }
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putOrderedObject(Unsafe unsafe, Object o, long offset, Object x, PhosphorStackFrame frame) {
        //
    }

    @Mask(owner = Unsafe.class)
    public static void putShort(Unsafe unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        //
    }

    public static void putShort(UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putShort(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putShort(Unsafe unsafe, long address, short x, PhosphorStackFrame frame) {
        //
    }

    public static void putShortVolatile(
            UnsafeProxy unsafe, Object obj, long offset, short val, PhosphorStackFrame stackFrame) {
        Taint<?> valTaint = stackFrame.getArgTaint(3);
        if (obj instanceof TaggedArray) {
            unsafe.putShortVolatile(((TaggedArray) obj).getVal(), offset, val);
            if ((valTaint != null && !valTaint.isEmpty()) || ((TaggedArray) obj).taints != null) {
                ((TaggedArray) obj).setTaint(unsafeIndexFor(unsafe, (TaggedArray) obj, offset), valTaint);
            }
        } else {
            unsafe.putShort(obj, offset, val);
            putTag(unsafe, obj, offset, valTaint, RuntimeSunMiscUnsafePropagator.SpecialAccessPolicy.NONE);
        }
    }

    @Mask(owner = Unsafe.class)
    public static void putShortVolatile(Unsafe unsafe, Object o, long offset, short x, PhosphorStackFrame frame) {
        //
    }
}