package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithObjTag;

public final class TaintedIntArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag{
	public int[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArrayWithObjTag(taint, val);
	}
	public TaintedIntArrayWithObjTag(){
		
	}
	public TaintedIntArrayWithObjTag(Object[] taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
}
