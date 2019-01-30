package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SourceSinkTaintingMV extends MethodVisitor implements Opcodes {
	static SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance();

	String owner;
	String name;
	String desc;
	boolean thisIsASource;
	boolean thisIsASink;
	boolean thisIsTaintThrough;
	String origDesc;
	int access;
	boolean isStatic;
	Object lbl;

	public SourceSinkTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc, String origDesc) {
		super(ASM5, mv);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.access = access;
		this.origDesc = origDesc;
		this.thisIsASource = sourceSinkManager.isSource(owner, name, desc);
		this.thisIsASink = sourceSinkManager.isSink(owner, name, desc);
		this.thisIsTaintThrough = sourceSinkManager.isTaintThrough(owner, name, desc);
		this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
		if (this.thisIsASource) {
			lbl = sourceSinkManager.getLabel(owner, name, desc);
			if(PreMain.DEBUG)
				System.out.println("Source: " + owner + "." + name + desc + " Label: " + lbl);
		}
		if (PreMain.DEBUG) {
			if (this.thisIsASink)
				System.out.println("Sink: " + owner + "." + name + desc);
			if (this.thisIsTaintThrough)
				System.out.println("Taint through: " + owner + "." + name + desc);
		}
	}

	private void loadSourceLblAndMakeTaint() {
		if (Configuration.MULTI_TAINTING) {
			super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "taintTagFactory", Type.getDescriptor(TaintTagFactory.class));
			super.visitLdcInsn(lbl);
			super.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(TaintTagFactory.class), "getAutoTaint", "(Ljava/lang/String;)"+Configuration.TAINT_TAG_DESC, true);
		} else {
			super.visitLdcInsn(lbl);
		}
	}

	@Override
	public void visitCode() {
		super.visitCode();
		if (this.thisIsASource || thisIsTaintThrough) {
			Type[] args = Type.getArgumentTypes(desc);
			int idx = 0;
			if (!isStatic)
				idx++;
			for (int i = 0; i < args.length; i++) {
				if (args[i].getSort() == Type.OBJECT) {
					super.visitVarInsn(ALOAD, idx);
					if(thisIsASource)
					{
						if(Configuration.MULTI_TAINTING)
						{
							super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
							super.visitInsn(SWAP);
							super.visitLdcInsn(lbl);
							super.visitIntInsn(BIPUSH, i);
							if(args[i].getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy"))
							{
								super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "("+args[i].getDescriptor()+"Ljava/lang/String;I)"+args[i].getDescriptor(), false);
							}
							else
							{
								super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;", false);
								super.visitTypeInsn(CHECKCAST, args[i].getInternalName());
							}
							super.visitVarInsn(ASTORE, idx);
						}
						else
						{
							loadSourceLblAndMakeTaint();
							super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
						}
					}
					else if(Configuration.MULTI_TAINTING)
					{
						super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
						super.visitInsn(SWAP);
						super.visitVarInsn(ALOAD, 0);
						super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
						if(Configuration.MULTI_TAINTING)
							super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "("+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
//						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
					}
				} else if (args[i].getSort() == Type.ARRAY
						&& args[i].getElementType().getSort() == Type.OBJECT) {
					super.visitVarInsn(ALOAD, idx);
					if(thisIsASource)
					{
						loadSourceLblAndMakeTaint();
					}
					else if(Configuration.MULTI_TAINTING)
					{
						super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
						super.visitInsn(SWAP);

						super.visitVarInsn(ALOAD, 0);
						super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
						if(Configuration.MULTI_TAINTING)
							super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "("+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					}
					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
				idx += args[i].getSize();
			}
		}
		if (sourceSinkManager.isSink(owner, name, desc)) {
			//TODO - check every arg to see if is taint tag
			Type[] args = Type.getArgumentTypes(desc);
			int idx = 0;
			if (!isStatic)
				idx++;
			boolean skipNextPrimitive = false;
			for (int i = 0; i < args.length; i++) {
				if ((args[i].getSort() == Type.OBJECT && !args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC)) || args[i].getSort() == Type.ARRAY) {
					if ((args[i].getSort() == Type.ARRAY && (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC))
							&& args[i].getDimensions() == 1) || args[i].getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy") ){
						if (!skipNextPrimitive) {
							super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
							super.visitVarInsn(ALOAD, idx);
							super.visitLdcInsn(owner+"."+name+desc);
							super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
						}
						skipNextPrimitive = !skipNextPrimitive;
					} else {
						super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
						super.visitVarInsn(ALOAD, idx);
						super.visitLdcInsn(owner+"."+name+desc);
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
					}
				} else if (!skipNextPrimitive) {
					super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
					super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
					super.visitLdcInsn(owner+"."+name+desc);
					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "checkTaint", "(" + Configuration.TAINT_TAG_DESC + "Ljava/lang/String;)V", false);
					skipNextPrimitive = true;
				} else if (skipNextPrimitive)
					skipNextPrimitive = false;
				idx += args[i].getSize();
			}
		}
	}

	@Override
	public void visitInsn(int opcode) {
		if (this.thisIsTaintThrough)
			switch (opcode) {
				case ARETURN:
				case IRETURN:
				case RETURN:
				case DRETURN:
				case FRETURN:
				case LRETURN:
					Type[] args = Type.getArgumentTypes(desc);
					int idx = 0;
					if (!isStatic)
						idx++;
					for (int i = 0; i < args.length; i++) {
						if (args[i].getSort() == Type.OBJECT) {
							super.visitVarInsn(ALOAD, idx);
							if (Configuration.MULTI_TAINTING) {
								super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
								super.visitInsn(SWAP);
								super.visitVarInsn(ALOAD, 0);
								super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
								if (Configuration.MULTI_TAINTING)
									super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "(" + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
//						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
								super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
							}
						} else if (args[i].getSort() == Type.ARRAY
								&& args[i].getElementType().getSort() == Type.OBJECT) {
							super.visitVarInsn(ALOAD, idx);
							if (Configuration.MULTI_TAINTING) {
								super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
								super.visitInsn(SWAP);

								super.visitVarInsn(ALOAD, 0);
								super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
								if (Configuration.MULTI_TAINTING)
									super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "(" + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
							}
							super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
						}
						idx += args[i].getSize();
					}
					break;
			}
		if (opcode == ARETURN && (this.thisIsASource || this.thisIsTaintThrough)) {
			Type returnType = Type.getReturnType(this.origDesc);
			Type boxedReturnType = Type.getReturnType(this.desc);

			if (returnType.getSort() == Type.VOID) {
				
			}
			else if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
				if(thisIsASource)
				{
					if(Configuration.MULTI_TAINTING)
					{
						if(returnType.getSort() == Type.ARRAY && returnType.getDimensions() == 1 && returnType.getElementType().getSort() != Type.OBJECT)
						{
							//Primitive 1D array
							super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
							super.visitInsn(SWAP);
							super.visitLdcInsn(lbl);
							super.visitInsn(ICONST_M1);
							super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "("+boxedReturnType.getDescriptor()+"Ljava/lang/String;I)"+boxedReturnType.getDescriptor(), false);
						}
						else
						{
							super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
							super.visitInsn(SWAP);
							super.visitLdcInsn(lbl);
							super.visitInsn(ICONST_M1);
							super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;", false);
							super.visitTypeInsn(CHECKCAST, returnType.getInternalName());
						}
					}
					else
					{
						super.visitInsn(DUP);
						loadSourceLblAndMakeTaint();
						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
					}
				}
				else if(thisIsTaintThrough)
				{
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, 0);
					super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					if(Configuration.MULTI_TAINTING)
						super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "("+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
			} else {
				// primitive
				if (thisIsASource) {
					if (Configuration.MULTI_TAINTING) {
						super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
						super.visitInsn(SWAP);
						super.visitLdcInsn(lbl);
						super.visitInsn(ICONST_M1);
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(" + boxedReturnType.getDescriptor() + "Ljava/lang/String;I)" + boxedReturnType.getDescriptor(), false);
					} else {
						super.visitInsn(DUP);
						loadSourceLblAndMakeTaint();
						super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithIntTag.class), "taint", "I");
					}
				} else if (thisIsTaintThrough) {
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, 0);
					super.visitFieldInsn(GETFIELD, owner, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					if (Configuration.MULTI_TAINTING)
						super.visitMethodInsn(INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "(" + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
					if (Configuration.MULTI_TAINTING)
						super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithObjTag.class), "taint", Configuration.TAINT_TAG_DESC);
					else
						super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithIntTag.class), "taint", "I");

				}
			}
		}
	
		super.visitInsn(opcode);
	}
}
