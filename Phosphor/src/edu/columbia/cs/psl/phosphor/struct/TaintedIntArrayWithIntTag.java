package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithIntTag;

public final class TaintedIntArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public int[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArrayWithIntTag(taint, val);
	}

	public TaintedIntArrayWithIntTag() {

	}

	public TaintedIntArrayWithIntTag(LazyArrayIntTags taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
	@Override
	public Object getValue() {
		return val;
	}

	@Override
	public int getLength() {
		return val.length;
	}
}
