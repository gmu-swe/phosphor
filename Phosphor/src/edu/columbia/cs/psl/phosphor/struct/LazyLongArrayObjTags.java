package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_LONG_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_LONG_ARRAY_SET;

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

    @InvokedViaInstrumentation(record = TAINTED_LONG_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, long val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyLongArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
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
    public TaintedLongWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedLongWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public static LazyLongArrayObjTags factory(Taint referenceTaint, long[] array) {
        if(array == null) {
            return null;
        }
        return new LazyLongArrayObjTags(referenceTaint, array);
    }

    public TaintedLongWithObjTag get(int idx, TaintedLongWithObjTag ret) {
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

    public void ensureVal(long[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static long[] unwrap(LazyLongArrayObjTags obj) {
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


