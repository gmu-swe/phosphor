package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithObjTag;

public final class TaintedShortArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public short[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedShortArrayWithObjTag(taint, val);
	}
	public TaintedShortArrayWithObjTag(LazyArrayObjTags taint, short[] val) {
		this.taint = taint;
		this.val = val;
	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedShortArrayWithObjTag() {
	}
}
