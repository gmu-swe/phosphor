package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.InstructionAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LocalVariableNode;

public class TaintAdapter extends InstructionAdapter implements Opcodes {

	protected LocalVariableManager lvs;
	protected NeverNullArgAnalyzerAdapter analyzer;
	protected String className;

	static final Type taintTagType = Type.getType(Configuration.TAINT_TAG_DESC);
	public static final Type getTagType(String internalName)
	{
		if(canRawTaintAccess(internalName))
			return taintTagType;
		return Type.INT_TYPE;
	}
	public static final boolean canRawTaintAccess(String internalName)
	{
		return !Configuration.MULTI_TAINTING || ( !(internalName.equals("java/lang/Float") || 
//				internalName.equals("java/lang/Boolean") || 
//				internalName.equals("java/lang/Byte") || 
//				internalName.equals("java/lang/Short") || 
				internalName.equals("java/lang/Character") || 
				internalName.equals("java/lang/Double") ||internalName.equals("java/lang/Integer") || 
				internalName.equals("java/lang/Long") || internalName.equals("java/lang/StackTraceElement")));
	}
	public TaintAdapter(int api, String className, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer) {
		super(api, mv);
		this.analyzer = analyzer;
		this.className = className;
	}
	protected void getTaintFieldOfBoxedType(String owner)
	{
		if(!Configuration.MULTI_TAINTING)
			super.visitFieldInsn(GETFIELD, owner, "taint", "I");
		else
		{
			super.visitFieldInsn(GETFIELD, owner, "taint", "Ljava/lang/Object;");
			super.visitTypeInsn(CHECKCAST, Configuration.TAINT_TAG_INTERNAL_NAME);
		}
	}
	public void setLocalVariableSorter(LocalVariableManager lvs) {
		this.lvs = lvs;
	}

	protected boolean topOfStackIsNull() {
		return stackElIsNull(0);
	}

	protected Type getTopOfStackType() {
//		System.out.println(analyzer.stack);
		if(analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.TOP)
			return getStackTypeAtOffset(1);
		return getStackTypeAtOffset(0);
	}

	protected Object getTopOfStackObject() {
		return analyzer.stack.get(analyzer.stack.size() - 1);
	}

	/**
	 * Returns the type of the stack element n down from the top: n=0 -> top of
	 * stack
	 * 
	 * @param n
	 * @return
	 */
	protected Type getStackTypeAtOffset(int n) {
		return getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1 - n));
	}

	protected boolean stackElIsNull(int n) {
		return analyzer.stack.get(analyzer.stack.size() - 1 - n) == Opcodes.NULL;
	}

	protected void retrieveTopOfStackTaintArray() {
		Type onStack = getTopOfStackType();
		generateEmptyTaintArray(onStack.getDescriptor());
	}

	protected void unconditionallyRetrieveTopOfStackTaintArray(boolean leaveOnStack) {
		if (leaveOnStack)
			super.visitInsn(DUP);
		super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintArray", "(Ljava/lang/Object;)I",false);
	}

	/**
	 * Retrieve the taint of the object at the top of the stack (popping that
	 * object) Assumes that the top of the stack is an object (may or may not be
	 * null)
	 */
	protected void safelyFetchObjectTaint() {
		//Check for null first.
		//		Check for common instanceof which are problematic
		//		nonInstrumentingMV.visitInsn(DUP);
		//		Label bail = new Label();
		//		Label end = new Label();
		//		nonInstrumentingMV.visitJumpInsn(IFNULL, bail);
		//		//Not null.
		//		nonInstrumentingMV.visitInsn(DUP);
		//		nonInstrumentingMV.visitTypeInsn(INSTANCEOF, Type.getInternalName(Tainted.class));
		//		nonInstrumentingMV.visitJumpInsn(IFNE, bail); //TODO handle numerics, other ignore classes for which we know the taint
		if (className.equals("java/util/HashMap")) {
			super.visitInsn(POP);
			super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
		} else
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaint", "(Ljava/lang/Object;)I",false);
		//		nonInstrumentingMV.visitLabel(bail);
		//		super.visitInsn(POP); 
		//		super.visitInsn(ICONST_0);
		//		nonInstrumentingMV.visitLabel(end);
	}

