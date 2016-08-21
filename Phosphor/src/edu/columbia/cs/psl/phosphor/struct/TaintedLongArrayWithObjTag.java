package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
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
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedLongArrayWithObjTag(LazyArrayObjTags taint, long[] val)
	{
		this.taint=taint;
		this.val=val;
	}
}
