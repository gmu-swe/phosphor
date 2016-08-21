package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArrayWithObjTag;

public final class TaintedFloatArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public float[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedFloatArrayWithObjTag(taint, val);
	}

	public TaintedFloatArrayWithObjTag() {

	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedFloatArrayWithObjTag(LazyArrayObjTags taint, float[] val) {
		this.taint = taint;
		this.val = val;
	}
}
