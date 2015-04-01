package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithIntTag;

public final class TaintedCharArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public char[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArrayWithIntTag(taint, val);
	}

	public TaintedCharArrayWithIntTag() {

	}

	public TaintedCharArrayWithIntTag(int[] taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}
}
