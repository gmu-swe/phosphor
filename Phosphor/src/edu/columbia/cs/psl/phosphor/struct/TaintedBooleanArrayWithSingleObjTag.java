package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArrayWithSingleObjTag;

public final class TaintedBooleanArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag{
	public boolean[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedBooleanArrayWithSingleObjTag(taint, val);
	}
	public TaintedBooleanArrayWithSingleObjTag()
	{
		
	}
	public TaintedBooleanArrayWithSingleObjTag(Object taint, boolean[] val) {
		this.taint = taint;
		this.val = val;
	}
}