//	protected void retrievePrimitiveArrayTaint(boolean leaveTAOnStack) {
////		System.out.println("Finding taint on " + getTopOfStackObject());
//		//A TA
//		super.visitInsn(SWAP);//TA A
//		Label bailTo = new Label();
//		Label end = new Label();
//		nonInstrumentingMV.visitInsn(DUP);
//		nonInstrumentingMV.visitJumpInsn(IFNULL, bailTo);
//		super.visitInsn(DUP); //TA TA A
//		if (leaveTAOnStack)
//			super.visitInsn(DUP);
//		super.visitInsn(ARRAYLENGTH);
//
//		//Idx TA A
//		super.visitInsn(IALOAD);//T A
//		if (leaveTAOnStack) {
//			super.visitInsn(DUP_X2);//T TA A T
//			super.visitInsn(POP); //TA A T
//			super.visitInsn(SWAP); //A TA T
//			super.visitInsn(DUP2_X1);//A TA T A TA
//			super.visitInsn(POP2);
//		}
//		nonInstrumentingMV.visitJumpInsn(GOTO, end);
//		nonInstrumentingMV.visitLabel(bailTo);
//		if (leaveTAOnStack)
//			nonInstrumentingMV.visitInsn(SWAP);
//		else
//			nonInstrumentingMV.visitInsn(POP);
//		nonInstrumentingMV.visitInsn(ICONST_0);
//		nonInstrumentingMV.visitLabel(end);
//	}

//	protected void retrievePrimitiveArrayTaintFromTopArray(boolean leaveTAOnStack) {
//		Label bailTo = new Label();
//		Label end = new Label();
//		nonInstrumentingMV.visitInsn(DUP);
//		nonInstrumentingMV.visitJumpInsn(IFNULL, bailTo);
//		super.visitInsn(DUP); //TA TA
//		if (leaveTAOnStack)
//			super.visitInsn(DUP); //TA TA TA
//		super.visitInsn(ARRAYLENGTH);
//		super.visitInsn(ICONST_M1);
//		super.visitInsn(IADD);
//		//Idx TA A
//		super.visitInsn(IALOAD);//T TA 
//		nonInstrumentingMV.visitJumpInsn(GOTO, end);
//		nonInstrumentingMV.visitLabel(bailTo);
//		if (!leaveTAOnStack)
//			nonInstrumentingMV.visitInsn(POP);
//		nonInstrumentingMV.visitInsn(ICONST_0);
//		nonInstrumentingMV.visitLabel(end);
//	}

//	protected void retrieveTopOfStackTaint() {
//		retrieveTopOfStackTaint(false, true);
//	}

