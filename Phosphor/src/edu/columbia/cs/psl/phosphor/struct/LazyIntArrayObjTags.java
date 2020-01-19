package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_INT_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_INT_ARRAY_SET;

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

    @InvokedViaInstrumentation(record = TAINTED_INT_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, int val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyIntArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, int val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_INT_ARRAY_GET)
    public TaintedIntWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedIntWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public static LazyIntArrayObjTags factory(Taint referenceTaint, int[] array) {
        if(array == null) {
            return null;
        }
        return new LazyIntArrayObjTags(referenceTaint, array);
    }

    public TaintedIntWithObjTag get(int idx, TaintedIntWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? Taint.emptyTaint() : taints[idx];
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
        if (v != val) {
            val = v;
        }
    }

    public static int[] unwrap(LazyIntArrayObjTags obj) {
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
            for (int el : val) {
                stream.writeInt(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new int[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readInt();
            }
        }
    }
}


