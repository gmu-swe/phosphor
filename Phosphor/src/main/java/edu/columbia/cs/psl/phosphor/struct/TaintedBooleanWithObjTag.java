package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;


public final class TaintedBooleanWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    static final TaintedBooleanWithObjTag[] cache = new TaintedBooleanWithObjTag[2];
    private static final long serialVersionUID = 2665598564631615110L;

    static {
        cache[0] = new TaintedBooleanWithObjTag(null, false);
        cache[1] = new TaintedBooleanWithObjTag(null, true);
    }

    public boolean val;

    public TaintedBooleanWithObjTag(Taint taint, boolean val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedBooleanWithObjTag() {

    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeBoolean(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readBoolean();
        taint = (Taint) stream.readObject();
    }

    @Override
    public Object getValue() {
        return val;
    }

    public static final TaintedBooleanWithObjTag valueOf(Taint taint, boolean val) {
        if(taint == null) {
            return cache[val ? 1 : 0];
        }
        return new TaintedBooleanWithObjTag(taint, val);
    }
}
