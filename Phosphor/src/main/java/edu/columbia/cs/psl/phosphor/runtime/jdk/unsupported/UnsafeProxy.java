package edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported;

import java.lang.reflect.Field;
import java.security.ProtectionDomain;

/**
 * We need to be able to compile Phosphor targeting Java 8 bytecode, but we also need to be able to be
 * forwards-compatible with Java 9+. Java 8 uses sun.misc.Unsafe, and 9+ refactors to jdk.internal.misc.Unsafe, which
 * is not exported to other modules. Our fix is to compile against this class, and then when Phosphor gets packaged into
 * a Java 9+ JRE, we rewrite the calls ot go to jdk.internal.misc.Unsafe.
 */
@SuppressWarnings("unused")
public class UnsafeProxy {

    public int getCount() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static UnsafeProxy getUnsafe() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native int getInt(Object o, long offset);

    public native void putInt(Object o, long offset, int x);

    public native Object getReference(Object o, long offset);

    public native void putReference(Object o, long offset, Object x);

    public native boolean getBoolean(Object o, long offset);

    public native void putBoolean(Object o, long offset, boolean x);

    public native byte getByte(Object o, long offset);

    public native void putByte(Object o, long offset, byte x);

    public native short getShort(Object o, long offset);

    public native void putShort(Object o, long offset, short x);

    public native char getChar(Object o, long offset);

    public native void putChar(Object o, long offset, char x);

    public native long getLong(Object o, long offset);

    public native void putLong(Object o, long offset, long x);

    public native float getFloat(Object o, long offset);

    public native void putFloat(Object o, long offset, float x);

    public native double getDouble(Object o, long offset);

    public native void putDouble(Object o, long offset, double x);

