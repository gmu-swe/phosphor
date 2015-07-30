package edu.columbia.cs.psl.phosphor.runtime;

public interface DerivedTaintListener {
	public void singleDepCreated(Taint in, Taint out);
	public void doubleDepCreated(Taint in1, Taint in2, Taint out);
}
