package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BYTE_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BYTE_ARRAY_SET;

public final class LazyByteArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -4166037313218353751L;

    public byte[] val;

    public LazyByteArrayObjTags(int len) {
        val = new byte[len];
    }

    public LazyByteArrayObjTags(byte[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyByteArrayObjTags(byte[] array) {
        this.val = array;
    }

    public LazyByteArrayObjTags(Taint lenTaint, byte[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @InvokedViaInstrumentation(record = TAINTED_BYTE_ARRAY_SET)
    public void set(Taint referenceTaint, int idx, Taint idxTag, byte val, Taint tag) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(referenceTaint, this, idxTag, idx, tag, val, null));
    }

    @Override
    public Object clone() {
        return new LazyByteArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, byte val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_BYTE_ARRAY_GET)
    public TaintedByteWithObjTag get(Taint referenceTaint, int idx, Taint idxTaint, TaintedByteWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public static LazyByteArrayObjTags factory(Taint referenceTaint, byte[] array) {
        if(array == null) {
            return null;
        }
        return new LazyByteArrayObjTags(referenceTaint, array);
    }

    public TaintedByteWithObjTag get(int idx, TaintedByteWithObjTag ret) {
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

    public void ensureVal(byte[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static byte[] unwrap(LazyByteArrayObjTags obj) {
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
            for (byte el : val) {
                stream.writeByte(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new byte[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readByte();
            }
        }
    }
}


