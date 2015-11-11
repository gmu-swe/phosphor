package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithSingleObjTag;

public final class TaintedIntArrayWithSingleObjTag extends TaintedPrimitiveArrayWithSingleObjTag{
	public int[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArrayWithSingleObjTag(taint, val);
	}
	public TaintedIntArrayWithSingleObjTag(){
		
	}
	public TaintedIntArrayWithSingleObjTag(Object taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
}
