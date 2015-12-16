package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.Arrays;
import java.util.LinkedList;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class PartialInstrumentationMV extends TaintAdapter implements Opcodes {

	private NeverNullArgAnalyzerAdapter analyzer;

	public PartialInstrumentationMV(String className, int access, String methodName, String desc, MethodVisitor mv, NeverNullArgAnalyzerAdapter an) {
		super(access, className, methodName, desc, null, null, mv, an);
		this.analyzer = an;
	}

	LocalVariableManager lvs;

	public void setLvs(LocalVariableManager lvs) {
		this.lvs = lvs;
	}

	public boolean doTaint;

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (doTaint) {
			String taintDesc = Configuration.TAINT_TAG_DESC;
			if (desc.startsWith("["))
				taintDesc = Configuration.TAINT_TAG_ARRAYDESC;
			switch (opcode) {
			case Opcodes.GETFIELD:
				super.visitInsn(DUP);
				super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, taintDesc);
				super.visitInsn(SWAP);
				super.visitFieldInsn(opcode, owner, name, desc);
				break;
			case Opcodes.GETSTATIC:
				super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, taintDesc);
				super.visitFieldInsn(opcode, owner, name, desc);
				break;
			default:
				throw new IllegalArgumentException();
			}
			doTaint = false;
		} else
			super.visitFieldInsn(opcode, owner, name, desc);

	}

	void generateTaintArray() {
		FrameNode fn = getCurrentFrameNode();
		analyzer.visitInsn(DUP);
		Label ok = new Label();
		Label isnull = new Label();
		analyzer.visitJumpInsn(IFNULL, isnull);
		analyzer.visitInsn(DUP);

		analyzer.visitInsn(Opcodes.ARRAYLENGTH);
		if (Configuration.MULTI_TAINTING)
			analyzer.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
		else
			analyzer.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
		analyzer.visitJumpInsn(Opcodes.GOTO, ok);
		analyzer.visitLabel(isnull);
		fn.accept(analyzer);
		analyzer.visitInsn(Opcodes.ACONST_NULL);
		analyzer.visitLabel(ok);
		fn.stack = new LinkedList(fn.stack);
		fn.stack.add(Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
		fn.accept(analyzer);
		analyzer.visitInsn(SWAP);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (doTaint) {
			System.out.println("Dotaint on  var");
			Type shadowType = Type.getType(Configuration.TAINT_TAG_DESC);
			if (opcode == Opcodes.ALOAD || opcode == Opcodes.ASTORE)
				shadowType = Type.getType(Configuration.TAINT_TAG_ARRAYDESC);
			if (!lvs.varToShadowVar.containsKey(var))
			{
				lvs.varToShadowVar.put(var, lvs.newShadowLV(shadowType, var));
//				analyzer.locals.add(shadowType.getInternalName());
			}
			int shadow = lvs.varToShadowVar.get(var);
			switch (opcode) {
			case ALOAD:
				System.out.println("ALOAD " + var + " lastarg " + lvs.lastArg );
				if (var <= lvs.lastArg) {
					if (Configuration.SINGLE_TAG_PER_ARRAY) {
						analyzer.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
						analyzer.visitVarInsn(Opcodes.ALOAD, var);
					} else {
						analyzer.visitVarInsn(Opcodes.ALOAD, var);
						generateTaintArray();
					}
				} else {
					super.visitVarInsn(Opcodes.ALOAD, shadow);
					super.visitVarInsn(Opcodes.ALOAD, var);
				}
				break;
			case ILOAD:
			case FLOAD:
			case LLOAD:
			case DLOAD:
				super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, shadow);
				super.visitVarInsn(Opcodes.ALOAD, var);
				break;
			case ASTORE:
				super.visitVarInsn(Opcodes.ASTORE, var);
				super.visitVarInsn(Opcodes.ASTORE, shadow);
				break;
			case ISTORE:
			case FSTORE:
			case LSTORE:
			case DSTORE:
				super.visitVarInsn(opcode, var);
				super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, shadow);
				break;
			default:
				throw new IllegalArgumentException();
			}
			doTaint = false;
		} else {
			super.visitVarInsn(opcode, var);
		}
	}
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		System.out.println("VF " + Arrays.toString(local));
		super.visitFrame(type, nLocal, local, nStack, stack);
	}

	public void retrieveTaintedArray(String type) {
		//A
		Label isNull = new Label();
		Label isDone = new Label();

		FrameNode fn = getCurrentFrameNode();

		analyzer.visitInsn(DUP);
		analyzer.visitJumpInsn(IFNULL, isNull);

		//		System.out.println("unbox: " + onStack + " type passed is " + type);

		Class boxType = MultiDTaintedArray.getClassForComponentType(Type.getType(type).getElementType().getSort());
		analyzer.visitTypeInsn(CHECKCAST, Type.getInternalName(boxType));

		analyzer.visitInsn(DUP);

		Type arrayDesc = Type.getType(type);
		//		System.out.println("Get tainted array from " + arrayDesc);
		//A A
		if (Configuration.SINGLE_TAG_PER_ARRAY) {
			if (!Configuration.MULTI_TAINTING)
				analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "I");
			else {
				analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "Ljava/lang/Object;");
				analyzer.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
			}
		} else {
			if (!Configuration.MULTI_TAINTING)
				analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "[I");
			else {
				if (Configuration.SINGLE_TAG_PER_ARRAY)
					analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "Ljava/lang/Object;");
				else
					analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "taint", "[Ljava/lang/Object;");
				analyzer.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
			}
		}
		//A TA
		analyzer.visitInsn(SWAP);
		analyzer.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "val", type);
		FrameNode fn2 = getCurrentFrameNode();
		analyzer.visitJumpInsn(GOTO, isDone);
		analyzer.visitLabel(isNull);
		acceptFn(fn, analyzer);
		analyzer.visitInsn(ACONST_NULL);
		if (arrayDesc.getElementType().getSort() == Type.OBJECT)
			analyzer.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
		else
			analyzer.visitTypeInsn(CHECKCAST, Type.getType(TaintUtils.getShadowTaintType(arrayDesc.getDescriptor())).getInternalName());
		analyzer.visitInsn(SWAP);
		analyzer.visitTypeInsn(CHECKCAST, type);
		analyzer.visitLabel(isDone);
		acceptFn(fn2, analyzer);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (doTaint) {
			switch (opcode) {
			case Opcodes.CHECKCAST:
				Type t = Type.getType(type);
				if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT) {
					if (getTopOfStackObject() == Opcodes.NULL) {
						analyzer.visitTypeInsn(opcode, type);
						analyzer.visitInsn(Opcodes.ACONST_NULL);
						analyzer.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_ARRAY_INTERNAL_NAME);
						analyzer.visitInsn(SWAP);
					} else {
						retrieveTaintedArray(type);
					}
				} else
					super.visitTypeInsn(opcode, type);
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		super.visitTypeInsn(opcode, type);
		doTaint = false;
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (doTaint) {
			switch (opcode) {
			case Opcodes.NEWARRAY:
				super.visitInsn(DUP);
				if (Configuration.MULTI_TAINTING)
					super.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
				else
					super.visitIntInsn(NEWARRAY, Opcodes.T_INT);
				super.visitInsn(SWAP);
				super.visitIntInsn(opcode, operand);
				break;
			default:
				throw new IllegalArgumentException();
			}
		} else
			super.visitIntInsn(opcode, operand);
		doTaint = false;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		//		System.out.println("Visit method: " + name+desc);
		//		System.out.println(analyzer.stack);
		//		System.out.println(analyzer.stackTaintedVector);

		if (doTaint) {
			super.visitInsn(TaintUtils.DONT_LOAD_TAINT);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			super.visitInsn(TaintUtils.DONT_LOAD_TAINT);
			Type origReturnType = Type.getReturnType(desc);
			Type newReturnType = TaintUtils.getContainerReturnType(origReturnType);
			if (Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc)) {
//				System.out.println("Dotaint uninst call " + owner + name + desc);
				generateTaintArray();
			} else {
//				System.out.println("Dotaint inst call " + owner + name + desc);
				if (origReturnType.getSort() == Type.ARRAY && origReturnType.getDimensions() == 1 && origReturnType.getElementType().getSort() != Type.OBJECT) {
					//unbox array
					super.visitInsn(DUP);
					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_ARRAYDESC);
					super.visitInsn(SWAP);
					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				} else if (origReturnType.getSort() != Type.ARRAY && origReturnType.getSort() != Type.OBJECT && origReturnType.getSort() != Type.VOID) {
					//unbox prim
					super.visitInsn(DUP);
					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
					super.visitInsn(SWAP);

					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				}
			}
			doTaint = false;
			System.out.println("do taint method");
		} else
			super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case TaintUtils.NEXT_INSN_TAINT_AWARE:
			System.out.println("next tint awayre");
			doTaint = true;
			break;
		case Opcodes.ACONST_NULL:
			if (doTaint) {
				super.visitInsn(ACONST_NULL);
				super.visitInsn(ACONST_NULL);
				doTaint = false;
				return;
			} else
				break;
		case Opcodes.AALOAD:
			if (doTaint) {
				//				System.out.println("Tainted aaload");
				Type t = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
				System.out.println(t);
				analyzer.visitInsn(opcode);
				try {
					retrieveTaintedArray("[" + (MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", ".")))));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				;
				doTaint = false;
				return;
			} else
				break;
		default:
			doTaint = false;
		}
		super.visitInsn(opcode);
	}
}
