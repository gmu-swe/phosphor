package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

public class MultiDArrayUtils {

    public static final long serialVersionUID = 40523489234L;
    private static final String TAGGED_BOOLEAN_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedBooleanArray.class);
    private static final String TAGGED_BYTE_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedByteArray.class);
    private static final String TAGGED_CHAR_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedCharArray.class);
    private static final String TAGGED_DOUBLE_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedDoubleArray.class);
    private static final String TAGGED_FLOAT_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedFloatArray.class);
    private static final String TAGGED_INT_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedIntArray.class);
    private static final String TAGGED_LONG_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedLongArray.class);
    private static final String TAGGED_SHORT_ARRAY_INTERNAL_NAME = Type.getInternalName(TaggedShortArray.class);
    public int sort;


    private MultiDArrayUtils() {

    }

    public void setTaints(Object t) {
        throw new UnsupportedOperationException();
    }
    public static Type getTypeForType(final Type originalElementType) {
        if (originalElementType.getSort() != Type.ARRAY) {
            throw new IllegalArgumentException("passed " + originalElementType);
        }
        if (originalElementType.getDimensions() > 1) {
            return Type.getType(TaggedReferenceArray.class);
        } else {
            return Type.getType(getDescriptorForComponentType(originalElementType.getElementType().getSort()));
        }
    }

    public static String getDescriptorForComponentType(final int componentSort) {
        switch (componentSort) {
            case Type.BOOLEAN:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedBooleanArray;";
            case Type.BYTE:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedByteArray;";
            case Type.CHAR:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedCharArray;";
            case Type.DOUBLE:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedDoubleArray;";
            case Type.FLOAT:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedFloatArray;";
            case Type.INT:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedIntArray;";
            case Type.LONG:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedLongArray;";
            case Type.SHORT:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedShortArray;";
            case Type.OBJECT:
                return "Ledu/columbia/cs/psl/phosphor/struct/TaggedReferenceArray;";
            default:
                throw new IllegalArgumentException("invalid sort: " + componentSort);
        }
    }

    public static String isPrimitiveBoxClass(Class c) {
        while (c.isArray()) {
            c = c.getComponentType();
        }
        if (c == TaggedDoubleArray.class) {
            return "D";
        }
        if (c == TaggedFloatArray.class) {
            return "F";
        }
        if (c == TaggedIntArray.class) {
            return "I";
        }
        if (c == TaggedLongArray.class) {
            return "J";
        }
        if (c == TaggedShortArray.class) {
            return "S";
        }
        if (c == TaggedBooleanArray.class) {
            return "Z";
        }
        if (c == TaggedByteArray.class) {
            return "B";
        }
        if (c == TaggedCharArray.class) {
            return "C";
        }
        return null;
    }

    public static String getPrimitiveTypeForWrapper(Class c) {
        while (c.isArray()) {
            c = c.getComponentType();
        }
        if (c == TaggedDoubleArray.class) {
            return "D";
        }
        if (c == TaggedFloatArray.class) {
            return "F";
        }
        if (c == TaggedIntArray.class) {
            return "I";
        }
        if (c == TaggedLongArray.class) {
            return "J";
        }
        if (c == TaggedShortArray.class) {
            return "S";
        }
        if (c == TaggedBooleanArray.class) {
            return "Z";
        }
        if (c == TaggedByteArray.class) {
            return "B";
        }
        if (c == TaggedCharArray.class) {
            return "C";
        }
        if (c == TaggedReferenceArray.class) {
            return "Ljava/lang/Object;";
        }
        throw new IllegalStateException("Got passed class: " + c);

    }

    public static Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
        int dims = 0;
        if (c.isArray()) {
            while (c.isArray()) {
                c = c.getComponentType();
                dims++;
            }
        }
        if (dims == 1) {
            if (c == Double.TYPE) {
                return TaggedDoubleArray.class;
            }
            if (c == Float.TYPE) {
                return TaggedFloatArray.class;
            }
            if (c == Integer.TYPE) {
                return TaggedIntArray.class;
            }
            if (c == Long.TYPE) {
                return TaggedLongArray.class;
            }
            if (c == Short.TYPE) {
                return TaggedShortArray.class;
            }
            if (c == Boolean.TYPE) {
                return TaggedBooleanArray.class;
            }
            if (c == Byte.TYPE) {
                return TaggedByteArray.class;
            }
            if (c == Character.TYPE) {
                return TaggedCharArray.class;
            }
        }
        return TaggedReferenceArray.class;
    }

    public static Class getClassForComponentType(final int componentSort) {
        switch (componentSort) {
            case Type.BOOLEAN:
                return TaggedBooleanArray.class;
            case Type.BYTE:
                return TaggedByteArray.class;
            case Type.CHAR:
                return TaggedCharArray.class;
            case Type.DOUBLE:
                return TaggedDoubleArray.class;
            case Type.FLOAT:
                return TaggedFloatArray.class;
            case Type.INT:
                return TaggedIntArray.class;
            case Type.LONG:
                return TaggedLongArray.class;
            case Type.SHORT:
                return TaggedShortArray.class;
            case Type.OBJECT:
                return TaggedReferenceArray.class;
            default:
                throw new IllegalArgumentException("invalid sort: " + componentSort);
        }
    }

    public static Object unboxRaw(final Object in) {
        if (in == null) {
            return null;
        }
        // if(!in.getClass().isArray()) {
        //     return unboxVal(in, getSortForBoxClass(in.getClass()), 0);
        // }
        // Class tmp = in.getClass();
        // int dims = 0;
        // while(tmp.isArray()) {
        //     tmp = tmp.getComponentType();
        //     dims++;
        // }
        // return unboxVal(in, getSortForBoxClass(tmp), dims);
        return unboxRawOnly1D(in); //TODO can we get away without ever going to real multid arrays?
    }

    public static Object unboxRawOnly1D(final Object in) {
        if (in instanceof TaggedArray) {
            return ((TaggedArray) in).getVal();
        }
        return in;
    }

    /* If the specified object is a one dimensional array of primitives, boxes and returns the specified object. Otherwise
     * returns the specified object. */
    public static Object boxOnly1D(final Object obj) {
        if (obj instanceof boolean[]) {
            return new TaggedBooleanArray((boolean[]) obj);
        } else if (obj instanceof byte[]) {
            return new TaggedByteArray((byte[]) obj);
        } else if (obj instanceof char[]) {
            return new TaggedCharArray((char[]) obj);
        } else if (obj instanceof double[]) {
            return new TaggedDoubleArray((double[]) obj);
        } else if (obj instanceof float[]) {
            return new TaggedFloatArray((float[]) obj);
        } else if (obj instanceof int[]) {
            return new TaggedIntArray((int[]) obj);
        } else if (obj instanceof long[]) {
            return new TaggedLongArray((long[]) obj);
        } else if (obj instanceof short[]) {
            return new TaggedShortArray((short[]) obj);
        } else if (obj instanceof Object[]) {
            return new TaggedReferenceArray((Object[]) obj);
        } else {
            return obj;
        }
    }

    public static Object unboxVal(final Object _in, final int componentType, final int dims) {

        if (dims == 0) {
            switch (componentType) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.DOUBLE:
                case Type.FLOAT:
                case Type.INT:
                case Type.LONG:
                case Type.SHORT:
                    return ((TaggedArray) _in).getVal();
                default:
                    throw new IllegalArgumentException();
            }
        } else if (dims == 1) {
            final Object[] in = (Object[]) _in;
            switch (componentType) {
                case Type.BOOLEAN:
                    boolean[][] retz = new boolean[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retz[i] = ((TaggedBooleanArray) in[i]).val;
                        }
                    }
                    return retz;
                case Type.BYTE:
                    byte[][] retb = new byte[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retb[i] = ((TaggedByteArray) in[i]).val;
                        }
                    }
                    return retb;
                case Type.CHAR:
                    char[][] retc = new char[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retc[i] = ((TaggedCharArray) in[i]).val;
                        }
                    }
                    return retc;
                case Type.DOUBLE:
                    double[][] retd = new double[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retd[i] = ((TaggedDoubleArray) in[i]).val;
                        }
                    }
                    return retd;
                case Type.FLOAT:
                    float[][] retf = new float[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retf[i] = ((TaggedFloatArray) in[i]).val;
                        }
                    }
                    return retf;
                case Type.INT:
                    int[][] reti = new int[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            reti[i] = ((TaggedIntArray) in[i]).val;
                        }
                    }
                    return reti;
                case Type.LONG:
                    long[][] retl = new long[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            retl[i] = ((TaggedLongArray) in[i]).val;
                        }
                    }
                    return retl;
                case Type.SHORT:
                    short[][] rets = new short[in.length][];
                    for (int i = 0; i < in.length; i++) {
                        if (in[i] != null) {
                            rets[i] = ((TaggedShortArray) in[i]).val;
                        }
                    }
                    return rets;
            }
        } else if (dims == 2) {
            final Object[][] ina = (Object[][]) _in;
            final Object[] in = (Object[]) _in;
            switch (componentType) {
                case Type.BOOLEAN:
                    boolean[][][] retz = new boolean[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retz[i] = new boolean[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retz[i][j] = ((TaggedBooleanArray) ina[i][j]).val;
                        }
                    }
                    return retz;
                case Type.BYTE:
                    byte[][][] retb = new byte[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retb[i] = new byte[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retb[i][j] = ((TaggedByteArray) ina[i][j]).val;
                        }
                    }
                    return retb;
                case Type.CHAR:
                    char[][][] retc = new char[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retc[i] = new char[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retc[i][j] = ((TaggedCharArray) ina[i][j]).val;
                        }
                    }
                    return retc;
                case Type.DOUBLE:
                    double[][][] retd = new double[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retd[i] = new double[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retd[i][j] = ((TaggedDoubleArray) ina[i][j]).val;
                        }
                    }
                    return retd;
                case Type.FLOAT:
                    float[][][] retf = new float[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retf[i] = new float[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retf[i][j] = ((TaggedFloatArray) ina[i][j]).val;
                        }
                    }
                    return retf;
                case Type.INT:
                    int[][][] reti = new int[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        reti[i] = new int[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            reti[i][j] = ((TaggedIntArray) ina[i][j]).val;
                        }
                    }
                    return reti;
                case Type.LONG:
                    long[][][] retl = new long[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        retl[i] = new long[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            retl[i][j] = ((TaggedLongArray) ina[i][j]).val;
                        }
                    }
                    return retl;
                case Type.SHORT:
                    short[][][] rets = new short[in.length][][];
                    for (int i = 0; i < in.length; i++) {
                        rets[i] = new short[ina[i].length][];
                        for (int j = 0; j < ina[i].length; j++) {
                            rets[i][j] = ((TaggedShortArray) ina[i][j]).val;
                        }
                    }
                    return rets;
            }
        }
        throw new IllegalArgumentException();
    }

    public static int getSortForBoxClass(Class c) {
        if (c == TaggedIntArray.class) {
            return Type.INT;
        }
        if (c == TaggedBooleanArray.class) {
            return Type.BOOLEAN;
        }
        if (c == TaggedByteArray.class) {
            return Type.BYTE;
        }
        if (c == TaggedFloatArray.class) {
            return Type.FLOAT;
        }
        if (c == TaggedCharArray.class) {
            return Type.CHAR;
        }
        if (c == TaggedDoubleArray.class) {
            return Type.DOUBLE;
        }
        if (c == TaggedLongArray.class) {
            return Type.LONG;
        }
        if (c == TaggedShortArray.class) {
            return Type.SHORT;
        }
        throw new IllegalArgumentException();
    }

    public static int getSort(Class c) {
        if (c == Integer.TYPE) {
            return Type.INT;
        }
        if (c == Boolean.TYPE) {
            return Type.BOOLEAN;
        }
        if (c == Byte.TYPE) {
            return Type.BYTE;
        }
        if (c == Float.TYPE) {
            return Type.FLOAT;
        }
        if (c == Character.TYPE) {
            return Type.CHAR;
        }
        if (c == Double.TYPE) {
            return Type.DOUBLE;
        }
        if (c == Long.TYPE) {
            return Type.LONG;
        }
        if (c == Short.TYPE) {
            return Type.SHORT;
        }
        throw new IllegalArgumentException();
    }

    public static Object boxIfNecessary(final Object in, final HashSet<Object> done) {
        if (in != null && in.getClass().isArray()) {
            if (in.getClass().getComponentType().isPrimitive()) {
                //Is prim arraytype
                Class tmp = in.getClass();
                int dims = 0;
                while (tmp.isArray()) {
                    tmp = tmp.getComponentType();
                    dims++;
                }
                if (dims > 1) { //this should never be possible.
                    Type t = Type.getType(in.getClass());
                    initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
                } else {
                    if (tmp == Boolean.TYPE) {
                        return new TaggedBooleanArray((boolean[]) in);
                    }
                    if (tmp == Byte.TYPE) {
                        return new TaggedByteArray(((byte[]) in));
                    }
                    if (tmp == Character.TYPE) {
                        return new TaggedCharArray(((char[]) in));
                    }
                    if (tmp == Double.TYPE) {
                        return new TaggedDoubleArray(((double[]) in));
                    }
                    if (tmp == Float.TYPE) {
                        return new TaggedFloatArray(((float[]) in));
                    }
                    if (tmp == Integer.TYPE) {
                        return new TaggedIntArray(((int[]) in));
                    }
                    if (tmp == Long.TYPE) {
                        return new TaggedLongArray(((long[]) in));
                    }
                    if (tmp == Short.TYPE) {
                        return new TaggedShortArray(((short[]) in));
                    }
                    throw new IllegalArgumentException();
                }
            } else if (in.getClass().getComponentType().isArray() && in.getClass().getComponentType().getComponentType().isPrimitive()) {
                //THIS array is an prim[][] array
                Object[] _in = (Object[]) in;

                Class tmp = in.getClass();
                while (tmp.isArray()) {
                    tmp = tmp.getComponentType();
                }
                if (tmp == Boolean.TYPE) {
                    TaggedBooleanArray[] ret = new TaggedBooleanArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedBooleanArray((boolean[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Byte.TYPE) {
                    TaggedByteArray[] ret = new TaggedByteArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedByteArray((byte[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Character.TYPE) {
                    TaggedCharArray[] ret = new TaggedCharArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedCharArray((char[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Double.TYPE) {
                    TaggedDoubleArray[] ret = new TaggedDoubleArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedDoubleArray((double[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Float.TYPE) {
                    TaggedFloatArray[] ret = new TaggedFloatArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedFloatArray((float[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Integer.TYPE) {
                    TaggedIntArray[] ret = new TaggedIntArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedIntArray((int[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Short.TYPE) {
                    TaggedShortArray[] ret = new TaggedShortArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedShortArray((short[]) _in[i]);
                    }
                    return ret;
                }
                if (tmp == Long.TYPE) {
                    TaggedLongArray[] ret = new TaggedLongArray[_in.length];
                    for (int i = 0; i < _in.length; i++) {
                        ret[i] = new TaggedLongArray((long[]) _in[i]);
                    }
                    return ret;
                }
                throw new UnsupportedOperationException();
            } else if (in.getClass().getComponentType().getName().equals("java.lang.Object")) {
                Object[] _in = (Object[]) in;
                for (int i = 0; i < _in.length; i++) {
                    if (done.add(_in[i])) {
                        _in[i] = boxIfNecessary(_in[i], done);
                    }
                }
            }
        }
        return in;
    }

    private static Object _boxIfNecessary(final Object in) {
        if (!in.getClass().getComponentType().isArray()) {
            //Is prim arraytype
            Class tmp = in.getClass();
            int dims = 0;
            while (tmp.isArray()) {
                tmp = tmp.getComponentType();
                dims++;
            }
            if (dims > 1) { //this should never be possible.
                Type t = Type.getType(in.getClass());
                initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
            } else {
                if (tmp == Boolean.TYPE) {
                    return new TaggedBooleanArray((boolean[]) in);
                }
                if (tmp == Byte.TYPE) {
                    return new TaggedByteArray(((byte[]) in));
                }
                if (tmp == Character.TYPE) {
                    return new TaggedCharArray(((char[]) in));
                }
                if (tmp == Double.TYPE) {
                    return new TaggedDoubleArray(((double[]) in));
                }
                if (tmp == Float.TYPE) {
                    return new TaggedFloatArray(((float[]) in));
                }
                if (tmp == Integer.TYPE) {
                    return new TaggedIntArray(((int[]) in));
                }
                if (tmp == Long.TYPE) {
                    return new TaggedLongArray(((long[]) in));
                }
                if (tmp == Short.TYPE) {
                    return new TaggedShortArray(((short[]) in));
                }
                return new TaggedReferenceArray((Object[]) in);
            }
        } else if (in.getClass().getComponentType().isArray() && in.getClass().getComponentType().getComponentType().isPrimitive()) {
            //THIS array is an prim[][] array
            Object[] _in = (Object[]) in;

            Class tmp = in.getClass();
            while (tmp.isArray()) {
                tmp = tmp.getComponentType();
            }
            if (tmp == Boolean.TYPE) {
                TaggedBooleanArray[] ret = new TaggedBooleanArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedBooleanArray((boolean[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Byte.TYPE) {
                TaggedByteArray[] ret = new TaggedByteArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedByteArray((byte[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Character.TYPE) {
                TaggedCharArray[] ret = new TaggedCharArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedCharArray((char[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Double.TYPE) {
                TaggedDoubleArray[] ret = new TaggedDoubleArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedDoubleArray((double[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Float.TYPE) {
                TaggedFloatArray[] ret = new TaggedFloatArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedFloatArray((float[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Integer.TYPE) {
                TaggedIntArray[] ret = new TaggedIntArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedIntArray((int[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Short.TYPE) {
                TaggedShortArray[] ret = new TaggedShortArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedShortArray((short[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            if (tmp == Long.TYPE) {
                TaggedLongArray[] ret = new TaggedLongArray[_in.length];
                for (int i = 0; i < _in.length; i++) {
                    ret[i] = new TaggedLongArray((long[]) _in[i]);
                }
                return _boxIfNecessary(ret);
            }
            throw new UnsupportedOperationException();
        } else if (in.getClass().getComponentType().getName().equals("java.lang.Object")) {
            Object[] _in = (Object[]) in;
            for (int i = 0; i < _in.length; i++) {
                _in[i] = boxIfNecessary(_in[i], new HashSet<Object>());
            }
        }
        return in;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.BOX_IF_NECESSARY)
    public static Object boxIfNecessary(final Object in) {
        if (in != null && in.getClass().isArray()) {
            return _boxIfNecessary(in);
        }
        return in;
    }

    public static Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
        if (dims == 2) {
            TaggedReferenceArray ret;
            switch (componentType) {
                case Type.BOOLEAN:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedBooleanArray[ar.length]);
                    break;
                case Type.BYTE:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedByteArray[ar.length]);
                    break;
                case Type.CHAR:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedCharArray[ar.length]);
                    break;
                case Type.DOUBLE:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedDoubleArray[ar.length]);
                    break;
                case Type.FLOAT:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedFloatArray[ar.length]);
                    break;
                case Type.INT:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedIntArray[ar.length]);
                    break;
                case Type.LONG:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedLongArray[ar.length]);
                    break;
                case Type.SHORT:
                    ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedShortArray[ar.length]);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            for (int i = 0; i < ar.length; i++) {
                if (ar[i] != null) {
                    Object entry = ar[i];
                    switch (componentType) {
                        case Type.BOOLEAN:
                            ret.val[i] = new TaggedBooleanArray(((boolean[]) entry));
                            break;
                        case Type.BYTE:
                            ret.val[i] = new TaggedByteArray(((byte[]) entry));
                            break;
                        case Type.CHAR:
                            ret.val[i] = new TaggedCharArray(((char[]) entry));
                            break;
                        case Type.DOUBLE:
                            ret.val[i] = new TaggedDoubleArray(((double[]) entry));
                            break;
                        case Type.FLOAT:
                            ret.val[i] = new TaggedFloatArray(((float[]) entry));
                            break;
                        case Type.INT:
                            ret.val[i] = new TaggedIntArray(((int[]) entry));
                            break;
                        case Type.LONG:
                            ret.val[i] = new TaggedLongArray(((long[]) entry));
                            break;
                        case Type.SHORT:
                            ret.val[i] = new TaggedShortArray(((short[]) entry));
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            }
            return ret;
        } else if (dims == 3) {
            TaggedReferenceArray ret = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedReferenceArray[ar.length]);
            for (int i = 0; i < ar.length; i++) {
                if (ar[i] != null) {
                    Object[] entry1 = (Object[]) ar[i];
                    switch (componentType) {
                        case Type.BOOLEAN:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedBooleanArray[entry1.length]);
                            break;
                        case Type.BYTE:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedByteArray[entry1.length]);
                            break;
                        case Type.CHAR:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedCharArray[entry1.length]);
                            break;
                        case Type.DOUBLE:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedDoubleArray[entry1.length]);
                            break;
                        case Type.FLOAT:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedFloatArray[entry1.length]);
                            break;
                        case Type.INT:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedIntArray[entry1.length]);
                            break;
                        case Type.LONG:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedLongArray[entry1.length]);
                            break;
                        case Type.SHORT:
                            ret.val[i] = new TaggedReferenceArray(Taint.emptyTaint(), new TaggedShortArray[entry1.length]);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    TaggedReferenceArray inner = (TaggedReferenceArray) ret.val[i];
                    for (int j = 0; j < entry1.length; j++) {
                        Object entry = entry1[j];
                        switch (componentType) {
                            case Type.BOOLEAN:
                                inner.val[j] = new TaggedBooleanArray(((boolean[]) entry));
                                break;
                            case Type.BYTE:
                                inner.val[j] = new TaggedByteArray(((byte[]) entry));
                                break;
                            case Type.CHAR:
                                inner.val[j] = new TaggedCharArray(((char[]) entry));
                                break;
                            case Type.DOUBLE:
                                inner.val[j] = new TaggedDoubleArray(((double[]) entry));
                                break;
                            case Type.FLOAT:
                                inner.val[j] = new TaggedFloatArray(((float[]) entry));
                                break;
                            case Type.INT:
                                inner.val[j] = new TaggedIntArray(((int[]) entry));
                                break;
                            case Type.LONG:
                                inner.val[j] = new TaggedLongArray(((long[]) entry));
                                break;
                            case Type.SHORT:
                                inner.val[j] = new TaggedShortArray((short[]) entry);
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                    }
                }
            }
            return ret;
        }
        throw new IllegalArgumentException();
    }

    public static void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
        for (int i = 0; i < ar.length; i++) {
            if (ar[i] == null) {
                switch (componentType) {
                    case Type.BOOLEAN:
                        ar[i] = new TaggedBooleanArray(new boolean[lastDimSize]);
                        break;
                    case Type.BYTE:
                        ar[i] = new TaggedByteArray(new byte[lastDimSize]);
                        break;
                    case Type.CHAR:
                        ar[i] = new TaggedCharArray(new char[lastDimSize]);
                        break;
                    case Type.DOUBLE:
                        ar[i] = new TaggedDoubleArray(new double[lastDimSize]);
                        break;
                    case Type.FLOAT:
                        ar[i] = new TaggedFloatArray(new float[lastDimSize]);
                        break;
                    case Type.INT:
                        ar[i] = new TaggedIntArray(new int[lastDimSize]);
                        break;
                    case Type.LONG:
                        ar[i] = new TaggedLongArray(new long[lastDimSize]);
                        break;
                    case Type.SHORT:
                        ar[i] = new TaggedShortArray(new short[lastDimSize]);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            } else {
                initLastDim((Object[]) ar[i], lastDimSize, componentType);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final Taint<?> dimTaint, final int lastDimSize, final int componentType) {
        for (int i = 0; i < ar.length; i++) {
            if (ar[i] == null) {
                switch (componentType) {
                    case Type.BOOLEAN:
                        ar[i] = new TaggedBooleanArray(dimTaint, new boolean[lastDimSize]);
                        break;
                    case Type.BYTE:
                        ar[i] = new TaggedByteArray(dimTaint, new byte[lastDimSize]);
                        break;
                    case Type.CHAR:
                        ar[i] = new TaggedCharArray(dimTaint, new char[lastDimSize]);
                        break;
                    case Type.DOUBLE:
                        ar[i] = new TaggedDoubleArray(dimTaint, new double[lastDimSize]);
                        break;
                    case Type.FLOAT:
                        ar[i] = new TaggedFloatArray(dimTaint, new float[lastDimSize]);
                        break;
                    case Type.INT:
                        ar[i] = new TaggedIntArray(dimTaint, new int[lastDimSize]);
                        break;
                    case Type.LONG:
                        ar[i] = new TaggedLongArray(dimTaint, new long[lastDimSize]);
                        break;
                    case Type.SHORT:
                        ar[i] = new TaggedShortArray(dimTaint, new short[lastDimSize]);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            } else {
                initLastDim((Object[]) ar[i], lastDimSize, componentType);
            }
        }
    }

    public static Type getPrimitiveTypeForWrapper(String internalName) {
        try {
            return Type.getType(getPrimitiveTypeForWrapper(Class.forName(internalName.replace("/", "."))));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the internal name of the taint array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getTaintArrayInternalName(int typeOperand) {
        switch (typeOperand) {
            case T_BOOLEAN:
                return TAGGED_BOOLEAN_ARRAY_INTERNAL_NAME;
            case T_CHAR:
                return TAGGED_CHAR_ARRAY_INTERNAL_NAME;
            case T_FLOAT:
                return TAGGED_FLOAT_ARRAY_INTERNAL_NAME;
            case T_DOUBLE:
                return TAGGED_DOUBLE_ARRAY_INTERNAL_NAME;
            case T_BYTE:
                return TAGGED_BYTE_ARRAY_INTERNAL_NAME;
            case T_SHORT:
                return TAGGED_SHORT_ARRAY_INTERNAL_NAME;
            case T_INT:
                return TAGGED_INT_ARRAY_INTERNAL_NAME;
            case T_LONG:
                return TAGGED_LONG_ARRAY_INTERNAL_NAME;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static Object[] unboxCK_ATTRIBUTE(Object[] in) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if (in == null || in[0] == null) {
            return null;
        }
        boolean needsFix = false;
        Field f = in[0].getClass().getDeclaredField("pValue");
        for (Object a : in) {
            Object v = f.get(a);
            if (v instanceof TaggedArray) {
                f.set(a, MultiDArrayUtils.unboxRaw(v));
            }
        }
        return in;
    }

    // ============ START GENERATED ===========

    public static TaggedReferenceArray MULTIANEWARRAY_B_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedByteArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedByteArray(t2, new byte[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_B_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedByteArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_B_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedByteArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_B_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedByteArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedByteArray(t3, new byte[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_B_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedByteArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_Z_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);

        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedBooleanArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedBooleanArray(t2, new boolean[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_Z_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);

        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedBooleanArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_Z_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedBooleanArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_Z_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedBooleanArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedBooleanArray(t3, new boolean[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_Z_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedBooleanArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_C_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedCharArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedCharArray(t2, new char[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_C_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedCharArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_C_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedCharArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_C_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedCharArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedCharArray(t3, new char[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_C_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedCharArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_F_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedFloatArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedFloatArray(t2, new float[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_F_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedFloatArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_F_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedFloatArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_F_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedFloatArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedFloatArray(t3, new float[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_F_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedFloatArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_I_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedIntArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedIntArray(t2, new int[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_I_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedIntArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_I_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedIntArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_I_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedIntArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedIntArray(t3, new int[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_I_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedIntArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_D_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedDoubleArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedDoubleArray(t2, new double[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_D_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedDoubleArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_D_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedDoubleArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_D_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedDoubleArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedDoubleArray(t3, new double[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_D_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedDoubleArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_S_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedShortArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedShortArray(t2, new short[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_S_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(tag, new TaggedShortArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_S_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedShortArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_S_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedShortArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedShortArray(t3, new short[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_S_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedShortArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_J_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedLongArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedLongArray(t2, new long[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_J_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedLongArray[dim]);
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_J_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, new TaggedLongArray[dim2]);
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_J_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedLongArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedLongArray(t3, new long[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_J_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedLongArray[dim3]);
            }
        }
        return ret;
    }

    // ============ END GENERATED =============

    public static TaggedReferenceArray MULTIANEWARRAY_REFERENCE_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new TaggedReferenceArray(t3, new TaggedReferenceArray[dim3]);
            }
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_REFERENCE_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
        }
        return ret;
    }


    public static TaggedReferenceArray MULTIANEWARRAY_REFERENCE_3DIMS(int dim1, int dim2, int dim3, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            TaggedReferenceArray d = new TaggedReferenceArray(t2, new TaggedReferenceArray[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[i] = new TaggedReferenceArray(t3, (Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(component, dim3));
            }
        }
        return ret;
    }


    public static TaggedReferenceArray MULTIANEWARRAY_REFERENCE_2DIMS(int dim1, int dim2, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new TaggedReferenceArray(t2, (Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(component, dim2));
        }
        return ret;
    }

    public static TaggedReferenceArray MULTIANEWARRAY_REFERENCE_2DIMS(int dim1, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        TaggedReferenceArray ret = new TaggedReferenceArray(t1, new TaggedReferenceArray[dim1]);
        return ret;
    }


    public static Object unbox1D(final Object in) {
        if (in instanceof TaggedArray) {
            return ((TaggedArray) in).getVal();
        }
        return in;
    }

    public static Object unbox1DOrNull(final Object in) {
        if (in instanceof TaggedReferenceArray) {
            Object ret = ((TaggedReferenceArray) in).getVal();
            if (TaggedArray.class.isAssignableFrom(ret.getClass().getComponentType())) {
                //We never want to actually unbox these things.
                return null;
            }
            return ret;
        }
        if (in instanceof TaggedArray) {
            return ((TaggedArray) in).getVal();
        }
        return in;
    }

    public static Object maybeUnbox(final Object in) {
        if (in == null) {
            return null;
        }
        if (null != isPrimitiveBoxClass(in.getClass())) {
            return unboxRaw(in);
        }
        return in;
    }

    @SuppressWarnings("unused")
    public static Object unboxMethodReceiverIfNecessary(Method m, Object obj) {
        if (TaggedArray.class.isAssignableFrom(m.getDeclaringClass())) {
            return obj; // No unboxing is necessary
        } else {
            return unboxRaw(obj);
        }
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the descriptor of the array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getArrayDescriptor(int typeOperand) {
        switch (typeOperand) {
            case T_BOOLEAN:
                return "[Z";
            case T_INT:
                return "[I";
            case T_BYTE:
                return "[B";
            case T_CHAR:
                return "[C";
            case T_DOUBLE:
                return "[D";
            case T_FLOAT:
                return "[F";
            case T_LONG:
                return "[J";
            case T_SHORT:
                return "[S";
            default:
                throw new IllegalArgumentException();
        }
    }
}
