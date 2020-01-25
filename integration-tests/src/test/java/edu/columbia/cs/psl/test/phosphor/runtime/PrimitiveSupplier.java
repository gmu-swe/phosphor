package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

import java.io.Serializable;
import java.lang.reflect.Array;

/* Supplies primitives and primitive arrays to a holder. */
public class PrimitiveSupplier implements Serializable {

    private static final long serialVersionUID = 8493043490151333333L;

    private int i = 7;
    private long j = 9L;
    private boolean z = false;
    private short s = 18;
    private double d = 77.3;
    private byte b = 1;
    private char c = 'q';
    private float f = 6.13f;

    /* Returns an int that is tainted if taint is true. */
    public int getInt(boolean taint) {
        return taint ? MultiTainter.taintedInt(i, "int") : i;
    }

    /* Returns a long that is tainted if taint is true. */
    public long getLong(boolean taint) {
        return taint ? MultiTainter.taintedLong(j, "long") : j;
    }

    /* Returns a boolean that is tainted if taint is true. */
    public boolean getBoolean(boolean taint) {
        return taint ? MultiTainter.taintedBoolean(z, "boolean") : z;
    }

    /* Returns a short that is tainted if taint is true. */
    public short getShort(boolean taint) {
        return taint ? MultiTainter.taintedShort(s, "short") : s;
    }

    /* Returns a double that is tainted if taint is true. */
    public double getDouble(boolean taint) {
        return taint ? MultiTainter.taintedDouble(d, "double") : d;
    }

    /* Returns a byte that is tainted if taint is true. */
    public byte getByte(boolean taint) {
        return taint ? MultiTainter.taintedByte(b, "byte") : b;
    }

    /* Returns a char that is tainted if taint is true. */
    public char getChar(boolean taint) {
        return taint ? MultiTainter.taintedChar(c, "char") : c;
    }

    /* Returns a float that is tainted if taint is true. */
    public float getFloat(boolean taint) {
        return taint ? MultiTainter.taintedFloat(f, "float") : f;
    }

    /* Makes an array with the specified component type and number of dimensions. */
    private static Object makeArray(Class<?> componentType, int dims) {
        int[] lens = new int[dims];
        for(int i = 0; i < dims; i++) {
            lens[i] = 1;
        }
        return Array.newInstance(componentType, lens);
    }

    /* Returns the first 1D element of the specified array object. */
    private static Object get1D(Object arr) {
        while(arr.getClass().getComponentType().isArray()) {
            arr = Array.get(arr, 0);
        }
        return arr;
    }

    /* Returns a int array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getIntArray(boolean taint, int dims) {
        Object arr = makeArray(int.class, dims);
        Array.setInt(get1D(arr), 0, getInt(taint));
        return arr;
    }

    /* Returns a long array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getLongArray(boolean taint, int dims) {
        Object arr = makeArray(long.class, dims);
        Array.setLong(get1D(arr), 0, getLong(taint));
        return arr;
    }

    /* Returns a boolean array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getBooleanArray(boolean taint, int dims) {
        Object arr = makeArray(boolean.class, dims);
        Array.setBoolean(get1D(arr), 0, getBoolean(taint));
        return arr;
    }

    /* Returns a short array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getShortArray(boolean taint, int dims) {
        Object arr = makeArray(short.class, dims);
        Array.setShort(get1D(arr), 0, getShort(taint));
        return arr;
    }

    /* Returns a double array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getDoubleArray(boolean taint, int dims) {
        Object arr = makeArray(double.class, dims);
        Array.setDouble(get1D(arr), 0, getDouble(taint));
        return arr;
    }

    /* Returns a byte array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getByteArray(boolean taint, int dims) {
        Object arr = makeArray(byte.class, dims);
        Array.setByte(get1D(arr), 0, getByte(taint));
        return arr;
    }

    /* Returns a char array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getCharArray(boolean taint, int dims) {
        Object arr = makeArray(char.class, dims);
        Array.setChar(get1D(arr), 0, getChar(taint));
        return arr;
    }

    /* Returns a float array with the specified number of dimensions whose elements are tainted if taint is true. */
    public Object getFloatArray(boolean taint, int dims) {
        Object arr = makeArray(float.class, dims);
        Array.setFloat(get1D(arr), 0, getFloat(taint));
        return arr;
    }

    /* Returns an instance of the specified multi-dimensional primitive array type whose elements are tainted if taint is true. */
    public Object getArray(boolean taint, Class<?> clazz) {
        int dims = 0;
        for(; clazz.isArray(); clazz = clazz.getComponentType()) {
            dims++;
        }
        if(boolean.class.equals(clazz)) {
            return getBooleanArray(taint, dims);
        } else if(byte.class.equals(clazz)) {
            return getByteArray(taint, dims);
        } else if(char.class.equals(clazz)) {
            return getCharArray(taint, dims);
        } else if(double.class.equals(clazz)) {
            return getDoubleArray(taint, dims);
        } else if(float.class.equals(clazz)) {
            return getFloatArray(taint, dims);
        } else if(int.class.equals(clazz)) {
            return getIntArray(taint, dims);
        } else if(long.class.equals(clazz)) {
            return getLongArray(taint, dims);
        } else if(short.class.equals(clazz)) {
            return getShortArray(taint, dims);
        } else {
            throw new RuntimeException("getArray expects multi-dimensional primitive array type.");
        }
    }

    /* Returns an instance of the specified boxed primitive type or a boxed instance of the specified primitive type that
     * is tainted if taint is true. */
    public Object getBoxedPrimitive(boolean taint, Class<?> clazz) {
        if(boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return getBoolean(taint);
        } else if(byte.class.equals(clazz) || Byte.class.equals(clazz)) {
            return getByte(taint);
        } else if(char.class.equals(clazz) || Character.class.equals(clazz)) {
            return getChar(taint);
        } else if(double.class.equals(clazz) || Double.class.equals(clazz)) {
            return getDouble(taint);
        } else if(float.class.equals(clazz) || Float.class.equals(clazz)) {
            return getFloat(taint);
        } else if(int.class.equals(clazz) || Integer.class.equals(clazz)) {
            return getInt(taint);
        } else if(long.class.equals(clazz) || Long.class.equals(clazz)) {
            return getLong(taint);
        } else if(short.class.equals(clazz) || Short.class.equals(clazz)) {
            return getShort(taint);
        } else {
            throw new RuntimeException("getBoxedPrimitive expects primitive or boxed primitive type.");
        }
    }
}
