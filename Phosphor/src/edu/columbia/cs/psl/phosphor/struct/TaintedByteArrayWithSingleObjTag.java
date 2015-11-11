package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArrayWithSingleObjTag;

public final class TaintedByteArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag {
	public byte[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedByteArrayWithSingleObjTag(taint, val);
	}
	public TaintedByteArrayWithSingleObjTag()
	{
		
	}
	public TaintedByteArrayWithSingleObjTag(Object taint, byte[] val) {
		this.val = val;
		this.taint = taint;
	}
}
