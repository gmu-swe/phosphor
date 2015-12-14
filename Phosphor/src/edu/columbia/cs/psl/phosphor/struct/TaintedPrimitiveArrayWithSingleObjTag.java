package edu.columbia.cs.psl.phosphor.struct;

public abstract class TaintedPrimitiveArrayWithSingleObjTag implements TaintedReturnHolderWithSingleObjTag {
	public Object taint;

	public abstract Object toStackType();

	public void setTaints(Object tag) {
		taint = tag;
	}
}
