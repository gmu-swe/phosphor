package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArray;

public final class TaintedByteArray extends TaintedPrimitiveArray {
	public byte[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedByteArray(taint, val);
	}
	public TaintedByteArray()
	{
		
	}
	public TaintedByteArray(int[] taint, byte[] val) {
		this.val = val;
		this.taint = taint;
	}
}
