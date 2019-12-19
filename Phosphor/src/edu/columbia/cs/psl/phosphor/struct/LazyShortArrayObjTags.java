package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyShortArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -4189650314277328488L;

    public short[] val;

    public LazyShortArrayObjTags(int len) {
        val = new short[len];
    }

    public LazyShortArrayObjTags(short[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyShortArrayObjTags(short[] array) {
        this.val = array;
    }

    public LazyShortArrayObjTags(Taint lenTaint, short[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyShortArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(short[] arr, Taint idxTag, int idx, short val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(short[] l, Taint idxTag, int idx, Taint tag, short val) {
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

    public void set(short[] arr, int idx, Taint tag, short val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(short[] arr, Taint idxTag, int idx, Taint tag, short val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(short[] arr, int idx, Taint tag, short val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedShortWithObjTag get(short[] arr, Taint idxTaint, int idx, TaintedShortWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedShortWithObjTag get(short[] arr, Taint idxTaint, int idx, TaintedShortWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedShortWithObjTag get(short[] arr, int idx, TaintedShortWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedShortWithObjTag get(short[] arr, int idx, TaintedShortWithObjTag ret, ControlTaintTagStack ctrl) {
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

    public void ensureVal(short[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(short el : val) {
                stream.writeShort(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new short[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readShort();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}
