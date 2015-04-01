package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithObjTag;

public final class TaintedDoubleArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public double[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedDoubleArrayWithObjTag(taint, val);
	}

	public TaintedDoubleArrayWithObjTag() {

	}

	public TaintedDoubleArrayWithObjTag(Object[] taint, double[] val) {
		this.taint = taint;
		this.val = val;
	}
}