    public long getAddress(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putAddress(Object o, long offset, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native Object getUncompressedObject(long address);

    public byte getByte(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putByte(long address, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public short getShort(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putShort(long address, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public char getChar(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putChar(long address, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getInt(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putInt(long address, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long getLong(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putLong(long address, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public float getFloat(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putFloat(long address, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public double getDouble(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putDouble(long address, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long getAddress(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void putAddress(long address, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long allocateMemory(long bytes) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long reallocateMemory(long address, long bytes) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void setMemory(Object o, long offset, long bytes, byte value) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void setMemory(long address, long bytes, byte value) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void copySwapMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes, long elemSize) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void copySwapMemory(long srcAddress, long destAddress, long bytes, long elemSize) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void freeMemory(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void writebackMemory(long address, long length) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static int INVALID_FIELD_OFFSET = -1;

    public long objectFieldOffset(Field f) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long objectFieldOffset(Class<?> c, String name) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long staticFieldOffset(Field f) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public Object staticFieldBase(Field f) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public boolean shouldBeInitialized(Class<?> c) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void ensureClassInitialized(Class<?> c) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int arrayBaseOffset(Class<?> arrayClass) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static int ARRAY_BOOLEAN_BASE_OFFSET = 0;
    public static int ARRAY_BYTE_BASE_OFFSET = 0;
    public static int ARRAY_SHORT_BASE_OFFSET = 0;
    public static int ARRAY_CHAR_BASE_OFFSET = 0;
    public static int ARRAY_INT_BASE_OFFSET = 0;
    public static int ARRAY_LONG_BASE_OFFSET = 0;
    public static int ARRAY_FLOAT_BASE_OFFSET = 0;
    public static int ARRAY_DOUBLE_BASE_OFFSET = 0;
    public static int ARRAY_OBJECT_BASE_OFFSET = 0;

    public int arrayIndexScale(Class<?> arrayClass) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static int ARRAY_BOOLEAN_INDEX_SCALE = 0;
    public static int ARRAY_BYTE_INDEX_SCALE = 0;
    public static int ARRAY_SHORT_INDEX_SCALE = 0;
    public static int ARRAY_CHAR_INDEX_SCALE = 0;
    public static int ARRAY_INT_INDEX_SCALE = 0;
    public static int ARRAY_LONG_INDEX_SCALE = 0;
    public static int ARRAY_FLOAT_INDEX_SCALE = 0;
    public static int ARRAY_DOUBLE_INDEX_SCALE = 0;
    public static int ARRAY_OBJECT_INDEX_SCALE = 0;

    public int addressSize() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static int ADDRESS_SIZE = 0;

    public int pageSize() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int dataCacheLineFlushSize() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public long dataCacheLineAlignDown(long address) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static boolean isWritebackEnabled() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native Class<?> defineClass0(String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain);

    public Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native Object allocateInstance(Class<?> cls);

    public Object allocateUninitializedArray(Class<?> componentType, int length) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native void throwException(Throwable ee);

    public final native boolean compareAndSetReference(Object o, long offset, Object expected, Object x);

    public final native Object compareAndExchangeReference(Object o, long offset, Object expected, Object x);

    public final Object compareAndExchangeReferenceAcquire(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object compareAndExchangeReferenceRelease(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetReferencePlain(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetReferenceAcquire(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetReferenceRelease(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetReference(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final native boolean compareAndSetInt(Object o, long offset, int expected, int x);

    public final native int compareAndExchangeInt(Object o, long offset, int expected, int x);

    public int compareAndExchangeIntAcquire(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int compareAndExchangeIntRelease(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetIntPlain(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetIntAcquire(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetIntRelease(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetInt(Object o, long offset, int expected, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte compareAndExchangeByte(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetByte(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetByte(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetByteAcquire(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetByteRelease(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetBytePlain(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte compareAndExchangeByteAcquire(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte compareAndExchangeByteRelease(Object o, long offset, byte expected, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short compareAndExchangeShort(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetShort(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetShort(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetShortAcquire(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetShortRelease(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetShortPlain(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short compareAndExchangeShortAcquire(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short compareAndExchangeShortRelease(Object o, long offset, short expected, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetChar(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char compareAndExchangeChar(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char compareAndExchangeCharAcquire(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char compareAndExchangeCharRelease(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetChar(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetCharAcquire(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetCharRelease(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetCharPlain(Object o, long offset, char expected, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetBoolean(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndExchangeBoolean(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndExchangeBooleanAcquire(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndExchangeBooleanRelease(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetBoolean(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetBooleanAcquire(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetBooleanRelease(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetBooleanPlain(Object o, long offset, boolean expected, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetFloat(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float compareAndExchangeFloat(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float compareAndExchangeFloatAcquire(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float compareAndExchangeFloatRelease(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetFloatPlain(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetFloatAcquire(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetFloatRelease(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetFloat(Object o, long offset, float expected, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetDouble(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double compareAndExchangeDouble(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double compareAndExchangeDoubleAcquire(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double compareAndExchangeDoubleRelease(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetDoublePlain(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetDoubleAcquire(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetDoubleRelease(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetDouble(Object o, long offset, double expected, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final native boolean compareAndSetLong(Object o, long offset, long expected, long x);

    public final native long compareAndExchangeLong(Object o, long offset, long expected, long x);

    public final long compareAndExchangeLongAcquire(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long compareAndExchangeLongRelease(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetLongPlain(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetLongAcquire(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetLongRelease(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetLong(Object o, long offset, long expected, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native Object getReferenceVolatile(Object o, long offset);

    public native void putReferenceVolatile(Object o, long offset, Object x);

    public native int getIntVolatile(Object o, long offset);

    public native void putIntVolatile(Object o, long offset, int x);

    public native boolean getBooleanVolatile(Object o, long offset);

    public native void putBooleanVolatile(Object o, long offset, boolean x);

    public native byte getByteVolatile(Object o, long offset);

    public native void putByteVolatile(Object o, long offset, byte x);

    public native short getShortVolatile(Object o, long offset);

    public native void putShortVolatile(Object o, long offset, short x);

    public native char getCharVolatile(Object o, long offset);

    public native void putCharVolatile(Object o, long offset, char x);

    public native long getLongVolatile(Object o, long offset);

    public native void putLongVolatile(Object o, long offset, long x);

    public native float getFloatVolatile(Object o, long offset);

    public native void putFloatVolatile(Object o, long offset, float x);

    public native double getDoubleVolatile(Object o, long offset);

    public native void putDoubleVolatile(Object o, long offset, double x);

    public final Object getReferenceAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getBooleanAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getByteAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getShortAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getCharAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getIntAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getFloatAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getLongAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getDoubleAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putReferenceRelease(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putBooleanRelease(Object o, long offset, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putByteRelease(Object o, long offset, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putShortRelease(Object o, long offset, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putCharRelease(Object o, long offset, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putIntRelease(Object o, long offset, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putFloatRelease(Object o, long offset, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putLongRelease(Object o, long offset, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putDoubleRelease(Object o, long offset, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getReferenceOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getBooleanOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getByteOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getShortOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getCharOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getIntOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getFloatOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getLongOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getDoubleOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putReferenceOpaque(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putBooleanOpaque(Object o, long offset, boolean x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putByteOpaque(Object o, long offset, byte x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putShortOpaque(Object o, long offset, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putCharOpaque(Object o, long offset, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putIntOpaque(Object o, long offset, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putFloatOpaque(Object o, long offset, float x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putLongOpaque(Object o, long offset, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putDoubleOpaque(Object o, long offset, double x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native void unpark(Object thread);

    public native void park(boolean isAbsolute, long time);

    public int getLoadAverage(double[] loadavg, int nelems) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndAddInt(Object o, long offset, int delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndAddIntRelease(Object o, long offset, int delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndAddIntAcquire(Object o, long offset, int delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndAddLong(Object o, long offset, long delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndAddLongRelease(Object o, long offset, long delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndAddLongAcquire(Object o, long offset, long delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndAddByte(Object o, long offset, byte delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndAddByteRelease(Object o, long offset, byte delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndAddByteAcquire(Object o, long offset, byte delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndAddShort(Object o, long offset, short delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndAddShortRelease(Object o, long offset, short delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndAddShortAcquire(Object o, long offset, short delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndAddChar(Object o, long offset, char delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndAddCharRelease(Object o, long offset, char delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndAddCharAcquire(Object o, long offset, char delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndAddFloat(Object o, long offset, float delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndAddFloatRelease(Object o, long offset, float delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndAddFloatAcquire(Object o, long offset, float delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndAddDouble(Object o, long offset, double delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndAddDoubleRelease(Object o, long offset, double delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndAddDoubleAcquire(Object o, long offset, double delta) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndSetInt(Object o, long offset, int newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndSetIntRelease(Object o, long offset, int newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndSetIntAcquire(Object o, long offset, int newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndSetLong(Object o, long offset, long newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndSetLongRelease(Object o, long offset, long newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndSetLongAcquire(Object o, long offset, long newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetReference(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetReferenceRelease(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetReferenceAcquire(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndSetByte(Object o, long offset, byte newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndSetByteRelease(Object o, long offset, byte newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndSetByteAcquire(Object o, long offset, byte newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndSetBoolean(Object o, long offset, boolean newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndSetBooleanRelease(Object o, long offset, boolean newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndSetBooleanAcquire(Object o, long offset, boolean newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndSetShort(Object o, long offset, short newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndSetShortRelease(Object o, long offset, short newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndSetShortAcquire(Object o, long offset, short newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndSetChar(Object o, long offset, char newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndSetCharRelease(Object o, long offset, char newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndSetCharAcquire(Object o, long offset, char newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndSetFloat(Object o, long offset, float newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndSetFloatRelease(Object o, long offset, float newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final float getAndSetFloatAcquire(Object o, long offset, float newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndSetDouble(Object o, long offset, double newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndSetDoubleRelease(Object o, long offset, double newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final double getAndSetDoubleAcquire(Object o, long offset, double newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseOrBoolean(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseOrBooleanRelease(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseOrBooleanAcquire(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseAndBoolean(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseAndBooleanRelease(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseAndBooleanAcquire(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseXorBoolean(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseXorBooleanRelease(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean getAndBitwiseXorBooleanAcquire(Object o, long offset, boolean mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseOrByte(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseOrByteRelease(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseOrByteAcquire(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseAndByte(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseAndByteRelease(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseAndByteAcquire(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseXorByte(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseXorByteRelease(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final byte getAndBitwiseXorByteAcquire(Object o, long offset, byte mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseOrChar(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseOrCharRelease(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseOrCharAcquire(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseAndChar(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseAndCharRelease(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseAndCharAcquire(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseXorChar(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseXorCharRelease(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getAndBitwiseXorCharAcquire(Object o, long offset, char mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseOrShort(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseOrShortRelease(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseOrShortAcquire(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseAndShort(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseAndShortRelease(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseAndShortAcquire(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseXorShort(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseXorShortRelease(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getAndBitwiseXorShortAcquire(Object o, long offset, short mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseOrInt(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseOrIntRelease(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseOrIntAcquire(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseAndInt(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseAndIntRelease(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseAndIntAcquire(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseXorInt(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseXorIntRelease(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getAndBitwiseXorIntAcquire(Object o, long offset, int mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseOrLong(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseOrLongRelease(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseOrLongAcquire(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseAndLong(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseAndLongRelease(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseAndLongAcquire(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseXorLong(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseXorLongRelease(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getAndBitwiseXorLongAcquire(Object o, long offset, long mask) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public native void loadFence();

    public native void storeFence();

    public native void fullFence();

    public final void loadLoadFence() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void storeStoreFence() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean isBigEndian() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean unalignedAccess() {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getLongUnaligned(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final long getLongUnaligned(Object o, long offset, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getIntUnaligned(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public int getIntUnaligned(Object o, long offset, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getShortUnaligned(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final short getShortUnaligned(Object o, long offset, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getCharUnaligned(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final char getCharUnaligned(Object o, long offset, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putLongUnaligned(Object o, long offset, long x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putLongUnaligned(Object o, long offset, long x, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putIntUnaligned(Object o, long offset, int x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putIntUnaligned(Object o, long offset, int x, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putShortUnaligned(Object o, long offset, short x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putShortUnaligned(Object o, long offset, short x, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putCharUnaligned(Object o, long offset, char x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putCharUnaligned(Object o, long offset, char x, boolean bigEndian) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getObject(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getObjectVolatile(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getObjectAcquire(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getObjectOpaque(Object o, long offset) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putObject(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putObjectVolatile(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putObjectOpaque(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final void putObjectRelease(Object o, long offset, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetObjectAcquire(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object getAndSetObjectRelease(Object o, long offset, Object newValue) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean compareAndSetObject(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object compareAndExchangeObject(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object compareAndExchangeObjectAcquire(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final Object compareAndExchangeObjectRelease(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetObject(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetObjectAcquire(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetObjectPlain(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public final boolean weakCompareAndSetObjectRelease(Object o, long offset, Object expected, Object x) {
        throw new IllegalStateException("This method should never be called at runtime");
    }

    public static long makeLong(byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public static long makeLong(short i0, short i1, short i2, short i3) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public static long makeLong(int i0, int i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public static int makeInt(short i0, short i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public static int makeInt(byte i0, byte i1, byte i2, byte i3) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public static short makeShort(byte i0, byte i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putLongParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putLongParts(Object o, long offset, short i0, short i1, short i2, short i3) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putLongParts(Object o, long offset, int i0, int i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putIntParts(Object o, long offset, short i0, short i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putIntParts(Object o, long offset, byte i0, byte i1, byte i2, byte i3) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
    public void putShortParts(Object o, long offset, byte i0, byte i1) {
        throw new IllegalStateException("This method should never be called at runtime");
    }
}
