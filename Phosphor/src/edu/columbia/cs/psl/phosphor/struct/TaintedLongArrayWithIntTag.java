package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArrayWithIntTag;

public class TaintedLongArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public long[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedLongArrayWithIntTag(taint, val);
	}

	public TaintedLongArrayWithIntTag() {

	}

	public TaintedLongArrayWithIntTag(LazyArrayIntTags taint, long[] val) {
		this.taint = taint;
		this.val = val;
	}

	@Override
	public Object getValue() {
		return val;
	}

	@Override
	public int getLength() {
		return val.length;
	}
}
