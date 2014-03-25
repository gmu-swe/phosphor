package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArray;

public final class TaintedShortArray extends TaintedPrimitiveArray {
	public short[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedShortArray(taint, val);
	}
	public TaintedShortArray(int[] taint, short[] val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedShortArray() {
	}
}
