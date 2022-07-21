package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_CHAR_ARRAY_GET;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.TAINTED_CHAR_ARRAY_SET;

public final class TaggedCharArray extends TaggedArray {

    private static final long serialVersionUID = -2087734134268751452L;

    public char[] val;

    public TaggedCharArray(int len) {
        val = new char[len];
    }

    public TaggedCharArray(char[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public TaggedCharArray(char[] array) {
        this.val = array;
    }

    public TaggedCharArray(Taint lenTaint, char[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @InvokedViaInstrumentation(record = TAINTED_CHAR_ARRAY_SET)
    public void set(int idx, char val, Taint idxTaint, Taint valTaint, PhosphorStackFrame stackFrame) {
        set(idx, val, Configuration.derivedTaintListener.arraySet(this, idx, val, idxTaint, valTaint, stackFrame));
    }

    @Override
    public Object clone() {
        return new TaggedCharArray(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(int idx, char val, Taint tag) {
        this.val[idx] = val;
        if(taints == null && tag != null && !tag.isEmpty()) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    @InvokedViaInstrumentation(record = TAINTED_CHAR_ARRAY_GET)
    public char get(int idx, Taint idxTaint, PhosphorStackFrame ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idx, idxTaint, ret);
    }

    public static TaggedCharArray factory(char[] array) {
        if(array == null) {
            return null;
        }
        return new TaggedCharArray(array);
    }

    public static TaggedCharArray factory(char[] array, Taint lengthTaint) {
        if(array == null) {
            return null;
        }
        return new TaggedCharArray(lengthTaint, array);
    }

    public int getLength() {
        return val.length;
    }

    @Override
    public Object getVal() {
        return val;
    }

    public void ensureVal(char[] v) {
        if (v != val) {
            val = v;
        }
    }

    public static char[] unwrap(TaggedCharArray obj) {
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
            for (char el : val) {
                stream.writeChar(el);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if (len == -1) {
            val = null;
        } else {
            val = new char[len];
            for (int i = 0; i < len; i++) {
                val[i] = stream.readChar();
            }
        }
    }
}


