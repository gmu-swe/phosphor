package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithSingleObjTag;

public final class TaintedCharArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public char[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArrayWithSingleObjTag(taint, val);
	}

	public TaintedCharArrayWithSingleObjTag() {

	}

	public TaintedCharArrayWithSingleObjTag(Object taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}
}
