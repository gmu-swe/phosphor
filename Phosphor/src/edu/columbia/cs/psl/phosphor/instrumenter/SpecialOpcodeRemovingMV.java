package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

	private boolean ignoreFrames;
	private String clazz;
	public SpecialOpcodeRemovingMV(MethodVisitor sup, boolean ignoreFrames, String clazz) {
		super(Opcodes.ASM5, sup);
		this.ignoreFrames = ignoreFrames;
		this.clazz = clazz;
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		Type descType = Type.getType(desc);
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if(!ignoreFrames)
			super.visitFrame(type, nLocal, local, nStack, stack);
	}
	@Override
	public void visitLdcInsn(Object cst) {
		if(cst instanceof Type && ignoreFrames)
		{
			super.visitLdcInsn(((Type)cst).getInternalName().replace("/", "."));
			super.visitInsn(Opcodes.ICONST_0);
			super.visitLdcInsn(clazz.replace("/", "."));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		}
		else
			super.visitLdcInsn(cst);
	}
	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case TaintUtils.RAW_INSN:
		case TaintUtils.NO_TAINT_STORE_INSN:
		case TaintUtils.IGNORE_EVERYTHING:
		case TaintUtils.DONT_LOAD_TAINT:
		case TaintUtils.GENERATETAINTANDSWAP:
		case TaintUtils.IS_TMP_STORE:
		case TaintUtils.ALWAYS_BOX_JUMP:
			break;
		default:
			super.visitInsn(opcode);
		}
	}
}
