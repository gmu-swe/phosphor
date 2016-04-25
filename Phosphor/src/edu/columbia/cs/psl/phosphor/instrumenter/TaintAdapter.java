package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

public class TaintAdapter extends MethodVisitor implements Opcodes {

	protected LocalVariableManager lvs;
	protected NeverNullArgAnalyzerAdapter analyzer;
	protected String className;

	private static final Type taintTagType = Type.getType(Configuration.TAINT_TAG_DESC);

	public LocalVariableManager getLvs() {
		return lvs;
	}

	public static final Type getTagType(String internalName) {
		if(canRawTaintAccess(internalName))
			return taintTagType;
		return Type.INT_TYPE;
	}

	public static final boolean canRawTaintAccess(String internalName) {
		return !Configuration.MULTI_TAINTING
			|| ( !(internalName.equals("java/lang/Float")
					//	|| internalName.equals("java/lang/Boolean")
					//	|| internalName.equals("java/lang/Byte")
					//	|| internalName.equals("java/lang/Short")
						|| internalName.equals("java/lang/Character")
						|| internalName.equals("java/lang/Double")
						|| internalName.equals("java/lang/Integer")
						|| internalName.equals("java/lang/Long")
						|| internalName.equals("java/lang/StackTraceElement")));
	}

	private TaintAdapter(MethodVisitor mv) {
		super(Opcodes.ASM5,mv);
	}

	private TaintAdapter(int api, MethodVisitor mv) {
		super(api, mv);
	}

	public TaintAdapter(int access, String className, String name,
			String desc, String signature, String[] exceptions,
			MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
		super(Opcodes.ASM5, mv);
		this.analyzer = analyzer;
		this.className = className;
	}

