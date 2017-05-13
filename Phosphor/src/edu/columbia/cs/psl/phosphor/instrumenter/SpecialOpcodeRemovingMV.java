package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

	private boolean ignoreFrames;
	private String clazz;

	private boolean fixLdcClass;
	public SpecialOpcodeRemovingMV(MethodVisitor sup, boolean ignoreFrames, String clazz, boolean fixLdcClass) {
		super(Opcodes.ASM5, sup);
		this.ignoreFrames = ignoreFrames;
		this.clazz = clazz;
		this.fixLdcClass = fixLdcClass;
	}
//
//	HashSet<Label> usedLabels = new HashSet<Label>();
//	HashSet<Label> definedLabels = new HashSet<Label>();
//	HashMap<Label, IllegalStateException> users = new HashMap<Label, IllegalStateException>();
//	@Override
//	public void visitJumpInsn(int opcode, Label label) {
//usedLabels.add(label);
//IllegalStateException e  =new IllegalStateException();
//e.fillInStackTrace();
//users.put(label,e);
//		super.visitJumpInsn(opcode, label);
//	}
//	@Override
//	public void visitLabel(Label label) {
//		definedLabels.add(label);
//		super.visitLabel(label);
//	}
//	@Override
//	public void visitMaxs(int maxStack, int maxLocals) {
//		for(Label l: usedLabels)
//		{
//			if(!definedLabels.contains(l))
//				throw users.get(l);
//		}
//		super.visitMaxs(maxStack, maxLocals);
//	}
	@Override
	public void visitVarInsn(int opcode, int var) {
		switch (opcode) {
		case TaintUtils.BRANCH_END:
		case TaintUtils.BRANCH_START:
		case TaintUtils.FORCE_CTRL_STORE:
		case TaintUtils.ALWAYS_AUTOBOX:
			break;
		default:
			super.visitVarInsn(opcode, var);
		}
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
		if (type == TaintUtils.RAW_INSN)
			type = Opcodes.F_NEW;
		//At this point, make sure that there are no TaggedValue's sitting around.
		Object[] newLocal = new Object[local.length];
		Object[] newStack = new Object[stack.length];
		for(int i = 0; i< local.length;i++)
			if(local[i] instanceof TaggedValue)
				newLocal[i] = ((TaggedValue)local[i]).v;
			else
				newLocal[i] = local[i];
		for(int i = 0; i< stack.length;i++)
			if(stack[i] instanceof TaggedValue)
				newStack[i] = ((TaggedValue)stack[i]).v;
			else
				newStack[i] = stack[i];
		if (!ignoreFrames)
			super.visitFrame(type, nLocal, newLocal, nStack, newStack);
	}

	@Override
	public void visitLdcInsn(Object cst) {
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
	public void visitInsn(int opcode) {
		switch (opcode) {
		case TaintUtils.FOLLOWED_BY_FRAME:
		case TaintUtils.RAW_INSN:
		case TaintUtils.NO_TAINT_STORE_INSN:
		case TaintUtils.IGNORE_EVERYTHING:
		case TaintUtils.IS_TMP_STORE:
		case TaintUtils.ALWAYS_BOX_JUMP:
		case TaintUtils.CUSTOM_SIGNAL_1:
		case TaintUtils.CUSTOM_SIGNAL_2:
		case TaintUtils.CUSTOM_SIGNAL_3:
		case TaintUtils.FORCE_CTRL_STORE:
		case TaintUtils.TRACKED_LOAD:
		case TaintUtils.LOOP_HEADER:
			break;
		default:
			super.visitInsn(opcode);
		}
	}
}
