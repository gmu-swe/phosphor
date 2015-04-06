package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.InstructionAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;

public class StringTaintVerifyingMV extends InstructionAdapter implements Opcodes{
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
		if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && t.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
				t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1 && !checkedThisFrame.contains(owner+"."+name)
				&& (owner.equals("java/lang/String") || implementsSerializable)
				)
		{
//			checkedThisFrame.add(owner+"."+name);
			
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
			super.visitJumpInsn(IFNULL, doInit); //if taint is null, def init
			//if taint is not null, check the length
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name,desc);
			super.visitInsn(ARRAYLENGTH);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			super.visitInsn(ARRAYLENGTH);
			super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
			super.visitLabel(doInit);
			fn1.accept(this);
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name, desc); //O O A
			super.visitInsn(ARRAYLENGTH);//O O L
			if (!Configuration.MULTI_TAINTING)
				super.visitIntInsn(NEWARRAY, T_INT);//O O A
			else
				super.visitTypeInsn(ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC); // O
			super.visitLabel(isOK);
			fn1.accept(this);
			//O
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);

			return;
		}
		else if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && t.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
				t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 2 && !checkedThisFrame.contains(owner+"."+name))
		{
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
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[I");
			super.visitJumpInsn(IFNULL, doInit); //if taint is null, def init
			//if taint is not null, check the length
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name,desc);
			super.visitInsn(ARRAYLENGTH);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[I");
			super.visitInsn(ARRAYLENGTH);
			super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
			super.visitLabel(doInit);
			fn1.accept(this);
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name, desc); //O O A
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);//O O A L
			super.visitMultiANewArrayInsn("[[I", 1); //O O A TA
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[I)[[I",false);
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, "[[I"); // O
			super.visitLabel(isOK);
			fn1.accept(this);
			//O
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[I");
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		else if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && t.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
				t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 3 && !checkedThisFrame.contains(owner+"."+name))
		{
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
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[[I");
			super.visitJumpInsn(IFNULL, doInit); //if taint is null, def init
			//if taint is not null, check the length
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name,desc);
			super.visitInsn(ARRAYLENGTH);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[[I");
			super.visitInsn(ARRAYLENGTH);
			super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
			super.visitLabel(doInit);
			fn1.accept(this);
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name, desc); //O O A
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);//O O A L
			super.visitMultiANewArrayInsn("[[[I", 1); //O O A TA
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[I)[[[I",false);
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, "[[[I"); // O
			super.visitLabel(isOK);
			fn1.accept(this);
			//O
			super.visitInsn(DUP);
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, "[[[I");
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
