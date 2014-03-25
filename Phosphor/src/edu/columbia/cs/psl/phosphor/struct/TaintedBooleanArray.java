package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArray;

public final class TaintedBooleanArray extends TaintedPrimitiveArray{
	public boolean[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedBooleanArray(taint, val);
	}
	public TaintedBooleanArray()
	{
		
	}
	public TaintedBooleanArray(int[] taint, boolean[] val) {
		this.taint = taint;
		this.val = val;
	}
}
