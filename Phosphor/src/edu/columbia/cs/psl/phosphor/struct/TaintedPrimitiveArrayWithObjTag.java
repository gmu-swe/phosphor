package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class TaintedPrimitiveArrayWithObjTag {
	public LazyArrayObjTags taint;

	public abstract Object toStackType();
	public abstract int getLength();

	public void setTaints(Object tag) {
		if(taint.taints == null)
			taint.taints = new Taint[getLength()];
			for (int i = 0; i < taint.taints.length; i++)
				taint.taints[i] = (Taint) tag;
	}
}
