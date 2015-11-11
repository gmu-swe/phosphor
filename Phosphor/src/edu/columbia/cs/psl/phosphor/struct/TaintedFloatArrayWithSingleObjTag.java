package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArrayWithSingleObjTag;

public final class TaintedFloatArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public float[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedFloatArrayWithSingleObjTag(taint, val);
	}

	public TaintedFloatArrayWithSingleObjTag() {

	}

	public TaintedFloatArrayWithSingleObjTag(Object taint, float[] val) {
		this.taint = taint;
		this.val = val;
	}
}
