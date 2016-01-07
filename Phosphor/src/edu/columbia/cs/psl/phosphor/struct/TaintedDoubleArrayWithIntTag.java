package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithIntTag;

public final class TaintedDoubleArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public double[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedDoubleArrayWithIntTag(taint, val);
	}

	public TaintedDoubleArrayWithIntTag() {

	}

	public TaintedDoubleArrayWithIntTag(LazyArrayIntTags taint, double[] val) {
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
