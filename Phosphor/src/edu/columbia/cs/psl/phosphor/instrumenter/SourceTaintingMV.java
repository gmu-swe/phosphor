package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.*;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
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
		this.lbl = sourceSinkManager.getLabel(owner, name, desc);
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

	/* Adds code to make a call to autoTaint. Supplies the specified int as the argument index. Check that the value returned
	* by the call can be cast to the class with the specified internal name. */
	private void callAutoTaint(int argIndex, String internalName) {
		super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
		super.visitInsn(SWAP);
		super.visitLdcInsn(lbl);
		super.visitIntInsn(BIPUSH, argIndex);
		super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "autoTaint", "(Ljava/lang/Object;Ljava/lang/String;I)Ljava/lang/Object;", false);
		super.visitTypeInsn(CHECKCAST, internalName);
	}

	/* Adds code to taint the arguments passed to this method. */
	private void autoTaintArguments() {
		Type[] args = Type.getArgumentTypes(desc);
		int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
		for (int i = 0; i < args.length; i++) {
			if(Configuration.MULTI_TAINTING) {
				if(args[i].getSort() == Type.OBJECT || args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT) {
					super.visitVarInsn(ALOAD, idx); // load the argument onto the stack
					callAutoTaint(i, args[i].getInternalName());
					super.visitVarInsn(ASTORE, idx); // replace the argument with the return of autoTaint
				}
			} else {
				if (args[i].getSort() == Type.OBJECT) {
					// Int-tags
                    super.visitVarInsn(ALOAD, idx);
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				} else if (args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT) {
					super.visitVarInsn(ALOAD, idx);
					loadSourceLblAndMakeTaint();
					super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
			}
			idx += args[i].getSize();
		}
	}

	@Override
	public void visitCode() {
		super.visitCode();
		autoTaintArguments();
	}

	@Override
	public void visitInsn(int opcode) {
		if (TaintUtils.isReturnOpcode(opcode)) {
			autoTaintArguments();
		}
		if (opcode == ARETURN) {
			Type boxedReturnType = Type.getReturnType(this.desc);
			if(Configuration.MULTI_TAINTING && origReturnType.getSort() != Type.VOID) {
				callAutoTaint(-1, boxedReturnType.getInternalName());
			} else if (origReturnType.getSort() != Type.VOID) {
				// Int tags
				super.visitInsn(DUP);
				loadSourceLblAndMakeTaint();
				if (origReturnType.getSort() == Type.OBJECT || origReturnType.getSort() == Type.ARRAY) {
					// Reference original return type
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintChecker.class), "setTaints", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);

				} else {
				//  Wrapped primitive return type.
					super.visitFieldInsn(PUTFIELD, Type.getInternalName(TaintedPrimitiveWithIntTag.class), "taint", "I");
				}
			}
		}
		super.visitInsn(opcode);
	}
}
