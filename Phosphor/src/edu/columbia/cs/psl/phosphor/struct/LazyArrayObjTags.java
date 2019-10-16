package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.Serializable;

public abstract class LazyArrayObjTags implements Cloneable, Serializable {

	private static final long serialVersionUID = -2635717960621951243L;

	public Taint[] taints;
	public Taint lengthTaint;
	// Used to mark this object as visited when searching
	public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

	public LazyArrayObjTags(Taint[] taints) {
		this.taints = taints;
	}

	public LazyArrayObjTags() {
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
				taints[i]=tag;
			}
		}
	}

	protected void checkAIOOB(Taint idxTaint, int idx, ControlTaintTagStack ctrl) {
		if (idx >= getLength()) {
			ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException("" + idx);
			MultiTainter.taintedObject(ex, Taint.combineTags(idxTaint, ctrl));
			throw ex;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LazyArrayObjTags)) return false;
		LazyArrayObjTags that = (LazyArrayObjTags) o;
		return this.getVal() == that.getVal();
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret) {
		ret.val = this.equals(o);
		ret.taint = null;
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		ret.val = this.equals(o);
		ret.taint = (this.taints != null) ? Taint.combineTaintArray(this.taints) : null;
		return ret;
	}

	@Override
	public int hashCode() {
		return this.getVal().hashCode();
	}

	@SuppressWarnings("unused")
	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret) {
		ret.val = this.hashCode();
		ret.taint = null;
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		return this.hashCode$$PHOSPHORTAGGED(ret);
	}
}