//	protected void retrieveTopOfStackTaint(boolean leavePrimitiveArrayOnStack, boolean onlyRemoveTaintArrays) {
////				System.out.println("getting taint for " + getTopOfStackObject() + " _ " + getTopOfStackType().getInternalName() + (leavePrimitiveArrayOnStack ? "T":"F") + (onlyRemoveTaintArrays ? "T" :"F"));
//
//		if (getTopOfStackType().getInternalName().equals("java/lang/Object")) {
//			if(leavePrimitiveArrayOnStack)
//			super.visitInsn(DUP);
//			safelyFetchObjectTaint();
//			//			System.out.println(analyzer.stack);
//		} else if (Instrumenter.isIgnoredClass(getTopOfStackType().getInternalName())) {
//			if (!leavePrimitiveArrayOnStack)
//				super.visitInsn(POP);
//			super.visitInsn(ICONST_0);//TODO make a sentinel to make this unique
//		} else if (getTopOfStackType().getSort() == Type.ARRAY) {
//
//			if (onlyRemoveTaintArrays && getTopOfStackType().getElementType().getSort() != Type.OBJECT && getTopOfStackType().getDimensions() > 1) {
//				super.visitInsn(SWAP);
//				super.visitInsn(POP);
//			}
//			if (getTopOfStackType().getElementType().getSort() != Type.OBJECT && getTopOfStackType().getDimensions() == 1) {
//				if(onlyRemoveTaintArrays)
//				{
//					super.visitInsn(SWAP);
//					super.visitInsn(POP);
//					unconditionallyRetrieveTopOfStackTaintArray(leavePrimitiveArrayOnStack);
//				}
////				retrievePrimitiveArrayTaint(!onlyRemoveTaintArrays);
//			} else {
//				unconditionallyRetrieveTopOfStackTaintArray(leavePrimitiveArrayOnStack);
//			}
//
//		} else {
//			//			System.out.println(getTopOfStackType());
//			//			System.out.println("top of stack is " + getTopOfStackType() + nonInstrumentingMV);
//			//			System.out.println(analyzer.stack);
//			Label isNull = new Label();
//			Label continu = new Label();
//			nonInstrumentingMV.visitInsn(DUP);
//			if(leavePrimitiveArrayOnStack)
//				super.visitInsn(DUP);
//			nonInstrumentingMV.visitJumpInsn(IFNULL, isNull);
//			if (Instrumenter.isInterface(getTopOfStackType().getInternalName()))
//				super.visitMethodInsn(Opcodes.INVOKEINTERFACE, getTopOfStackType().getInternalName(), "get" + TaintUtils.TAINT_FIELD, "()I",true);
//			else
//				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getTopOfStackType().getInternalName(), "get" + TaintUtils.TAINT_FIELD, "()I",false);
//			nonInstrumentingMV.visitJumpInsn(GOTO, continu);
//			nonInstrumentingMV.visitLabel(isNull);
//			nonInstrumentingMV.visitInsn(POP);
//			nonInstrumentingMV.visitInsn(ICONST_0);
//			nonInstrumentingMV.visitJumpInsn(GOTO, continu);
//			nonInstrumentingMV.visitLabel(continu);
//			//			System.out.println(analyzer.stack);
//		}
//	}

