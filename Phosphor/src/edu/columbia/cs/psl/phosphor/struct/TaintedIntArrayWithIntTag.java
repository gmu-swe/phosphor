package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithIntTag;

public final class TaintedIntArrayWithIntTag extends TaintedPrimitiveArrayWithIntTag{
	public int[] val;

	@Override
	public Object toStackType() {
		return new MultiDTaintedIntArrayWithIntTag(new LazyArrayIntTags(taint), val);
	}
	public TaintedIntArrayWithIntTag(){
		
	}
	public TaintedIntArrayWithIntTag(int[] taint, int[] val) {
		this.taint = taint;
		this.val = val;
	}
}
