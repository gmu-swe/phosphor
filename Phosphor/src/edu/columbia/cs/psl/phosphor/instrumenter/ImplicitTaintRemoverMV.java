package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

public class ImplicitTaintRemoverMV extends TaintAdapter implements Opcodes {

	public ImplicitTaintRemoverMV(int api, String className, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
		super(api, className, mv, analyzer);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(this.isIgnoreEverything)
		{
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
//		if((opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) && desc.equals("Ljava/lang/Object;") && getTopOfStackType().getSort() == Type.ARRAY && getTopOfStackType().getElementType().getSort() != Type.OBJECT)
//		{
//			super.visitInsn(SWAP);
//			super.visitInsn(POP);
//		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	boolean isIgnoreEverything;
	public void visitInsn(int opcode) {
		if(opcode == TaintUtils.IGNORE_EVERYTHING)
		{
			this.isIgnoreEverything = !this.isIgnoreEverything;
		}
		else if(opcode == TaintUtils.DONT_LOAD_TAINT)
		{
			this.isIgnoreEverything = !this.isIgnoreEverything;		
		}
		super.visitInsn(opcode);
	}
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if(this.isIgnoreEverything || Configuration.IMPLICIT_TRACKING)
		{
			super.visitJumpInsn(opcode, label);
			return;
		}
		switch (opcode) {
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
			//top is val, taint
			super.visitInsn(SWAP);
			super.visitInsn(POP);
			super.visitJumpInsn(opcode, label);

			break;
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
			//top is val, taint, val, taint
			super.visitInsn(SWAP);
			super.visitInsn(POP);
			//val, val, taint
			super.visitInsn(DUP2_X1);
			super.visitInsn(POP2);
			super.visitInsn(POP);
			super.visitJumpInsn(opcode, label);

			break;
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:

			Type typeOnStack = getTopOfStackType();
			if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
				super.visitInsn(SWAP);
				super.visitInsn(POP);
			}
			//O1 O2 (t2?)
			Type secondOnStack = getStackTypeAtOffset(1);
			if (secondOnStack.getSort() == Type.ARRAY && secondOnStack.getElementType().getSort() != Type.OBJECT) {
				//O1 O2 T2
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitInsn(POP);
			}
			super.visitJumpInsn(opcode, label);

			break;
		case Opcodes.GOTO:
			//we don't care about goto
			super.visitJumpInsn(opcode, label);
			break;
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:

			typeOnStack = getTopOfStackType();
			if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
				//O1 T1
				super.visitInsn(SWAP);
				super.visitInsn(POP);
			}
			super.visitJumpInsn(opcode, label);

			break;
		default:
			throw new IllegalArgumentException();
		}
	}
}
