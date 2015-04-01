package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArrayWithIntTag;

public final class TaintedFloatArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public float[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedFloatArrayWithIntTag(taint, val);
	}

	public TaintedFloatArrayWithIntTag() {

	}

	public TaintedFloatArrayWithIntTag(int[] taint, float[] val) {
		this.taint = taint;
		this.val = val;
	}
}
