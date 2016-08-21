package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithObjTag;

public final class TaintedIntArrayWithObjTag extends TaintedPrimitiveArrayWithObjTag{
	public int[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArrayWithObjTag(taint, val);
	}
	public TaintedIntArrayWithObjTag(){
		
	}
	@Override
	public int getLength() {
		return val.length;
	}
	public TaintedIntArrayWithObjTag(LazyArrayObjTags taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
}
