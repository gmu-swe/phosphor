package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithIntTag;

public final class TaintedCharArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public char[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArrayWithIntTag(taint, val);
	}

	public TaintedCharArrayWithIntTag() {

	}

	public TaintedCharArrayWithIntTag(LazyArrayIntTags taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}

	@Override
	public int getLength() {
		return val.length;
	}
}
