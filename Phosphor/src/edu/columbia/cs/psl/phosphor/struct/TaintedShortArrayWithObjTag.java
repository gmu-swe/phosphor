package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithObjTag;

public final class TaintedShortArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public short[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedShortArrayWithObjTag(taint, val);
	}
	public TaintedShortArrayWithObjTag(Object[] taint, short[] val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedShortArrayWithObjTag() {
	}
}
