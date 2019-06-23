package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyLongArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -6718708899858849708L;

    public long[] val;

    public LazyLongArrayObjTags(int len) {
        val = new long[len];
    }

    public LazyLongArrayObjTags(long[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyLongArrayObjTags(long[] array) {
        this.val = array;
    }

    public LazyLongArrayObjTags(Taint lenTaint, long[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyLongArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(long[] arr, Taint idxTag, int idx, long val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(long[] l, Taint idxTag, int idx, Taint tag, long val) {
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

    public void set(long[] arr, int idx, Taint tag, long val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(long[] arr, Taint idxTag, int idx, Taint tag, long val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(long[] arr, int idx, Taint tag, long val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedLongWithObjTag get(long[] arr, Taint idxTaint, int idx, TaintedLongWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedLongWithObjTag get(long[] arr, Taint idxTaint, int idx, TaintedLongWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedLongWithObjTag get(long[] arr, int idx, TaintedLongWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedLongWithObjTag get(long[] arr, int idx, TaintedLongWithObjTag ret, ControlTaintTagStack ctrl) {
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

    public void ensureVal(long[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(long el : val) {
                stream.writeLong(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new long[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readLong();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


