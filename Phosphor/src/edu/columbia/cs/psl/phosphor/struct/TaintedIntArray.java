package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArray;

public final class TaintedIntArray extends TaintedPrimitiveArray{
	public int[] val;
	public int[] taint;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArray(taint, val);
	}
	public TaintedIntArray(){
		
	}
	public TaintedIntArray(int[] taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
}
