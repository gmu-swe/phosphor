package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArrayWithObjTag;

public final class TaintedBooleanArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag{
	public boolean[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedBooleanArrayWithObjTag(taint, val);
	}
	public TaintedBooleanArrayWithObjTag()
	{
		
	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedBooleanArrayWithObjTag(LazyArrayObjTags taint, boolean[] val) {
		this.taint = taint;
		this.val = val;
	}
}
