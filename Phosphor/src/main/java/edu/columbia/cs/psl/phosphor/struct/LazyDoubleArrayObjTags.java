package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_DOUBLE_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_DOUBLE_ARRAY_SET;

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

    @InvokedViaInstrumentation(record = TAINTED_DOUBLE_ARRAY_SET)
    public void set(int idx, double val, Taint idxTaint, Taint valTaint, PhosphorStackFrame stackFrame) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(this, idx, val, idxTaint, valTaint, stackFrame));
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

    @InvokedViaInstrumentation(record = TAINTED_DOUBLE_ARRAY_GET)
    public double get(int idx, Taint idxTaint, PhosphorStackFrame ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idx, idxTaint, ret);
    }

    public static LazyDoubleArrayObjTags factory(double[] array) {
        if(array == null) {
            return null;
        }
        return new LazyDoubleArrayObjTags(array);
    }

    public static LazyDoubleArrayObjTags factory(double[] array, Taint lengthTaint) {
        if(array == null) {
            return null;
        }
        return new LazyDoubleArrayObjTags(lengthTaint, array);
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


