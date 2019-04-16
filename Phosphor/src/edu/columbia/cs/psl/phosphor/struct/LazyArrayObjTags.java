package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;


public abstract class LazyArrayObjTags implements Cloneable, Serializable {

	private static final long serialVersionUID = -2635717960621951243L;

	// Used to mark this object as visited when searching
	public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;
	public Taint[] taints;
	public Taint taint;

	public LazyArrayObjTags(Taint[] taints) {
		this.taints = taints;
	}

	public LazyArrayObjTags() {
	}

	public Taint lengthTaint;

	public Taint getLengthTaint() {
		return lengthTaint;
	}


	public abstract int getLength();
	public void setTaints(Taint tag) {
		if(taints == null)
			taints = new Taint[getLength()];
		for(int i = 0; i < taints.length; i++)
		{
			taints[i]=tag;
		}
	}

	public abstract Object getVal();

	protected void checkAIOOB(Taint idxTaint, int idx, ControlTaintTagStack ctrl) {
		if (idx >= getLength()) {
			ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException("" + idx);
			MultiTainter.taintedObject(ex, Taint.combineTags(idxTaint, ctrl));
			throw ex;
		}
	}

	/*

	Please also add wrappers for equals and hashCode for each of the appropriate modes
	(for int, one that returns TaintedBooleanWithIntTag, for obj, one that returns TaintedBooleanWithObjTag,
	 and also one for obj that also takes a ControlTaintTagStack).
	 */

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof  LazyArrayObjTags)) return false;
		LazyArrayObjTags that = (LazyArrayObjTags) o;
		return this.getVal() == that.getVal();
	}

	// Phosphor Wrappers to handle tags
	public TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(int taint, Object o, TaintedBooleanWithIntTag ret) {
		ret.val = this.equals(o);
		ret.taint = taint;
		return ret;
	}

	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret) {
		ret.val = this.equals(o);
		ret.taint = Taint.combineTags(this.taint, ret.taint);
		return ret;
	}

	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		ret.val = this.equals(o);
		ret.taint = Taint.combineTags(this.taint, ret.taint);
		return ret;
	}

	@Override
	public int hashCode() {
		return this.getVal().hashCode();
	}

	// Phosphor Wrappers to handle tags
    // TODO needs integration test
	public TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(TaintedIntWithIntTag ret) {
		ret.val = this.hashCode();
		return ret;
	}

	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret) {
		ret.val = this.hashCode();
		ret.taint = Taint.combineTags(this.taint, ret.taint);
		return ret;
	}


	// TODO needs integration test
	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		return this.hashCode$$PHOSPHORTAGGED(ret);
	}
}
