package edu.columbia.cs.psl.phosphor.struct;

public final class TaintedMisc {
	public Object val;
	public int taint;

	public TaintedMisc(int taint, Object val) {
		this.taint = taint;
		this.val = val;
	}
}
