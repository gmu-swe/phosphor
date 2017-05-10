package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public interface TaintTagFactory {

	public Taint<?> getAutoTaint(String source);
	
	public void instrumentationStarting(String className);
	public void instrumentationStarting(int access, String methodName, String methodDesc);

	public void instrumentationEnding(String className);

	public boolean isInternalTaintingClass(String classname);
	
	public void insnIndexVisited(int offset);
	public boolean isIgnoredClass(String classname);
	public void generateEmptyTaint(MethodVisitor mv);
	public void generateEmptyTaintArray(Object[] array, int dimensions);
	
	public void methodOp(int opcode, String owner, String name, String desc, boolean itfc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void stackOp(int opcode, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void jumpOp(int opcode, int branchStarting, Label label, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void typeOp(int opcode, String type, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void intOp(int opcode, int arg, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void signalOp(int signal, Object option);
	public void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta, boolean trackedLoad);
	public void methodEntered(String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);
	public void lineNumberVisited(int line);
	public void lookupSwitch(Label dflt, int[] keys, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV);
	public void tableSwitch(int min, int max, Label dflt, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV);
	public void propogateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv);
}
