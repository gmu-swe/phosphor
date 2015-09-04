package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class DataAndControlFlowTagFactory implements TaintTagFactory, Opcodes {
	@Override
	public Taint dynamicallyGenerateEmptyTaint() {
		return new Taint();
	}

	@Override
	public void methodEntered(String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
	}
	@Override
	public void signalOp(int signal, Object option) {
	}

	@Override
	public void generateEmptyTaint(MethodVisitor mv) {
		mv.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
	}

	@Override
	public void generateEmptyTaintArray(Object[] array, int dims) {

	}

	@Override
	public void intOp(int opcode, int arg, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV adapter) {
		switch (opcode) {
		case Opcodes.NEWARRAY:
			if (Configuration.ARRAY_LENGTH_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
				if (Configuration.MULTI_TAINTING) {
					//Length Length-tag
					mv.visitInsn(DUP);
					mv.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
					mv.visitInsn(SWAP);
					mv.visitIntInsn(opcode, arg);
					//Array ArrayTags length-tag
					mv.visitInsn(DUP2_X1);
					//Ar At lt Ar At
					mv.visitInsn(DUP_X2);
					//Ar At lt Ar ar at
					mv.visitInsn(POP2);
					//lt ar ar at
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				mv.visitInsn(SWAP);
				mv.visitInsn(POP);
				mv.visitInsn(DUP);
				if (!Configuration.MULTI_TAINTING)
					mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
				else
					mv.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
				mv.visitInsn(SWAP);
				mv.visitIntInsn(opcode, arg);
			}
			break;

		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void stackOp(int opcode, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV adapter) {
		switch (opcode) {
		case Opcodes.FADD:
		case Opcodes.FREM:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
			//T V T V
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			int tmp = lvs.getTmpLV(adapter.getTopOfStackType());
			mv.visitVarInsn(FSTORE, tmp);
			//T V T
			mv.visitInsn(SWAP);
			//T T V
			mv.visitVarInsn(FLOAD, tmp);
			//T T V V
			mv.visitInsn(opcode);
			//T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			mv.visitInsn(SWAP);
			lvs.freeTmpLV(tmp);
			break;

		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.IOR:
		case Opcodes.IAND:
		case Opcodes.IXOR:
			tmp = lvs.getTmpLV(Type.INT_TYPE);
			//T V T V
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(ISTORE, tmp);
			//T V T
			mv.visitInsn(SWAP);
			//T T V
			mv.visitVarInsn(ILOAD, tmp);
			lvs.freeTmpLV(tmp);
			//T T V V
			mv.visitInsn(opcode);
			//T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			mv.visitInsn(SWAP);
			break;
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			boolean secondHas0Taint = adapter.secondHas0Taint();

			//Do it with LVs where it is less opcodes.
			//T VV T VV
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(DSTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			int tmp2 = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(DSTORE, tmp2);
			if (secondHas0Taint) {
				//0 T
				mv.visitInsn(SWAP);
				mv.visitInsn(POP);
			} else {
				//T T
				if (Configuration.MULTI_TAINTING) {
					if(Configuration.WITHOUT_PROPOGATION)
					{
						mv.visitInsn(POP2);
						mv.visitInsn(ACONST_NULL);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					} else {
						mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
								+ Configuration.TAINT_TAG_DESC, false);
					}
				} else {
					if (Configuration.DATAFLOW_TRACKING)
						mv.visitInsn(Opcodes.IOR);
					else
						mv.visitInsn(Opcodes.POP2);
				}
			}
			//T 
			mv.visitVarInsn(DLOAD, tmp2);
			mv.visitVarInsn(DLOAD, tmp);
			mv.visitInsn(opcode);
			lvs.freeTmpLV(tmp2);
			lvs.freeTmpLV(tmp);
			break;
		case Opcodes.LSHL:
		case Opcodes.LUSHR:
		case Opcodes.LSHR: {
			//T VV T V
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(ISTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			mv.visitVarInsn(ILOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV V
			mv.visitInsn(opcode);
			// T T VV
			mv.visitInsn(DUP2_X2);
			mv.visitInsn(POP2);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			// VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
		}
			break;
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LADD:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
			//T VV T VV
			tmp = lvs.getTmpLV();
			mv.visitVarInsn(LSTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			mv.visitVarInsn(LLOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV VV
			mv.visitInsn(opcode);
			// T T VV
			mv.visitInsn(DUP2_X2);
			mv.visitInsn(POP2);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			// VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			break;
		case Opcodes.INEG:
		case Opcodes.FNEG:
		case Opcodes.LNEG:
		case Opcodes.DNEG:
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
			mv.visitInsn(opcode);
			break;
		case Opcodes.LCMP: {
			//T VV T VV
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(LSTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			mv.visitVarInsn(LLOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV VV
			mv.visitInsn(opcode);
			// T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			// V T
			mv.visitInsn(SWAP);
		}
			break;
		case Opcodes.DCMPL: {
			//T VV T VV
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(DSTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			mv.visitVarInsn(DLOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV VV
			mv.visitInsn(opcode);
			// T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			// V T
			mv.visitInsn(SWAP);
		}
			break;
		case Opcodes.DCMPG: {
			//T VV T VV
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(DSTORE, tmp);
			//T VV T
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			//T T VV
			mv.visitVarInsn(DLOAD, tmp);
			lvs.freeTmpLV(tmp);
			// T T VV VV
			mv.visitInsn(opcode);
			// T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			// VV T
			mv.visitInsn(SWAP);
		}
			break;
		case Opcodes.FCMPL: {
			//T V T V
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(FSTORE, tmp);
			//T V T
			mv.visitInsn(SWAP);
			//T T V
			mv.visitVarInsn(FLOAD, tmp);
			lvs.freeTmpLV(tmp);
			//T T V V
			mv.visitInsn(opcode);
			//T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			mv.visitInsn(SWAP);
		}
			break;
		case Opcodes.FCMPG: {
			//T V T V
			tmp = lvs.getTmpLV();
			mv.visitInsn(TaintUtils.IS_TMP_STORE);
			mv.visitVarInsn(FSTORE, tmp);
			//T V T
			mv.visitInsn(SWAP);
			//T T V
			mv.visitVarInsn(FLOAD, tmp);
			lvs.freeTmpLV(tmp);
			//T T V V
			mv.visitInsn(opcode);
			//T T V
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			if (Configuration.MULTI_TAINTING) {
				if(Configuration.WITHOUT_PROPOGATION)
				{
					mv.visitInsn(POP2);
					mv.visitInsn(ACONST_NULL);
					mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
				} else {
					mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")"
							+ Configuration.TAINT_TAG_DESC, false);
				}
			} else {
				if (Configuration.DATAFLOW_TRACKING)
					mv.visitInsn(Opcodes.IOR);
				else
					mv.visitInsn(Opcodes.POP2);
			}
			mv.visitInsn(SWAP);
		}
			break;
		case Opcodes.ARRAYLENGTH:
			Type arrType = TaintAdapter.getTypeForStackType(adapter.analyzer.stack.get(adapter.analyzer.stack.size() - 1));
			{
				boolean loaded = false;
				if (arrType.getElementType().getSort() != Type.OBJECT) {
					//TA A
					mv.visitInsn(SWAP);
					loaded = true;
					if(Configuration.MULTI_TAINTING && Configuration.IMPLICIT_TRACKING)
					{
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					}
					else
					{
						mv.visitInsn(POP);
						mv.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
						if (Configuration.MULTI_TAINTING)
							mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					}
					mv.visitInsn(SWAP);
					//A
				}
				if (!loaded) {
					mv.visitInsn(DUP);
					if(Configuration.MULTI_TAINTING && Configuration.IMPLICIT_TRACKING)
					{
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					}
					else
					{
						mv.visitInsn(POP);
						mv.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
						if (Configuration.MULTI_TAINTING)
							mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					}
					mv.visitInsn(SWAP);
				}
				mv.visitInsn(opcode);

			}
			break;
		}
	}

	@Override
	public void jumpOp(int opcode, int branchStarting, Label label, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
		if (Configuration.IMPLICIT_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
			switch (opcode) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
				mv.visitInsn(SWAP);
				mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				mv.visitInsn(SWAP);
				mv.visitIntInsn(BIPUSH, branchStarting);
				mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(" + Configuration.TAINT_TAG_DESC + "I)V", false);
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				Type typeOnStack = ta.getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT && typeOnStack.getDimensions() == 1) {
					//O1 T1
					mv.visitInsn(SWAP);
					mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					mv.visitInsn(SWAP);
					mv.visitIntInsn(BIPUSH, branchStarting);
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(Ljava/lang/Object;I)V", false);
				} else {
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					mv.visitInsn(SWAP);
					mv.visitIntInsn(BIPUSH, branchStarting);
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(Ljava/lang/Object;I)V", false);
				}
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				//T V T V
				int tmp = lvs.getTmpLV(Type.INT_TYPE);
				//T V T V
				mv.visitInsn(SWAP);
				mv.visitInsn(TaintUtils.IS_TMP_STORE);
				mv.visitVarInsn(Configuration.TAINT_STORE_OPCODE, tmp);
				//T V V
				mv.visitInsn(DUP2_X1);
				mv.visitInsn(POP2);
				//V V T  
				mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				mv.visitInsn(SWAP);
				//V V C T
				mv.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, tmp);
				lvs.freeTmpLV(tmp);
				//V V T T
				mv.visitIntInsn(BIPUSH, branchStarting);
				mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "appendTag", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + "I)V", false);
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ACMPEQ:
				typeOnStack = ta.getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
					mv.visitInsn(SWAP);
					mv.visitInsn(POP);
				}
				//O1 O2 (t2?)
				Type secondOnStack = ta.getStackTypeAtOffset(1);
				if (secondOnStack.getSort() == Type.ARRAY && secondOnStack.getElementType().getSort() != Type.OBJECT) {
					//O1 O2 T2
					mv.visitInsn(DUP2_X1);
					mv.visitInsn(POP2);
					mv.visitInsn(POP);
				}
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.GOTO:
				mv.visitJumpInsn(opcode, label);
				break;
			default:
				throw new IllegalStateException("Unimplemented: " + opcode);
			}
		} else {
			switch (opcode) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
				//top is val, taint
				mv.visitInsn(SWAP);
				mv.visitInsn(POP);
				mv.visitJumpInsn(opcode, label);

				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				//top is val, taint, val, taint
				mv.visitInsn(SWAP);
				mv.visitInsn(POP);
				//val, val, taint
				mv.visitInsn(DUP2_X1);
				mv.visitInsn(POP2);
				mv.visitInsn(POP);
				mv.visitJumpInsn(opcode, label);

				break;
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:

				Type typeOnStack = ta.getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
					mv.visitInsn(SWAP);
					mv.visitInsn(POP);
				}
				//O1 O2 (t2?)
				Type secondOnStack = ta.getStackTypeAtOffset(1);
				if (secondOnStack.getSort() == Type.ARRAY && secondOnStack.getElementType().getSort() != Type.OBJECT) {
					//O1 O2 T2
					mv.visitInsn(DUP2_X1);
					mv.visitInsn(POP2);
					mv.visitInsn(POP);
				}
				mv.visitJumpInsn(opcode, label);

				break;
			case Opcodes.GOTO:
				//we don't care about goto
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:

				typeOnStack = ta.getTopOfStackType();
				if (typeOnStack.getSort() == Type.ARRAY && typeOnStack.getElementType().getSort() != Type.OBJECT) {
					//O1 T1
					mv.visitInsn(SWAP);
					mv.visitInsn(POP);
				}
				mv.visitJumpInsn(opcode, label);

				break;
			default:
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public void typeOp(int opcode, String type, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
		switch (opcode) {
		case Opcodes.ANEWARRAY:
			if (Configuration.ARRAY_LENGTH_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
				Type t = Type.getType(type);
				if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
					//e.g. [I for a 2 D array -> MultiDTaintedIntArray
					type = MultiDTaintedArray.getTypeForType(t).getInternalName();
				}
				//L TL
				mv.visitTypeInsn(opcode, type);
				//A TL
				mv.visitInsn(DUP_X1);
				//A TL A
				mv.visitInsn(SWAP);
				//TL A A
				mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
			} else {
				mv.visitInsn(SWAP);//We should just drop the taint for the size of the new array
				mv.visitInsn(POP);
				Type t = Type.getType(type);
				if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
					//e.g. [I for a 2 D array -> MultiDTaintedIntArray
					type = MultiDTaintedArray.getTypeForType(t).getInternalName();
				}
				mv.visitTypeInsn(opcode, type);
				
			}
			break;
		case Opcodes.NEW:
			mv.visitTypeInsn(opcode, type);
			break;
		case Opcodes.INSTANCEOF:
			Type t = Type.getType(type);

			boolean doIOR = false;
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				if (t.getDimensions() > 1) {
					type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
				} else if (!ta.topStackElCarriesTaints()) {
					doIOR = true;
					//Maybe we have it boxed on the stack, maybe we don't - how do we know? Who cares, just check both...
					mv.visitInsn(DUP);
					mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.getClassForComponentType(t.getElementType().getSort())));
					mv.visitInsn(SWAP);
				}
			}
			if (ta.ignoreLoadingNextTaint) {
				mv.visitTypeInsn(opcode, type);
				if (doIOR)
					mv.visitInsn(IOR);
				return;
			}
			{
				boolean loaded = false;
				if (ta.topStackElCarriesTaints()) {
					loaded = true;
					//TA A
					mv.visitInsn(SWAP);
					if (Configuration.MULTI_TAINTING) {
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					} else
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintInt", "(Ljava/lang/Object;)I", false);
					mv.visitInsn(SWAP);
				}
				if (!loaded) {
					mv.visitInsn(DUP);
				}
				mv.visitTypeInsn(opcode, type);
				if (!loaded) {
					//O I
					mv.visitInsn(SWAP);
					if (Configuration.MULTI_TAINTING) {
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
					} else
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintInt", "(Ljava/lang/Object;)I", false);
					mv.visitInsn(SWAP);
				}
				if (doIOR) {
					//I T I
					mv.visitInsn(SWAP);
					//I I T
					mv.visitInsn(DUP_X2);
					mv.visitInsn(POP);
					mv.visitInsn(IOR);
				}

			}
			break;
		case Opcodes.CHECKCAST:
			t = Type.getType(type);

			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				if (t.getDimensions() > 1) {
					//Hahaha you thought you could cast to a primitive multi dimensional array!

					mv.visitTypeInsn(opcode, MultiDTaintedArray.getTypeForType(Type.getType(type)).getDescriptor());
					return;
				} else {
					//what is on the top of the stack that we are checkcast'ing?
					Object o = ta.analyzer.stack.get(ta.analyzer.stack.size() - 1);
					if (o instanceof String) {
						Type zz = TaintAdapter.getTypeForStackType(o);
						if (zz.getSort() == Type.ARRAY && zz.getElementType().getSort() != Type.OBJECT) {
							mv.visitTypeInsn(opcode, type);
							break;
						}
					}
					//cast of Object[] or Object to char[] or int[] etc.
					if (o == Opcodes.NULL) {
						mv.visitInsn(ACONST_NULL);
						mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
						mv.visitInsn(SWAP);
					} else
						ta.retrieveTaintedArray(type);
					mv.visitTypeInsn(opcode, type);
				}
				//			} else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT) {
				//				Object o = analyzer.stack.get(analyzer.stack.size() - 1);
				//				if (o instanceof String) {
				//					Type zz = getTypeForStackType(o);
				//					if (zz.getSort() == Type.OBJECT) {
				//						//casting object to Object[]
				//						mv.visitTypeInsn(opcode, type);
				//						break;
				//					}
				//				}
				//				if (topStackElCarriesTaints()) {
				//					//Casting array to non-array type
				//
				//					//Register the taint array for later.
				//					registerTaintedArray(t.getDescriptor());
				//				}
			} else {

				//What if we are casting an array to Object?
				if (ta.topStackElCarriesTaints()) {
					//Casting array to non-array type

					//Register the taint array for later.
					ta.registerTaintedArray(ta.getTopOfStackType().getDescriptor());
				}
				mv.visitTypeInsn(opcode, type);
			}
			break;
		}
	}

	@Override
	public void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
		if (Configuration.IMPLICIT_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
			if (ta.isIgnoreAllInstrumenting || ta.isRawInsns) {
				mv.visitIincInsn(var, increment);
				return;
			}
			int shadowVar = 0;
			if (var < ta.lastArg && TaintUtils.getShadowTaintType(ta.paramTypes[var].getDescriptor()) != null) {
				//accessing an arg; remap it
				Type localType = ta.paramTypes[var];
				if (TaintUtils.getShadowTaintType(localType.getDescriptor()) != null)
					shadowVar = var - 1;
			} else {
				shadowVar = lvs.varToShadowVar.get(var);
			}
			mv.visitVarInsn(ALOAD, shadowVar);
			mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
			mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"
					+ Configuration.TAINT_TAG_DESC, false);
			mv.visitVarInsn(ASTORE, shadowVar);

		}
		mv.visitIincInsn(var, increment);
	}
	@Override
	public void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
	}

	@Override
	public boolean isIgnoredClass(String classname) {
		return false;
	}

}
