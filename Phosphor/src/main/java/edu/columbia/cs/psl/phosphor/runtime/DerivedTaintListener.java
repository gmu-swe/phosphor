package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;

public class DerivedTaintListener {

    public void singleDepCreated(Taint in, Taint out) {

    }


    public void doubleDepCreated(Taint in1, Taint in2, Taint out) {

    }


    public void controlApplied(Object o, ControlFlowStack tags) {

    }

    public short arrayGet(LazyShortArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public int arrayGet(LazyIntArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public byte arrayGet(LazyByteArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public boolean arrayGet(LazyBooleanArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public long arrayGet(LazyLongArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public float arrayGet(LazyFloatArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public double arrayGet(LazyDoubleArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public char arrayGet(LazyCharArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }


    public Object arrayGet(LazyReferenceArrayObjTags b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public Taint arraySet(LazyShortArrayObjTags a, int idx, short v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyIntArrayObjTags a, int idx, int v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyByteArrayObjTags a, int idx, byte v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyBooleanArrayObjTags a, int idx, boolean v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyCharArrayObjTags a, int idx, char v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyFloatArrayObjTags a, int idx, float v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyDoubleArrayObjTags a, int idx, double v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyLongArrayObjTags a, int idx, long v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(LazyReferenceArrayObjTags a, int idx, Object v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

}
