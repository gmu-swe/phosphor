package edu.columbia.cs.psl.phosphor.instrumenter;


import edu.columbia.cs.psl.phosphor.BasicSourceSinkManager;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.NullSourceSinkManager;
import edu.columbia.cs.psl.phosphor.SourceSinkManager;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.InstructionAdapter;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitive;

public class SourceSinkTaintingMV extends InstructionAdapter implements Opcodes{
	static SourceSinkManager sourceSinkManager = new NullSourceSinkManager();//BasicSourceSinkManager.getInstance(Instrumenter.callgraph);

	String owner;
	String name;
	String desc;
	boolean thisIsASource;
	boolean thisIsASink;
	String origDesc;
	int access;
	boolean isStatic;
	public SourceSinkTaintingMV(MethodVisitor mv,int access, String owner, String name, String desc, String origDesc) {
		super(ASM5,mv);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.access =access;
		this.origDesc = origDesc;
		this.thisIsASource = sourceSinkManager.isSource(owner,name,desc);
		this.thisIsASink = sourceSinkManager.isSink(owner, name, desc);
		this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
	}

	@Override
	public void visitCode() {
		super.visitCode();
//		if(this.thisIsASource)
//		{
//			//TODO - add a taint tag to each array arg
//			Type[] origArgs = Type.getArgumentTypes(origDesc);
//			int idx = 0;
//			if(!isStatic)
//				idx++;
//			for(int i = 0; i < origArgs.length; i++)
//			{
//				if(origArgs[i].getSort() == Type.OBJECT)
//				{
//					super.visitVarInsn(ALOAD, idx);
//					super.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.class));
//					Label isOK = new Label();
//					super.visitJumpInsn(IFEQ, isOK);
//					super.visitVarInsn(ALOAD, idx);
//					super.visitInsn(ICONST_1);
//					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MultiDTaintedArray.class), "setTaints", "(I)V", false);
//					super.visitLabel(isOK);
//				}
//				else if(origArgs[i].getSort() == Type.ARRAY)
//				{
//					super.visitVarInsn(ALOAD, idx);
//					super.visitInsn(ICONST_1);
//					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "([II)V",false);
//					idx++;
//				}
//				else
//				{
//					idx++;
//				}
//				idx += origArgs[i].getSize();
//			}
//		}
//		if(sourceSinkManager.isSink(owner,name,desc))
//		{
//			//TODO - check every arg to see if is taint tag
//			Type[] origArgs = Type.getArgumentTypes(origDesc);
//			int idx = 0;
//			if(!isStatic)
//				idx++;
//			for(int i = 0; i < origArgs.length; i++)
//			{
//				if(origArgs[i].getSort() == Type.OBJECT)
//				{
//					super.visitVarInsn(ALOAD, idx);
//					super.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.class));
//					Label isOK = new Label();
//					super.visitJumpInsn(IFEQ, isOK);
//					super.visitVarInsn(ALOAD, idx);
//					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MultiDTaintedArray.class), "hasTaints", "()Z", false);
//					super.visitJumpInsn(IFEQ, isOK);
//					super.visitTypeInsn(NEW, Type.getInternalName(IllegalArgumentException.class));
//					super.visitInsn(DUP);
//					super.visitLdcInsn("Arg " + i + " is tainted");
//					super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class), "<init>", "(Ljava/lang/String;)V", false);
//					super.visitInsn(ATHROW);
//					super.visitLabel(isOK);
//				}
//				else if(origArgs[i].getSort() == Type.ARRAY)
//				{
//					super.visitVarInsn(ALOAD, idx);
//					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "hasTaints", "([I)Z",false);
//					Label isOK = new Label();
//					super.visitJumpInsn(IFEQ, isOK);
//					super.visitTypeInsn(NEW, Type.getInternalName(IllegalArgumentException.class));
//					super.visitInsn(DUP);
//					super.visitLdcInsn("Arg " + i + " is tainted");
//					super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class), "<init>", "(Ljava/lang/String;)V", false);
//					super.visitInsn(ATHROW);
//					super.visitLabel(isOK);
//					idx++;
//				}
//				else
//				{
//					super.visitVarInsn(ILOAD, idx);
//					Label isOK = new Label();
//					super.visitJumpInsn(IFEQ, isOK);
//					super.visitTypeInsn(NEW, Type.getInternalName(IllegalArgumentException.class));
//					super.visitInsn(DUP);
//					super.visitLdcInsn("Arg " + i + " is tainted");
//					super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class), "<init>", "(Ljava/lang/String;)V", false);
//					super.visitInsn(ATHROW);
//					super.visitLabel(isOK);
//					idx++;
//				}
//				idx += origArgs[i].getSize();
//			}
//		}
	}
	@Override
	public void visitInsn(int opcode) {
//		if(opcode == ARETURN && this.thisIsASource)
//		{
//			Type returnType = Type.getReturnType(this.origDesc);
//			if(returnType.getSort() == Type.OBJECT)
//			{
//				
//			}
//			else if(returnType.getSort() == Type.ARRAY)
//			{
//				if(returnType.getElementType().getSort() != Type.OBJECT)
//				{
//					super.visitInsn(DUP);
//					super.visitInsn(ICONST_1);
//					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintedPrimitiveArray.class), "setTaints", "(I)V",false);
//				}
//			}
//			else if(returnType.getSort() == Type.VOID)
//			{
//				
//			}
//			else
//			{
//				//primitive
//				super.visitInsn(DUP);
//				super.visitInsn(ICONST_1);
//				super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitive.class), "taint", "I");
//			}
//		}
		super.visitInsn(opcode);
	}
}
