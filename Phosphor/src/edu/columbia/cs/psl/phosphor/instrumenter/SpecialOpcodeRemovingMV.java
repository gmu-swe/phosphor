package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SpecialOpcodeRemovingMV extends MethodVisitor {

	private boolean ignoreFrames;
	private String clazz;

	private boolean fixLdcClass;

	/**
	 * Visits a field instruction. A field instruction is an instruction that
	 * loads or stores the value of a field of an object.
	 *
	 * @param opcode the opcode of the type instruction to be visited. This opcode
	 *               is either GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
	 * @param owner  the internal name of the field's owner class (see
	 *               {@link Type#getInternalName() getInternalName}).
	 * @param name   the field's name.
	 * @param desc   the field's descriptor (see {@link Type Type}).
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(opcode<200)
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	private LocalVariableManager lvs;
	public void setLVS(LocalVariableManager lvs){
		this.lvs = lvs;
	}
	private int localIdxOfControlTag;
	public SpecialOpcodeRemovingMV(MethodVisitor sup, boolean ignoreFrames, int acc, String clazz, String desc, boolean fixLdcClass) {
		super(Opcodes.ASM5, sup);
		this.ignoreFrames = ignoreFrames;
		this.clazz = clazz;
		this.fixLdcClass = fixLdcClass;
		int n = 0;
		if((acc & Opcodes.ACC_STATIC) == 0)
			n++;
		for(Type t : Type.getArgumentTypes(desc)){
			if(t.getDescriptor().equals(Type.getDescriptor(ControlTaintTagStack.class)))
				this.localIdxOfControlTag = n;
			n+= t.getSize();
		}
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
			case TaintUtils.IGNORE_EVERYTHING:
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
	public void visitTypeInsn(int opcode, String type) {
		if(opcode > 200)
			return;
		super.visitTypeInsn(opcode, type);
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
//		if (!ignoreFrames)
			super.visitFrame(type, nLocal, newLocal, nStack, newStack);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		if (cst instanceof Type && fixLdcClass) {
			super.visitLdcInsn(((Type) cst).getInternalName().replace("/", "."));
			if(Configuration.IMPLICIT_TRACKING)
				super.visitInsn(Opcodes.ACONST_NULL);
			super.visitInsn(Opcodes.ICONST_0);
			super.visitLdcInsn(clazz.replace("/", "."));
			if (Configuration.IMPLICIT_TRACKING){
				if(this.localIdxOfControlTag < 0)
					localIdxOfControlTag = lvs.idxOfMasterControlLV;
				super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)Ljava/lang/Class;", false);
				super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader$$PHOSPHORTAGGED", "(Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)Ljava/lang/ClassLoader;", false);
				super.visitVarInsn(Opcodes.ALOAD, localIdxOfControlTag);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName$$PHOSPHORTAGGED", "(Ljava/lang/String;"+ Configuration.TAINT_TAG_DESC +"ZLjava/lang/ClassLoader;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)Ljava/lang/Class;", false);

			}
			else {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
			}
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
