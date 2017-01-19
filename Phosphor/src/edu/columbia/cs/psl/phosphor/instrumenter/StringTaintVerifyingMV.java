package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;

public class StringTaintVerifyingMV extends MethodVisitor implements Opcodes {
	boolean implementsSerializable;
	NeverNullArgAnalyzerAdapter analyzer;

	public StringTaintVerifyingMV(MethodVisitor mv, boolean implementsSerializable,
			NeverNullArgAnalyzerAdapter analyzer) {
		super(Opcodes.ASM5, mv);
		this.analyzer = analyzer;
		this.implementsSerializable = implementsSerializable;
	}

	HashSet<String> checkedThisFrame = new HashSet<String>();

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		checkedThisFrame = new HashSet<String>();
		super.visitFrame(type, nLocal, local, nStack, stack);
	}

	private boolean nextLoadIsTainted = false;

	@Override
	public void visitInsn(int opcode) {
		if (opcode == TaintUtils.TRACKED_LOAD)
			nextLoadIsTainted = true;
		super.visitInsn(opcode);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		nextLoadIsTainted = false;
		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		nextLoadIsTainted = false;
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
	@Override
	public void visitTypeInsn(int opcode, String type) {
		nextLoadIsTainted = false;
		super.visitTypeInsn(opcode, type);
	}
	@Override
	public void visitIincInsn(int var, int increment) {
		nextLoadIsTainted = false;
		super.visitIincInsn(var, increment);
	}
	@Override
	public void visitIntInsn(int opcode, int operand) {
		nextLoadIsTainted = false;
		super.visitIntInsn(opcode, operand);
	}
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		nextLoadIsTainted = false;
		super.visitMultiANewArrayInsn(desc, dims);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Type t = Type.getType(desc);
		if(nextLoadIsTainted && opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && t.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD) && !name.equals("taint") && 
				t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1 && !checkedThisFrame.contains(owner+"."+name)
				&& (owner.equals("java/lang/String") || implementsSerializable || owner.equals("java/io/BufferedInputStream")
						|| owner.startsWith("java/lang/reflect"))
				)
		{
//			System.out.println(owner+name+desc+analyzer.stackTagStatus);
			nextLoadIsTainted = false;
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
			String shadowDesc = TaintUtils.getShadowTaintType(desc);
			String shadowObj = Type.getType(shadowDesc).getInternalName();
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, shadowDesc);
			//Obj tf
			super.visitJumpInsn(IFNONNULL, isOK); //if taint is null, def init
			//Obj
			//if taint is not null, check the length
//			super.visitInsn(DUP); // O O
//			super.visitInsn(DUP); // O O O
//			super.visitFieldInsn(opcode, owner, name,desc);
//			super.visitInsn(ARRAYLENGTH);
//			super.visitInsn(SWAP);
//			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
//			super.visitInsn(ARRAYLENGTH);
//			super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
//			super.visitLabel(doInit);
//			TaintAdapter.acceptFn(fn1, this);
//			super.visitInsn(DUP); // O O
//			super.visitInsn(DUP); // O O O
//			super.visitFieldInsn(opcode, owner, name, desc); //O O A
//			super.visitInsn(ARRAYLENGTH);//O O L
//			if (!Configuration.MULTI_TAINTING)
//				super.visitIntInsn(NEWARRAY, T_INT);//O O A
//			else
//				super.visitTypeInsn(ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
			super.visitInsn(DUP);
			super.visitInsn(DUP);
			//Obj Obj
			super.visitFieldInsn(opcode, owner, name,desc);

			//Obj Obj

			super.visitTypeInsn(NEW, shadowObj);
			super.visitInsn(DUP_X1);
			super.visitInsn(SWAP);
			super.visitMethodInsn(INVOKESPECIAL, shadowObj, "<init>", "("+desc+")V", false);
			
			//Obj Obj TF
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, shadowDesc); // O
			//Obj
			super.visitLabel(isOK);
			TaintAdapter.acceptFn(fn1, this);
			//O
			super.visitInsn(DUP);
			//OO
			super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, shadowDesc);
			super.visitInsn(SWAP);
			super.visitFieldInsn(opcode, owner, name, desc);
//			super.visitInsn(DUP2);
//			super.visitMethodInsn(INVOKEVIRTUAL, shadowObj, "ensureVal", "("+desc+")V", false);
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
			TaintAdapter.acceptFn(fn1, this);
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name, desc); //O O A
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);//O O A L
			super.visitMultiANewArrayInsn("[[I", 1); //O O A TA
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[I)[[I",false);
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, "[[I"); // O
			super.visitLabel(isOK);
			TaintAdapter.acceptFn(fn1, this);
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
			TaintAdapter.acceptFn(fn1, this);
			super.visitInsn(DUP); // O O
			super.visitInsn(DUP); // O O O
			super.visitFieldInsn(opcode, owner, name, desc); //O O A
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);//O O A L
			super.visitMultiANewArrayInsn("[[[I", 1); //O O A TA
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[I)[[[I",false);
			super.visitFieldInsn(PUTFIELD, owner, name+TaintUtils.TAINT_FIELD, "[[[I"); // O
			super.visitLabel(isOK);
			TaintAdapter.acceptFn(fn1, this);
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
