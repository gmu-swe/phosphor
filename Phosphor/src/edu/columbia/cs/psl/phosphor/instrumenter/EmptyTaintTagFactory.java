package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

public interface EmptyTaintTagFactory {
	public void generateEmptyTaint(MethodVisitor mv);
	public void generateEmptyTaintArray(Object[] array, int dimensions);
}
