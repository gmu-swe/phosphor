package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

public class TaggedValue {
	public Object v;

	public TaggedValue(Object v) {
		this.v = v;
	}
	@Override
	public String toString() {
		return "T:"+v;
	}
}
