package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArrayWithSingleObjTag;

public class TaintedLongArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public long[] val;
	
	@Override
	public Object toStackType() {
		return new MultiDTaintedLongArrayWithSingleObjTag(taint, val);
	}
	public TaintedLongArrayWithSingleObjTag()
	{
		
	}
	public TaintedLongArrayWithSingleObjTag(Object taint, long[] val)
	{
		this.taint=taint;
		this.val=val;
	}
}
