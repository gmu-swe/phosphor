package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyBooleanArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -608551807469942060L;

    public boolean[] val;

    public LazyBooleanArrayObjTags(int len) {
        val = new boolean[len];
    }

    public LazyBooleanArrayObjTags(boolean[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyBooleanArrayObjTags(boolean[] array) {
        this.val = array;
    }

    public LazyBooleanArrayObjTags(Taint lenTaint, boolean[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyBooleanArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(boolean[] arr, Taint idxTag, int idx, boolean val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(boolean[] l, Taint idxTag, int idx, Taint tag, boolean val) {
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

    public void set(boolean[] arr, int idx, Taint tag, boolean val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(boolean[] arr, Taint idxTag, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(boolean[] arr, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedBooleanWithObjTag get(boolean[] arr, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedBooleanWithObjTag get(boolean[] arr, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedBooleanWithObjTag get(boolean[] arr, int idx, TaintedBooleanWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedBooleanWithObjTag get(boolean[] arr, int idx, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
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

    public void ensureVal(boolean[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(boolean el : val) {
                stream.writeBoolean(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new boolean[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readBoolean();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


