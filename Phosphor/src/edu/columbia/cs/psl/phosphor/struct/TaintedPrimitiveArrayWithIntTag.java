package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;

public abstract class TaintedPrimitiveArrayWithIntTag {
	public LazyArrayIntTags taint;

	public abstract Object toStackType();

	public abstract int getLength();
	
	public void setTaints(int tag) {
		if(taint.taints == null)
			taint.taints = new int[getLength()];
			for (int i = 0; i < taint.taints.length; i++)
				taint.taints[i] = tag;
	}
}
