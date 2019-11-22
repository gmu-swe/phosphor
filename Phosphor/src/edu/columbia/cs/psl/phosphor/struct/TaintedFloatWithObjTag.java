package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedFloatWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    private static final long serialVersionUID = 4950080138821634577L;
    public float val;

    public TaintedFloatWithObjTag(Taint taint, float val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedFloatWithObjTag() {

    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeFloat(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readFloat();
        taint = (Taint) stream.readObject();
    }

    @Override
    public Object getValue() {
        return val;
    }

    public static final TaintedFloatWithObjTag valueOf(Taint taint, float val) {
        return new TaintedFloatWithObjTag(taint, val);
    }
}
