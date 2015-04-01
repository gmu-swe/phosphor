package edu.columbia.cs.psl.phosphor.struct;

public abstract class TaintedPrimitiveArrayWithObjTag {
	public Object[] taint;

	public abstract Object toStackType();

	public void setTaints(Object tag) {
		if (taint != null)
			for (int i = 0; i < taint.length; i++)
				taint[i] = tag;
	}
}
