package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.MethodDescriptor;
import edu.columbia.cs.psl.phosphor.SelectiveInstrumentationManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class UninstrumentedCompatMV extends TaintAdapter {
	private NeverNullArgAnalyzerAdapter analyzer;
	private boolean skipFrames;
	int preallocArg = 0;
	private MethodVisitor uninst;
	private String desc;
	private String name;

	public UninstrumentedCompatMV(int access, String className, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer,
			boolean skipFrames, MethodVisitor uninst) {
		super(access, className, name, desc, signature, exceptions, mv, analyzer);
		this.uninst = uninst;
		this.desc = desc;
		this.analyzer = analyzer;
		this.name = name;
		this.skipFrames = skipFrames;
		if ((access & Opcodes.ACC_STATIC) == 0) {
			preallocArg++;
		}
		for (Type t : Type.getArgumentTypes(desc))
			preallocArg += t.getSize();
		if (name.equals("<init>"))
			preallocArg++;
	}

	@Override
	public void visitCode() {
		super.visitCode();
		//		analyzer.locals.add("[Ljava/lang/Object;");
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		Object[] newLocal = new Object[local.length];
		Object[] newStack = new Object[stack.length];
		for (int i = 0; i < local.length; i++) {
			if (local[i] instanceof TaggedValue && ((TaggedValue) local[i]).v instanceof String) {
				Type t = Type.getObjectType((String) ((TaggedValue) local[i]).v);
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1)
					newLocal[i] = new TaggedValue(MultiDTaintedArray.getTypeForType(t).getInternalName());
				else
					newLocal[i] = local[i];
			} else if (local[i] instanceof String) {
				Type t = Type.getObjectType(((String) local[i]));
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1)
					newLocal[i] = MultiDTaintedArray.getTypeForType(t).getInternalName();
				else
					newLocal[i] = local[i];
			} else
				newLocal[i] = local[i];
		}
		for (int i = 0; i < stack.length; i++) {
			if (stack[i] instanceof TaggedValue && ((TaggedValue) stack[i]).v instanceof String) {
				Type t = Type.getObjectType((String) ((TaggedValue) stack[i]).v);
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1)
					newStack[i] = new TaggedValue(MultiDTaintedArray.getTypeForType(t).getInternalName());
				else
					newStack[i] = stack[i];
			} else if (stack[i] instanceof String) {
				Type t = Type.getObjectType(((String) stack[i]));
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1)
					newStack[i] = MultiDTaintedArray.getTypeForType(t).getInternalName();
				else
					newStack[i] = stack[i];
			} else
				newStack[i] = stack[i];
		}
		super.visitFrame(type, nLocal, newLocal, nStack, newStack);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Type t = Type.getType(desc);
		if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT)
			desc = MultiDTaintedArray.getTypeForType(t).getDescriptor();
		switch (opcode) {
		case Opcodes.GETFIELD:
		case Opcodes.GETSTATIC:
			super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.PUTFIELD:
		case Opcodes.PUTSTATIC:
			Type onStack = getTopOfStackType();
			//			System.out.println("PF " + onStack + t);
			//			System.out.println(analyzer.stack);
			if (TaintUtils.isPrimitiveArrayType(t)) {
				//1d prim array - need to make sure that there is some taint here
				if (!TaintUtils.LAZY_TAINT_ARRAY_INIT && !Configuration.SINGLE_TAG_PER_ARRAY && !name.endsWith(TaintUtils.TAINT_FIELD)) {
					if (opcode == PUTFIELD) {
						super.visitInsn(DUP2_X1);
						super.visitInsn(POP2);
						super.visitInsn(DUP_X2);
						super.visitInsn(SWAP);
						super.visitFieldInsn(opcode, owner, name, desc);
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
					} else {
						super.visitFieldInsn(opcode, owner, name, desc);
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
					}
				} else
				super.visitFieldInsn(opcode, owner, name, desc);
			} else if (TaintUtils.isPrimitiveArrayType(onStack) && desc.equals("Ljava/lang/Object;")) {
				//need to box
				//				Type newT = Type.getType(MultiDTaintedArray.getClassForComponentType(onStack.getElementType().getSort()));
				//				super.visitTypeInsn(Opcodes.NEW, newT.getInternalName());
				//				super.visitInsn(DUP_X2);
				//				super.visitInsn(DUP_X2);
				//				super.visitInsn(POP);
				//				if (!Configuration.MULTI_TAINTING)
				//					super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([I" + onStack.getDescriptor() + ")V", false);
				//				else
				//					super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([Ljava/lang/Object;" + onStack.getDescriptor() + ")V", false);
				registerTaintedArray(onStack.getDescriptor());
				super.visitFieldInsn(opcode, owner, name, desc);
			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		}
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		Type arrayType = Type.getType(desc);
		Type origType = arrayType;
		boolean needToHackDims = false;
		int tmp = 0;
		if (arrayType.getElementType().getSort() != Type.OBJECT) {
			if (dims == arrayType.getDimensions()) {
				needToHackDims = true;
				dims--;
				tmp = lvs.getTmpLV(Type.INT_TYPE);
				super.visitVarInsn(Opcodes.ISTORE, tmp);
			}
			arrayType = MultiDTaintedArray.getTypeForType(arrayType);
			//Type.getType(MultiDTaintedArray.getClassForComponentType(arrayType.getElementType().getSort()));
			desc = arrayType.getInternalName();
		}
		if (dims == 1) {
			//It's possible that we dropped down to a 1D object type array
			super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
		} else
			super.visitMultiANewArrayInsn(desc, dims);
		if (needToHackDims) {
			super.visitInsn(DUP);
			super.visitVarInsn(ILOAD, tmp);
			lvs.freeTmpLV(tmp);
			super.visitIntInsn(BIPUSH, origType.getElementType().getSort());
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)), "initLastDim",
					"([Ljava/lang/Object;II)V", false);

		}
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.CHECKCAST) {
			if (TaintUtils.isPrimitiveArrayType(Type.getObjectType(type)) && getTopOfStackType().getInternalName().equals(type))
				return;
			if (analyzer.stack.size() > 0 && "java/lang/Object".equals(analyzer.stack.get(analyzer.stack.size() - 1)) && type.startsWith("[") && type.length() == 2) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			}
			if (TaintUtils.isPrimitiveArrayType(getTopOfStackType()) && type.equals("java/lang/Object"))
				ensureBoxedAt(0, getTopOfStackType());
			Type t = Type.getType(type);
			if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
				type = MultiDTaintedArray.getTypeForType(t).getInternalName();
			}
		} else if (opcode == Opcodes.ANEWARRAY) {
			Type t = Type.getType(type);
			if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
				//e.g. [I for a 2 D array -> MultiDTaintedIntArray
				type = MultiDTaintedArray.getTypeForType(t).getInternalName();
			}
		} else if (opcode == Opcodes.INSTANCEOF) {
			Type t = Type.getType(type);
			if (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT)
				type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
			else if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT && getTopOfStackObject().equals("java/lang/Object")) {
				type = MultiDTaintedArray.getTypeForType(t).getInternalName();
			}
		}
		super.visitTypeInsn(opcode, type);
	}

	boolean dontUnbox = false;

	boolean forceTaintedCall = false;
	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case TaintUtils.DONT_LOAD_TAINT:
			dontUnbox = !dontUnbox;
			break;
		case TaintUtils.NEXTLOAD_IS_TAINTED:
			forceTaintedCall = true;
			return;
		case Opcodes.ARETURN:
			Type onTop = getTopOfStackType();
			Type retType = Type.getReturnType(this.desc);
			if (retType.getDescriptor().equals("Ljava/lang/Object;") && onTop.getSort() == Type.ARRAY && onTop.getDimensions() == 1 && onTop.getElementType().getSort() != Type.OBJECT) {
				registerTaintedArray(onTop.getDescriptor());
			}
			super.visitInsn(opcode);
			break;
		case Opcodes.AASTORE:
			Type arT = getTopOfStackType();
			//			System.out.println("... AASTORE " + analyzer.stack + analyzer.stackTaintedVector);
			if (TaintUtils.isPrimitiveArrayType(arT)) {

				//				Type newT = Type.getType(MultiDTaintedArray.getClassForComponentType(arT.getElementType().getSort()));
				//				super.visitTypeInsn(Opcodes.NEW, newT.getInternalName());
				//				super.visitInsn(DUP_X2);
				//				super.visitInsn(DUP_X2);
				//				super.visitInsn(POP);
				//				if (!Configuration.MULTI_TAINTING)
				//					super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([I" + arT.getDescriptor() + ")V", false);
				//				else
				//					super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([Ljava/lang/Object;" + arT.getDescriptor() + ")V", false);
				registerTaintedArray(arT.getDescriptor());
			}
			//			else if (arType instanceof String &&
			//					((String) arType).contains("[Ledu/columbia/cs/psl/phosphor/struct/multid/MultiDTainted")) {
			//				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			//			}
			super.visitInsn(opcode);
			break;
		case Opcodes.AALOAD:
			Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
			Type t = getTypeForStackType(arrayType);
			//			System.out.println(this.name+"AALOAD of " + t);
			if (t.getDimensions() == 1 && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/multid/MultiDTainted")) {
				//				System.out.println("it's a multi array in disguise!!!");
				super.visitInsn(opcode);
				try {
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					super.visitTypeInsn(Opcodes.CHECKCAST, "[" + MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", "."))));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//				System.out.println("AALOAD " + analyzer.stack);

			} else
				super.visitInsn(opcode);
			break;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			//			if (getTopOfStackObject().equals("java/lang/Object")) {
			//				//never allow monitor to occur on a multid type
			//				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			//			}
			super.visitInsn(opcode);
			break;
		case Opcodes.ARRAYLENGTH:
			Type onStack = getTopOfStackType();
			if (onStack.getSort() == Type.OBJECT) {
				Type underlying = MultiDTaintedArray.getPrimitiveTypeForWrapper(onStack.getInternalName());
				if (underlying != null) {
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					super.visitTypeInsn(Opcodes.CHECKCAST, "[" + underlying.getDescriptor());
				}
			}
			super.visitInsn(opcode);

			break;
		default:
			super.visitInsn(opcode);
			break;
		}
	}

	void ensureBoxedAt(int n, Type t) {
		Type newT = Type.getType(MultiDTaintedArray.getClassForComponentType(t.getElementType().getSort()));
		switch (n) {
		case 0:
			//			super.visitTypeInsn(Opcodes.NEW, newT.getInternalName());
			//			super.visitInsn(DUP_X2);
			//			super.visitInsn(DUP_X2);
			//			super.visitInsn(POP);
			//			if (!Configuration.MULTI_TAINTING)
			//				super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([I" + t.getDescriptor() + ")V", false);
			//			else
			//				super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([Ljava/lang/Object;" + t.getDescriptor() + ")V", false);
			registerTaintedArray(t.getDescriptor());
			break;
		default:
			LocalVariableNode[] d = storeToLocals(n);

			//			super.visitTypeInsn(Opcodes.NEW, newT.getInternalName());
			//			super.visitInsn(DUP_X2);
			//			super.visitInsn(DUP_X2);
			//			super.visitInsn(POP);
			//			if (!Configuration.MULTI_TAINTING)
			//				super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([I" + t.getDescriptor() + ")V", false);
			//			else
			//				super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([Ljava/lang/Object;" + t.getDescriptor() + ")V", false);

			registerTaintedArray(t.getDescriptor());
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
	}

	HashSet<Integer> boxAtNextJump = new HashSet<Integer>();

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (opcode == TaintUtils.ALWAYS_BOX_JUMP) {
			boxAtNextJump.add(var);
			return;
		}

		if (opcode == TaintUtils.ALWAYS_AUTOBOX && analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
			Type t = Type.getType((String) analyzer.locals.get(var));
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
				//				System.out.println("Restoring " + var + " to be boxed");
				super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));

				super.visitVarInsn(ALOAD, var);
				registerTaintedArray(getTopOfStackType().getDescriptor());
				super.visitVarInsn(ASTORE, var);
			}
			return;
		}
		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (Configuration.WITH_UNBOX_ACMPEQ && (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE)) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitInsn(SWAP);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitInsn(SWAP);
		}

		if (boxAtNextJump.size() > 0 && opcode != Opcodes.GOTO) {
			Label origDest = label;
			Label newDest = new Label();
			Label origFalseLoc = new Label();

			super.visitJumpInsn(opcode, newDest);
			FrameNode fn = getCurrentFrameNode();

			super.visitJumpInsn(GOTO, origFalseLoc);
			//			System.out.println("taint passing mv monkeying with jump");
			super.visitLabel(newDest);
			fn.accept(this);
			for (Integer var : boxAtNextJump) {
				if (lvs.varToShadowVar.containsKey(var)) {
					if (analyzer.locals.get(var) != Opcodes.NULL) {
						String arT = (String) analyzer.locals.get(var);
						Type newT = Type.getType(MultiDTaintedArray.getClassForComponentType(Type.getObjectType(arT).getElementType().getSort()));
						//					super.visitTypeInsn(Opcodes.NEW, newT.getInternalName());
						//					super.visitInsn(DUP);
						super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
						super.visitVarInsn(ALOAD, var);
						registerTaintedArray(arT);
						//					if (!Configuration.MULTI_TAINTING)
						//						super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([I" + arT + ")V", false);
						//					else
						//						super.visitMethodInsn(INVOKESPECIAL, newT.getInternalName(), "<init>", "([Ljava/lang/Object;" + arT + ")V", false);

						super.visitVarInsn(ASTORE, var);
					}
				}
			}
			super.visitJumpInsn(GOTO, origDest);
			super.visitLabel(origFalseLoc);
			fn.accept(this);
			boxAtNextJump.clear();
		} else
			super.visitJumpInsn(opcode, label);
	}

	public void registerTaintedArray(String descOfDest) {
		Type onStack = Type.getType(descOfDest);//getTopOfStackType();
		//TA A
		Type wrapperType = Type.getType(MultiDTaintedArray.getClassForComponentType(onStack.getElementType().getSort()));
		//				System.out.println("zzzNEW " + wrapperType);
		Label isNull = new Label();
		FrameNode fn = getCurrentFrameNode();
		//		System.out.println("Pre register " + analyzer.stack);
		//		if (!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
		//			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitInsn(DUP);
		super.visitJumpInsn(IFNULL, isNull);
		//		if (!ignoreLoadingNextTaint&& !isIgnoreAllInstrumenting)
		//			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitTypeInsn(NEW, wrapperType.getInternalName());
		//TA A N
		super.visitInsn(DUP_X2);
		super.visitInsn(DUP_X2);
		super.visitInsn(POP);
		//N N TA A
		super.visitMethodInsn(INVOKESPECIAL, wrapperType.getInternalName(), "<init>", "("+Configuration.TAINT_TAG_ARRAYDESC + onStack.getDescriptor() + ")V", false);
		Label isDone = new Label();
		FrameNode fn2 = getCurrentFrameNode();

		super.visitJumpInsn(GOTO, isDone);
		super.visitLabel(isNull);
		acceptFn(fn);
		super.visitInsn(SWAP);
		super.visitInsn(POP);
		super.visitLabel(isDone);
		fn2.stack.set(fn2.stack.size() - 1, "java/lang/Object");
		acceptFn(fn2);
		//		System.out.println("post register " + analyzer.stack);
	}

	//A

	public void visitIntInsn(int opcode, int operand) {
		super.visitIntInsn(opcode, operand);
	};

	void popAt(int n) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP)
				super.visitInsn(POP2);
			else
				super.visitInsn(POP);
			break;
		case 1:
			top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				Object second = analyzer.stack.get(analyzer.stack.size() - 3);
				//				System.out.println("Second is " + second);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					super.visitInsn(POP2);
				} else {
					//V VV
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				}
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					super.visitInsn(POP2);
				} else {
					//V V
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n);

			//			System.out.println("POST load the top " + n + ":" + analyzer.stack);

			super.visitInsn(POP);
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		boolean isCallToPrimitiveArrayClone = opcode == INVOKEVIRTUAL && desc.equals("()Ljava/lang/Object;") && name.equals("clone") && isPrimitiveType(getTopOfStackType());
		if (isCallToPrimitiveArrayClone) {
			//			System.out.println("Pre clone " + analyzer.stack);
			Type origType = getTopOfStackType();
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			super.visitTypeInsn(Opcodes.CHECKCAST, origType.getInternalName());
			generateEmptyTaintArray(origType.getInternalName());
			registerTaintedArray(origType.getDescriptor());
			//			System.out.println("Post clone " + analyzer.stack);
			return;
		}
		boolean isIgnoredMethodCall = Configuration.WITH_SELECTIVE_INST && !forceTaintedCall && !owner.startsWith("[") && Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc, true);
		forceTaintedCall = false;
