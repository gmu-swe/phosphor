package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArray;

public final class TaintedFloatArray extends TaintedPrimitiveArray {
	public float[] val;
	public int[] taint;

	@Override
	public Object toStackType() {
		return new MultiDTaintedFloatArray(taint, val);
	}

	public TaintedFloatArray() {

	}

	public TaintedFloatArray(int[] taint, float[] val) {
		this.taint = taint;
		this.val = val;
	}
}
