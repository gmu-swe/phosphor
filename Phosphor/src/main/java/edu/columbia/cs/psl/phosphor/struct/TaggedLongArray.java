package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_LONG_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_LONG_ARRAY_SET;

public final class TaggedLongArray extends TaggedArray {

    private static final long serialVersionUID = -6718708899858849708L;

    public long[] val;

    public TaggedLongArray(int len) {
        val = new long[len];
    }

    public TaggedLongArray(long[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public TaggedLongArray(long[] array) {
        this.val = array;
    }

    public TaggedLongArray(Taint lenTaint, long[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @InvokedViaInstrumentation(record = TAINTED_LONG_ARRAY_SET)
    public void set(int idx, long val, Taint idxTaint, Taint valTaint, PhosphorStackFrame stackFrame) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(this, idx, val, idxTaint, valTaint, stackFrame));
    }

    @Override
    public Object clone() {
        return new TaggedLongArray(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, long val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_LONG_ARRAY_GET)
    public long get(int idx, Taint idxTaint, PhosphorStackFrame ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idx, idxTaint, ret);
    }

    public static TaggedLongArray factory(long[] array) {
        if(array == null) {
            return null;
        }
        return new TaggedLongArray(array);
    }

    public static TaggedLongArray factory(long[] array, Taint lengthTaint) {
        if(array == null) {
            return null;
        }
        return new TaggedLongArray(lengthTaint, array);
    }

    public int getLength() {
        return val.length;
    }

    @Override
    public Object getVal() {
        return val;
    }

    public void ensureVal(long[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static long[] unwrap(TaggedLongArray obj) {
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
            for (long el : val) {
                stream.writeLong(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new long[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readLong();
            }
        }
    }
}


