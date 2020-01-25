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

    public TaintedShortWithObjTag arrayGet(LazyShortArrayObjTags b, Taint idxTaint, int idx, TaintedShortWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedIntWithObjTag arrayGet(LazyIntArrayObjTags b, Taint idxTaint, int idx, TaintedIntWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedByteWithObjTag arrayGet(LazyByteArrayObjTags b, Taint idxTaint, int idx, TaintedByteWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedBooleanWithObjTag arrayGet(LazyBooleanArrayObjTags b, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedLongWithObjTag arrayGet(LazyLongArrayObjTags b, Taint idxTaint, int idx, TaintedLongWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedFloatWithObjTag arrayGet(LazyFloatArrayObjTags b, Taint idxTaint, int idx, TaintedFloatWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedDoubleWithObjTag arrayGet(LazyDoubleArrayObjTags b, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public TaintedCharWithObjTag arrayGet(LazyCharArrayObjTags b, Taint idxTaint, int idx, TaintedCharWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }


    public TaintedReferenceWithObjTag arrayGet(LazyReferenceArrayObjTags b, Taint idxTaint, int idx, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl) {
        ret.val = b.val[idx];
        if(b.taints == null) {
            ret.taint = idxTaint;
        } else {
            ret.taint = Taint.combineTags(idxTaint, b.taints[idx]);
        }
        return ret;
    }

    public Taint arraySet(Taint referenceTaint, LazyShortArrayObjTags a, Taint idxTaint, int idx, Taint t, short v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyIntArrayObjTags a, Taint idxTaint, int idx, Taint t, int v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyByteArrayObjTags a, Taint idxTaint, int idx, Taint t, byte v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyBooleanArrayObjTags a, Taint idxTaint, int idx, Taint t, boolean v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyCharArrayObjTags a, Taint idxTaint, int idx, Taint t, char v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyFloatArrayObjTags a, Taint idxTaint, int idx, Taint t, float v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyDoubleArrayObjTags a, Taint idxTaint, int idx, Taint t, double v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyLongArrayObjTags a, Taint idxTaint, int idx, Taint t, long v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

    public Taint arraySet(Taint referenceTaint, LazyReferenceArrayObjTags a, Taint idxTaint, int idx, Taint t, Object v, ControlFlowStack ctrl) {
        return Taint.combineTags(t, Taint.combineTags(idxTaint, ctrl));
    }

}
