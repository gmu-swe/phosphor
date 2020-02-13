package edu.columbia.cs.psl.phosphor.struct.multid;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.*;
import org.objectweb.asm.Type;

import java.util.HashSet;

import static org.objectweb.asm.Opcodes.*;

public abstract class MultiDTaintedArrayWithObjTag {

    public static final long serialVersionUID = 40523489234L;
    private static final String LAZY_BOOLEAN_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyBooleanArrayObjTags.class);
    private static final String LAZY_BYTE_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyByteArrayObjTags.class);
    private static final String LAZY_CHAR_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyCharArrayObjTags.class);
    private static final String LAZY_DOUBLE_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyDoubleArrayObjTags.class);
    private static final String LAZY_FLOAT_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyFloatArrayObjTags.class);
    private static final String LAZY_INT_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyIntArrayObjTags.class);
    private static final String LAZY_LONG_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyLongArrayObjTags.class);
    private static final String LAZY_SHORT_ARRAY_INTERNAL_NAME = Type.getInternalName(LazyShortArrayObjTags.class);
    public int sort;


    public MultiDTaintedArrayWithObjTag() {

    }

    public void setTaints(Object t) {
        throw new UnsupportedOperationException();
    }

    public abstract Object getVal();

    public final int getSort() {
        return sort;
    }


    public abstract Object clone();

    @Override
    public final boolean equals(Object obj) {
        return getVal().equals(obj);
    }

    @Override
    public final int hashCode() {
        return getVal().hashCode();
    }

    public final TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret) {
        ret.taint = null;
        ret.val = hashCode();
        return ret;
    }

    @Override
    public String toString() {
        switch(sort) {
            case Type.BOOLEAN:
                return "[Z";
            case Type.BYTE:
                return "[B";
            case Type.CHAR:
                return "[C";
            case Type.DOUBLE:
                return "[D";
            case Type.INT:
                return "[I";
            case Type.FLOAT:
                return "[F";
            case Type.LONG:
                return "[J";
            case Type.SHORT:
                return "[S";
        }
        return super.toString();
    }

    public static Type getTypeForType(final Type originalElementType) {
        if(originalElementType.getSort() != Type.ARRAY) {
            throw new IllegalArgumentException("passed " + originalElementType);
        }
        if(originalElementType.getDimensions() > 1) {
            return Type.getType(LazyReferenceArrayObjTags.class);
        } else {
            return Type.getType(getDescriptorForComponentType(originalElementType.getElementType().getSort()));
        }
    }

    public static String getDescriptorForComponentType(final int componentSort) {
        switch (componentSort) {
            case Type.BOOLEAN:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyBooleanArrayObjTags;";
            case Type.BYTE:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyByteArrayObjTags;";
            case Type.CHAR:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyCharArrayObjTags;";
            case Type.DOUBLE:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyDoubleArrayObjTags;";
            case Type.FLOAT:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyFloatArrayObjTags;";
            case Type.INT:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyIntArrayObjTags;";
            case Type.LONG:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyLongArrayObjTags;";
            case Type.SHORT:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyShortArrayObjTags;";
            case Type.OBJECT:
                return "Ledu/columbia/cs/psl/phosphor/struct/LazyReferenceArrayObjTags;";
            default:
                throw new IllegalArgumentException("invalid sort: " + componentSort);
        }
    }

    public static String isPrimitiveBoxClass(Class c) {
        while(c.isArray()) {
            c = c.getComponentType();
        }
        if(c == LazyDoubleArrayObjTags.class) {
            return "D";
        }
        if(c == LazyFloatArrayObjTags.class) {
            return "F";
        }
        if(c == LazyIntArrayObjTags.class) {
            return "I";
        }
        if(c == LazyLongArrayObjTags.class) {
            return "J";
        }
        if(c == LazyShortArrayObjTags.class) {
            return "S";
        }
        if(c == LazyBooleanArrayObjTags.class) {
            return "Z";
        }
        if(c == LazyByteArrayObjTags.class) {
            return "B";
        }
        if(c == LazyCharArrayObjTags.class) {
            return "C";
        }
        return null;
    }

    public static String getPrimitiveTypeForWrapper(Class c) {
        while(c.isArray()) {
            c = c.getComponentType();
        }
        if(c == LazyDoubleArrayObjTags.class) {
            return "D";
        }
        if(c == LazyFloatArrayObjTags.class) {
            return "F";
        }
        if(c == LazyIntArrayObjTags.class) {
            return "I";
        }
        if(c == LazyLongArrayObjTags.class) {
            return "J";
        }
        if(c == LazyShortArrayObjTags.class) {
            return "S";
        }
        if(c == LazyBooleanArrayObjTags.class) {
            return "Z";
        }
        if(c == LazyByteArrayObjTags.class) {
            return "B";
        }
        if(c == LazyCharArrayObjTags.class) {
            return "C";
        }
        if(c == LazyReferenceArrayObjTags.class) {
            return "Ljava/lang/Object;";
        }
        throw new IllegalStateException("Got passed class: " + c);

    }

    public static Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
        int dims = 0;
        if(c.isArray()) {
            while(c.isArray()) {
                c = c.getComponentType();
                dims++;
            }
        }
        if(dims == 1) {
            if(c == Double.TYPE) {
                return LazyDoubleArrayObjTags.class;
            }
            if(c == Float.TYPE) {
                return LazyFloatArrayObjTags.class;
            }
            if(c == Integer.TYPE) {
                return LazyIntArrayObjTags.class;
            }
            if(c == Long.TYPE) {
                return LazyLongArrayObjTags.class;
            }
            if(c == Short.TYPE) {
                return LazyShortArrayObjTags.class;
            }
            if(c == Boolean.TYPE) {
                return LazyBooleanArrayObjTags.class;
            }
            if(c == Byte.TYPE) {
                return LazyByteArrayObjTags.class;
            }
            if(c == Character.TYPE) {
                return LazyCharArrayObjTags.class;
            }
        }
        return LazyReferenceArrayObjTags.class;
    }

    public static Class getClassForComponentType(final int componentSort) {
        switch(componentSort) {
            case Type.BOOLEAN:
                return LazyBooleanArrayObjTags.class;
            case Type.BYTE:
                return LazyByteArrayObjTags.class;
            case Type.CHAR:
                return LazyCharArrayObjTags.class;
            case Type.DOUBLE:
                return LazyDoubleArrayObjTags.class;
            case Type.FLOAT:
                return LazyFloatArrayObjTags.class;
            case Type.INT:
                return LazyIntArrayObjTags.class;
            case Type.LONG:
                return LazyLongArrayObjTags.class;
            case Type.SHORT:
                return LazyShortArrayObjTags.class;
            case Type.OBJECT:
                return LazyReferenceArrayObjTags.class;
            default:
                throw new IllegalArgumentException("invalid sort: " + componentSort);
        }
    }

    public static Object unboxRaw(final Object in) {
        if(in == null) {
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
        if(in instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) in).getVal();
        }
        return in;
    }

    /* If the specified object is a one dimensional array of primitives, boxes and returns the specified object. Otherwise
     * returns the specified object. */
    public static Object boxOnly1D(final Object obj) {
        if(obj instanceof boolean[]) {
            return new LazyBooleanArrayObjTags((boolean[]) obj);
        } else if(obj instanceof byte[]) {
            return new LazyByteArrayObjTags((byte[]) obj);
        } else if(obj instanceof char[]) {
            return new LazyCharArrayObjTags((char[]) obj);
        } else if(obj instanceof double[]) {
            return new LazyDoubleArrayObjTags((double[]) obj);
        } else if(obj instanceof float[]) {
            return new LazyFloatArrayObjTags((float[]) obj);
        } else if(obj instanceof int[]) {
            return new LazyIntArrayObjTags((int[]) obj);
        } else if(obj instanceof long[]) {
            return new LazyLongArrayObjTags((long[]) obj);
        } else if(obj instanceof short[]) {
            return new LazyShortArrayObjTags((short[]) obj);
        } else if(obj instanceof Object[]) {
            return new LazyReferenceArrayObjTags((Object[]) obj);
        } else {
            return obj;
        }
    }

    public static Object unboxVal(final Object _in, final int componentType, final int dims) {

        if(dims == 0) {
            switch(componentType) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.DOUBLE:
                case Type.FLOAT:
                case Type.INT:
                case Type.LONG:
                case Type.SHORT:
                    return ((LazyArrayObjTags) _in).getVal();
                default:
                    throw new IllegalArgumentException();
            }
        } else if(dims == 1) {
            final Object[] in = (Object[]) _in;
            switch(componentType) {
                case Type.BOOLEAN:
                    boolean[][] retz = new boolean[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retz[i] = ((LazyBooleanArrayObjTags) in[i]).val;
                        }
                    }
                    return retz;
                case Type.BYTE:
                    byte[][] retb = new byte[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retb[i] = ((LazyByteArrayObjTags) in[i]).val;
                        }
                    }
                    return retb;
                case Type.CHAR:
                    char[][] retc = new char[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retc[i] = ((LazyCharArrayObjTags) in[i]).val;
                        }
                    }
                    return retc;
                case Type.DOUBLE:
                    double[][] retd = new double[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retd[i] = ((LazyDoubleArrayObjTags) in[i]).val;
                        }
                    }
                    return retd;
                case Type.FLOAT:
                    float[][] retf = new float[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retf[i] = ((LazyFloatArrayObjTags) in[i]).val;
                        }
                    }
                    return retf;
                case Type.INT:
                    int[][] reti = new int[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            reti[i] = ((LazyIntArrayObjTags) in[i]).val;
                        }
                    }
                    return reti;
                case Type.LONG:
                    long[][] retl = new long[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            retl[i] = ((LazyLongArrayObjTags) in[i]).val;
                        }
                    }
                    return retl;
                case Type.SHORT:
                    short[][] rets = new short[in.length][];
                    for(int i = 0; i < in.length; i++) {
                        if(in[i] != null) {
                            rets[i] = ((LazyShortArrayObjTags) in[i]).val;
                        }
                    }
                    return rets;
            }
        } else if(dims == 2) {
            final Object[][] ina = (Object[][]) _in;
            final Object[] in = (Object[]) _in;
            switch(componentType) {
                case Type.BOOLEAN:
                    boolean[][][] retz = new boolean[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retz[i] = new boolean[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retz[i][j] = ((LazyBooleanArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retz;
                case Type.BYTE:
                    byte[][][] retb = new byte[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retb[i] = new byte[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retb[i][j] = ((LazyByteArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retb;
                case Type.CHAR:
                    char[][][] retc = new char[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retc[i] = new char[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retc[i][j] = ((LazyCharArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retc;
                case Type.DOUBLE:
                    double[][][] retd = new double[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retd[i] = new double[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retd[i][j] = ((LazyDoubleArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retd;
                case Type.FLOAT:
                    float[][][] retf = new float[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retf[i] = new float[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retf[i][j] = ((LazyFloatArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retf;
                case Type.INT:
                    int[][][] reti = new int[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        reti[i] = new int[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            reti[i][j] = ((LazyIntArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return reti;
                case Type.LONG:
                    long[][][] retl = new long[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        retl[i] = new long[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            retl[i][j] = ((LazyLongArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return retl;
                case Type.SHORT:
                    short[][][] rets = new short[in.length][][];
                    for(int i = 0; i < in.length; i++) {
                        rets[i] = new short[ina[i].length][];
                        for(int j = 0; j < ina[i].length; j++) {
                            rets[i][j] = ((LazyShortArrayObjTags) ina[i][j]).val;
                        }
                    }
                    return rets;
            }
        }
        throw new IllegalArgumentException();
    }

    public static int getSortForBoxClass(Class c) {
        if(c == LazyIntArrayObjTags.class) {
            return Type.INT;
        }
        if(c == LazyBooleanArrayObjTags.class) {
            return Type.BOOLEAN;
        }
        if(c == LazyByteArrayObjTags.class) {
            return Type.BYTE;
        }
        if(c == LazyFloatArrayObjTags.class) {
            return Type.FLOAT;
        }
        if(c == LazyCharArrayObjTags.class) {
            return Type.CHAR;
        }
        if(c == LazyDoubleArrayObjTags.class) {
            return Type.DOUBLE;
        }
        if(c == LazyLongArrayObjTags.class) {
            return Type.LONG;
        }
        if(c == LazyShortArrayObjTags.class) {
            return Type.SHORT;
        }
        throw new IllegalArgumentException();
    }

    public static int getSort(Class c) {
        if(c == Integer.TYPE) {
            return Type.INT;
        }
        if(c == Boolean.TYPE) {
            return Type.BOOLEAN;
        }
        if(c == Byte.TYPE) {
            return Type.BYTE;
        }
        if(c == Float.TYPE) {
            return Type.FLOAT;
        }
        if(c == Character.TYPE) {
            return Type.CHAR;
        }
        if(c == Double.TYPE) {
            return Type.DOUBLE;
        }
        if(c == Long.TYPE) {
            return Type.LONG;
        }
        if(c == Short.TYPE) {
            return Type.SHORT;
        }
        throw new IllegalArgumentException();
    }

    public static Object boxIfNecessary(final Object in, final HashSet<Object> done) {
        if(in != null && in.getClass().isArray()) {
            if(in.getClass().getComponentType().isPrimitive()) {
                //Is prim arraytype
                Class tmp = in.getClass();
                int dims = 0;
                while(tmp.isArray()) {
                    tmp = tmp.getComponentType();
                    dims++;
                }
                if(dims > 1) { //this should never be possible.
                    Type t = Type.getType(in.getClass());
                    initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
                } else {
                    if(tmp == Boolean.TYPE) {
                        return new LazyBooleanArrayObjTags((boolean[]) in);
                    }
                    if(tmp == Byte.TYPE) {
                        return new LazyByteArrayObjTags(((byte[]) in));
                    }
                    if(tmp == Character.TYPE) {
                        return new LazyCharArrayObjTags(((char[]) in));
                    }
                    if(tmp == Double.TYPE) {
                        return new LazyDoubleArrayObjTags(((double[]) in));
                    }
                    if(tmp == Float.TYPE) {
                        return new LazyFloatArrayObjTags(((float[]) in));
                    }
                    if(tmp == Integer.TYPE) {
                        return new LazyIntArrayObjTags(((int[]) in));
                    }
                    if(tmp == Long.TYPE) {
                        return new LazyLongArrayObjTags(((long[]) in));
                    }
                    if(tmp == Short.TYPE) {
                        return new LazyShortArrayObjTags(((short[]) in));
                    }
                    throw new IllegalArgumentException();
                }
            } else if(in.getClass().getComponentType().isArray() && in.getClass().getComponentType().getComponentType().isPrimitive()) {
                //THIS array is an prim[][] array
                Object[] _in = (Object[]) in;

                Class tmp = in.getClass();
                while(tmp.isArray()) {
                    tmp = tmp.getComponentType();
                }
                if(tmp == Boolean.TYPE) {
                    LazyBooleanArrayObjTags[] ret = new LazyBooleanArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyBooleanArrayObjTags((boolean[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Byte.TYPE) {
                    LazyByteArrayObjTags[] ret = new LazyByteArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyByteArrayObjTags((byte[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Character.TYPE) {
                    LazyCharArrayObjTags[] ret = new LazyCharArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyCharArrayObjTags((char[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Double.TYPE) {
                    LazyDoubleArrayObjTags[] ret = new LazyDoubleArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyDoubleArrayObjTags((double[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Float.TYPE) {
                    LazyFloatArrayObjTags[] ret = new LazyFloatArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyFloatArrayObjTags((float[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Integer.TYPE) {
                    LazyIntArrayObjTags[] ret = new LazyIntArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyIntArrayObjTags((int[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Short.TYPE) {
                    LazyShortArrayObjTags[] ret = new LazyShortArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyShortArrayObjTags((short[]) _in[i]);
                    }
                    return ret;
                }
                if(tmp == Long.TYPE) {
                    LazyLongArrayObjTags[] ret = new LazyLongArrayObjTags[_in.length];
                    for(int i = 0; i < _in.length; i++) {
                        ret[i] = new LazyLongArrayObjTags((long[]) _in[i]);
                    }
                    return ret;
                }
                throw new UnsupportedOperationException();
            } else if(in.getClass().getComponentType().getName().equals("java.lang.Object")) {
                Object[] _in = (Object[]) in;
                TaintedBooleanWithObjTag tmpRet = new TaintedBooleanWithObjTag();
                for(int i = 0; i < _in.length; i++) {
                    if(done.add$$PHOSPHORTAGGED(_in[i], tmpRet).val) {
                        _in[i] = boxIfNecessary(_in[i], done);
                    }
                }
            }
        }
        return in;
    }

    private static Object _boxIfNecessary(final Object in){
        if(!in.getClass().getComponentType().isArray()) {
            //Is prim arraytype
            Class tmp = in.getClass();
            int dims = 0;
            while(tmp.isArray()) {
                tmp = tmp.getComponentType();
                dims++;
            }
            if(dims > 1) { //this should never be possible.
                Type t = Type.getType(in.getClass());
                initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
            } else {
                if(tmp == Boolean.TYPE) {
                    return new LazyBooleanArrayObjTags((boolean[]) in);
                }
                if(tmp == Byte.TYPE) {
                    return new LazyByteArrayObjTags(((byte[]) in));
                }
                if(tmp == Character.TYPE) {
                    return new LazyCharArrayObjTags(((char[]) in));
                }
                if(tmp == Double.TYPE) {
                    return new LazyDoubleArrayObjTags(((double[]) in));
                }
                if(tmp == Float.TYPE) {
                    return new LazyFloatArrayObjTags(((float[]) in));
                }
                if(tmp == Integer.TYPE) {
                    return new LazyIntArrayObjTags(((int[]) in));
                }
                if(tmp == Long.TYPE) {
                    return new LazyLongArrayObjTags(((long[]) in));
                }
                if(tmp == Short.TYPE) {
                    return new LazyShortArrayObjTags(((short[]) in));
                }
                return new LazyReferenceArrayObjTags((Object[]) in);
            }
        } else if(in.getClass().getComponentType().isArray() && in.getClass().getComponentType().getComponentType().isPrimitive()) {
            //THIS array is an prim[][] array
            Object[] _in = (Object[]) in;

            Class tmp = in.getClass();
            while(tmp.isArray()) {
                tmp = tmp.getComponentType();
            }
            if(tmp == Boolean.TYPE) {
                LazyBooleanArrayObjTags[] ret = new LazyBooleanArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyBooleanArrayObjTags((boolean[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Byte.TYPE) {
                LazyByteArrayObjTags[] ret = new LazyByteArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyByteArrayObjTags((byte[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Character.TYPE) {
                LazyCharArrayObjTags[] ret = new LazyCharArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyCharArrayObjTags((char[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Double.TYPE) {
                LazyDoubleArrayObjTags[] ret = new LazyDoubleArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyDoubleArrayObjTags((double[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Float.TYPE) {
                LazyFloatArrayObjTags[] ret = new LazyFloatArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyFloatArrayObjTags((float[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Integer.TYPE) {
                LazyIntArrayObjTags[] ret = new LazyIntArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyIntArrayObjTags((int[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Short.TYPE) {
                LazyShortArrayObjTags[] ret = new LazyShortArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyShortArrayObjTags((short[]) _in[i]);
                }
                return ret;
            }
            if(tmp == Long.TYPE) {
                LazyLongArrayObjTags[] ret = new LazyLongArrayObjTags[_in.length];
                for(int i = 0; i < _in.length; i++) {
                    ret[i] = new LazyLongArrayObjTags((long[]) _in[i]);
                }
                return ret;
            }
            throw new UnsupportedOperationException();
        } else if(in.getClass().getComponentType().getName().equals("java.lang.Object")) {
            Object[] _in = (Object[]) in;
            for(int i = 0; i < _in.length; i++) {
                _in[i] = boxIfNecessary(_in[i], new HashSet<Object>());
            }
        }
        return in;
    }

    public static Object boxIfNecessary(final Object in) {
        if(in != null && in.getClass().isArray()) {
            return _boxIfNecessary(in);
        }
        return in;
    }

    public static Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
        if(dims == 2) {
            LazyReferenceArrayObjTags ret;
            switch(componentType) {
                case Type.BOOLEAN:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyBooleanArrayObjTags[ar.length]);
                    break;
                case Type.BYTE:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyByteArrayObjTags[ar.length]);
                    break;
                case Type.CHAR:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyCharArrayObjTags[ar.length]);
                    break;
                case Type.DOUBLE:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyDoubleArrayObjTags[ar.length]);
                    break;
                case Type.FLOAT:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyFloatArrayObjTags[ar.length]);
                    break;
                case Type.INT:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyIntArrayObjTags[ar.length]);
                    break;
                case Type.LONG:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyLongArrayObjTags[ar.length]);
                    break;
                case Type.SHORT:
                    ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyShortArrayObjTags[ar.length]);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            for(int i = 0; i < ar.length; i++) {
                if(ar[i] != null) {
                    Object entry = ar[i];
                    switch(componentType) {
                        case Type.BOOLEAN:
                            ret.val[i] = new LazyBooleanArrayObjTags(((boolean[]) entry));
                            break;
                        case Type.BYTE:
                            ret.val[i] = new LazyByteArrayObjTags(((byte[]) entry));
                            break;
                        case Type.CHAR:
                            ret.val[i] = new LazyCharArrayObjTags(((char[]) entry));
                            break;
                        case Type.DOUBLE:
                            ret.val[i] = new LazyDoubleArrayObjTags(((double[]) entry));
                            break;
                        case Type.FLOAT:
                            ret.val[i] = new LazyFloatArrayObjTags(((float[]) entry));
                            break;
                        case Type.INT:
                            ret.val[i] = new LazyIntArrayObjTags(((int[]) entry));
                            break;
                        case Type.LONG:
                            ret.val[i] = new LazyLongArrayObjTags(((long[]) entry));
                            break;
                        case Type.SHORT:
                            ret.val[i] = new LazyShortArrayObjTags(((short[]) entry));
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            }
            return ret;
        } else if(dims == 3) {
            LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyReferenceArrayObjTags[ar.length]);
            for(int i = 0; i < ar.length; i++) {
                if(ar[i] != null) {
                    Object[] entry1 = (Object[]) ar[i];
                    switch(componentType) {
                        case Type.BOOLEAN:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyBooleanArrayObjTags[entry1.length]);
                            break;
                        case Type.BYTE:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyByteArrayObjTags[entry1.length]);
                            break;
                        case Type.CHAR:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyCharArrayObjTags[entry1.length]);
                            break;
                        case Type.DOUBLE:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyDoubleArrayObjTags[entry1.length]);
                            break;
                        case Type.FLOAT:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyFloatArrayObjTags[entry1.length]);
                            break;
                        case Type.INT:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyIntArrayObjTags[entry1.length]);
                            break;
                        case Type.LONG:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyLongArrayObjTags[entry1.length]);
                            break;
                        case Type.SHORT:
                            ret.val[i] = new LazyReferenceArrayObjTags(Taint.emptyTaint(), new LazyShortArrayObjTags[entry1.length]);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    LazyReferenceArrayObjTags inner = (LazyReferenceArrayObjTags) ret.val[i];
                    for(int j = 0; j < entry1.length; j++) {
                        Object entry = entry1[j];
                        switch(componentType) {
                            case Type.BOOLEAN:
                                inner.val[j] = new LazyBooleanArrayObjTags(((boolean[]) entry));
                                break;
                            case Type.BYTE:
                                inner.val[j] = new LazyByteArrayObjTags(((byte[]) entry));
                                break;
                            case Type.CHAR:
                                inner.val[j] = new LazyCharArrayObjTags(((char[]) entry));
                                break;
                            case Type.DOUBLE:
                                inner.val[j] = new LazyDoubleArrayObjTags(((double[]) entry));
                                break;
                            case Type.FLOAT:
                                inner.val[j] = new LazyFloatArrayObjTags(((float[]) entry));
                                break;
                            case Type.INT:
                                inner.val[j] = new LazyIntArrayObjTags(((int[]) entry));
                                break;
                            case Type.LONG:
                                inner.val[j] = new LazyLongArrayObjTags(((long[]) entry));
                                break;
                            case Type.SHORT:
                                inner.val[j] = new LazyShortArrayObjTags((short[]) entry);
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
        for(int i = 0; i < ar.length; i++) {
            if(ar[i] == null) {
                switch(componentType) {
                    case Type.BOOLEAN:
                        ar[i] = new LazyBooleanArrayObjTags(new boolean[lastDimSize]);
                        break;
                    case Type.BYTE:
                        ar[i] = new LazyByteArrayObjTags(new byte[lastDimSize]);
                        break;
                    case Type.CHAR:
                        ar[i] = new LazyCharArrayObjTags(new char[lastDimSize]);
                        break;
                    case Type.DOUBLE:
                        ar[i] = new LazyDoubleArrayObjTags(new double[lastDimSize]);
                        break;
                    case Type.FLOAT:
                        ar[i] = new LazyFloatArrayObjTags(new float[lastDimSize]);
                        break;
                    case Type.INT:
                        ar[i] = new LazyIntArrayObjTags(new int[lastDimSize]);
                        break;
                    case Type.LONG:
                        ar[i] = new LazyLongArrayObjTags(new long[lastDimSize]);
                        break;
                    case Type.SHORT:
                        ar[i] = new LazyShortArrayObjTags(new short[lastDimSize]);
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
        for(int i = 0; i < ar.length; i++) {
            if(ar[i] == null) {
                switch(componentType) {
                    case Type.BOOLEAN:
                        ar[i] = new LazyBooleanArrayObjTags(dimTaint, new boolean[lastDimSize]);
                        break;
                    case Type.BYTE:
                        ar[i] = new LazyByteArrayObjTags(dimTaint, new byte[lastDimSize]);
                        break;
                    case Type.CHAR:
                        ar[i] = new LazyCharArrayObjTags(dimTaint, new char[lastDimSize]);
                        break;
                    case Type.DOUBLE:
                        ar[i] = new LazyDoubleArrayObjTags(dimTaint, new double[lastDimSize]);
                        break;
                    case Type.FLOAT:
                        ar[i] = new LazyFloatArrayObjTags(dimTaint, new float[lastDimSize]);
                        break;
                    case Type.INT:
                        ar[i] = new LazyIntArrayObjTags(dimTaint, new int[lastDimSize]);
                        break;
                    case Type.LONG:
                        ar[i] = new LazyLongArrayObjTags(dimTaint, new long[lastDimSize]);
                        break;
                    case Type.SHORT:
                        ar[i] = new LazyShortArrayObjTags(dimTaint, new short[lastDimSize]);
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
        } catch(ClassNotFoundException e) {
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
        switch(typeOperand) {
            case T_BOOLEAN:
                return LAZY_BOOLEAN_ARRAY_INTERNAL_NAME;
            case T_CHAR:
                return LAZY_CHAR_ARRAY_INTERNAL_NAME;
            case T_FLOAT:
                return LAZY_FLOAT_ARRAY_INTERNAL_NAME;
            case T_DOUBLE:
                return LAZY_DOUBLE_ARRAY_INTERNAL_NAME;
            case T_BYTE:
                return LAZY_BYTE_ARRAY_INTERNAL_NAME;
            case T_SHORT:
                return LAZY_SHORT_ARRAY_INTERNAL_NAME;
            case T_INT:
                return LAZY_INT_ARRAY_INTERNAL_NAME;
            case T_LONG:
                return LAZY_LONG_ARRAY_INTERNAL_NAME;
            default:
                throw new IllegalArgumentException();
        }
    }
}
