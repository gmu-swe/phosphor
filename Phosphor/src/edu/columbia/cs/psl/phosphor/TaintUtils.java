package edu.columbia.cs.psl.phosphor;

import java.lang.reflect.Array;

import sun.misc.VM;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStore;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.Tainted;
import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedByte;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedChar;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedDouble;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloat;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedLong;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitive;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class TaintUtils {
	static Object lock = new Object();

	public static final boolean TAINT_THROUGH_SERIALIZATION = false;
	public static final boolean OPT_PURE_METHODS = false;
	public static final boolean GENERATE_FASTPATH_VERSIONS = false;

	public static final boolean OPT_IGNORE_EXTRA_TAINTS = true;
	public static final boolean OPT_CONSTANT_ARITHMETIC = true;
	public static final boolean OPT_USE_STACK_ONLY = false; //avoid using LVs where possible if true
	
	public static final boolean MULTI_TAINT = false;
	
	public static final int RAW_INSN = 201;
	public static final int NO_TAINT_STORE_INSN = 202;
	public static final int IGNORE_EVERYTHING = 203;
	public static final int NO_TAINT_UNBOX = 204;
	public static final int DONT_LOAD_TAINT = 205;
	public static final int GENERATETAINTANDSWAP = 206;

	public static final int NEXTLOAD_IS_TAINTED = 207;
	public static final int NEXTLOAD_IS_NOT_TAINTED = 208;
	public static final int NEVER_AUTOBOX = 209;
	public static final int ALWAYS_AUTOBOX = 210;
	public static final int ALWAYS_BOX_JUMP = 211;
	public static final int ALWAYS_UNBOX_JUMP = 212;
	public static final int IS_TMP_STORE = 213;

	
	public static final String TAINT_FIELD = "INVIVO_PC_TAINT";
	public static final String HAS_TAINT_FIELD = "INVIVO_IS_TAINTED";
	public static final String IS_TAINT_SEATCHING_FIELD = "INVIVO_IS_TAINT_SEARCHING";

	public static final String METHOD_SUFFIX = "$$INVIVO_PC";
	public static final boolean DEBUG_ALL = false;
	public static final boolean DEBUG_DUPSWAP = false || DEBUG_ALL;
	public static final boolean DEBUG_FRAMES = false || DEBUG_ALL;
	public static final boolean DEBUG_FIELDS = false || DEBUG_ALL;
	public static final boolean DEBUG_LOCAL = false || DEBUG_ALL;
	public static final boolean DEBUG_CALLS = false || DEBUG_ALL;
	public static final boolean DEBUG_OPT = false;
	public static final boolean DEBUG_PURE = false;

	public static final boolean ADD_BASIC_ARRAY_CONSTRAINTS = true;
	public static final boolean ADD_HEAVYWEIGHT_ARRAY_TAINTS = ADD_BASIC_ARRAY_CONSTRAINTS || true;

	public static int nextTaint = 0;
	public static int nextTaintINVIVO_PC_TAINT = 0;

	public static int nextMethodId = 0;


	public static final int MAX_CONCURRENT_BRANCHES = 500;

	public static final String STR_LDC_WRAPPER = "INVIVO_LDC_STR";

	public static final int UNCONSTRAINED_NEW_STRING = 4;

	public static final boolean VERIFY_CLASS_GENERATION = false;


	public static boolean isPreAllocReturnType(String methodDescriptor)
	{
		Type retType = Type.getReturnType(methodDescriptor);
		if(retType.getSort() == Type.OBJECT || retType.getSort() == Type.VOID)
			return false;
		if(retType.getSort() == Type.ARRAY && retType.getElementType().getSort() != Type.OBJECT && retType.getDimensions() == 1)
			return true;
		else if(retType.getSort() == Type.ARRAY)
			return false;
		return true;
	}
	public static boolean isNotRealArg(Type t)
	{
		if(t.equals(Type.getType(TaintSentinel.class)))
			return true;
		if(t.equals(Type.getType(UninstrumentedTaintSentinel.class)))
			return true;
		if(t.getInternalName().startsWith("edu/columbia/cs/psl/"))
		{
			try {
				Class c = Class.forName(t.getInternalName().replace("/", "."));
				if(TaintedPrimitive.class.isAssignableFrom(c) || TaintedPrimitiveArray.class.isAssignableFrom(c))
					return true;
			} catch (ClassNotFoundException e) {
			}
		}
		return false;
	}
	public static boolean arrayHasTaints(int[] a) {
		for (int i : a)
			if (i != 0)
				return true;
		return false;
	}

	public static boolean arrayHasTaints(int[][] a) {
		for (int[] j : a)
			for (int i : j)
				if (i != 0)
					return true;
		return false;
	}

	public static boolean arrayHasTaints(int[][][] a) {
		for (int[][] k : a)
			for (int[] j : k)
				for (int i : j)
					if (i != 0)
						return true;
		return false;
	}


	public static int getTaint(Object obj) {
		if (obj instanceof Tainted) {
			return ((Tainted) obj).getINVIVO_PC_TAINT();
		}
		if(BoxedPrimitiveStore.tags.containsKey(obj))
			return BoxedPrimitiveStore.tags.get(obj);
		return 0;
	}


	public static int[][] create2DTaintArray(Object in, int[][] ar) {
		for (int i = 0; i < Array.getLength(in); i++) {
			Object entry = Array.get(in, i);
			if (entry != null)
				ar[i] = new int[Array.getLength(entry)];
		}
		return ar;
	}

	public static int[][][] create3DTaintArray(Object in, int[][][] ar) {
		for (int i = 0; i < Array.getLength(in); i++) {
			Object entry = Array.get(in, i);
			if (entry != null) {
				ar[i] = new int[Array.getLength(entry)][];
				for (int j = 0; j < Array.getLength(entry); j++) {
					Object e = Array.get(entry, j);
					if (e != null)
						ar[i][j] = new int[Array.getLength(e)];
				}
			}
		}
		return ar;
	}

	public static void generateMultiDTaintArray(Object in, Object taintRef) {
		//Precondition is that taintArrayRef is an array with the same number of dimensions as obj, with each allocated.
		for (int i = 0; i < Array.getLength(in); i++) {
			Object entry = Array.get(in, i);
			Class<?> clazz = entry.getClass();
			if (clazz.isArray()) {
				//Multi-D array
				int innerDims = Array.getLength(entry);
				Array.set(taintRef, i, Array.newInstance(Integer.TYPE, innerDims));
			}
		}
	}


	public static boolean OKtoDebug = false;
	public static int OKtoDebugINVIVO_PC_TAINT;

	public static void arraycopy(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		if(!src.getClass().isArray())
		{
			System.arraycopy(((MultiDTaintedArray)src).getVal(), srcPos, ((MultiDTaintedArray)dest).getVal(), destPos, length);
			System.arraycopy(((MultiDTaintedArray)src).taint, srcPos, ((MultiDTaintedArray)dest).taint, destPos, length);
		}
		else
			System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopyVM(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		if(!src.getClass().isArray())
		{
			VMSystem.arraycopy0(((MultiDTaintedArray)src).getVal(), srcPos, ((MultiDTaintedArray)dest).getVal(), destPos, length);
			VMSystem.arraycopy0(((MultiDTaintedArray)src).taint, srcPos, ((MultiDTaintedArray)dest).taint, destPos, length);
		}
		else
			VMSystem.arraycopy0(src, srcPos, dest, destPos, length);
	}
	
	public static void arraycopy(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		throw new ArrayStoreException("Can't copy from src with taint to dest w/o taint!");
	}

	public static void arraycopy(Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		throw new ArrayStoreException("Can't copy from src w/ no taint to dest w/ taint!!");
	}

	public static boolean weakHashMapInitialized = false;

	public static void arraycopy(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);

		if (VM.isBooted$$INVIVO_PC(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
			if(srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
				System.arraycopy(srcTaint, srcPos, destTaint, destPos, length);
		}

	}
	public static void arraycopyVM(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		VMSystem.arraycopy0(src, srcPos, dest, destPos, length);
		
//		if (VM.isBooted$$INVIVO_PC(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
//			if(srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
//		System.out.println(src);
//		System.out.println(srcTaint);
		if(srcTaint != null && destTaint != null && srcTaint.getClass() == destTaint.getClass())
			VMSystem.arraycopy0(srcTaint, srcPos, destTaint, destPos, length);
//		}

	}
	static int bar;
	static void truep()
	{
		bar++;
	}
	static void falsep()
	{
		bar++;
	}
//	public static void arraycopyHarmony(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
//		if(!src.getClass().isArray())
//		{
//			VMMemoryManager.arrayCopy(((MultiDTaintedArray)src).getVal(), srcPos, ((MultiDTaintedArray)dest).getVal(), destPos, length);
//			VMMemoryManager.arrayCopy(((MultiDTaintedArray)src).taint, srcPos, ((MultiDTaintedArray)dest).taint, destPos, length);
//		}
//		else
//		{
//			VMMemoryManager.arrayCopy(src, srcPos, dest, destPos, length);
//		}
////		dest = src;
//	}
//	public static void arraycopyHarmony(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
////		System.err.println("OK");
//		VMMemoryManager.arrayCopy(src, srcPos, dest, destPos, length);
////		System.arraycopy(src, srcPos, dest, destPos, length);
////		dest = src;
////		if (VM.isBooted$$INVIVO_PC(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
////			if(srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
////		System.out.println(src);
////		System.out.println(srcTaint);
//		if(srcTaint != null && destTaint != null && srcTaint.getClass() == destTaint.getClass())
//			VMMemoryManager.arrayCopy(srcTaint, srcPos, destTaint, destPos, length);
////		}
//
//	}
	public static Object getShadowTaintTypeForFrame(String typeDesc) {
		Type t = Type.getType(typeDesc);
		if (t.getSort() == Type.OBJECT || t.getSort() == Type.VOID)
			return null;
		if(t.getSort() == Type.ARRAY && t.getDimensions() > 1)
			return null;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
			return typeDesc.substring(0, typeDesc.length() - 1) + "I";
		if (t.getSort() == Type.ARRAY)
			return null;
		return Opcodes.INTEGER;
	}
	public static String getShadowTaintType(String typeDesc) {
		Type t = Type.getType(typeDesc);
		if (t.getSort() == Type.OBJECT || t.getSort() == Type.VOID)
			return null;
		if(t.getSort() == Type.ARRAY && t.getDimensions() > 1)
			return null;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
			return typeDesc.substring(0, typeDesc.length() - 1) + "I";
		if (t.getSort() == Type.ARRAY)
			return null;
		return "I";
	}

	public static Type getContainerReturnType(String originalReturnType) {
		return getContainerReturnType(Type.getType(originalReturnType));
	}

	public static Type getContainerReturnType(Type originalReturnType) {
		switch (originalReturnType.getSort()) {
		case Type.BYTE:
			return Type.getType(TaintedByte.class);
		case Type.BOOLEAN:
			return Type.getType(TaintedBoolean.class);
		case Type.CHAR:
			return Type.getType(TaintedChar.class);
		case Type.DOUBLE:
			return Type.getType(TaintedDouble.class);
		case Type.FLOAT:
			return Type.getType(TaintedFloat.class);
		case Type.INT:
			return Type.getType(TaintedInt.class);
		case Type.LONG:
			return Type.getType(TaintedLong.class);
		case Type.SHORT:
			return Type.getType(TaintedShort.class);
		case Type.ARRAY:
			if (originalReturnType.getDimensions() > 1)
			{
				
				switch (originalReturnType.getElementType().getSort()) {
				case Type.BYTE:
				case Type.BOOLEAN:
				case Type.CHAR:
				case Type.DOUBLE:
				case Type.FLOAT:
				case Type.INT:
				case Type.LONG:
				case Type.SHORT:
					return MultiDTaintedArray.getTypeForType(originalReturnType);
				case Type.OBJECT:
					return originalReturnType;
				}
			}
			switch (originalReturnType.getElementType().getSort()) {
			case Type.OBJECT:
				return originalReturnType;
			case Type.BYTE:
				return Type.getType(TaintedByteArray.class);
			case Type.BOOLEAN:
				return Type.getType(TaintedBooleanArray.class);
			case Type.CHAR:
				return Type.getType(TaintedCharArray.class);
			case Type.DOUBLE:
				return Type.getType(TaintedDoubleArray.class);
			case Type.FLOAT:
				return Type.getType(TaintedFloatArray.class);
			case Type.INT:
				return Type.getType(TaintedIntArray.class);
			case Type.LONG:
				return Type.getType(TaintedLongArray.class);
			case Type.SHORT:
				return Type.getType(TaintedShortArray.class);
			default:
				return Type.getType("[" + getContainerReturnType(originalReturnType.getElementType()).getDescriptor());
			}
		default:
			return originalReturnType;
		}
	}

	public static String remapMethodDesc(String desc) {
		String r = "(";
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if(t.getSort() == Type.ARRAY && t.getElementType().getSort()!= Type.OBJECT && t.getDimensions() > 1)
			{
				r += MultiDTaintedArray.getTypeForType(t);
			}
			else
				r += t;
		}
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static Object getStackTypeForType(Type t)
	{
		switch(t.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			return t.getInternalName();
		case Type.BYTE:
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.SHORT:
		case Type.INT:
			return Opcodes.INTEGER;
		case Type.DOUBLE:
			return Opcodes.DOUBLE;
		case Type.FLOAT:
			return Opcodes.FLOAT;
		case Type.LONG:
			return Opcodes.LONG;

			default:
				throw new IllegalArgumentException("Got: "+t);
		}
	}
}
