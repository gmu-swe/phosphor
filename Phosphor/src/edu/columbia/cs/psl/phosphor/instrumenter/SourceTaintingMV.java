package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SourceTaintingMV extends MethodVisitor implements Opcodes {
	static SourceSinkManager sourceSinkManager = BasicSourceSinkManager.getInstance();

	private final String owner;
	private final String name;
	private final String desc;
	private final Type origReturnType;
	private final boolean isStatic;
	private final Object lbl;


	public SourceTaintingMV(MethodVisitor mv, int access, String owner, String name, String desc) {
		super(ASM5, mv);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.origReturnType = Type.getReturnType(SourceSinkManager.remapMethodDescToRemoveTaints(desc));
		this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
		this. lbl = sourceSinkManager.getLabel(owner, name, desc);
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
		Type[] args = Type.getArgumentTypes(desc);
		int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
		for (int i = 0; i < args.length; i++) {
			if (args[i].getSort() == Type.OBJECT) {
				super.visitVarInsn(ALOAD, idx);
				if(Configuration.MULTI_TAINTING) {
					super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
					super.visitInsn(SWAP);
					super.visitLdcInsn(lbl);
					super.visitIntInsn(BIPUSH, i);
					if(args[i].getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "("+args[i].getDescriptor()+"Ljava/lang/String;I)"+args[i].getDescriptor(), false);
					} else {
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;", false);
						super.visitTypeInsn(CHECKCAST, args[i].getInternalName());
					}
					super.visitVarInsn(ASTORE, idx);
				} else {
					// Int-tags
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
			} else if (args[i].getSort() == Type.ARRAY
					&& args[i].getElementType().getSort() == Type.OBJECT) {
				super.visitVarInsn(ALOAD, idx);
				loadSourceLblAndMakeTaint();
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
			}
			idx += args[i].getSize();
		}
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == ARETURN) {
			Type boxedReturnType = Type.getReturnType(this.desc);
			if (origReturnType.getSort() == Type.OBJECT || origReturnType.getSort() == Type.ARRAY) {
				// Reference original return type
				if(Configuration.MULTI_TAINTING) {
					super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
					super.visitInsn(SWAP);
					super.visitLdcInsn(lbl);
					super.visitInsn(ICONST_M1);
					if(origReturnType.getSort() == Type.ARRAY && origReturnType.getDimensions() == 1 && origReturnType.getElementType().getSort() != Type.OBJECT)  {
						// Primitive 1D array
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "("+boxedReturnType.getDescriptor()+"Ljava/lang/String;I)"+boxedReturnType.getDescriptor(), false);
					} else {
						super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;", false);
						super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
					}
				} else {
					super.visitInsn(DUP);
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
			} else if(origReturnType.getSort() != Type.VOID){
				//  Wrapped primitive return type.
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
			}
		}
		super.visitInsn(opcode);
	}
}
