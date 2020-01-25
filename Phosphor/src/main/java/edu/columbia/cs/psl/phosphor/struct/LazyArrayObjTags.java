package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class LazyArrayObjTags implements Cloneable, Serializable {

    private static final long serialVersionUID = -2635717960621951243L;

    public Taint[] taints;
    public Taint lengthTaint = Taint.emptyTaint();
    // Used to mark this object as visited when searching
    public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

    public LazyArrayObjTags(Taint[] taints) {
        this.taints = taints;
    }

    public LazyArrayObjTags() {
    }

    public abstract Object getVal();

    public abstract int getLength();

    public Taint getLengthTaint(Taint referenceTaint) {
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

    protected void checkAIOOB(Taint idxTaint, int idx, ControlFlowStack ctrl) {
        if(idx >= getLength()) {
            ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException("" + idx);
            MultiTainter.taintedObject(ex, Taint.combineTags(idxTaint, ctrl));
            throw ex;
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof LazyArrayObjTags)) {
            return false;
        }
        LazyArrayObjTags that = (LazyArrayObjTags) o;
        return this.getVal() == that.getVal();
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Taint thisTaint, Object o, Taint otherTaint, TaintedBooleanWithObjTag ret) {
        ret.val = this.equals(o);
        ret.taint = thisTaint.union(otherTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Taint thisTaint, Object o, Taint otherTaint, TaintedBooleanWithObjTag ret, ControlFlowStack controlFlowStack) {
        return equals$$PHOSPHORTAGGED(thisTaint, o, otherTaint, ret);
    }

    @Override
    public int hashCode() {
        return this.getVal().hashCode();
    }

    @SuppressWarnings("unused")
    public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Taint thisTaint, TaintedIntWithObjTag ret) {
        ret.val = this.hashCode();
        ret.taint = thisTaint;
        return ret;
    }

    @SuppressWarnings("unused")
    public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Taint thisTaint, TaintedIntWithObjTag ret, ControlFlowStack controlFlowStack) {
        return this.hashCode$$PHOSPHORTAGGED(thisTaint, ret);
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

    public int unsafeIndexFor(Unsafe unsafe, long offset) {
        Class<?> clazz = getVal().getClass();
        long baseOffset = unsafe.arrayBaseOffset(clazz);
        long scale = unsafe.arrayIndexScale(clazz);
        // Calculate the index based off the offset
        int index = (int) ((offset - baseOffset) / scale);
        return index;
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
