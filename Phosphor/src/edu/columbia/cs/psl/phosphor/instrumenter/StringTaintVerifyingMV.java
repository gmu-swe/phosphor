package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.HashSet;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.FrameNode;

public class StringTaintVerifyingMV extends MethodVisitor implements Opcodes{
boolean implementsSerializable;
NeverNullArgAnalyzerAdapter analyzer;
	public StringTaintVerifyingMV(MethodVisitor mv,boolean implementsSerializable, NeverNullArgAnalyzerAdapter analyzer) {
		super(Opcodes.ASM5,mv);
		this.analyzer = analyzer;
		this.implementsSerializable=implementsSerializable;
	}
	HashSet<String> checkedThisFrame = new HashSet<String>();
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		checkedThisFrame = new HashSet<String>();
		super.visitFrame(type, nLocal, local, nStack, stack);
	}
	
	boolean ignoretaints = false;
	@Override
	public void visitInsn(int opcode) {
		if(opcode == TaintUtils.DONT_LOAD_TAINT)
			ignoretaints = !ignoretaints;
		super.visitInsn(opcode);
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(ignoretaints)
		{
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		Type t = Type.getType(desc);
		if(Configuration.SINGLE_TAG_PER_ARRAY &&
				(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && t.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
						t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1 && !checkedThisFrame.contains(owner+"."+name)
						&& (owner.equals("java/lang/String") || implementsSerializable))
				){
			super.visitInsn(SWAP);
			super.visitInsn(POP);
			
			//Make sure that it's been initialized
			Label isOK = new Label();
			Label doInit = new Label();
			
			FrameNode fn1 = TaintAdapter.getCurrentFrameNode(analyzer);
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name,desc);
			super.visitJumpInsn(IFNULL, isOK); //if value is null, do nothing
			
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			super.visitJumpInsn(IFNONNULL, isOK); //if taint is null, def init
			TaintAdapter.acceptFn(fn1, this);
			super.visitInsn(DUP); // O O
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "createEmptyTaint", "()"+Configuration.TAINT_TAG_DESC, false);

			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC); // O
			super.visitLabel(isOK);
			TaintAdapter.acceptFn(fn1, this);
			//O
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);

			return;
		}
		if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && TaintUtils.isPrimitiveArrayType(t) && 
				!name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
				(owner.equals("java/lang/String") || implementsSerializable || TaintUtils.LAZY_TAINT_ARRAY_INIT)
				)
		{
//			checkedThisFrame.add(owner+"."+name);
			//Tag Obj
			super.visitInsn(SWAP);
 			//Obj Tag
			super.visitInsn(POP);
			//Obj
			
			//Make sure that it's been initialized
			Label isOK = new Label();
//			Label doInit = new Label();
			
			FrameNode fn1 = TaintAdapter.getCurrentFrameNode(analyzer);
			super.visitInsn(DUP);
			//Obj Obj
			super.visitFieldInsn(opcode, owner, name,desc);
			//Obj F
			super.visitJumpInsn(IFNULL, isOK); //if value is null, do nothing
			//Obj
			super.visitInsn(DUP);
			//Obj Obj
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			//Obj tf
			super.visitJumpInsn(IFNONNULL, isOK); //if taint is null, def init
			//Obj

			super.visitInsn(DUP);
			//Obj Obj
			super.visitTypeInsn(NEW, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
			super.visitInsn(DUP);
			super.visitMethodInsn(INVOKESPECIAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "<init>", "()V", false);
			//Obj Obj TF
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC); // O
			//Obj
			super.visitLabel(isOK);
			TaintAdapter.acceptFn(fn1, this);
			//O
			super.visitInsn(DUP);
			//OO
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);

			return;
		}
		else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
