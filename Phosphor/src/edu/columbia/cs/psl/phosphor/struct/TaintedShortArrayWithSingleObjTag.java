package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithSingleObjTag;

public final class TaintedShortArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public short[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedShortArrayWithSingleObjTag(taint, val);
	}
	public TaintedShortArrayWithSingleObjTag(Object taint, short[] val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedShortArrayWithSingleObjTag() {
	}
}
