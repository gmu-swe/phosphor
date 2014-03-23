package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArray;

public final class TaintedCharArray extends TaintedPrimitiveArray {
	public char[] val;
	public int[] taint;

	@Override
	public Object toStackType() {
		return new MultiDTaintedCharArray(taint, val);
	}

	public TaintedCharArray() {

	}

	public TaintedCharArray(int[] taint, char[] val) {
		this.taint = taint;
		this.val = val;
	}
}
