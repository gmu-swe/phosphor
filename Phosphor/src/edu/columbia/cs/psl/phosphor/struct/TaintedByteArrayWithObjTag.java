package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArrayWithObjTag;

public final class TaintedByteArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag {
	public byte[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedByteArrayWithObjTag(taint, val);
	}
	public TaintedByteArrayWithObjTag()
	{
		
	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedByteArrayWithObjTag(LazyArrayObjTags taint, byte[] val) {
		this.val = val;
		this.taint = taint;
	}
}
