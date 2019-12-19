package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyIntArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = 5106261987550616988L;

    public int[] val;

    public LazyIntArrayObjTags(int len) {
        val = new int[len];
    }

    public LazyIntArrayObjTags(int[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyIntArrayObjTags(int[] array) {
        this.val = array;
    }

    public LazyIntArrayObjTags(Taint lenTaint, int[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyIntArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int[] arr, Taint idxTag, int idx, int val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(int[] l, Taint idxTag, int idx, Taint tag, int val) {
        if(Configuration.derivedTaintListener != null) {
            set(l, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, null), val);
        } else if(idxTag == null) {
            set(l, idx, tag, val);
        } else if(tag == null) {
            set(l, idx, idxTag, val);
        } else {
            set(l, idx, tag.union(idxTag), val);
        }
    }

    public void set(int[] arr, int idx, Taint tag, int val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(int[] arr, Taint idxTag, int idx, Taint tag, int val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(int[] arr, int idx, Taint tag, int val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedIntWithObjTag get(int[] arr, Taint idxTaint, int idx, TaintedIntWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedIntWithObjTag get(int[] arr, Taint idxTaint, int idx, TaintedIntWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedIntWithObjTag get(int[] arr, int idx, TaintedIntWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedIntWithObjTag get(int[] arr, int idx, TaintedIntWithObjTag ret, ControlTaintTagStack ctrl) {
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

    public void ensureVal(int[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(int el : val) {
                stream.writeInt(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new int[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readInt();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


