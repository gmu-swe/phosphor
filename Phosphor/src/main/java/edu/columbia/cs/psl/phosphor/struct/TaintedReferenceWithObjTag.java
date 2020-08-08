package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.Serializable;

public class TaintedReferenceWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

    private static final long serialVersionUID = 3266794454006241592L;
    public Object val;

    public TaintedReferenceWithObjTag(Taint taint, Object val) {
        this.taint = taint;
        this.val = val;
    }

    public TaintedReferenceWithObjTag() {

    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeObject(val);
        stream.writeObject(taint);
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        val = stream.readObject();
        taint = (Taint) stream.readObject();
    }

    @Override
    public Object getValue() {
        return val;
    }

    public void unwrapPrimitives() {
        if(val instanceof TaintedPrimitiveWithObjTag) {
            TaintedPrimitiveWithObjTag tmp = (TaintedPrimitiveWithObjTag) val;
            val = tmp.getValue();
            taint = taint.union(tmp.taint);
        }
    }

    public static final TaintedReferenceWithObjTag valueOf(Taint taint, Object val) {
        return new TaintedReferenceWithObjTag(taint, val);
    }
    public void fromPrimitive(TaintedPrimitiveWithObjTag prim){
        this.val = prim.getValue();
        this.taint = prim.taint;
    }
}