//				System.out.println(isIgnoredMethodCall + "\tCall : <" + owner + ">." + name + desc);
//				System.out.println(analyzer.stack);
//				System.out.println(analyzer.stackTaintedVector);

		int stackItemsToCallee = 0;
		int nArgsActaul = 0;
		Type[] args = Type.getArgumentTypes(desc);
		Type[] argsInReverse = new Type[args.length];

		for (int i = 0; i < args.length; i++) {
			argsInReverse[args.length - i - 1] = args[i];
			stackItemsToCallee += args[i].getSize();
			nArgsActaul++;
			if (//!isIgnoredMethodCall && 
					TaintUtils.isPrimitiveArrayType(args[i])) {
				nArgsActaul++;
				stackItemsToCallee++;
			}
		}
		if (Instrumenter.isIgnoredClass(owner)) {// && !((name.equals("equals") || name.equals("clone")) && owner.equals("java/lang/Object"))) {
			//Still need to make sure boxed...
			int n=1;
			int i=1;
			for (Type t : argsInReverse) {
				if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
					i++;

				//			if(ignoreNext)
				//				System.out.println("ignore next i");
				Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
				//				System.out.println(name+ " ONStack: " + onStack + " and t = " + t);
				//System.out.println(ignoreNext + " and "+ analyzer.stack.get(analyzer.stack.size() - i) );
				if (t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(onStack) && analyzer.stackTaintedVector.get(analyzer.stack.size()-i)) {
					//There is an extra taint on the stack at this position
					if (TaintUtils.DEBUG_CALLS)
						System.err.println("removing taint array in call at " + n);
					ensureBoxedAt(n - 1, onStack);
				}
				n++;
				i++;
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}
		//Stupid workaround for eclipse benchmark
		if (name.equals("getProperty") && className.equals("org/eclipse/jdt/core/tests/util/Util")) {
			owner = Type.getInternalName(ReflectionMasker.class);
			name = "getPropertyHideBootClasspath";
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}
		Type ownerType = Type.getObjectType(owner);
		if (opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
			//			System.out.println("got to change the owner on primitive array call from " +owner+" to " + MultiDTaintedArray.getTypeForType(ownerType));
			owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
		}
		Type origReturnType = Type.getReturnType(desc);

		boolean isCalledOnArrayType = false;
		if ((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
				&& !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
			owner = Type.getInternalName(TaintUtils.class);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}
		if (Instrumenter.isIgnoredMethod(owner, name, desc)) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}

	
		int c = 0;
		for (int i = 0; i < args.length; i++) {
			if (argsInReverse[i].getSize() == 2)
				c++;
			Type onStack = getStackTypeAtOffset(c);
			Type a = argsInReverse[i];
			if (a.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(onStack)) {
				c++;
				nArgsActaul++;
				stackItemsToCallee++;
			} else if (TaintUtils.isPrimitiveArrayType(argsInReverse[i]))
				c++;
			c++;
		}
		if ((name.equals("equals") || name.equals("clone")) && owner.equals("java/lang/Object")) {
			if (TaintUtils.isPrimitiveArrayType(getTopOfStackType()))
				stackItemsToCallee--;
		}
		if ((opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) && analyzer.stack.size() > 0) {
//						System.out.println(name + desc + analyzer.stack);

			Object calledOn = analyzer.stack.get(analyzer.stack.size() - stackItemsToCallee - 1);
			if (calledOn instanceof String && ((String) calledOn).startsWith("[")) {
				//				System.out.println("Called on arraystack");
				isCalledOnArrayType = true;
			}
		}

		if (!isCalledOnArrayType && isIgnoredMethodCall) {
			args = Type.getArgumentTypes(TaintUtils.remapMethodDescForUninst(desc));
			argsInReverse = new Type[args.length];

			for (int i = 0; i < args.length; i++) {
				argsInReverse[args.length - i - 1] = args[i];
				stackItemsToCallee += args[i].getSize();
				nArgsActaul++;
				if (//!isIgnoredMethodCall && 
						TaintUtils.isPrimitiveArrayType(args[i])) {
					nArgsActaul++;
					stackItemsToCallee++;
				}
			}
			//			System.out.println("Load prealloc arg "  + preallocArg +" -- "+ analyzer.locals.get(preallocArg)+" , now " +analyzer.stack);
			boolean ignoreNext = false;

			int argsSize = 0;
			for (int i = 0; i < args.length; i++) {
				argsSize += args[i].getSize();
			}
			int i = 1;
			int n = 1;
//									System.out.println("12dw23 Calling "+owner+"."+name+desc + "with " + analyzer.stack);
			for (Type t : argsInReverse) {
				if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
					i++;

				//			if(ignoreNext)
				//				System.out.println("ignore next i");
				Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
//								System.out.println(name+ " ONStack: " + onStack + " and t = " + t);
				//System.out.println(ignoreNext + " and "+ analyzer.stack.get(analyzer.stack.size() - i) );
				if (t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(onStack)) {
					//There is an extra taint on the stack at this position
					if (TaintUtils.DEBUG_CALLS)
						System.err.println("removing taint array in call at " + n);
					ensureBoxedAt(n - 1, onStack);
				}
				n++;
				i++;
			}
			if (name.equals("<init>")) {
				super.visitInsn(Opcodes.ACONST_NULL);
				desc = desc.substring(0, desc.indexOf(')')) + Type.getDescriptor(UninstrumentedTaintSentinel.class) + ")" + desc.substring(desc.indexOf(')') + 1);
			} else
				name += TaintUtils.METHOD_SUFFIX_UNINST;
			desc = TaintUtils.remapMethodDescForUninst(desc);

			if (TaintUtils.PREALLOC_RETURN_ARRAY)
				analyzer.visitVarInsn(Opcodes.ALOAD, preallocArg);
//			System.out.println("Last fchance ignored " + name+desc+analyzer.stack);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		} else if (!Instrumenter.isIgnoredClass(owner) && !owner.startsWith("[") && !isCalledOnArrayType) {
			//call into the instrumented version			
			boolean hasChangedDesc = false;

			if (desc.equals(TaintUtils.remapMethodDesc(desc)) && !TaintUtils.PREALLOC_RETURN_ARRAY) {
				//Calling an instrumented method possibly!
//								System.out.println("Calling: " + name+desc + " with " + analyzer.stack);
				//Only thing we need to do here is make sure that prim arrays were boxed if the dest type is object.

				int i = 1;
				int n = 1;
				for (Type t : argsInReverse) {
					if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
						i++;

					//			if(ignoreNext)
					//				System.out.println("ignore next i");
					Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
					//System.out.println(name+ " ONStack: " + onStack + " and t = " + t);
					//System.out.println(ignoreNext + " and "+ analyzer.stack.get(analyzer.stack.size() - i) );
					if (t.getDescriptor().equals("Ljava/lang/Object;") && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
						//There is an extra taint on the stack at this position
						if (TaintUtils.DEBUG_CALLS)
							System.err.println("removing taint array in call at " + n);
						ensureBoxedAt(n - 1, onStack);
					}
					n++;
					i++;
				}
			} else {
				hasChangedDesc = true;
				String newDesc = TaintUtils.remapMethodDesc(desc);

//								System.out.println("Have to dael with" + newDesc);
//								System.out.println(analyzer.stack);
//								System.out.println(analyzer.stackTaintedVector);
				int argStorageIdx = nArgsActaul - 1;
				int howFarBackToStoreInTermsOfArgNumbers = -1;
				boolean hasDoubleWides = false;
				int nArgsToStoreIncludingTaints = 0;
				int runningOffset = 0;
				//Do we actually need to insert any null taints? (or box)
				for (int i = 0; i < argsInReverse.length; i++) {
					runningOffset += argsInReverse[i].getSize();
//					System.out.println("Check " + argsInReverse[i] + " - " + getStackTypeAtOffset(runningOffset-1));
					if (TaintUtils.isPrimitiveType(argsInReverse[i])) {
						howFarBackToStoreInTermsOfArgNumbers = i;
					}
					else if(TaintUtils.isPrimitiveArrayType(argsInReverse[i]))
					{
						runningOffset++;
					}
					else if(argsInReverse[i].getSort() == Type.OBJECT && TaintUtils.isPrimitiveArrayType(getStackTypeAtOffset(runningOffset-1)))
					{
//						System.out.println("Found box cand at " + i);
						hasDoubleWides=true;
						howFarBackToStoreInTermsOfArgNumbers = i;
						runningOffset++;
						nArgsToStoreIncludingTaints++;
					}
					hasDoubleWides |= argsInReverse[i].getSize() == 2;
				}
//				System.out.println("Args inr everser " + Arrays.toString(argsInReverse));
				runningOffset = 0;
				for(int i = 0; i < howFarBackToStoreInTermsOfArgNumbers; i++)
				{
					nArgsToStoreIncludingTaints ++;
					runningOffset += argsInReverse[i].getSize();
					if(TaintUtils.isPrimitiveArrayType(argsInReverse[i]) || TaintAdapter.isPrimitiveStackType(getStackTypeAtOffset(runningOffset-1)))
					{
						if(TaintUtils.isPrimitiveArrayType(argsInReverse[i]))
							runningOffset++;
						nArgsToStoreIncludingTaints++;
					}
				}
				int[] argStorage = new int[nArgsToStoreIncludingTaints];

				if (howFarBackToStoreInTermsOfArgNumbers >= 0) {
					if(!hasDoubleWides && howFarBackToStoreInTermsOfArgNumbers == 1)
					{
						if(TaintUtils.isPrimitiveType(argsInReverse[0]))
						{
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitInsn(DUP_X2);
							super.visitInsn(SWAP);
						}
						else
						{
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							super.visitInsn(DUP_X2);
							super.visitInsn(POP);
						}
					} else {
						argStorageIdx = nArgsToStoreIncludingTaints - 1;
//						System.out.println(nArgsToStoreIncludingTaints + "len, how far back " + howFarBackToStoreInTermsOfArgNumbers);
//						System.out.println("Actual len " + args.length);
						for (int i = 0; i < howFarBackToStoreInTermsOfArgNumbers; i++) {
							Type t = argsInReverse[i];
							Type s = getTopOfStackType();
//							System.out.println("Store " + t + "- " + s);
							int lv = lvs.getTmpLV(t);
//							System.out.println("Get tmp LV " + lv + " for "+ argStorageIdx);
							super.visitVarInsn(t.getOpcode(Opcodes.ISTORE), lv);
							//					System.out.println("Store to "  +t+s + argStorageIdx);
							argStorage[argStorageIdx] = lv;
							argStorageIdx--;
							if (TaintUtils.isPrimitiveArrayType(t) || TaintUtils.isPrimitiveArrayType(s)) {
								t = Type.getType(Configuration.TAINT_TAG_ARRAYDESC);
								lv = lvs.getTmpLV(t);
								super.visitVarInsn(t.getOpcode(Opcodes.ISTORE), lv);
//								System.out.println("Get tmp LV " + lv + " for "+ argStorageIdx);

								argStorage[argStorageIdx] = lv;
								argStorageIdx--;
							}
						}
						//Now on stack is the last item that we are going to put a null taint under or box
						if (getTopOfStackType().getSort() == Type.ARRAY) {
							registerTaintedArray(getTopOfStackType().getDescriptor());

						} else {
							super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							if (argsInReverse[howFarBackToStoreInTermsOfArgNumbers].getSize() == 2) {
								super.visitInsn(DUP_X2);
								super.visitInsn(POP);
							} else
								super.visitInsn(SWAP);
						}

						argStorageIdx = 0;
						for (int i = args.length-howFarBackToStoreInTermsOfArgNumbers; i < args.length; i++) {
							Type t = args[i];
							if(TaintUtils.isPrimitiveType(t)){
								super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
							}
							super.visitVarInsn(t.getOpcode(Opcodes.ILOAD), argStorage[argStorageIdx]);
							lvs.freeTmpLV(argStorage[argStorageIdx]);
							argStorageIdx++;
							if(TaintUtils.isPrimitiveArrayType(t))
							{
								super.visitVarInsn(t.getOpcode(Opcodes.ILOAD), argStorage[argStorageIdx]);
								lvs.freeTmpLV(argStorage[argStorageIdx]);
								argStorageIdx++;
							}
							else if(TaintUtils.isPrimitiveArrayType(getTopOfStackType()) || getTopOfStackObject().equals(Configuration.TAINT_TAG_ARRAY_STACK_TYPE))
							{
								super.visitVarInsn(t.getOpcode(Opcodes.ILOAD), argStorage[argStorageIdx]);
								lvs.freeTmpLV(argStorage[argStorageIdx]);
								argStorageIdx++;

								registerTaintedArray(getTopOfStackType().getDescriptor());
							}
						}
					}
				}
				desc = newDesc;
			}
			Type newReturnType = Type.getReturnType(desc);
			if (name.equals("<init>") && hasChangedDesc) {
				super.visitInsn(Opcodes.ACONST_NULL);
				desc = desc.substring(0, desc.indexOf(')')) + Type.getDescriptor(TaintSentinel.class) + ")" + desc.substring(desc.indexOf(')') + 1);
			} else if (!TaintUtils.PREALLOC_RETURN_ARRAY) {
				if ((origReturnType.getSort() == Type.ARRAY && origReturnType.getDimensions() == 1 && origReturnType.getElementType().getSort() != Type.OBJECT)
						|| (origReturnType.getSort() != Type.ARRAY && origReturnType.getSort() != Type.OBJECT && origReturnType.getSort() != Type.VOID)) {
					desc = desc.substring(0, desc.indexOf(')')) + newReturnType.getDescriptor() + ")" + desc.substring(desc.indexOf(')') + 1);
					super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(newReturnType));
					name += TaintUtils.METHOD_SUFFIX;
				} else if (hasChangedDesc)
					name += TaintUtils.METHOD_SUFFIX;
			}
			if (TaintUtils.PREALLOC_RETURN_ARRAY) {
				desc = desc.substring(0, desc.indexOf(')')) + Configuration.TAINTED_RETURN_HOLDER_DESC + ")" + desc.substring(desc.indexOf(')') + 1);
				if (!name.equals("<init>"))
					name += TaintUtils.METHOD_SUFFIX;
				analyzer.visitVarInsn(Opcodes.ALOAD, preallocArg);
			}
//			System.out.println("Pre call to " + owner+name+desc + analyzer.stack);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
//			System.out.println("Post call to " + owner+name+desc + analyzer.stack);
			if (!dontUnbox) {
				if (origReturnType.getSort() == Type.ARRAY && origReturnType.getDimensions() == 1 && origReturnType.getElementType().getSort() != Type.OBJECT) {
					//unbox array
					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				} else if (origReturnType.getSort() != Type.ARRAY && origReturnType.getSort() != Type.OBJECT && origReturnType.getSort() != Type.VOID) {
					//unbox prim
					super.visitFieldInsn(GETFIELD, newReturnType.getInternalName(), "val", origReturnType.getDescriptor());
				}
			}
		} else {
			if (!name.equals("clone") && !name.equals("equals")) {
				System.out.println("Call UNTOUCHED" + owner + name + desc);
				throw new IllegalArgumentException();
			}
			//			super.visitInsn(Opcodes.SWAP);
			//			super.visitInsn(Opcodes.POP);
			//			System.out.println("Call UNTOUCHED" + owner + name + desc);

			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

	}

}
