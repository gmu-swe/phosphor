package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedShortWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    private static final long serialVersionUID = 719385575251672639L;
    public short val;

    public TaintedShortWithObjTag(Taint taint, short val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedShortWithObjTag() {

    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeShort(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readShort();
        taint = (Taint) stream.readObject();
    }

    @Override
    public Object getValue() {
        return val;
    }

    public static final TaintedShortWithObjTag valueOf(Taint taint, short val) {
        return new TaintedShortWithObjTag(taint, val);
    }
}
