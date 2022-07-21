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

    public short arrayGet(TaggedShortArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public int arrayGet(TaggedIntArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public byte arrayGet(TaggedByteArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public boolean arrayGet(TaggedBooleanArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public long arrayGet(TaggedLongArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public float arrayGet(TaggedFloatArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public double arrayGet(TaggedDoubleArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public char arrayGet(TaggedCharArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }


    public Object arrayGet(TaggedReferenceArray b, int idx, Taint idxTaint, PhosphorStackFrame ret) {
        if(b.taints == null) {
            ret.setReturnTaint(idxTaint);
        } else {
            ret.setReturnTaint(Taint.combineTags(idxTaint, b.taints[idx]));
        }
        return b.val[idx];
    }

    public Taint arraySet(TaggedShortArray a, int idx, short v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedIntArray a, int idx, int v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedByteArray a, int idx, byte v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedBooleanArray a, int idx, boolean v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedCharArray a, int idx, char v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedFloatArray a, int idx, float v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedDoubleArray a, int idx, double v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedLongArray a, int idx, long v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

    public Taint arraySet(TaggedReferenceArray a, int idx, Object v, Taint idxTaint, Taint valTaint, PhosphorStackFrame ctrl) {
        return Taint.combineTags(idxTaint, Taint.combineTags(valTaint, ctrl));
    }

}