	void ensureUnBoxedAt(int n, Type t) {
		switch (n) {
		case 0:
			super.visitMethodInsn(Opcodes.INVOKESTATIC,
					Type.getInternalName(MultiDTaintedArray.class),
					"unboxRaw",
					"(Ljava/lang/Object;)Ljava/lang/Object;",
					false);
			super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			break;
		case 1:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG
					|| top == Opcodes.DOUBLE
					|| top == Opcodes.TOP) {
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(MultiDTaintedArray.class),
						"unboxRaw",
						"(Ljava/lang/Object;)Ljava/lang/Object;",
						false);
				super.visitTypeInsn(Opcodes.CHECKCAST,t.getInternalName());
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			} else {
				super.visitInsn(SWAP);
				super.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(MultiDTaintedArray.class),
						"unboxRaw",
						"(Ljava/lang/Object;)Ljava/lang/Object;",
						false);
				super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
				super.visitInsn(SWAP);
			}
			break;
		default:
			LocalVariableNode[] d = storeToLocals(n);

			super.visitMethodInsn(Opcodes.INVOKESTATIC,
					Type.getInternalName(MultiDTaintedArray.class),
					"unboxRaw",
					"(Ljava/lang/Object;)Ljava/lang/Object;",
					false);
			super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);
		}
	}

	public boolean topHas0Taint() {
		if (getTopOfStackObject() == Opcodes.TOP)
			return Integer.valueOf(0).equals(
					analyzer.stackConstantVals.get(
						analyzer.stackConstantVals.size() - 3));
		else
			return Integer.valueOf(0).equals(
					analyzer.stackConstantVals.get(
						analyzer.stackConstantVals.size() - 2));
	}

	public boolean secondHas0Taint() {
		int offset = 2;
		if (getTopOfStackObject() == Opcodes.TOP)
			offset++;
		if (getStackElementSize(offset) == Opcodes.TOP)
			offset++;
		offset++;
		return Integer.valueOf(0).equals(
				analyzer.stackConstantVals.get(
					analyzer.stackConstantVals.size() - offset));
	}

	protected void getTaintFieldOfBoxedType(String owner) {
		if(!Configuration.MULTI_TAINTING)
			super.visitFieldInsn(GETFIELD, owner, "taint", "I");
		else {
			super.visitFieldInsn(GETFIELD, owner, "taint", "Ljava/lang/Object;");
			super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
		}
	}

	public void setLocalVariableSorter(LocalVariableManager lvs) {
		this.lvs = lvs;
	}

	public boolean topOfStackIsNull() {
		return stackElIsNull(0);
	}

	public Type getTopOfStackType() {
		if(analyzer.stack == null)
			throw new NullPointerException();
		if(analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.TOP)
			return getStackTypeAtOffset(1);
		return getStackTypeAtOffset(0);
	}

	public Object getTopOfStackObject() {
		return analyzer.stack.get(analyzer.stack.size() - 1);
	}

	/**
	 * Returns the type of the stack element n down from the top: n=0 -> top of
	 * stack
	 * 
	 * @param n
	 * @return
	 */
	public Type getStackTypeAtOffset(int n) {
		return getTypeForStackType(
				analyzer.stack.get(
					analyzer.stack.size() - 1 - n));
	}

	public boolean stackElIsNull(int n) {
		return analyzer.stack.get(
				analyzer.stack.size() - 1 - n) == Opcodes.NULL;
	}

	public NeverNullArgAnalyzerAdapter getAnalyzer() {
		return analyzer;
	}

	public void retrieveTopOfStackTaintArray() {
		Type onStack = getTopOfStackType();
		generateEmptyTaintArray(onStack.getDescriptor());
	}

	public void unconditionallyRetrieveTopOfStackTaintArray(boolean leaveOnStack) {
		if (leaveOnStack)
			super.visitInsn(DUP);
		super.visitMethodInsn(INVOKESTATIC,
				Type.getInternalName(TaintUtils.class),
				"getTaintArray",
				"(Ljava/lang/Object;)I",
				false);
	}

	public boolean topStackElCarriesTaints() {
		Object o = analyzer.stack.get(analyzer.stack.size() - 1);
		return isPrimitiveStackType(o);
	}

	public boolean topStackElIsNull() {
		Object o = analyzer.stack.get(analyzer.stack.size() - 1);
		return o == Opcodes.NULL;
	}

	/**
	 * Retrieve the taint of the object at the top of the stack (popping that
	 * object) Assumes that the top of the stack is an object (may or may not be
	 * null)
	 */
	protected void safelyFetchObjectTaint() {
		if (className.equals("java/util/HashMap")) {
			super.visitInsn(POP);
			Configuration.taintTagFactory.generateEmptyTaint(mv);
		} else
			super.visitMethodInsn(INVOKESTATIC,
					Type.getInternalName(TaintUtils.class),
					"getTaint",
					"(Ljava/lang/Object;)I",
					false);
	}

	protected void generateUnconstrainedTaint(int reason) {
		Configuration.taintTagFactory.generateEmptyTaint(mv);
	}

	/**
	 * Precondition: top stack element is an array Postconditoins: top stack
	 * element is the same array, second stack element is an int array of the
	 * same length.
	 */
	protected void generateEmptyTaintArray(String arrayDesc) {
		Type arrayType = Type.getType(arrayDesc);
		Label isNull = new Label();
		Label done = new Label();
		if (arrayType.getDimensions() == 2) {

			FrameNode fn = getCurrentFrameNode();
			super.visitInsn(DUP);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitJumpInsn(IFNULL, isNull);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitInsn(DUP);
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);
			super.visitMultiANewArrayInsn("[[I", 1);
			super.visitMethodInsn(INVOKESTATIC,
					Type.getInternalName(TaintUtils.class),
					"create2DTaintArray",
					"(Ljava/lang/Object;[[I)[[I",
					false);
			
			if (!(Configuration.taintTagFactory instanceof DataAndControlFlowTagFactory)) {
				super.visitInsn(DUP);
				super.visitInsn(ICONST_2);
				super.visitMethodInsn(Opcodes.INVOKEINTERFACE,
						Type.getInternalName(TaintTagFactory.class),
						"generateEmptyTaintArray",
						"([Ljava/lang/Object;I)V",
						false);
			}
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();

			super.visitJumpInsn(GOTO, done);
			super.visitLabel(isNull);
			acceptFn(fn);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			super.visitLabel(done);
			acceptFn(fn2);

		} else if (arrayType.getDimensions() == 3) {
			FrameNode fn = getCurrentFrameNode();
			super.visitInsn(DUP);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitJumpInsn(IFNULL, isNull);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitInsn(DUP);
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);
			super.visitMultiANewArrayInsn("[[[I", 1);
			super.visitMethodInsn(INVOKESTATIC,
					Type.getInternalName(TaintUtils.class),
					"create3DTaintArray",
					"(Ljava/lang/Object;[[[I)[[[I",
					false);
			if (!(Configuration.taintTagFactory instanceof DataAndControlFlowTagFactory)) {
				super.visitInsn(DUP);
				super.visitInsn(ICONST_3);
				super.visitMethodInsn(Opcodes.INVOKEINTERFACE,
						Type.getInternalName(TaintTagFactory.class),
						"generateEmptyTaintArray",
						"([Ljava/lang/Object;I)V",
						false);
			}
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();

			super.visitJumpInsn(GOTO, done);
			super.visitLabel(isNull);
			acceptFn(fn);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			super.visitLabel(done);
			acceptFn(fn2);
		} else if (arrayType.getDimensions() == 1) {
			FrameNode fn = getCurrentFrameNode();
			super.visitInsn(DUP);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitJumpInsn(IFNULL, isNull);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);
			if (!Configuration.MULTI_TAINTING)
				super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			else {
				super.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);	
				if (!(Configuration.taintTagFactory instanceof DataAndControlFlowTagFactory)) {
					super.visitInsn(DUP);
					super.visitInsn(ICONST_1);
					super.visitMethodInsn(Opcodes.INVOKEINTERFACE,
							Type.getInternalName(TaintTagFactory.class),
							"generateEmptyTaintArray",
							"([Ljava/lang/Object;I)V",
							false);
				}
			}
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();

			super.visitJumpInsn(GOTO, done);
			super.visitLabel(isNull);
			acceptFn(fn);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			super.visitLabel(done);
			acceptFn(fn2);
		} else {
			throw new IllegalStateException("Can't handle casts to multi-d array type of dimension "
					+ arrayType.getDimensions());
		}
	}

	public static Object[] removeLongsDoubleTopVal(List<Object> in) {
		ArrayList<Object> ret = new ArrayList<Object>();
		boolean lastWas2Word = false;
		for (Object n : in) {
			if (n == Opcodes.TOP && lastWas2Word) {
				//nop
			} else
				ret.add(n);
			if (n == Opcodes.DOUBLE || n == Opcodes.LONG)
				lastWas2Word = true;
			else
				lastWas2Word = false;
		}
		return ret.toArray();
	}

	public static void createNewTaintArray(String arrayDesc,
			NeverNullArgAnalyzerAdapter analyzer,
			MethodVisitor mv, LocalVariableManager lvs) {
		Type arrayType = Type.getType(arrayDesc);
		Label isNull = new Label();
		Label done = new Label();
		Object[] locals1 = removeLongsDoubleTopVal(analyzer.locals);
		int localSize1 = locals1.length;

		Object[] stack1 = removeLongsDoubleTopVal(analyzer.stack);
		int stackSize1 = stack1.length;

		mv.visitInsn(Opcodes.DUP);
		mv.visitJumpInsn(Opcodes.IFNULL, isNull);

		if (arrayType.getDimensions() == 2) {
			mv.visitInsn(DUP);
			mv.visitInsn(DUP);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitMultiANewArrayInsn(Configuration.TAINT_TAG_ARRAYDESC, 1);
			if (Configuration.MULTI_TAINTING)
				mv.visitMethodInsn(INVOKESTATIC,
						Type.getInternalName(TaintUtils.class),
						"create2DTaintArray",
						"(Ljava/lang/Object;[[Ljava/lang/Object;)[[Ljava/lang/Object;",
						false);
			else
				mv.visitMethodInsn(INVOKESTATIC,
						Type.getInternalName(TaintUtils.class),
						"create2DTaintArray",
						"(Ljava/lang/Object;[[I)[[I",
						false);
			mv.visitInsn(SWAP);
		} else if (arrayType.getDimensions() == 3) {
			mv.visitInsn(DUP);
			mv.visitInsn(DUP);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitMultiANewArrayInsn("[" + Configuration.TAINT_TAG_ARRAYDESC, 1);
			if(Configuration.MULTI_TAINTING)
				mv.visitMethodInsn(INVOKESTATIC,
						Type.getInternalName(TaintUtils.class),
						"create3DTaintArray",
						"(Ljava/lang/Object;[[[Ljava/lang/Object;)[[[Ljava/lang/Object;",
						false);
			else
				mv.visitMethodInsn(INVOKESTATIC,
						Type.getInternalName(TaintUtils.class),
						"create3DTaintArray",
						"(Ljava/lang/Object;[[[I)[[[I",
						false);
			mv.visitInsn(SWAP);
		} else if (arrayType.getDimensions() > 1) {
			int tmp = lvs.getTmpLV(arrayType);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, tmp);

			for(int i = 0; i < arrayType.getDimensions(); i++) {
				mv.visitVarInsn(ALOAD, tmp);
				for(int j = 0; j < i; j++) {
					mv.visitInsn(ICONST_0);
					mv.visitInsn(AALOAD);
				}
				mv.visitInsn(Opcodes.ARRAYLENGTH);
			}
			mv.visitMultiANewArrayInsn(
					arrayDesc.substring(0, arrayDesc.length() - 1)
						+ Configuration.TAINT_TAG_DESC,
					arrayType.getDimensions()); //TODO this won't be properly initialized. 

		} else {
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(Opcodes.ARRAYLENGTH);
			if(Configuration.MULTI_TAINTING)
				mv.visitTypeInsn(Opcodes.ANEWARRAY,
						Configuration.TAINT_TAG_INTERNAL_NAME);
			else
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			mv.visitInsn(Opcodes.SWAP);
		}

		Object[] locals = removeLongsDoubleTopVal(analyzer.locals);
		int localSize = locals.length;

		Object[] stack = removeLongsDoubleTopVal(analyzer.stack);
		int stackSize = stack.length;

		mv.visitJumpInsn(Opcodes.GOTO, done);
		mv.visitFrame(TaintUtils.RAW_INSN, localSize1, locals1, stackSize1, stack1);
		mv.visitLabel(isNull);
		mv.visitInsn(Opcodes.ACONST_NULL);
		mv.visitInsn(Opcodes.SWAP);
		mv.visitLabel(done);
		mv.visitFrame(TaintUtils.RAW_INSN, localSize, locals, stackSize, stack);
	}

	public static Type getTypeForStackType(Object obj) {
		if (obj == Opcodes.INTEGER)
			return Type.INT_TYPE;
		if (obj == Opcodes.FLOAT)
			return Type.FLOAT_TYPE;
		if (obj == Opcodes.DOUBLE)
			return Type.DOUBLE_TYPE;
		if (obj == Opcodes.LONG)
			return Type.LONG_TYPE;
		if (obj instanceof String)
			if (!(((String) obj).charAt(0) == '[')
					&& ((String) obj).length() > 1)
				return Type.getType("L" + obj + ";");
			else
				return Type.getType((String) obj);
		if (obj == Opcodes.NULL)
			return Type.getType("Ljava/lang/Object;");
		if (obj instanceof Label || obj == Opcodes.UNINITIALIZED_THIS)
			return Type.getType("Luninitialized;");
		throw new IllegalArgumentException("got " + obj + " zzz" + obj.getClass());
	}

	public static boolean isPrimitiveStackType(Object obj) {
		if (obj instanceof String) {
			if (((String) obj).startsWith("[")) {
				Type t = Type.getType((String) obj);
				if (t.getSort() == Type.ARRAY
						&& t.getElementType().getSort() != Type.OBJECT
						&& t.getDimensions() == 1)
					return true;
			}
			return false;
		}
		return obj == Opcodes.INTEGER
			|| obj == Opcodes.FLOAT
			|| obj == Opcodes.DOUBLE
			|| obj == Opcodes.LONG
			|| obj == Opcodes.TOP;
	}

	public static boolean isPrimitiveType(Type t) {
		if(t == null)
			return false;
		switch (t.getSort()) {
		case Type.ARRAY:
			return t.getElementType().getSort() != Type.OBJECT
				&& t.getDimensions() == 1;
		case Type.OBJECT:
		case Type.VOID:
			return false;
		default:
			return true;
		}
	}

	public static int getStackElementSize(Object obj) {
		if (obj == Opcodes.DOUBLE
				|| obj == Opcodes.LONG
				|| obj == Opcodes.TOP)
			return 2;
		return 1;
	}

	protected void freeLVs(LocalVariableNode[] lvArray) {
		for(LocalVariableNode n : lvArray)
			lvs.freeTmpLV(n.index);
	}

	protected void loadLV(int n, LocalVariableNode[] lvArray) {
		super.visitVarInsn(
				Type.getType(
					lvArray[n].desc).getOpcode(ILOAD), lvArray[n].index);
	}

	/**
	 * Generates instructions equivalent to an instruction DUP{N}_X{U}, e.g.
	 * DUP2_X1 will dup the top 2 elements under the 1 beneath them.
	 * 
	 * @param n
	 * @param u
	 */
	protected void DUPN_XU(int n, int u) {
		switch (n) {
		case 1:
			switch (u) {
			case 1:
				super.visitInsn(DUP_X1);
				break;
			case 2:
				super.visitInsn(DUP_X2);
				break;
			case 3:
				// A B C D -> D A B C D
				LocalVariableNode d[] = storeToLocals(4);
				loadLV(0, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 4:
				d = storeToLocals(5);
				loadLV(0, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			default:
				throw new IllegalArgumentException("DUP" + n + "_" + u + " is unimp.");
			}
			break;
		case 2:
			switch (u) {
			case 1:
				LocalVariableNode[] d = storeToLocals(3);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 2:
				d = storeToLocals(4);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 3:
				d = storeToLocals(5);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 4:
				d = storeToLocals(6);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			default:
				throw new IllegalArgumentException("DUP" + n + "_" + u + " is unimp.");
			}
			break;
		case 3:
			switch (u) {
			case 0:
				LocalVariableNode[] d = storeToLocals(3);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 1:
				d = storeToLocals(4);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 2:
				d = storeToLocals(5);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 3:
				d = storeToLocals(6);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 4:
				d = storeToLocals(7);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(6, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			default:
				throw new IllegalArgumentException("DUP" + n + "_" + u + " is unimp.");
			}
			break;
		case 4:
			switch (u) {
			case 1:
				LocalVariableNode[] d = storeToLocals(5);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 2:
				d = storeToLocals(6);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 3:
				d = storeToLocals(7);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(6, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			case 4:
				d = storeToLocals(8);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				loadLV(7, d);
				loadLV(6, d);
				loadLV(5, d);
				loadLV(4, d);
				loadLV(3, d);
				loadLV(2, d);
				loadLV(1, d);
				loadLV(0, d);
				freeLVs(d);
				break;
			default:
				throw new IllegalArgumentException("DUP" + n + "_" + u + " is unimp.");
			}
			break;
		default:
			throw new IllegalArgumentException("DUP" + n + "_" + u + " is unimp.");
		}
	}

	private static Object[] asArray(final List<Object> l) {
		Object[] objs = new Object[l.size()];
		for (int i = 0; i < objs.length; ++i) {
			Object o = l.get(i);
			if (o instanceof LabelNode) {
				o = ((LabelNode) o).getLabel();
			}
			objs[i] = o;
		}
		return objs;
	}

	public void acceptFn(FrameNode fn) {
		acceptFn(fn, mv);
	}

	public static void acceptFn(FrameNode fn, MethodVisitor mv) {
		switch (fn.type) {
		case Opcodes.F_NEW:
		case Opcodes.F_FULL:
		case TaintUtils.RAW_INSN:
			mv.visitFrame(fn.type, fn.local.size(), asArray(fn.local),
					fn.stack.size(), asArray(fn.stack));
			break;
		case Opcodes.F_APPEND:
			mv.visitFrame(fn.type, fn.local.size(), asArray(fn.local), 0, null);
			break;
		case Opcodes.F_CHOP:
			mv.visitFrame(fn.type, fn.local.size(), null, 0, null);
			break;
		case Opcodes.F_SAME:
			mv.visitFrame(fn.type, 0, null, 0, null);
			break;
		case Opcodes.F_SAME1:
			mv.visitFrame(fn.type, 0, null, 1, asArray(fn.stack));
			break;
		}
	}

	public FrameNode getCurrentFrameNode() {
		return getCurrentFrameNode(analyzer);
	}

	public static FrameNode getCurrentFrameNode(NeverNullArgAnalyzerAdapter a) {
		if(a.locals == null || a.stack == null)
			throw new IllegalArgumentException();
		Object[] locals = removeLongsDoubleTopVal(a.locals);
		Object[] stack = removeLongsDoubleTopVal(a.stack);
		FrameNode ret = new FrameNode(Opcodes.F_FULL, locals.length,
				locals, stack.length, stack);
		ret.type = TaintUtils.RAW_INSN;
		return ret;
	}

	/**
	 * Stores the top n stack elements as local variables. Returns an array of
	 * all of the lv indices. return[0] is the top element.
	 * 
	 * @param n
	 * @return
	 */
	protected LocalVariableNode[] storeToLocals(int n) {
		LocalVariableNode[] ret = new LocalVariableNode[n];
		//		System.out.println("Store to locals top " + n);
		//		System.out.println(analyzer.stack);
		for (int i = 0; i < n; i++) {
			Type elType = null;
			if (analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.TOP)
				elType = getTypeForStackType(
						analyzer.stack.get(
							analyzer.stack.size() - 2));
			else
				elType = getTypeForStackType(
						analyzer.stack.get(
							analyzer.stack.size() - 1));
			ret[i] = new LocalVariableNode(null,
					elType.getDescriptor(),
					null, null, null, lvs.getTmpLV());
			super.visitVarInsn(elType.getOpcode(ISTORE), ret[i].index);
		}
		return ret;
	}

	public void unwrapTaintedInt() {
		super.visitInsn(DUP);
		getTaintFieldOfBoxedType(Configuration.TAINTED_INT_INTERNAL_NAME);
		super.visitInsn(SWAP);
		super.visitFieldInsn(GETFIELD,
				Configuration.TAINTED_INT_INTERNAL_NAME, "val", "I");
	}
}
