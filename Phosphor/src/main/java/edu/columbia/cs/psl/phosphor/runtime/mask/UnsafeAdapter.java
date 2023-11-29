package edu.columbia.cs.psl.phosphor.runtime.mask;

import java.lang.reflect.Field;
import java.security.ProtectionDomain;

@SuppressWarnings("unused")
public interface UnsafeAdapter {
    long getInvalidFieldOffset();

    int getInt(Object o, long offset);

    void putInt(Object o, long offset, int x);

    boolean getBoolean(Object o, long offset);

    void putBoolean(Object o, long offset, boolean x);

    byte getByte(Object o, long offset);

    void putByte(Object o, long offset, byte x);

    short getShort(Object o, long offset);

    void putShort(Object o, long offset, short x);

    char getChar(Object o, long offset);

    void putChar(Object o, long offset, char x);

    long getLong(Object o, long offset);

    void putLong(Object o, long offset, long x);

    float getFloat(Object o, long offset);

    void putFloat(Object o, long offset, float x);

    double getDouble(Object o, long offset);

    void putDouble(Object o, long offset, double x);

    byte getByte(long address);

    short getShort(long address);

    char getChar(long address);

    int getInt(long address);

    long getLong(long address);

    float getFloat(long address);

    double getDouble(long address);

    void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);

    void copyMemory(long srcAddress, long destAddress, long bytes);

    long objectFieldOffset(Field f);

    long staticFieldOffset(Field f);

    int arrayBaseOffset(Class<?> arrayClass);

    int arrayIndexScale(Class<?> arrayClass);

    Class<?> defineClass(
            String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain);

    int getIntVolatile(Object o, long offset);

    void putIntVolatile(Object o, long offset, int x);

    boolean getBooleanVolatile(Object o, long offset);

    void putBooleanVolatile(Object o, long offset, boolean x);

    byte getByteVolatile(Object o, long offset);

    void putByteVolatile(Object o, long offset, byte x);

    short getShortVolatile(Object o, long offset);

    void putShortVolatile(Object o, long offset, short x);

    char getCharVolatile(Object o, long offset);

    void putCharVolatile(Object o, long offset, char x);

    long getLongVolatile(Object o, long offset);

    void putLongVolatile(Object o, long offset, long x);

    float getFloatVolatile(Object o, long offset);

    void putFloatVolatile(Object o, long offset, float x);

    double getDoubleVolatile(Object o, long offset);

    void putDoubleVolatile(Object o, long offset, double x);

    Object getObject(Object o, long offset);

    Object getObjectVolatile(Object o, long offset);

    void putObject(Object o, long offset, Object x);

    void putObjectVolatile(Object o, long offset, Object x);

    boolean compareAndSwapObject(Object obj, long offset, Object expected, Object value);

    void putOrderedObject(Object obj, long offset, Object val);

    boolean compareAndSwapInt(Object val, long offset, int expected, int value);

    boolean compareAndSwapLong(Object val, long offset, long expected, long value);

    void putOrderedInt(Object obj, long offset, int val);

    void putOrderedLong(Object obj, long offset, long val);
}
