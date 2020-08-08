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
        if(obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if(obj instanceof LazyArrayObjTags) {
            return Array.getLength(((LazyArrayObjTags) obj).getVal());
        }
        throw new ArrayStoreException("Uknown array type: " + obj.getClass());
    }

    public static TaintedIntWithObjTag getLength$$PHOSPHORTAGGED(Object obj, Taint objTaint, TaintedIntWithObjTag ret) {
        if(obj.getClass().isArray()) {
            ret.taint = null;
            ret.val = Array.getLength(obj);
            return ret;
        } else if(obj instanceof LazyArrayObjTags) {
            ret.taint = null;
            ret.val = Array.getLength(((LazyArrayObjTags) obj).getVal());
            return ret;
        }
        throw new IllegalArgumentException("Not an array type: " + obj.getClass());
    }

    public static TaintedIntWithObjTag getLength$$PHOSPHORTAGGED(Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        return getLength$$PHOSPHORTAGGED(obj, objTaint, ret);
    }

    public static TaintedReferenceWithObjTag newInstance$$PHOSPHORTAGGED(Class clazz, Taint clazzTaint, int len, Taint lenTaint, ControlFlowStack zz, TaintedReferenceWithObjTag ret, Object e) {
        return newInstance$$PHOSPHORTAGGED(clazz, clazzTaint, len, lenTaint, ret, e);
    }

    public static TaintedReferenceWithObjTag newInstance$$PHOSPHORTAGGED(Class clazz, Taint clazzTaint, int len, Taint lenTaint, TaintedReferenceWithObjTag ret, Object e) {
        Class tmp = clazz;
        ret.taint = Taint.emptyTaint();
        if(tmp.isArray()) {
            return newInstance$$PHOSPHORTAGGED(clazz, clazzTaint, new LazyIntArrayObjTags(new int[]{len}), lenTaint, ret, e);
        } else {
            if(tmp == Double.TYPE) {
                ret.val = new LazyDoubleArrayObjTags(new double[len]);
                return ret;
            }
            if(tmp == Float.TYPE) {
                ret.val = new LazyFloatArrayObjTags(new float[len]);
                return ret;
            }
            if(tmp == Integer.TYPE) {
                ret.val = new LazyIntArrayObjTags(new int[len]);
                return ret;
            }
            if(tmp == Long.TYPE) {
                ret.val = new LazyLongArrayObjTags(new long[len]);
                return ret;
            }
            if(tmp == Short.TYPE) {
                ret.val = new LazyShortArrayObjTags(new short[len]);
                return ret;
            }
            if(tmp == Boolean.TYPE) {
                ret.val = new LazyBooleanArrayObjTags(new boolean[len]);
                return ret;
            }
            if(tmp == Byte.TYPE) {
                ret.val = new LazyByteArrayObjTags(new byte[len]);
                return ret;
            }
            if(tmp == Character.TYPE) {
                ret.val = new LazyCharArrayObjTags(new char[len]);
                return ret;
            }
            ret.val = new LazyReferenceArrayObjTags((Object[]) Array.newArray(tmp, len));
            return ret;
        }
    }

    public static TaintedReferenceWithObjTag newInstance$$PHOSPHORTAGGED(Class clazz, Taint clazztaint, LazyIntArrayObjTags dimsTaint, Taint actualDimsTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, Object e) {
        return newInstance$$PHOSPHORTAGGED(clazz, clazztaint, dimsTaint, actualDimsTaint, ret, e);
    }

    public static LazyArrayObjTags newInstanceForType(int componentSort, int len) {
        switch(componentSort) {
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

    public static TaintedReferenceWithObjTag newInstance$$PHOSPHORTAGGED(Class clazz, Taint clazzTaint, LazyIntArrayObjTags dimstaint, Taint dimsActualTaint, TaintedReferenceWithObjTag _ret, Object e) {
        int[] dims = dimstaint.val;
        _ret.taint = Taint.emptyTaint();
        Type t = Type.getType(clazz);
        if(t.getSort() == Type.ARRAY) {
            int innerDims = t.getDimensions();
            if(innerDims == 1) {
                //2D array, init only the top level
                switch(t.getElementType().getSort()) {
                    case Type.BOOLEAN:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_Z_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.BYTE:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_B_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.CHAR:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_C_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.DOUBLE:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_D_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.FLOAT:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_F_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.INT:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_I_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.LONG:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_J_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.SHORT:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_S_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0));
                        break;
                    case Type.OBJECT:
                        _ret.val = MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), clazz.getComponentType());
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else if(dims.length == 1) {
            if(t.getSort() == Type.OBJECT) {
                _ret.val = new LazyReferenceArrayObjTags((Object[]) Array.newArray(clazz, dims[0]));
            } else {
                _ret.val = newInstanceForType(t.getSort(), dims[0]);
            }
        } else if(dims.length == 2) {
            //2D array, init all levels
            switch(t.getSort()) {
                case Type.BOOLEAN:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_Z_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.BYTE:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_B_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.CHAR:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_C_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.DOUBLE:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_D_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.FLOAT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_F_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.INT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_I_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.LONG:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_J_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.SHORT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_S_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1));
                    break;
                case Type.OBJECT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_2DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), clazz);
                    break;
            }
        } else if(dims.length == 3) {
            switch(t.getSort()) {
                case Type.BOOLEAN:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_Z_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.BYTE:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_B_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.CHAR:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_C_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.DOUBLE:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_D_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.FLOAT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_F_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.INT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_I_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.LONG:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_J_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.SHORT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_S_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2));
                    break;
                case Type.OBJECT:
                    _ret.val = MultiDTaintedArray.MULTIANEWARRAY_REFERENCE_3DIMS(dims[0], dimstaint.getTaintOrEmpty(0), dims[1], dimstaint.getTaintOrEmpty(1), dims[2], dimstaint.getTaintOrEmpty(2), clazz);
                    break;
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return _ret;
    }

    public static TaintedReferenceWithObjTag get$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, Object e) {
        return get$$PHOSPHORTAGGED(obj, null, idx, null, ret, e);
    }

    static void fillInTaint(TaintedReferenceWithObjTag ret, LazyArrayObjTags ar, int idx) {
        if(ar.taints != null) {
            ret.taint = ar.taints[idx];
        } else {
            ret.taint = Taint.emptyTaint();
        }
    }

    public static TaintedReferenceWithObjTag get$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedReferenceWithObjTag ret, Object e) {
        if(obj instanceof LazyBooleanArrayObjTags) {
            ret.val = ((LazyBooleanArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyByteArrayObjTags) {
            ret.val = ((LazyBooleanArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyCharArrayObjTags) {
            ret.val = ((LazyCharArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyDoubleArrayObjTags) {
            ret.val = ((LazyDoubleArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyFloatArrayObjTags) {
            ret.val = ((LazyFloatArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyIntArrayObjTags) {
            ret.val = ((LazyIntArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyLongArrayObjTags) {
            ret.val = ((LazyLongArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyShortArrayObjTags) {
            ret.val = ((LazyShortArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        } else if(obj instanceof LazyReferenceArrayObjTags) {
            ret.val = ((LazyReferenceArrayObjTags) obj).val[idx];
            fillInTaint(ret, (LazyArrayObjTags) obj, idx);
            return ret;
        }
        ret.taint = Taint.emptyTaint(); //TODO reference tainting
        ret.val = Array.get(obj, idx);
        return ret;
    }

    public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedByteWithObjTag ret) {
        if(obj instanceof LazyByteArrayObjTags) {
            return ((LazyByteArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        return getBoolean$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        return getInt$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedCharWithObjTag ret) {
        return getChar$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedDoubleWithObjTag ret) {
        return getDouble$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedFloatWithObjTag ret) {
        return getFloat$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedShortWithObjTag ret) {
        return getShort$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedLongWithObjTag ret) {
        return getLong$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedByteWithObjTag ret) {
        return getByte$$PHOSPHORTAGGED(obj, objTaint, idx, idxTaint, ret);
    }

    public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedBooleanWithObjTag ret) {
        if(obj instanceof LazyBooleanArrayObjTags) {
            return ((LazyBooleanArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedCharWithObjTag ret) {
        if(obj instanceof LazyCharArrayObjTags) {
            return ((LazyCharArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedDoubleWithObjTag ret) {
        if(obj instanceof LazyDoubleArrayObjTags) {
            return ((LazyDoubleArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedIntWithObjTag ret) {
        if(obj instanceof LazyIntArrayObjTags) {
            return ((LazyIntArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedLongWithObjTag ret) {
        if(obj instanceof LazyLongArrayObjTags) {
            return ((LazyLongArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedShortWithObjTag ret) {
        if(obj instanceof LazyShortArrayObjTags) {
            return ((LazyShortArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Taint objTaint, int idx, Taint idxTaint, TaintedFloatWithObjTag ret) {
        if(obj instanceof LazyFloatArrayObjTags) {
            return ((LazyFloatArrayObjTags) obj).get(idx, ret);
        }
        throw new ArrayStoreException("Called getX, but don't have tainted X array!");
    }

    public static void set$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, Object val, Taint valTaint) {
        if(obj != null && !obj.getClass().isArray()) {
            //in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
            if(obj instanceof LazyBooleanArrayObjTags) {
                setBoolean$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Boolean) val, valTaint);
            } else if(obj instanceof LazyByteArrayObjTags) {
                setByte$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Byte) val, valTaint);
            } else if(obj instanceof LazyCharArrayObjTags) {
                setChar$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Character) val, valTaint);
            } else if(obj instanceof LazyDoubleArrayObjTags) {
                setDouble$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Double) val, valTaint);
            } else if(obj instanceof LazyFloatArrayObjTags) {
                setFloat$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Float) val, valTaint);
            } else if(obj instanceof LazyIntArrayObjTags) {
                setInt$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Integer) val, valTaint);
            } else if(obj instanceof LazyLongArrayObjTags) {
                setLong$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Long) val, valTaint);
            } else if (obj instanceof LazyShortArrayObjTags) {
                setShort$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Short) val, valTaint);
            } else if (obj instanceof LazyReferenceArrayObjTags) {
                ((LazyReferenceArrayObjTags) obj).set(referenceTaint, idx, idxTaint, val, valTaint);
            } else {
                throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
            }
        } else {
            Array.set(obj, idx, val);
        }
    }

    public static void set$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, Object val, Taint valTaint, ControlFlowStack ctrl) {
        if(obj != null && !obj.getClass().isArray()) {
            //in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
            if(obj instanceof LazyBooleanArrayObjTags) {
                setBoolean$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Boolean) val, valTaint, ctrl);
            } else if(obj instanceof LazyByteArrayObjTags) {
                setByte$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Byte) val, valTaint, ctrl);
            } else if(obj instanceof LazyCharArrayObjTags) {
                setChar$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Character) val, valTaint, ctrl);
            } else if(obj instanceof LazyDoubleArrayObjTags) {
                setDouble$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Double) val, valTaint, ctrl);
            } else if(obj instanceof LazyFloatArrayObjTags) {
                setFloat$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Float) val, valTaint, ctrl);
            } else if(obj instanceof LazyIntArrayObjTags) {
                setInt$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Integer) val, valTaint, ctrl);
            } else if(obj instanceof LazyLongArrayObjTags) {
                setLong$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Long) val, valTaint, ctrl);
            } else if(obj instanceof LazyShortArrayObjTags) {
                setShort$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, (Short) val, valTaint, ctrl);
            } else if(obj instanceof LazyReferenceArrayObjTags){
                ((LazyReferenceArrayObjTags) obj).set(referenceTaint, idx, idxTaint, val, Taint.combineTags(valTaint, ctrl));
            } else {
                throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
            }
        } else {
            Array.set(obj, idx, val);
        }
    }

    public static void setBoolean$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, boolean val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setBoolean$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setByte$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, byte val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setByte$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setChar$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, char val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setChar$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setDouble$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, double val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setDouble$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setFloat$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, float val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setFloat$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setInt$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, int val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setInt$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setLong$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, long val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setLong$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setShort$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, short val, Taint taint, ControlFlowStack ctrl) {
        taint = Taint.combineTags(taint, ctrl);
        setShort$$PHOSPHORTAGGED(obj, referenceTaint, idx, idxTaint, val, taint);
    }

    public static void setBoolean$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, boolean val, Taint taint) {
        if(obj instanceof LazyBooleanArrayObjTags) {
            LazyBooleanArrayObjTags a = (LazyBooleanArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setByte$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, byte val, Taint taint) {
        if(obj instanceof LazyByteArrayObjTags) {
            LazyByteArrayObjTags a = (LazyByteArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
        }
    }

    public static void setChar$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, char val, Taint taint) {
        if(obj instanceof LazyCharArrayObjTags) {
            LazyCharArrayObjTags a = (LazyCharArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setDouble$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, double val, Taint taint) {
        if(obj instanceof LazyDoubleArrayObjTags) {
            LazyDoubleArrayObjTags a = (LazyDoubleArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setFloat$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, float val, Taint taint) {
        if(obj instanceof LazyFloatArrayObjTags) {
            LazyFloatArrayObjTags a = (LazyFloatArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setInt$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, int val, Taint taint) {
        if(obj instanceof LazyIntArrayObjTags) {
            LazyIntArrayObjTags a = (LazyIntArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setLong$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, long val, Taint taint) {
        if(obj instanceof LazyLongArrayObjTags) {
            LazyLongArrayObjTags a = (LazyLongArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }

    public static void setShort$$PHOSPHORTAGGED(Object obj, Taint referenceTaint, int idx, Taint idxTaint, short val, Taint taint) {
        if(obj instanceof LazyShortArrayObjTags) {
            LazyShortArrayObjTags a = (LazyShortArrayObjTags) obj;
            a.set(referenceTaint, idx, idxTaint, val, taint);
        } else {
            throw new ArrayStoreException("Called setX, but don't have tainted X array!");
        }
    }
}
