package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyFloatArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = 6150577887427835055L;

    public float[] val;

    public LazyFloatArrayObjTags(int len) {
        val = new float[len];
    }

    public LazyFloatArrayObjTags(float[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyFloatArrayObjTags(float[] array) {
        this.val = array;
    }

    public LazyFloatArrayObjTags(Taint lenTaint, float[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyFloatArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(float[] arr, Taint idxTag, int idx, float val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(float[] l, Taint idxTag, int idx, Taint tag, float val) {
        if(Configuration.derivedTaintListener != null) {
            set(l, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, null), val);
        } else if(idxTag == null) {
            set(l, idx, tag, val);
        } else if(tag == null) {
            set(l, idx, idxTag, val);
        } else {
            set(l, idx, new Taint(tag, idxTag), val);
        }
    }

    public void set(float[] arr, int idx, Taint tag, float val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(float[] arr, Taint idxTag, int idx, Taint tag, float val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(float[] arr, int idx, Taint tag, float val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedFloatWithObjTag get(float[] arr, Taint idxTaint, int idx, TaintedFloatWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedFloatWithObjTag get(float[] arr, Taint idxTaint, int idx, TaintedFloatWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedFloatWithObjTag get(float[] arr, int idx, TaintedFloatWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedFloatWithObjTag get(float[] arr, int idx, TaintedFloatWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        get(arr, idx, ret);
        ret.taint = Taint.combineTags(ret.taint, ctrl);
        return ret;
    }

    public int getLength() {
        return val.length;
    }

    @Override
    public Object getVal() {
        return val;
    }

    public void ensureVal(float[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(float el : val) {
                stream.writeFloat(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new float[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readFloat();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


