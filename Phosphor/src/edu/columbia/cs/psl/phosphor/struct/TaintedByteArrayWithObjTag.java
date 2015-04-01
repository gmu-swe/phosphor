package edu.columbia.cs.psl.phosphor.struct;

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
	public TaintedByteArrayWithObjTag(Object[] taint, byte[] val) {
		this.val = val;
		this.taint = taint;
	}
}
