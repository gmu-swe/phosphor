package edu.columbia.cs.psl.phosphor.struct;

public abstract class TaintedPrimitiveArray {
	public int[] taint;

	public abstract Object toStackType();

	public void setTaints(int tag) {
//		for (int i = 0; i < taint.length; i++)
//			taint[i] = tag;
	}
}
