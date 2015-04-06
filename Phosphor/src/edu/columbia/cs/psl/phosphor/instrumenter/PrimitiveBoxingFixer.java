package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;

public class PrimitiveBoxingFixer extends TaintAdapter implements Opcodes {

	public PrimitiveBoxingFixer(int api, String className, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
		super(api, className, mv, analyzer);
	}

	int tmpInt = -1;;
	boolean ignoreLoadingTaint;


	@Override
	public void visitInsn(int opcode) {
		if (opcode == TaintUtils.DONT_LOAD_TAINT)
			ignoreLoadingTaint = !ignoreLoadingTaint;
		super.visitInsn(opcode);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (ignoreLoadingTaint) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		int nArgs = Type.getArgumentTypes(desc).length;
		boolean argIsStr = false;
		for (Type t : Type.getArgumentTypes(desc))
			if (t.getSort() == Type.OBJECT)
				argIsStr = true;
		//Get an extra copy of the taint
		if ((owner.equals(Type.getInternalName(Integer.class))
		//				|| owner.equals(Type.getInternalName(Byte.class))
		//				|| owner.equals(Type.getInternalName(Character.class))
		//				|| owner.equals(Type.getInternalName(Short.class)) ||  owner.equals(Type.getInternalName(Float.class)) 
				|| owner.equals(Type.getInternalName(Long.class)) || owner.equals(Type.getInternalName(Double.class))) && name.equals("valueOf$$PHOSPHORTAGGED") && nArgs == 2 && !argIsStr) {
			Type argT = Type.getArgumentTypes(desc)[1];
			int argSize = argT.getSize();
			if (argSize == 1) {
//				System.out.println(analyzer.stack);
				//stack is currently T I <top>
				//we'll support (Integer) 1 == (Integer) 1 as long as there is no taint on it.
				super.visitInsn(SWAP);
				FrameNode fn = getCurrentFrameNode();
				super.visitInsn(DUP);
				Label makeNew = new Label();
				Label isOK = new Label();
				super.visitJumpInsn(IFNE, makeNew);
				super.visitInsn(SWAP);
				super.visitMethodInsn(opcode, owner, name, desc, itfc);
				super.visitJumpInsn(GOTO, isOK);
				super.visitLabel(makeNew);
				fn.accept(this);
				super.visitInsn(SWAP);
				super.visitTypeInsn(Opcodes.NEW, owner);
				super.visitInsn(Opcodes.DUP);
				//T I N N
				super.visitInsn(Opcodes.DUP2_X2);
				super.visitInsn(Opcodes.POP2);
				//N N T I
				super.visitInsn(Opcodes.ACONST_NULL);
				super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "(I" + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(TaintSentinel.class) + ")V",false);
				FrameNode fn2 = getCurrentFrameNode();
				super.visitLabel(isOK);
				fn2.accept(this);
			} else {
				//T V V <top>
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				//VV T
				FrameNode fn = getCurrentFrameNode();
				super.visitInsn(DUP);
				Label makeNew = new Label();
				Label isOK = new Label();
				super.visitJumpInsn(IFNE, makeNew);
				//T VV 
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				super.visitMethodInsn(opcode, owner, name, desc, false);
				FrameNode fn2 = getCurrentFrameNode();
				super.visitJumpInsn(GOTO, isOK);
				super.visitLabel(makeNew);
				fn.accept(this);

				Type taintType = Type.getType(Configuration.TAINT_TAG_DESC);
				
				//VV T
				int tmp = lvs.getTmpLV(argT);
				int tmpT = lvs.getTmpLV(taintType);
				super.visitVarInsn(taintType.getOpcode(ISTORE), tmpT);
				super.visitVarInsn(argT.getOpcode(ISTORE), tmp);
				super.visitTypeInsn(Opcodes.NEW, owner);
				super.visitInsn(Opcodes.DUP);
				//T I N N

				super.visitVarInsn(taintType.getOpcode(ILOAD), tmpT);
				super.visitVarInsn(argT.getOpcode(ILOAD), tmp);

				super.visitInsn(Opcodes.ACONST_NULL);
				super.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "("+Configuration.TAINT_TAG_DESC + Type.getArgumentTypes(desc)[1].getDescriptor() + Type.getDescriptor(TaintSentinel.class) + ")V", false);
				lvs.freeTmpLV(tmp);
				lvs.freeTmpLV(tmpT);
				super.visitLabel(isOK);
				fn2.accept(this);
//				super.visitMethodInsn(opcode, owner, name, desc,itfc);

			}
		} 
//		else if (owner.equals(Type.getInternalName(Integer.class)) && name.equals("parseInt$$PHOSPHORTAGGED")) {
//			if (nArgs == 2) {
//				super.visitInsn(Opcodes.DUP);
//				super.visitMethodInsn(opcode, owner, name, desc,itfc);
//				super.visitInsn(DUP_X1);
//				super.visitInsn(SWAP);
//				retrieveTopOfStackTaintAndPop();
//			} else if (nArgs == 4) {
//				//S I I
//				super.visitInsn(DUP2_X1);
//				//				//I I S II
//				super.visitInsn(POP2);
//				//				//II S
//				retrieveTopOfStackTaint(true, true);
//				int tmpInt = lvs.getTmpLV(Type.INT_TYPE);
//
//				super.visitVarInsn(ISTORE, tmpInt);
//				super.visitInsn(DUP_X2);
//				super.visitInsn(POP);
//				super.visitMethodInsn(opcode, owner, name, desc,itfc);
//				super.visitInsn(DUP);
//				super.visitVarInsn(ILOAD, tmpInt);
//				lvs.freeTmpLV(tmpInt);
//
//			}
//			//stack is now <Integer Integer taint TOP>
//			super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedInt.class), "taint", "I");
//		} 
			else
			super.visitMethodInsn(opcode, owner, name, desc,itfc);

		//O T
		//		if((owner.equals(Type.getInternalName(Integer.class))
		////				|| owner.equals(Type.getInternalName(String.class))
		//				|| owner.equals(Type.getInternalName(Byte.class))
		//				|| owner.equals(Type.getInternalName(Character.class))
		//				|| owner.equals(Type.getInternalName(Short.class))
		////				|| owner.equals(Type.getInternalName(Long.class))
		//				|| owner.equals(Type.getInternalName(Float.class))
		////				|| owner.equals(Type.getInternalName(Double.class))
		//				)//TODO support strings and long/double
		//				&& name.equals("valueOf$$PHOSPHORTAGGED") && nArgs == 1 && !argIsStr)		{
		//			super.visitInsn(Opcodes.DUP_X1); //O T O
		//			super.visitInsn(Opcodes.SWAP);//T O O
		////			if(owner.equals("java/lang/String"))
		////			{
		//				//Initialize the taint array so each element is the 
		//				//TODO i think we were supposed to do something here and i forgot.
		////				super.visitFieldInsn(Opcodes.PUTFIELD, owner, "value"+TaintUtils.TAINT_FIELD, "[I");
		////			}
		////			else
		//			super.visitFieldInsn(Opcodes.PUTFIELD, owner, TaintUtils.TAINT_FIELD, "I");
		//		}
		//TODO boxing the other way maybe too?
		//TODO handle situations with radix param
	}

}
