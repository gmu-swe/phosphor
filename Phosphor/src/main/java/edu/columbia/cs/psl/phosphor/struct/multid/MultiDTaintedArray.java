package edu.columbia.cs.psl.phosphor.struct.multid;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.BOX_IF_NECESSARY;
import static org.objectweb.asm.Opcodes.*;

public abstract class MultiDTaintedArray {

    public static Object[] unboxCK_ATTRIBUTE(Object[] in) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if (in == null || in[0] == null) {
            return null;
        }
        boolean needsFix = false;
        Field f = in[0].getClass().getDeclaredField("pValue");
        for (Object a : in) {
            Object v = f.get(a);
            if (v instanceof LazyArrayObjTags) {
                f.set(a, MultiDTaintedArrayWithObjTag.unboxRaw(v));
            }
        }
        return in;
    }

    // ============ START GENERATED ===========

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_B_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyByteArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyByteArrayObjTags(t2, new byte[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_B_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyByteArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_B_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyByteArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_B_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyByteArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyByteArrayObjTags(t3, new byte[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_B_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyByteArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_Z_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);

        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyBooleanArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyBooleanArrayObjTags(t2, new boolean[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_Z_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);

        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyBooleanArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_Z_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyBooleanArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_Z_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyBooleanArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyBooleanArrayObjTags(t3, new boolean[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_Z_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyBooleanArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_C_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyCharArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyCharArrayObjTags(t2, new char[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_C_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyCharArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_C_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyCharArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_C_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyCharArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyCharArrayObjTags(t3, new char[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_C_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyCharArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_F_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyFloatArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyFloatArrayObjTags(t2, new float[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_F_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyFloatArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_F_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyFloatArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_F_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyFloatArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyFloatArrayObjTags(t3, new float[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_F_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyFloatArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_I_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyIntArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyIntArrayObjTags(t2, new int[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_I_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyIntArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_I_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyIntArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_I_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyIntArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyIntArrayObjTags(t3, new int[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_I_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyIntArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_D_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyDoubleArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyDoubleArrayObjTags(t2, new double[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_D_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyDoubleArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_D_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyDoubleArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_D_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyDoubleArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyDoubleArrayObjTags(t3, new double[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_D_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyDoubleArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_S_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyShortArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyShortArrayObjTags(t2, new short[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_S_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint tag = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyShortArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_S_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyShortArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_S_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyShortArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyShortArrayObjTags(t3, new short[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_S_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyShortArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_J_2DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyLongArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyLongArrayObjTags(t2, new long[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_J_2DIMS(int dim, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyLongArrayObjTags[dim]);
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_J_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyLongArrayObjTags[dim2]);
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_J_3DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyLongArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyLongArrayObjTags(t3, new long[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_J_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyLongArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    // ============ END GENERATED =============

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_REFERENCE_4DIMS(int dim1, int dim2, int dim3, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyReferenceArrayObjTags[dim3]);
            }
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_REFERENCE_3DIMS(int dim1, int dim2, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
        }
        return ret;
    }


    public static LazyReferenceArrayObjTags MULTIANEWARRAY_REFERENCE_3DIMS(int dim1, int dim2, int dim3, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        Taint t3 = phosphorStackFrame.getArgTaint(2);

        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[i] = new LazyReferenceArrayObjTags(t3, (Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(component, dim3));
            }
        }
        return ret;
    }


    public static LazyReferenceArrayObjTags MULTIANEWARRAY_REFERENCE_2DIMS(int dim1, int dim2, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        Taint t2 = phosphorStackFrame.getArgTaint(1);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, (Object[]) InstrumentedJREMethodHelper.java_lang_reflect_Array_newArray(component, dim2));
        }
        return ret;
    }

    public static LazyReferenceArrayObjTags MULTIANEWARRAY_REFERENCE_2DIMS(int dim1, Class<?> component, PhosphorStackFrame phosphorStackFrame) {
        Taint t1 = phosphorStackFrame.getArgTaint(0);
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        return ret;
    }


    public static Object unbox1D(final Object in) {
        if (in instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) in).getVal();
        }
        return in;
    }

    public static Object unbox1DOrNull(final Object in) {
        if (in instanceof LazyReferenceArrayObjTags) {
            Object ret = ((LazyReferenceArrayObjTags) in).getVal();
            if (LazyArrayObjTags.class.isAssignableFrom(ret.getClass().getComponentType())) {
                //We never want to actually unbox these things.
                return null;
            }
            return ret;
        }
        if (in instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) in).getVal();
        }
        return in;
    }

    /* If the specified object is a one dimensional array of primitives, boxes and returns the specified object. Otherwise
     * returns the specified object. */
    public static Object boxOnly1D(final Object obj) {
        return MultiDTaintedArrayWithObjTag.boxOnly1D(obj);
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

    public static Type getTypeForType(final Type originalElementType) {
        return MultiDTaintedArrayWithObjTag.getTypeForType(originalElementType);
    }

    public static String isPrimitiveBoxClass(Class c) {
        return MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(c);
    }

    public static String getPrimitiveTypeForWrapper(Class c) {
        return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
    }

    public static Object unboxRaw(final Object in) {
        return MultiDTaintedArrayWithObjTag.unboxRaw(in);
    }

    public static int getSort(Class c) {
        return MultiDTaintedArrayWithObjTag.getSort(c);
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = BOX_IF_NECESSARY)
    public static Object boxIfNecessary(final Object in) {
        return MultiDTaintedArrayWithObjTag.boxIfNecessary(in);
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
        MultiDTaintedArrayWithObjTag.initLastDim(ar, lastDimSize, componentType);
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final Taint<?> dimTaint, final int lastDimSize, final int componentType) {
        MultiDTaintedArrayWithObjTag.initLastDim(ar, dimTaint, lastDimSize, componentType);
    }

    public static Type getPrimitiveTypeForWrapper(String internalName) {
        return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(internalName);
    }

    @SuppressWarnings("unused")
    public static Object unboxMethodReceiverIfNecessary(Method m, Object obj) {
        if (LazyArrayObjTags.class.isAssignableFrom(m.getDeclaringClass())) {
            return obj; // No unboxing is necessary
        } else {
            return unboxRaw(obj);
        }
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the internal name of the taint array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getTaintArrayInternalName(int typeOperand) {
        return MultiDTaintedArrayWithObjTag.getTaintArrayInternalName(typeOperand);
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
