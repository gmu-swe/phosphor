package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public interface TaintTagFactory {

    Taint<?> getAutoTaint(String source);

    void instrumentationStarting(String className);

    void instrumentationStarting(int access, String methodName, String methodDesc);

    void instrumentationEnding(String className);

    boolean isInternalTaintingClass(String className);

    void insnIndexVisited(int offset);

    boolean isIgnoredClass(String className);

    void generateEmptyTaint(MethodVisitor mv);

    void generateEmptyTaintArray(Object[] array, int dimensions);

    void methodOp(int opcode, String owner, String name, String desc, boolean isInterface, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void stackOp(int opcode, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void jumpOp(int opcode, int branchStarting, Label label, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void typeOp(int opcode, String type, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void intOp(int opcode, int arg, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void signalOp(int signal, Object option);

    void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta, boolean trackedLoad);

    void methodEntered(String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta);

    void lineNumberVisited(int line);

    void lookupSwitch(Label defaultLabel, int[] keys, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV);

    void tableSwitch(int min, int max, Label defaultLabel, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV);

    void propogateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv);

    void generateSetTag(MethodVisitor mv, String className);
}
