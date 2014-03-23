package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArray;

public class TaintedLongArray extends TaintedPrimitiveArray {
	public long[] val;
	public int[] taint;
	
	@Override
	public Object toStackType() {
		return new MultiDTaintedLongArray(taint, val);
	}
	public TaintedLongArray()
	{
		
	}
	public TaintedLongArray(int[] taint, long[] val)
	{
		this.taint=taint;
		this.val=val;
	}
}
