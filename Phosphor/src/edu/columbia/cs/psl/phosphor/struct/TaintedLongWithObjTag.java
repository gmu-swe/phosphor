package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedLongWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    private static final long serialVersionUID = -2036116913949916760L;
    public long val;

    public TaintedLongWithObjTag(Taint taint, long val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedLongWithObjTag() {

    }

    @Override
    public Object getValue() {
        return val;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeLong(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readLong();
        taint = (Taint) stream.readObject();
    }

    public static final TaintedLongWithObjTag valueOf(Taint taint, long val) {
        return new TaintedLongWithObjTag(taint, val);
    }
}
