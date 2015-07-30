package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

public interface TaintTagFactory {
	public void generateEmptyTaint(MethodVisitor mv);
	public void generateEmptyTaintArray(Object[] array, int dimensions);
	public void stackOp(int opcode, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void jumpOp(int opcode, int branchStarting, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void typeOp(int opcode, String type, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
}
