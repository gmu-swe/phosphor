package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;

public abstract class LazyArrayIntTags implements Serializable {

	private static final long serialVersionUID = 7377241443004037122L;

	public int[] taints;
	public int lengthTaint;
	// Used to mark this object as visited when searching
	public int $$PHOSPHOR_MARK = Integer.MIN_VALUE;

	public LazyArrayIntTags(int[] taints) {
		this.taints = taints;
	}

	public LazyArrayIntTags() {
	}

	public abstract Object getVal();

	public abstract int getLength();

	public int getLengthTaint() {
		return lengthTaint;
	}

	public void setTaints(int tag) {
		if(getVal() != null && getLength() != 0) {
			if(taints == null) {
				taints = new int[getLength()];
			}
			for(int i = 0; i < taints.length; i++) {
				taints[i]=tag;
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof  LazyArrayIntTags)) return false;
		LazyArrayIntTags that = (LazyArrayIntTags) o;
		return this.getVal() == that.getVal();
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(int taint, Object o, TaintedBooleanWithIntTag ret) {
		ret.val = this.equals(o);
		ret.taint = 0;
		return ret;
	}

	@Override
	public int hashCode() {
		return this.getVal().hashCode();
	}

	@SuppressWarnings("unused")
	public TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(TaintedIntWithIntTag ret) {
		ret.val = this.hashCode();
		ret.taint = 0;
		return ret;
	}
}
