package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArrayWithIntTag;

public final class TaintedBooleanArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag{
	public boolean[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedBooleanArrayWithIntTag(taint, val);
	}
	public TaintedBooleanArrayWithIntTag()
	{
		
	}
	public TaintedBooleanArrayWithIntTag(int[] taint, boolean[] val) {
		this.taint = taint;
		this.val = val;
	}
}
