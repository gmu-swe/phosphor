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

    public void set(Taint referenceTaint, int idx, Taint idxTag, double val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyDoubleArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, double val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(Taint referenceTaint, int idx, Taint idxTag, double val, Taint tag, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, ctrl));
    }

    public TaintedDoubleWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedDoubleWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedDoubleWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedDoubleWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public static LazyDoubleArrayObjTags factory(Taint referenceTaint, double[] array) {
        if(array == null) {
            return null;
        }
        return new LazyDoubleArrayObjTags(referenceTaint, array);
    }

    public TaintedDoubleWithObjTag get(int idx, TaintedDoubleWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? Taint.emptyTaint() : taints[idx];
        return ret;
    }

    public TaintedDoubleWithObjTag get(int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        get(idx, ret);
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
        if (v != val) {
            val = v;
        }
    }

    public static double[] unwrap(LazyDoubleArrayObjTags obj) {
        if (obj != null) {
            return obj.val;
        }
        return null;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for (double el : val) {
                stream.writeDouble(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new double[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readDouble();
            }
        }
    }
}


