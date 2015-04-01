package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithIntTag;

public final class TaintedDoubleArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public double[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedDoubleArrayWithIntTag(taint, val);
	}

	public TaintedDoubleArrayWithIntTag() {

	}

	public TaintedDoubleArrayWithIntTag(int[] taint, double[] val) {
		this.taint = taint;
		this.val = val;
	}
}
