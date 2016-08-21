package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithObjTag;

public final class TaintedCharArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public char[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArrayWithObjTag(taint, val);
	}

	public TaintedCharArrayWithObjTag() {

	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedCharArrayWithObjTag(LazyArrayObjTags taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}
}
