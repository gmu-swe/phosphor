package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArrayWithIntTag;

public final class TaintedByteArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag {
	public byte[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedByteArrayWithIntTag(taint, val);
	}
	public TaintedByteArrayWithIntTag()
	{
		
	}
	public TaintedByteArrayWithIntTag(int[] taint, byte[] val) {
		this.val = val;
		this.taint = taint;
	}
}
