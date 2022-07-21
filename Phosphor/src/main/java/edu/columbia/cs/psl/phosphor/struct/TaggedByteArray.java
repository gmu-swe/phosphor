package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BYTE_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_BYTE_ARRAY_SET;

public final class TaggedByteArray extends TaggedArray {

    private static final long serialVersionUID = -4166037313218353751L;

    public byte[] val;

    public TaggedByteArray(int len) {
        val = new byte[len];
    }

    public TaggedByteArray(byte[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public TaggedByteArray(byte[] array) {
        this.val = array;
    }

    public TaggedByteArray(Taint lenTaint, byte[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @InvokedViaInstrumentation(record = TAINTED_BYTE_ARRAY_SET)
    public void set(int idx, byte val, Taint idxTaint, Taint valTaint, PhosphorStackFrame stackFrame) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(this, idx, val, idxTaint, valTaint, stackFrame));
    }

    @Override
    public Object clone() {
        return new TaggedByteArray(val.clone(), (taints != null) ? taints.clone() : null);
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
    public byte get(int idx, Taint idxTaint, PhosphorStackFrame ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idx, idxTaint, ret);
    }

    public static TaggedByteArray factory(byte[] array) {
        if(array == null) {
            return null;
        }
        return new TaggedByteArray(array);
    }

    public static TaggedByteArray factory(byte[] array, Taint lengthTaint) {
        if(array == null) {
            return null;
        }
        return new TaggedByteArray(lengthTaint, array);
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

    public static byte[] unwrap(TaggedByteArray obj) {
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


