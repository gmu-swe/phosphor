package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DataAndControlFlowTagFactory implements TaintTagFactory, Opcodes {

	@Override
	public Taint<?> getAutoTaint(String source) {
		return new Taint(source);
	}
	@Override
	public void methodOp(int opcode, String owner, String name, String desc, boolean itfc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
	}
	@Override
	public void instrumentationStarting(int access, String methodName, String methodDesc) {
	}
	@Override
	public void insnIndexVisited(int offset) {
		
	}
	@Override
	public void lineNumberVisited(int line) {
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
					loaded = true;
//					if(Configuration.MULTI_TAINTING && (Configuration.IMPLICIT_TRACKING))
//					{
//						mv.visitInsn(SWAP);
//						mv.visitInsn(POP);
//						mv.visitInsn(DUP);
//						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)"+Configuration.TAINT_TAG_DESC, false);
//						mv.visitInsn(SWAP);
//					}
//					else
					{
						mv.visitInsn(SWAP);
						if(Configuration.ARRAY_LENGTH_TRACKING)
						{
							mv.visitMethodInsn(INVOKEVIRTUAL, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME, "getLengthTaint", "()"+Configuration.TAINT_TAG_DESC, false);
						} else {
							mv.visitInsn(POP);
							mv.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							if (Configuration.MULTI_TAINTING)
								mv.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
						}
						mv.visitInsn(SWAP);
					}
					//A
				}
				if (!loaded) {
					mv.visitInsn(DUP);
					if(Configuration.MULTI_TAINTING && (Configuration.IMPLICIT_TRACKING || Configuration.ARRAY_LENGTH_TRACKING))
					{
						mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)"+Configuration.TAINT_TAG_DESC, false);
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
		if ((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) && !Configuration.WITHOUT_PROPOGATION) {
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
				mv.visitVarInsn(ALOAD, ta.controlTaintArray);
				ta.callPushControlTaint(branchStarting);
				ta.doForceCtrlStores();
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				if (Configuration.IMPLICIT_TRACKING) {
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					mv.visitInsn(SWAP);
					mv.visitVarInsn(ALOAD, ta.controlTaintArray);
					ta.callPushControlTaintObj(branchStarting);
				}
//				mv.visitJumpInsn(opcode, label);
				ta.doForceCtrlStores();
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
				mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				mv.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, tmp);
				lvs.freeTmpLV(tmp);
				//V V C T CT
				mv.visitVarInsn(ALOAD, ta.controlTaintArray);
				ta.callPushControlTaint(branchStarting);
				mv.visitVarInsn(ALOAD, ta.controlTaintArray);
				ta.callPushControlTaint(branchStarting+1);
				ta.doForceCtrlStores();
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ACMPEQ:
				if (Configuration.IMPLICIT_TRACKING) {
					//OBJ OBJ
					mv.visitInsn(DUP2);
					mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					mv.visitInsn(SWAP);
					mv.visitVarInsn(ALOAD, ta.controlTaintArray);
					ta.callPushControlTaintObj(branchStarting);

					mv.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					mv.visitInsn(SWAP);
					mv.visitVarInsn(ALOAD, ta.controlTaintArray);
					ta.callPushControlTaintObj(branchStarting + 1);
				}
				if(!isIgnoreAcmp && Configuration.WITH_UNBOX_ACMPEQ && (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE))
				{
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					mv.visitInsn(SWAP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					mv.visitInsn(SWAP);
				}
				ta.doForceCtrlStores();
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
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
				if(!isIgnoreAcmp && Configuration.WITH_UNBOX_ACMPEQ && (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE))
				{
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					mv.visitInsn(SWAP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					mv.visitInsn(SWAP);
				}
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.GOTO:
				//we don't care about goto
				mv.visitJumpInsn(opcode, label);
				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
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
		case Opcodes.INSTANCEOF:

			break;
		case Opcodes.CHECKCAST:
			
			break;
		}
	}

	@Override
	public void iincOp(int var, int increment, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta) {
		if ((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) && !Configuration.WITHOUT_PROPOGATION) {
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
	}
	@Override
	public void fieldOp(int opcode, String owner, String name, String desc, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV ta, boolean loadIsTracked) {
	}

	@Override
	public boolean isIgnoredClass(String classname) {
		return false;
	}

	boolean isIgnoreAcmp;
	@Override
	public void instrumentationStarting(String className) {
		isIgnoreAcmp = className.equals("java/io/ObjectOutputStream$HandleTable");
	}

	@Override
	public void instrumentationEnding(String className) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isInternalTaintingClass(String classname) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void lookupSwitch(Label dflt, int[] keys, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV) {
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	@Override
	public void tableSwitch(int min, int max, Label dflt, Label[] labels, MethodVisitor mv, LocalVariableManager lvs, TaintPassingMV taintPassingMV) {
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	@Override
	public void propogateTagNative(String className, int acc, String methodName, String newDesc, MethodVisitor mv) {
		int idx = 0;
		Type[] argTypes = Type.getArgumentTypes(newDesc);
		if ((acc & Opcodes.ACC_STATIC) == 0) {
			idx++;
		}
		for (Type t : argTypes) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
					idx++;
				}
			} else if (t.getSort() != Type.OBJECT) {
				mv.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, idx);
				if (Configuration.MULTI_TAINTING) {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + Configuration.TAINT_TAG_DESC + ")" + Configuration.TAINT_TAG_DESC, false);
				} else {
					mv.visitInsn(Opcodes.IOR);
				}
				idx++;
			}
			idx += t.getSize();
		}
	}

	@Override
	public void generateSetTag(MethodVisitor mv, String className) {
		if(Configuration.MULTI_TAINTING) {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitTypeInsn(Opcodes.CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
			mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
		}
		else
		{
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ILOAD, 1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
		}
	}
}
