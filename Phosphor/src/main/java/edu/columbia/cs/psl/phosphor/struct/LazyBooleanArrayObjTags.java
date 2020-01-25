package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BOOLEAN_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BOOLEAN_ARRAY_SET;

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

    @InvokedViaInstrumentation(record = TAINTED_BOOLEAN_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, boolean val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyBooleanArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, boolean val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_BOOLEAN_ARRAY_GET)
    public TaintedBooleanWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedBooleanWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public static LazyBooleanArrayObjTags factory(Taint referenceTaint, boolean[] array) {
        if(array == null) {
            return null;
        }
        return new LazyBooleanArrayObjTags(referenceTaint, array);
    }

    public TaintedBooleanWithObjTag get(int idx, TaintedBooleanWithObjTag ret) {
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

    public void ensureVal(boolean[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static boolean[] unwrap(LazyBooleanArrayObjTags obj) {
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
            for (boolean el : val) {
                stream.writeBoolean(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new boolean[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readBoolean();
            }
        }
    }
}


