package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;

public interface DerivedTaintListener {
	public void singleDepCreated(Taint in, Taint out);
	public void doubleDepCreated(Taint in1, Taint in2, Taint out);
	public void controlApplied(Object o, ControlTaintTagStack tags);
}
