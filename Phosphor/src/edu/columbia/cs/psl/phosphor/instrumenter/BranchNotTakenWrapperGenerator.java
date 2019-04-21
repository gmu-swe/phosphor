package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import edu.columbia.cs.psl.phosphor.struct.analysis.Field;
import edu.columbia.cs.psl.phosphor.struct.analysis.ForceControlStoreAdvice;
import edu.columbia.cs.psl.phosphor.struct.analysis.LVAccess;
import edu.columbia.cs.psl.phosphor.struct.analysis.ThrownException;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashSet;

import static org.objectweb.asm.Opcodes.*;

public class BranchNotTakenWrapperGenerator {


	HashSet<MethodNode> additionalMethodsToAdd = new HashSet<>();

	private HashSet<ForceControlStoreAdvice> analyze(String owner, MethodNode mn) throws AnalyzerException {
		HashSet<ForceControlStoreAdvice> ret = new HashSet<>();
		if (mn.instructions == null)
			return ret;
		Analyzer an = new Analyzer(new BasicArrayInterpreter((mn.access & Opcodes.ACC_STATIC) != 0, null));
		int idx = 0;
		AbstractInsnNode insn = mn.instructions.getFirst();
		Frame[] frames = an.analyze(owner, mn);
		boolean debug = mn.name.startsWith("add$$");
		while (insn != null) {
			switch (insn.getOpcode()) {
				case Opcodes.PUTFIELD:
					FieldInsnNode fin = ((FieldInsnNode) insn);
					Frame<BasicValue> f = frames[idx];
					if (f != null) {
						BasicValue _val = f.getStack(f.getStackSize() - 2);
						if (_val instanceof BasicArrayInterpreter.BasicLVValue) {
							BasicArrayInterpreter.BasicLVValue lvValue = (BasicArrayInterpreter.BasicLVValue) _val;
							ret.add(new Field(fin, new LVAccess(lvValue), additionalMethodsToAdd, true));
						} else if (_val instanceof BasicArrayInterpreter.BasicFieldValue) {
							BasicArrayInterpreter.BasicFieldValue fieldValue = (BasicArrayInterpreter.BasicFieldValue) _val;
							ret.add(new Field(fin, fieldValue.getField(), additionalMethodsToAdd));
						}
					}
					break;
				case Opcodes.PUTSTATIC:
					fin = ((FieldInsnNode) insn);
					ret.add(new Field(fin, null, additionalMethodsToAdd));
					break;
				case Opcodes.INVOKEVIRTUAL:
				case Opcodes.INVOKEINTERFACE:
				case Opcodes.INVOKESPECIAL:
					MethodInsnNode min = (MethodInsnNode) insn;
					f = frames[idx];
					if (f != null) {
						int offset = 1 + Type.getArgumentTypes(min.desc).length;
						BasicValue _val = f.getStack(f.getStackSize() - offset);
						if (_val instanceof BasicArrayInterpreter.BasicLVValue) {
							BasicArrayInterpreter.BasicLVValue lvValue = (BasicArrayInterpreter.BasicLVValue) _val;
							ret.add(new LVAccess(lvValue));
						} else if (_val instanceof BasicArrayInterpreter.BasicFieldValue) {
							BasicArrayInterpreter.BasicFieldValue fieldValue = (BasicArrayInterpreter.BasicFieldValue) _val;
							fieldValue.getField().fixMethodCollector(additionalMethodsToAdd);
							ret.add(fieldValue.getField());
						}
					}
					break;
				case Opcodes.ATHROW:
					if (Configuration.IMPLICIT_EXCEPTION_FLOW || Configuration.IMPLICIT_EXCEPTION_BRANCH_SUMMARIZATION) {
						f = frames[idx];
						if (f != null) {
							BasicValue _val = f.getStack(f.getStackSize() - 1);
							Type t = _val.getType();
							if (t != null && !t.getDescriptor().equals("Ljava/lang/Object;"))
								ret.add(new ThrownException(t.getInternalName()));
						}
					}
					break;
				//TODO: should we consider (1) more method calls or (2) array stores?
			}
			insn = insn.getNext();
			idx++;
		}

		return ret;
	}

	public void generate(ClassVisitor cv, String owner, MethodNode originalMethod, boolean fixLdcClass) {
		String newName = originalMethod.name.replace(TaintUtils.METHOD_SUFFIX, "");
		if (newName.startsWith("<"))
			return;
		newName = newName + TaintUtils.METHOD_SUFFIX_SUMMARY;
		String newDesc = TaintUtils.remapMethodDesc(originalMethod.desc);
		if (Configuration.IMPLICIT_EXCEPTION_FLOW || Configuration.IMPLICIT_EXCEPTION_BRANCH_SUMMARIZATION) {
			//Also need to pass the current method's exception data to the wrapper, to emulate as if that exception were being thrown immediately under this branch
			Type[] args = Type.getArgumentTypes(newDesc);
			Type returnType = Type.getReturnType(newDesc);
			newDesc = "(";
			for (Type t : args) {
				newDesc += t.getDescriptor();
			}
			newDesc += Type.getDescriptor(ExceptionalTaintData.class);
			newDesc += ')' + returnType.getDescriptor();
		}
		int acc = originalMethod.access & ~Opcodes.ACC_NATIVE;
		MethodVisitor mv = cv.visitMethod(acc, newName, newDesc, null, null);
		if ((originalMethod.access & Opcodes.ACC_ABSTRACT) != 0) {
			return;

		}
		NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(owner, acc, newName, newDesc, mv);
		LocalVariableManager lvm = new LocalVariableManager(acc, newDesc, analyzer, analyzer, mv, false);
		lvm.disable();
		SpecialOpcodeRemovingMV smv = new SpecialOpcodeRemovingMV(lvm, false, acc, owner, newDesc, fixLdcClass);
		mv = smv;
		mv.visitCode();

		Type returnType = Type.getReturnType(originalMethod.desc);
		try {
			HashSet<ForceControlStoreAdvice> advice = analyze(owner, originalMethod);
			if(!advice.isEmpty()) {
				Label start = new Label();
				Label end = new Label();
				Label handler = new Label();
				mv.visitTryCatchBlock(start, end, handler, null);
				mv.visitLabel(start);
				for (ForceControlStoreAdvice each : advice) {
					each.apply(mv, lvm, analyzer, owner);
				}
				if (returnType.getSort() == Type.VOID) {
					mv.visitInsn(Opcodes.RETURN);
				} else {
					mv.visitInsn(Opcodes.ACONST_NULL);
					mv.visitInsn(Opcodes.ARETURN);
				}
				mv.visitLabel(end);
				mv.visitLabel(handler);
				mv.visitFrame(F_NEW, 0, new Object[0], 1, new Object[]{"java/lang/Throwable"});
				mv.visitInsn(POP);
			}
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}

		if (returnType.getSort() == Type.VOID) {
			mv.visitInsn(Opcodes.RETURN);
		} else {
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitInsn(Opcodes.ARETURN);
		}
		mv.visitMaxs(0, 0);
		mv.visitEnd();

	}
}
