package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyCharArrayObjTags extends LazyArrayObjTags {

    private static final long serialVersionUID = -2087734134268751452L;

    public char[] val;

    public LazyCharArrayObjTags(int len) {
        val = new char[len];
    }

    public LazyCharArrayObjTags(char[] array, Taint[] taints) {
        this.taints = taints;
        this.val = array;
    }

    public LazyCharArrayObjTags(char[] array) {
        this.val = array;
    }

    public LazyCharArrayObjTags(Taint lenTaint, char[] array) {
        this.val = array;
        this.lengthTaint = lenTaint;
    }

    @Override
    public Object clone() {
        return new LazyCharArrayObjTags(val.clone(), (taints != null) ? taints.clone() : null);
    }

    public void set(char[] arr, Taint idxTag, int idx, char val) {
        set(arr, idxTag, idx, null, val);
    }

    public void set(char[] l, Taint idxTag, int idx, Taint tag, char val) {
        if(Configuration.derivedTaintListener != null) {
            set(l, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, null), val);
        } else if(idxTag == null) {
            set(l, idx, tag, val);
        } else if(tag == null) {
            set(l, idx, idxTag, val);
        } else {
            set(l, idx, tag.union(idxTag), val);
        }
    }

    public void set(char[] arr, int idx, Taint tag, char val) {
        this.val[idx] = val;
        if(taints == null && tag != null) {
            taints = new Taint[this.val.length];
        }
        if(taints != null) {
            taints[idx] = tag;
        }
    }

    public void set(char[] arr, Taint idxTag, int idx, Taint tag, char val, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTag, idx, ctrl);
        set(arr, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
    }

    public void set(char[] arr, int idx, Taint tag, char val, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        set(arr, idx, Taint.combineTags(tag, ctrl), val);
    }

    public TaintedCharWithObjTag get(char[] arr, Taint idxTaint, int idx, TaintedCharWithObjTag ret) {
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
    }

    public TaintedCharWithObjTag get(char[] arr, Taint idxTaint, int idx, TaintedCharWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(idxTaint, idx, ctrl);
        return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
    }

    public TaintedCharWithObjTag get(char[] arr, int idx, TaintedCharWithObjTag ret) {
        ret.val = val[idx];
        ret.taint = (taints == null) ? null : taints[idx];
        return ret;
    }

    public TaintedCharWithObjTag get(char[] arr, int idx, TaintedCharWithObjTag ret, ControlTaintTagStack ctrl) {
        checkAIOOB(null, idx, ctrl);
        get(arr, idx, ret);
        ret.taint = Taint.combineTags(ret.taint, ctrl);
        return ret;
    }

    public int getLength() {
        return val.length;
    }

    @Override
    public Object getVal() {
        return val;
    }

    public void ensureVal(char[] v) {
        if(v != val) {
            val = v;
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(val == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(val.length);
            for(char el : val) {
                stream.writeChar(el);
            }
        }
        stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            val = null;
        } else {
            val = new char[len];
            for(int i = 0; i < len; i++) {
                val[i] = stream.readChar();
            }
        }
        taints = (Taint[]) stream.readObject();
    }
}


