package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

	private boolean ignoreFrames;
	private String clazz;

	private boolean fixLdcClass;

	private boolean lastWasFrame;
	
	public SpecialOpcodeRemovingMV(MethodVisitor sup, boolean ignoreFrames, String clazz, boolean fixLdcClass) {
		super(Opcodes.ASM5, sup);
		this.ignoreFrames = ignoreFrames;
		this.clazz = clazz;
		this.fixLdcClass = fixLdcClass;
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		lastWasFrame = false;
		switch (opcode) {
		case TaintUtils.BRANCH_END:
		case TaintUtils.BRANCH_START:
		case TaintUtils.FORCE_CTRL_STORE:
			break;
		default:
			super.visitVarInsn(opcode, var);
		}
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);
	}
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		Type descType = Type.getType(desc);
		if (descType.getSort() == Type.ARRAY && descType.getDimensions() > 1 && descType.getElementType().getSort() != Type.OBJECT) {
			//remap!
			desc = MultiDTaintedArray.getTypeForType(descType).getDescriptor();
		}
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if(lastWasFrame)
			return;
		lastWasFrame = true;
		if (type == TaintUtils.RAW_INSN)
			type = Opcodes.F_NEW;
		Object[] newLocal = new Object[local.length];
		Object[] newStack = new Object[stack.length];
		for(int i = 0; i < nLocal; i++)
			newLocal[i] = (local[i] instanceof TaggedValue ? ((TaggedValue) local[i]).v : local[i]);
		for(int i = 0; i < nStack; i++)
			newStack[i] = (stack[i] instanceof TaggedValue ? ((TaggedValue) stack[i]).v : stack[i]);
		if (!ignoreFrames)
			super.visitFrame(type, nLocal, newLocal, nStack, newStack);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		lastWasFrame = false;
		if (cst instanceof Type && fixLdcClass) {
			super.visitLdcInsn(((Type) cst).getInternalName().replace("/", "."));
			super.visitInsn(Opcodes.ICONST_0);
			super.visitLdcInsn(clazz.replace("/", "."));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			super.visitLdcInsn(cst);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		lastWasFrame = false;
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	@Override
	public void visitIincInsn(int var, int increment) {
		lastWasFrame = false;
		super.visitIincInsn(var, increment);
	}
	@Override
	public void visitIntInsn(int opcode, int operand) {
		lastWasFrame = false;
		super.visitIntInsn(opcode, operand);
	}
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		lastWasFrame = false;
		super.visitJumpInsn(opcode, label);
	}
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		lastWasFrame = false;
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		lastWasFrame = false;
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		lastWasFrame = false;
		super.visitMultiANewArrayInsn(desc, dims);
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		lastWasFrame = false;
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
	@Override
	public void visitTypeInsn(int opcode, String type) {
		lastWasFrame = false;
		super.visitTypeInsn(opcode, type);
	}
	@Override
	public void visitInsn(int opcode) {
		lastWasFrame = false;
		switch (opcode) {
		case TaintUtils.FOLLOWED_BY_FRAME:
		case TaintUtils.RAW_INSN:
		case TaintUtils.NO_TAINT_STORE_INSN:
		case TaintUtils.IGNORE_EVERYTHING:
		case TaintUtils.DONT_LOAD_TAINT:
		case TaintUtils.GENERATETAINT:
		case TaintUtils.IS_TMP_STORE:
		case TaintUtils.ALWAYS_BOX_JUMP:
		case TaintUtils.CUSTOM_SIGNAL_1:
		case TaintUtils.CUSTOM_SIGNAL_2:
		case TaintUtils.CUSTOM_SIGNAL_3:
		case TaintUtils.FORCE_CTRL_STORE:
		case TaintUtils.NEXT_INSN_TAINT_AWARE:
			break;
		default:
			super.visitInsn(opcode);
		}
	}
}
