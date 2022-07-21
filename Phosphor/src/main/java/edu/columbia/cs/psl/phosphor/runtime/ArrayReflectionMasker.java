package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;

public class ArrayReflectionMasker {

    private ArrayReflectionMasker() {
        // Prevents this class from being instantiated
    }

    public static int getLength(Object obj) {
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof TaggedArray) {
            return Array.getLength(((TaggedArray) obj).getVal());
        }
        throw new ArrayStoreException("Uknown array type: " + obj.getClass());
    }

    public static int getLength(Object obj, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof TaggedArray) {
            return Array.getLength(((TaggedArray) obj).getVal());
        }
        throw new IllegalArgumentException("Not an array type: " + obj.getClass());
    }

    public static Object newInstance(Class clazz, int len,
            PhosphorStackFrame phosphorStackFrame) {
        Class tmp = clazz;
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        if (tmp.isArray()) {
            return newInstance(clazz, new int[] {len}, phosphorStackFrame);
        } else {
            if (tmp == Double.TYPE) {
                return new TaggedDoubleArray(new double[len]);
            }
            if (tmp == Float.TYPE) {
                return new TaggedFloatArray(new float[len]);
            }
            if (tmp == Integer.TYPE) {
                return new TaggedIntArray(new int[len]);
            }
            if (tmp == Long.TYPE) {
                return new TaggedLongArray(new long[len]);
            }
            if (tmp == Short.TYPE) {
                return new TaggedShortArray(new short[len]);
            }
            if (tmp == Boolean.TYPE) {
                return new TaggedBooleanArray(new boolean[len]);
            }
            if (tmp == Byte.TYPE) {
                return new TaggedByteArray(new byte[len]);
            }
            if (tmp == Character.TYPE) {
                return new TaggedCharArray(new char[len]);
            }
            return new TaggedReferenceArray((Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(tmp, len));
        }
    }

    public static TaggedArray newInstanceForType(int componentSort, int len) {
        switch (componentSort) {
            case Type.BOOLEAN:
                return new TaggedBooleanArray(new boolean[len]);
            case Type.BYTE:
                return new TaggedByteArray(new byte[len]);
            case Type.CHAR:
                return new TaggedCharArray(new char[len]);
            case Type.DOUBLE:
                return new TaggedDoubleArray(new double[len]);
            case Type.FLOAT:
                return new TaggedFloatArray(new float[len]);
            case Type.INT:
                return new TaggedIntArray(new int[len]);
            case Type.LONG:
                return new TaggedLongArray(new long[len]);
            case Type.SHORT:
                return new TaggedShortArray(new short[len]);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Object newInstance(Class clazz,
            int[] dims, PhosphorStackFrame stackFrame) {
        stackFrame.returnTaint = Taint.emptyTaint();
        Type t = Type.getType(clazz);
        TaggedIntArray dimstaint = (TaggedIntArray) stackFrame.getArgWrapper(0, dims);
        if (t.getSort() == Type.ARRAY) {
            int innerDims = t.getDimensions();
            if (innerDims == 1) {
                // 2D array, init only the top level
                switch (t.getElementType().getSort()) {
                    case Type.BOOLEAN:
                        return MultiDArrayUtils.MULTIANEWARRAY_Z_2DIMS(dims[0], stackFrame);
                    case Type.BYTE:
                        return MultiDArrayUtils.MULTIANEWARRAY_B_2DIMS(dims[0], stackFrame);
                    case Type.CHAR:
                        return MultiDArrayUtils.MULTIANEWARRAY_C_2DIMS(dims[0], stackFrame);
                    case Type.DOUBLE:
                        return MultiDArrayUtils.MULTIANEWARRAY_D_2DIMS(dims[0], stackFrame);
                    case Type.FLOAT:
                        return MultiDArrayUtils.MULTIANEWARRAY_F_2DIMS(dims[0], stackFrame);
                    case Type.INT:
                        return MultiDArrayUtils.MULTIANEWARRAY_I_2DIMS(dims[0], stackFrame);
                    case Type.LONG:
                        return MultiDArrayUtils.MULTIANEWARRAY_J_2DIMS(dims[0], stackFrame);
                    case Type.SHORT:
                        return MultiDArrayUtils.MULTIANEWARRAY_S_2DIMS(dims[0], stackFrame);
                    case Type.OBJECT:
                        return MultiDArrayUtils.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0], clazz.getComponentType(), stackFrame);
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else if(dims.length == 1) {
            if(t.getSort() == Type.OBJECT) {
                return new TaggedReferenceArray((Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(clazz, dims[0]));
            } else {
                return newInstanceForType(t.getSort(), dims[0]);
            }
        } else if (dims.length == 2) {
            // 2D array, init all levels
            switch (t.getSort()) {
                case Type.BOOLEAN:
                    return MultiDArrayUtils.MULTIANEWARRAY_Z_2DIMS(dims[0], dims[1], stackFrame);
                case Type.BYTE:
                    return MultiDArrayUtils.MULTIANEWARRAY_B_2DIMS(dims[0], dims[1], stackFrame);
                case Type.CHAR:
                    return MultiDArrayUtils.MULTIANEWARRAY_C_2DIMS(dims[0], dims[1], stackFrame);
                case Type.DOUBLE:
                    return MultiDArrayUtils.MULTIANEWARRAY_D_2DIMS(dims[0], dims[1], stackFrame);
                case Type.FLOAT:
                    return MultiDArrayUtils.MULTIANEWARRAY_F_2DIMS(dims[0], dims[1], stackFrame);
                case Type.INT:
                    return MultiDArrayUtils.MULTIANEWARRAY_I_2DIMS(dims[0], dims[1], stackFrame);
                case Type.LONG:
                    return MultiDArrayUtils.MULTIANEWARRAY_J_2DIMS(dims[0], dims[1], stackFrame);
                case Type.SHORT:
                    return MultiDArrayUtils.MULTIANEWARRAY_S_2DIMS(dims[0], dims[1], stackFrame);
                case Type.OBJECT:
                    return MultiDArrayUtils.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0],
                            dims[1], clazz, stackFrame);
            }
        } else if (dims.length == 3) {
            switch (t.getSort()) {
                case Type.BOOLEAN:
                    return MultiDArrayUtils.MULTIANEWARRAY_Z_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.BYTE:
                    return MultiDArrayUtils.MULTIANEWARRAY_B_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.CHAR:
                    return MultiDArrayUtils.MULTIANEWARRAY_C_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.DOUBLE:
                    return MultiDArrayUtils.MULTIANEWARRAY_D_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.FLOAT:
                    return MultiDArrayUtils.MULTIANEWARRAY_F_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.INT:
                    return MultiDArrayUtils.MULTIANEWARRAY_I_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.LONG:
                    return MultiDArrayUtils.MULTIANEWARRAY_J_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.SHORT:
                    return MultiDArrayUtils.MULTIANEWARRAY_S_3DIMS(dims[0], dims[1], dims[2], stackFrame);
                case Type.OBJECT:
                    return MultiDArrayUtils.MULTIANEWARRAY_REFERENCE_3DIMS(dims[0],
                            dims[1], dims[2], clazz, stackFrame);
            }
        }
        throw new UnsupportedOperationException();
    }

    static void fillInTaint(PhosphorStackFrame ret, TaggedArray ar, int idx) {
        if (ar.taints != null) {
            ret.returnTaint = ar.taints[idx];
        } else {
            ret.returnTaint = Taint.emptyTaint();
        }
    }

    public static Object get(Object obj, int idx, PhosphorStackFrame stackFrame) {
        if (obj instanceof TaggedBooleanArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedBooleanArray) obj).val[idx];
        } else if (obj instanceof TaggedByteArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedBooleanArray) obj).val[idx];
        } else if (obj instanceof TaggedCharArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedCharArray) obj).val[idx];
        } else if (obj instanceof TaggedDoubleArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedDoubleArray) obj).val[idx];
        } else if (obj instanceof TaggedFloatArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedFloatArray) obj).val[idx];
        } else if (obj instanceof TaggedIntArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedIntArray) obj).val[idx];
        } else if (obj instanceof TaggedLongArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedLongArray) obj).val[idx];
        } else if (obj instanceof TaggedShortArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedShortArray) obj).val[idx];
        } else if (obj instanceof TaggedReferenceArray) {
            fillInTaint(stackFrame, (TaggedArray) obj, idx);
            return ((TaggedReferenceArray) obj).val[idx];
        }
        stackFrame.returnTaint = Taint.emptyTaint();
        return Array.get(obj, idx);
    }

    public static byte getByte(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedByteArray) {
            return ((TaggedByteArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static boolean getBoolean(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedBooleanArray) {
            return ((TaggedBooleanArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static char getChar(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedCharArray) {
            return ((TaggedCharArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static double getDouble(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedDoubleArray) {
            return ((TaggedDoubleArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static int getInt(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedIntArray) {
            return ((TaggedIntArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static long getLong(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedLongArray) {
            return ((TaggedLongArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static short getShort(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedShortArray) {
            return ((TaggedShortArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static float getFloat(Object obj, int idx, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedFloatArray) {
            return ((TaggedFloatArray) obj).get(idx, phosphorStackFrame.getArgTaint(1), phosphorStackFrame);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static void set(Object obj, int idx, Object val, PhosphorStackFrame phosphorStackFrame) {
        if (obj != null && !obj.getClass().isArray()) {
            // in this case obj will be boxed, and we need to pull the taint out of val when
            // we unbox it
            if (obj instanceof TaggedBooleanArray) {
                setBoolean(obj, idx, (Boolean) val, phosphorStackFrame);
            } else if (obj instanceof TaggedByteArray) {
                setByte(obj, idx, (Byte) val, phosphorStackFrame);
            } else if (obj instanceof TaggedCharArray) {
                setChar(obj, idx, (Character) val, phosphorStackFrame);
            } else if (obj instanceof TaggedDoubleArray) {
                setDouble(obj, idx, (Double) val, phosphorStackFrame);
            } else if (obj instanceof TaggedFloatArray) {
                setFloat(obj, idx, (Float) val, phosphorStackFrame);
            } else if (obj instanceof TaggedIntArray) {
                setInt(obj, idx, (Integer) val, phosphorStackFrame);
            } else if (obj instanceof TaggedLongArray) {
                setLong(obj, idx, (Long) val, phosphorStackFrame);
            } else if (obj instanceof TaggedShortArray) {
                setShort(obj, idx, (Short) val, phosphorStackFrame);
            } else if (obj instanceof TaggedReferenceArray) {
                ((TaggedReferenceArray) obj).set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
            } else {
                throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
            }
        } else {
            Array.set(obj, idx, val);
        }
    }

    public static void setBoolean(Object obj, int idx, boolean val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedBooleanArray) {
            TaggedBooleanArray a = (TaggedBooleanArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setByte(Object obj, int idx, byte val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedByteArray) {
            TaggedByteArray a = (TaggedByteArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
        }
    }

    public static void setChar(Object obj, int idx, char val, PhosphorStackFrame phosphorStackFrame){
        if (obj instanceof TaggedCharArray) {
            TaggedCharArray a = (TaggedCharArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setDouble(Object obj, int idx, double val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedDoubleArray) {
            TaggedDoubleArray a = (TaggedDoubleArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setFloat(Object obj, int idx, float val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedFloatArray) {
            TaggedFloatArray a = (TaggedFloatArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setInt(Object obj, int idx, int val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedIntArray) {
            TaggedIntArray a = (TaggedIntArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setLong(Object obj, int idx, long val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedLongArray) {
            TaggedLongArray a = (TaggedLongArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setShort(Object obj, int idx, short val, PhosphorStackFrame phosphorStackFrame) {
        if (obj instanceof TaggedShortArray) {
            TaggedShortArray a = (TaggedShortArray) obj;
            a.set(idx, val, phosphorStackFrame.getArgTaint(1), phosphorStackFrame.getArgTaint(2), phosphorStackFrame);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }
}
