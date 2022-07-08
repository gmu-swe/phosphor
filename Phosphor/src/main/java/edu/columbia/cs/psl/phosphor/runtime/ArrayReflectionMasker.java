package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;

public class ArrayReflectionMasker {

    private ArrayReflectionMasker() {
        // Prevents this class from being instantiated
    }

    public static int getLength(Object obj) {
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof LazyArrayObjTags) {
            return Array.getLength(((LazyArrayObjTags) obj).getVal());
        }
        throw new ArrayStoreException("Uknown array type: " + obj.getClass());
    }

    public static int getLength(Object obj, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof LazyArrayObjTags) {
            return Array.getLength(((LazyArrayObjTags) obj).getVal());
        }
        throw new IllegalArgumentException("Not an array type: " + obj.getClass());
    }

    public static Object newInstance(Class clazz, int len,
            PhosphorStackFrame phosphorStackFrame) {
        Class tmp = clazz;
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        if (tmp.isArray()) {
            return newInstance(clazz, new int[] { len }, phosphorStackFrame);
        } else {
            if (tmp == Double.TYPE) {
                return new LazyDoubleArrayObjTags(new double[len]);
            }
            if (tmp == Float.TYPE) {
                return new LazyFloatArrayObjTags(new float[len]);
            }
            if (tmp == Integer.TYPE) {
                return new LazyIntArrayObjTags(new int[len]);
            }
            if (tmp == Long.TYPE) {
                return new LazyLongArrayObjTags(new long[len]);
            }
            if (tmp == Short.TYPE) {
                return new LazyShortArrayObjTags(new short[len]);
            }
            if (tmp == Boolean.TYPE) {
                return new LazyBooleanArrayObjTags(new boolean[len]);
            }
            if (tmp == Byte.TYPE) {
                return new LazyByteArrayObjTags(new byte[len]);
            }
            if (tmp == Character.TYPE) {
                return new LazyCharArrayObjTags(new char[len]);
            }
            return new LazyReferenceArrayObjTags((Object[]) Array.newArray(tmp, len));
        }
    }

    public static LazyArrayObjTags newInstanceForType(int componentSort, int len) {
        switch (componentSort) {
            case Type.BOOLEAN:
                return new LazyBooleanArrayObjTags(new boolean[len]);
            case Type.BYTE:
                return new LazyByteArrayObjTags(new byte[len]);
            case Type.CHAR:
                return new LazyCharArrayObjTags(new char[len]);
            case Type.DOUBLE:
                return new LazyDoubleArrayObjTags(new double[len]);
            case Type.FLOAT:
                return new LazyFloatArrayObjTags(new float[len]);
            case Type.INT:
                return new LazyIntArrayObjTags(new int[len]);
            case Type.LONG:
                return new LazyLongArrayObjTags(new long[len]);
            case Type.SHORT:
                return new LazyShortArrayObjTags(new short[len]);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Object newInstance(Class clazz,
            int[] dims, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        Type t = Type.getType(clazz);
        LazyIntArrayObjTags dimstaint = (LazyIntArrayObjTags) stackFrame.getArgWrapper(0, dims);
        if (t.getSort() == Type.ARRAY) {
            int innerDims = t.getDimensions();
            if (innerDims == 1) {
                // 2D array, init only the top level
                switch (t.getElementType().getSort()) {
                    case Type.BOOLEAN:
                        return MultiDTaintedArray.MULTIANEWARRAY_Z_2DIMS(dims[0], stackFrame);
                    case Type.BYTE:
                        return MultiDTaintedArray.MULTIANEWARRAY_B_2DIMS(dims[0], stackFrame);
                    case Type.CHAR:
                        return MultiDTaintedArray.MULTIANEWARRAY_C_2DIMS(dims[0], stackFrame);
                    case Type.DOUBLE:
                        return MultiDTaintedArray.MULTIANEWARRAY_D_2DIMS(dims[0], stackFrame);
                    case Type.FLOAT:
                        return MultiDTaintedArray.MULTIANEWARRAY_F_2DIMS(dims[0], stackFrame);
                    case Type.INT:
                        return MultiDTaintedArray.MULTIANEWARRAY_I_2DIMS(dims[0], stackFrame);
                    case Type.LONG:
                        return MultiDTaintedArray.MULTIANEWARRAY_J_2DIMS(dims[0], stackFrame);
                    case Type.SHORT:
                        return MultiDTaintedArray.MULTIANEWARRAY_S_2DIMS(dims[0], stackFrame);
                    case Type.OBJECT:
                        return MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0], clazz.getComponentType(), stackFrame);
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (dims.length == 1) {
            if (t.getSort() == Type.OBJECT) {
                return new LazyReferenceArrayObjTags((Object[]) Array.newArray(clazz, dims[0]));
            } else {
                return newInstanceForType(t.getSort(), dims[0]);
            }
        } else if (dims.length == 2) {
            // 2D array, init all levels
            switch (t.getSort()) {
                case Type.BOOLEAN:
                    return MultiDTaintedArray.MULTIANEWARRAY_Z_2DIMS(dims[0], dims[1], stackFrame);
                case Type.BYTE:
                    return MultiDTaintedArray.MULTIANEWARRAY_B_2DIMS(dims[0], dims[1], stackFrame);
                case Type.CHAR:
                    return MultiDTaintedArray.MULTIANEWARRAY_C_2DIMS(dims[0], dims[1], stackFrame);
                case Type.DOUBLE:
                    return MultiDTaintedArray.MULTIANEWARRAY_D_2DIMS(dims[0], dims[1], stackFrame);
                case Type.FLOAT:
                    return MultiDTaintedArray.MULTIANEWARRAY_F_2DIMS(dims[0], dims[1], stackFrame);
                case Type.INT:
                    return MultiDTaintedArray.MULTIANEWARRAY_I_2DIMS(dims[0], dims[1], stackFrame);
                case Type.LONG:
                    return MultiDTaintedArray.MULTIANEWARRAY_J_2DIMS(dims[0], dims[1], stackFrame);
                case Type.SHORT:
                    return MultiDTaintedArray.MULTIANEWARRAY_S_2DIMS(dims[0], dims[1], stackFrame);
                case Type.OBJECT:
                    return MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0],
                            dims[1], clazz, stackFrame);
            }
        } else if (dims.length == 3) {
            switch (t.getSort()) {
                case Type.BOOLEAN:
                    return MultiDTaintedArray.MULTIANEWARRAY_Z_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.BYTE:
                    return MultiDTaintedArray.MULTIANEWARRAY_B_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.CHAR:
                    return MultiDTaintedArray.MULTIANEWARRAY_C_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.DOUBLE:
                    return MultiDTaintedArray.MULTIANEWARRAY_D_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.FLOAT:
                    return MultiDTaintedArray.MULTIANEWARRAY_F_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.INT:
                    return MultiDTaintedArray.MULTIANEWARRAY_I_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.LONG:
                    return MultiDTaintedArray.MULTIANEWARRAY_J_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.SHORT:
                    return MultiDTaintedArray.MULTIANEWARRAY_S_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.OBJECT:
                    return MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_3DIMS(dims[0],
                            dims[1], dims[2], clazz, stackFrame);
            }
        }
        throw new UnsupportedOperationException();
    }

    static void fillInTaint(PhosphorStackFrame ret, LazyArrayObjTags ar, int idx) {
        if (ar.taints != null) {
            ret.returnTaint = ar.taints[idx];
        } else {
            ret.returnTaint = Taint.emptyTaint();
        }
    }

    public static Object get(Object obj, int idx, PhosphorStackFrame stackFrame) {
        if (obj instanceof LazyBooleanArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyBooleanArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyByteArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyBooleanArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyCharArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyCharArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyDoubleArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyDoubleArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyFloatArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyFloatArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyIntArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyIntArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyLongArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyLongArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyShortArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyShortArrayObjTags) obj).val[idx];
        } else if (obj instanceof LazyReferenceArrayObjTags) {
            fillInTaint(stackFrame, (LazyArrayObjTags) obj, idx);
            return ((LazyReferenceArrayObjTags) obj).val[idx];
        }
        stackFrame.returnTaint = Taint.emptyTaint();
        return Array.get(obj, idx);
    }

    public static byte getByte(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyByteArrayObjTags) {
            return ((LazyByteArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static boolean getBoolean(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyBooleanArrayObjTags) {
            return ((LazyBooleanArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static char getChar(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyCharArrayObjTags) {
            return ((LazyCharArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static double getDouble(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyDoubleArrayObjTags) {
            return ((LazyDoubleArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static int getInt(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyIntArrayObjTags) {
            return ((LazyIntArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static long getLong(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyLongArrayObjTags) {
            return ((LazyLongArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static short getShort(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyShortArrayObjTags) {
            return ((LazyShortArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static float getFloat(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyFloatArrayObjTags) {
            return ((LazyFloatArrayObjTags) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static void set(Object obj, int idx, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj != null && !obj.getClass().isArray()) {
            // in this case obj will be boxed, and we need to pull the taint out of val when
            // we unbox it
            if (obj instanceof LazyBooleanArrayObjTags) {
                setBoolean(obj, idx, (Boolean) val, phosphorStackFrame);
            } else if (obj instanceof LazyByteArrayObjTags) {
                setByte(obj, idx, (Byte) val, phosphorStackFrame);
            } else if (obj instanceof LazyCharArrayObjTags) {
                setChar(obj, idx, (Character) val, phosphorStackFrame);
            } else if (obj instanceof LazyDoubleArrayObjTags) {
                setDouble(obj, idx, (Double) val, phosphorStackFrame);
            } else if (obj instanceof LazyFloatArrayObjTags) {
                setFloat(obj, idx, (Float) val, phosphorStackFrame);
            } else if (obj instanceof LazyIntArrayObjTags) {
                setInt(obj, idx, (Integer) val, phosphorStackFrame);
            } else if (obj instanceof LazyLongArrayObjTags) {
                setLong(obj, idx, (Long) val, phosphorStackFrame);
            } else if (obj instanceof LazyShortArrayObjTags) {
                setShort(obj, idx, (Short) val, phosphorStackFrame);
            } else if (obj instanceof LazyReferenceArrayObjTags) {
                ((LazyReferenceArrayObjTags) obj).set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
            } else {
                throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
            }
        } else {
            Array.set(obj, idx, val);
        }
    }

    public static void setBoolean(Object obj, int idx, boolean val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyBooleanArrayObjTags) {
            LazyBooleanArrayObjTags a = (LazyBooleanArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setByte(Object obj, int idx, byte val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyByteArrayObjTags) {
            LazyByteArrayObjTags a = (LazyByteArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
        }
    }

    public static void setChar(Object obj, int idx, char val, PhosphorStackFrame phosphorStackFrame){
        if (obj instanceof LazyCharArrayObjTags) {
            LazyCharArrayObjTags a = (LazyCharArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setDouble(Object obj, int idx, double val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyDoubleArrayObjTags) {
            LazyDoubleArrayObjTags a = (LazyDoubleArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setFloat(Object obj, int idx, float val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyFloatArrayObjTags) {
            LazyFloatArrayObjTags a = (LazyFloatArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setInt(Object obj, int idx, int val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyIntArrayObjTags) {
            LazyIntArrayObjTags a = (LazyIntArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setLong(Object obj, int idx, long val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyLongArrayObjTags) {
            LazyLongArrayObjTags a = (LazyLongArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setShort(Object obj, int idx, short val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof LazyShortArrayObjTags) {
            LazyShortArrayObjTags a = (LazyShortArrayObjTags) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }
}
