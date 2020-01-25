package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_SHORT_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_SHORT_ARRAY_SET;

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

    @InvokedViaInstrumentation(record = TAINTED_SHORT_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, short val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyShortArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, short val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_SHORT_ARRAY_GET)
    public TaintedShortWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedShortWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public static LazyShortArrayObjTags factory(Taint referenceTaint, short[] array) {
        if(array == null) {
            return null;
        }
        return new LazyShortArrayObjTags(referenceTaint, array);
    }

    public TaintedShortWithObjTag get(int idx, TaintedShortWithObjTag ret) {
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

    public void ensureVal(short[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static short[] unwrap(LazyShortArrayObjTags obj) {
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
            for (short el : val) {
                stream.writeShort(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new short[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readShort();
            }
        }
    }
}
