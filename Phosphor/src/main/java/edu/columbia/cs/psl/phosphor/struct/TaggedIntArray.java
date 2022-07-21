package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_INT_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_INT_ARRAY_SET;

public final class TaggedIntArray extends TaggedArray {

    private static final long serialVersionUID = 5106261987550616988L;

    public int[] val;

    public TaggedIntArray(int len) {
        val = new int[len];
    }

    public TaggedIntArray(int[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public TaggedIntArray(int[] array) {
        this.val = array;
    }

    public TaggedIntArray(Taint lenTaint, int[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @InvokedViaInstrumentation(record = TAINTED_INT_ARRAY_SET)
    public void set(int idx, int val, Taint idxTaint, Taint valTaint, PhosphorStackFrame stackFrame) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(this, idx, val, idxTaint, valTaint, stackFrame));
    }

    @Override
    public Object clone() {
        return new TaggedIntArray(val.clone(), (taints != null) ? taints.clone() : null);
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
    public int get(int idx, Taint idxTaint, PhosphorStackFrame ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idx, idxTaint, ret);
    }

    public static TaggedIntArray factory(int[] array) {
        if(array == null) {
            return null;
        }
        return new TaggedIntArray(array);
    }

    public static TaggedIntArray factory(int[] array, Taint lengthTaint) {
        if(array == null) {
            return null;
        }
        return new TaggedIntArray(lengthTaint, array);
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

    public static int[] unwrap(TaggedIntArray obj) {
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


