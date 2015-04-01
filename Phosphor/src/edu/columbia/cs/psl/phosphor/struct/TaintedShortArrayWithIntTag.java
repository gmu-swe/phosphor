package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithIntTag;

public final class TaintedShortArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public short[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedShortArrayWithIntTag(taint, val);
	}
	public TaintedShortArrayWithIntTag(int[] taint, short[] val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedShortArrayWithIntTag() {
	}
}