//	protected void retrieveTopOfStackTaintAndPop() {
//		if (getTopOfStackType().getInternalName().equals("java/lang/Object")) {
//			safelyFetchObjectTaint();
//			//			System.out.println(analyzer.stack);
//		} else if (Instrumenter.isIgnoredClass(getTopOfStackType().getInternalName())) {
//			super.visitInsn(POP);
//			super.visitInsn(ICONST_0);//TODO make a sentinel to make this unique
//		} else if (getTopOfStackType().getSort() == Type.ARRAY) {
//
//			if (getTopOfStackType().getElementType().getSort() != Type.OBJECT && getTopOfStackType().getDimensions() > 1) {
//				super.visitInsn(SWAP);
//				super.visitInsn(POP);
//			}
//			//			super.visitInsn(ICONST_0);//TODO
//			//			System.out.println(getTopOfStackType());
//			if (getTopOfStackType().getElementType().getSort() != Type.OBJECT && getTopOfStackType().getDimensions() == 1) {
////				retrievePrimitiveArrayTaint(false);
//				super.visitInsn(SWAP);
//				super.visitInsn(POP);
//				unconditionallyRetrieveTopOfStackTaintArray(false);
//				//				super.visitInsn(POP);
//				//				super.visitInsn(ICONST_0);
//
//			} else {
//				unconditionallyRetrieveTopOfStackTaintArray(false);
//			}
//
//		} else {
//			//			System.out.println(getTopOfStackType());
//			//			System.out.println("top of stack is " + getTopOfStackType() + nonInstrumentingMV);
//			//			System.out.println(analyzer.stack);
//			Label isNull = new Label();
//			Label continu = new Label();
//			nonInstrumentingMV.visitInsn(DUP);
//			nonInstrumentingMV.visitJumpInsn(IFNULL, isNull);
//			if (Instrumenter.isInterface(getTopOfStackType().getInternalName()))
//				super.visitMethodInsn(Opcodes.INVOKEINTERFACE, getTopOfStackType().getInternalName(), "get" + TaintUtils.TAINT_FIELD, "()I",true);
//			else
//				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getTopOfStackType().getInternalName(), "get" + TaintUtils.TAINT_FIELD, "()I",false);
//			nonInstrumentingMV.visitJumpInsn(GOTO, continu);
//			nonInstrumentingMV.visitLabel(isNull);
//			nonInstrumentingMV.visitInsn(POP);
//			nonInstrumentingMV.visitInsn(ICONST_0);
//			nonInstrumentingMV.visitJumpInsn(GOTO, continu);
//			nonInstrumentingMV.visitLabel(continu);
//			//			System.out.println(analyzer.stack);
//		}
//	}

	protected void generateUnconstrainedTaint(int reason) {
			super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
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
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[I)[[I",false);
			
			super.visitInsn(SWAP);
			super.visitJumpInsn(GOTO, done);
			super.visitLabel(isNull);
			fn.accept(this);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();
			super.visitLabel(done);
			fn2.accept(this);
			
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
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[I)[[[I",false);
			
			super.visitInsn(SWAP);
			super.visitJumpInsn(GOTO, done);

			super.visitLabel(isNull);
			fn.accept(this);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();
			super.visitLabel(done);
			fn2.accept(this);
		} else if (arrayType.getDimensions() == 1) {
			FrameNode fn = getCurrentFrameNode();
			super.visitInsn(DUP);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitJumpInsn(IFNULL, isNull);
			super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
			super.visitInsn(DUP);
			super.visitInsn(ARRAYLENGTH);
			if(!Configuration.MULTI_TAINTING)
				super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			else
				super.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);	
			super.visitInsn(SWAP);
			super.visitJumpInsn(GOTO, done);

			super.visitLabel(isNull);
			fn.accept(this);
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			FrameNode fn2 = getCurrentFrameNode();
			super.visitLabel(done);
			fn2.accept(this);
		} else {
			throw new IllegalStateException("Can't handle casts to multi-d array type of dimension " + arrayType.getDimensions());
		}
	}

	protected static Object[] removeLongsDoubleTopVal(List<Object> in) {
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
	static int[][][] foo(int[][][] in)
	{
		int[][][] ret = new int[in.length][][];
		for(int i = 0; i < ret.length;i++)
		{
			ret[i] = new int[in[i].length][];
			for(int j = 0; j < ret[i].length; j++)
			{
				ret[i][j] = new int[in[i][j].length];
			}
		}
		return ret;

	}
	static int[][] foo(int[][] in)
	{
		int[][] ret = new int[in.length][];
		for(int i = 0; i < ret.length;i++)
		{
			ret[i] = new int[in[i].length];
		}
		return ret;
	}
	public static void createNewTaintArray(String arrayDesc, NeverNullArgAnalyzerAdapter analyzer, MethodVisitor mv, LocalVariableManager lvs) {
		Type arrayType = Type.getType(arrayDesc);
		Label isNull = new Label();
		Label done = new Label();
		Object[] locals1 = removeLongsDoubleTopVal(analyzer.locals);
		int localSize1 = locals1.length;

		Object[] stack1 = removeLongsDoubleTopVal(analyzer.stack);
		int stackSize1 = stack1.length;

		mv.visitInsn(Opcodes.DUP);
		mv.visitJumpInsn(Opcodes.IFNULL, isNull);

		if(arrayType.getDimensions() == 2)
		{
			mv.visitInsn(DUP);
			mv.visitInsn(DUP);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitMultiANewArrayInsn(Configuration.TAINT_TAG_ARRAYDESC, 1);
			if (Configuration.MULTI_TAINTING)
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[Ljava/lang/Object;)[[Ljava/lang/Object;", false);
			else
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[I)[[I", false);
	
			mv.visitInsn(SWAP);
		}
		else if(arrayType.getDimensions() == 3)
		{
			mv.visitInsn(DUP);
			mv.visitInsn(DUP);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitMultiANewArrayInsn("["+Configuration.TAINT_TAG_ARRAYDESC, 1);
			if(Configuration.MULTI_TAINTING)
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[Ljava/lang/Object;)[[[Ljava/lang/Object;",false);
			else
				mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[I)[[[I",false);
			mv.visitInsn(SWAP);
		}
		else if (arrayType.getDimensions() > 1)
		{
			int tmp = lvs.getTmpLV(arrayType);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, tmp);

			for(int i = 0; i < arrayType.getDimensions(); i++)
			{
				mv.visitVarInsn(ALOAD, tmp);

				for(int j = 0; j < i; j++)
				{
					mv.visitInsn(ICONST_0);
					mv.visitInsn(AALOAD);
				}
				mv.visitInsn(Opcodes.ARRAYLENGTH);
			}
			mv.visitMultiANewArrayInsn(arrayDesc.substring(0, arrayDesc.length() - 1) + Configuration.TAINT_TAG_DESC, arrayType.getDimensions()); //TODO XXX this won't be properly initialized. 

		}
		else
		{
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(Opcodes.ARRAYLENGTH);
			if(!Configuration.MULTI_TAINTING)
				mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
			else
				mv.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
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
			if (!(((String) obj).charAt(0) == '[') && ((String) obj).length() > 1)
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
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					return true;
			}
			return false;
		}
		return obj == Opcodes.INTEGER || obj == Opcodes.FLOAT || obj == Opcodes.DOUBLE || obj == Opcodes.LONG || obj == Opcodes.TOP;
	}

	public static boolean isPrimitiveType(Type t) {
		if(t == null)
			return false;
		switch (t.getSort()) {
		case Type.ARRAY:
			return t.getElementType().getSort() != Type.OBJECT;
		case Type.OBJECT:
		case Type.VOID:
			return false;
		default:
			return true;
		}
	}

	public static int getStackElementSize(Object obj) {
		if (obj == Opcodes.DOUBLE || obj == Opcodes.LONG || obj == Opcodes.TOP)
			return 2;
		return 1;
	}

	protected void freeLVs(LocalVariableNode[] lvArray)
	{
		for(LocalVariableNode n : lvArray)
			lvs.freeTmpLV(n.index);
	}
	protected void loadLV(int n, LocalVariableNode[] lvArray) {
		super.visitVarInsn(Type.getType(lvArray[n].desc).getOpcode(ILOAD), lvArray[n].index);
	}

	/**
	 * Generates instructions equivalent to an instruction DUP{N}_X{U}, e.g.
	 * DUP2_X1 will dup the top 2 elements under the 1 beneath them.
	 * 
	 * @param n
	 * @param u
	 */
	protected void DUPN_XU(int n, int u) {
		//		if (TaintUtils.DEBUG_DUPSWAP)
		//			System.out.println(name + ": DUP" + n + "_X" + u + analyzer.stack);
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
		//		if (TaintUtils.DEBUG_DUPSWAP)
		//			System.out.println("POST " + name + ": DUP" + n + "_X" + u + analyzer.stack);
	}

	public FrameNode getCurrentFrameNode()
	{
		return getCurrentFrameNode(analyzer);
	}
	public static FrameNode getCurrentFrameNode(NeverNullArgAnalyzerAdapter a)
	{
		if(a.locals == null || a.stack == null)
			throw new IllegalArgumentException();
		Object[] locals = removeLongsDoubleTopVal(a.locals);
		Object[] stack = removeLongsDoubleTopVal(a.stack);
		FrameNode ret = new FrameNode(TaintUtils.RAW_INSN, locals.length, locals, stack.length, stack);
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
				elType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
			else
				elType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1));
			ret[i] = new LocalVariableNode(null, elType.getDescriptor(), null, null, null, lvs.getTmpLV());
			super.visitVarInsn(elType.getOpcode(ISTORE), ret[i].index);
		}
		return ret;
	}
}
