package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.Serializable;


public abstract class LazyArrayIntTags implements Serializable {

	private static final long serialVersionUID = 7377241443004037122L;
	public int[] taints;

	public LazyArrayIntTags(int[] taints) {
		this.taints = taints;
	}

	public LazyArrayIntTags() {
	}

	public int lengthTaint;

	public int getLengthTaint() {
		return lengthTaint;
	}


	public abstract int getLength();
	public void setTaints(int tag) {
		if(taints == null)
			taints = new int[getLength()];
		for(int i = 0; i < taints.length; i++)
		{
			taints[i]=tag;
		}
	}

	public abstract Object getVal();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LazyArrayIntTags)) return false;
		LazyArrayIntTags that = (LazyArrayIntTags) o;
		return this.getVal() == that.getVal();
	}

	// Phosphor Wrappers to handle tags
	// TODO Write integration tests for below
	public TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(int taint, Object o, TaintedBooleanWithIntTag ret) {
		ret.val = this.equals(o);
		ret.taint = taint;
	    return ret;
	}

	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object taint, Object o, TaintedBooleanWithObjTag ret) {
		ret.val = this.equals(o);
		ret.taint = (Taint) taint;
		return ret;
	}

	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object taint, Object o, TaintedBooleanWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		ret.val = this.equals(o);
		ret.taint = (Taint) taint;
		return ret;
	}

	@Override
	public int hashCode() {
		return this.getVal().hashCode();
	}

	// Phosphor Wrappers to handle tags
	// TODO Write integration tests for below
	public TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(int taint, TaintedIntWithIntTag ret) {
		ret.val = this.hashCode();
		ret.taint = taint;
		return ret;
	}

	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object taint, TaintedIntWithObjTag ret) {
		ret.val = this.hashCode();
		ret.taint = (Taint) taint;
		return ret;
	}

	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object taint, TaintedIntWithObjTag ret, ControlTaintTagStack controlTaintTagStack) {
		ret.val = this.hashCode();
		ret.taint = (Taint) taint;
		return ret;
	}
}
