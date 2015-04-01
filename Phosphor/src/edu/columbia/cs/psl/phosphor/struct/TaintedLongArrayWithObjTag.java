package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArrayWithObjTag;

public class TaintedLongArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public long[] val;
	
	@Override
	public Object toStackType() {
		return new MultiDTaintedLongArrayWithObjTag(taint, val);
	}
	public TaintedLongArrayWithObjTag()
	{
		
	}
	public TaintedLongArrayWithObjTag(Object[] taint, long[] val)
	{
		this.taint=taint;
		this.val=val;
	}
}
