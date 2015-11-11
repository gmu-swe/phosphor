package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithSingleObjTag;

public final class TaintedDoubleArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public double[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedDoubleArrayWithSingleObjTag(taint, val);
	}

	public TaintedDoubleArrayWithSingleObjTag() {

	}

	public TaintedDoubleArrayWithSingleObjTag(Object taint, double[] val) {
		this.taint = taint;
		this.val = val;
	}
}
