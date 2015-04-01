package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithObjTag;

public final class TaintedCharArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public char[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArrayWithObjTag(taint, val);
	}

	public TaintedCharArrayWithObjTag() {

	}

	public TaintedCharArrayWithObjTag(Object[] taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}
}
