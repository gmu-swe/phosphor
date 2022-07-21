package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class TaggedArray implements Cloneable, Serializable {

    private static final long serialVersionUID = -2635717960621951243L;

    public Taint[] taints;
    public Taint lengthTaint = Taint.emptyTaint();
    // Used to mark this object as visited when searching
    public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

    public TaggedArray(Taint[] taints) {
        this.taints = taints;
    }

    public TaggedArray() {
    }

    public abstract Object getVal();

    public abstract int getLength();

    public Taint getLengthTaint() {
        return lengthTaint;
    }

    public void setTaints(Taint tag) {
        if(getVal() != null && getLength() != 0) {
            if(taints == null) {
                taints = new Taint[getLength()];
            }
            for(int i = 0; i < taints.length; i++) {
                taints[i] = tag;
            }
        }
    }

    protected void checkAIOOB(Taint idxTaint, int idx, PhosphorStackFrame ctrl) {
        if(idx >= getLength()) {
            ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException("" + idx);
            //TODO when we start tainting phosphorStackFrame exception references, do that here...
            throw ex;
        }
    }

    @Override
    public boolean equals(Object o) {
        PhosphorStackFrame stackFrame = PhosphorStackFrame.forMethod(null);
        Object wrappedOther = stackFrame.wrappedArgs[1];
        if(wrappedOther != null){
            o = wrappedOther;
        }
        stackFrame.returnTaint = stackFrame.getArgTaint(0).union(stackFrame.getArgTaint(1));
        if(this == o) {
            return true;
        }
        if(!(o instanceof TaggedArray)) {
            return false;
        }
        TaggedArray that = (TaggedArray) o;
        return this.getVal() == that.getVal();
    }

    @Override
    public int hashCode() {
        return this.getVal().hashCode();
    }

    /**
     * FOR INTERNAL USE ONLY
     **/
    public Taint getTaintOrEmpty(int idx) {
        if(taints == null) {
            return Taint.emptyTaint();
        }
        return taints[idx];
    }

    public void setTaint(int idx, Taint valTaint) {
        if(taints == null) {
            taints = new Taint[getLength()];
        }
        taints[idx] = valTaint;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if(taints == null) {
            stream.writeInt(-1);
        } else {
            stream.writeInt(taints.length);
            for(Taint el : taints) {
                stream.writeObject(el);
            }
        }
        // stream.writeObject(taints);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int len = stream.readInt();
        if(len == -1) {
            taints = null;
        } else {
            taints = new Taint[len];
            for(int i = 0; i < len; i++) {
                taints[i] = (Taint) stream.readObject();
            }
        }
        // taints = (Taint[]) stream.readObject();
    }
}
