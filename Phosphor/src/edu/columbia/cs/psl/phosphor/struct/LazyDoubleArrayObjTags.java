package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyDoubleArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -3150902081462512196L;

    public double[] val;

    public LazyDoubleArrayObjTags(int len) {
        val = new double[len];
    }

    public LazyDoubleArrayObjTags(double[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyDoubleArrayObjTags(double[] array) {
        this.val = array;
    }

    public LazyDoubleArrayObjTags(Taint lenTaint, double[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyDoubleArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(double[] arr, Taint idxTag, int idx, double val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(double[] l, Taint idxTag, int idx, Taint tag, double val) {
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

    public void set(double[] arr, int idx, Taint tag, double val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(double[] arr, Taint idxTag, int idx, Taint tag, double val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(double[] arr, int idx, Taint tag, double val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedDoubleWithObjTag get(double[] arr, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedDoubleWithObjTag get(double[] arr, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedDoubleWithObjTag get(double[] arr, int idx, TaintedDoubleWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedDoubleWithObjTag get(double[] arr, int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack ctrl) {
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

    public void ensureVal(double[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(double el : val) {
                stream.writeDouble(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new double[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readDouble();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


