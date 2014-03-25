package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArray;

public final class TaintedDoubleArray extends TaintedPrimitiveArray {
	public double[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedDoubleArray(taint, val);
	}

	public TaintedDoubleArray() {

	}

	public TaintedDoubleArray(int[] taint, double[] val) {
		this.taint = taint;
		this.val = val;
	}
}
