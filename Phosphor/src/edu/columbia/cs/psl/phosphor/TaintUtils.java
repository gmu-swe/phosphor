package edu.columbia.cs.psl.phosphor;

import java.lang.reflect.Array;
import java.lang.reflect.Method;


import sun.misc.VM;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class TaintUtils {
	static Object lock = new Object();

	public static final boolean TAINT_THROUGH_SERIALIZATION = false;
	public static final boolean OPT_PURE_METHODS = false;
	public static final boolean GENERATE_FASTPATH_VERSIONS = false;

	public static final boolean OPT_IGNORE_EXTRA_TAINTS = true;

	public static final boolean OPT_USE_STACK_ONLY = false; //avoid using LVs where possible if true
	
	public static final int RAW_INSN = 201;
	public static final int NO_TAINT_STORE_INSN = 202;
	public static final int IGNORE_EVERYTHING = 203;
	public static final int NO_TAINT_UNBOX = 204;
	public static final int DONT_LOAD_TAINT = 205;
	public static final int GENERATETAINT = 206;

	public static final int NEXTLOAD_IS_TAINTED = 207;
	public static final int NEXTLOAD_IS_NOT_TAINTED = 208;
	public static final int NEVER_AUTOBOX = 209;
	public static final int ALWAYS_AUTOBOX = 210;
	public static final int ALWAYS_BOX_JUMP = 211;
	public static final int ALWAYS_UNBOX_JUMP = 212;
	public static final int IS_TMP_STORE = 213;
	
	public static final int BRANCH_START = 214;
	public static final int BRANCH_END = 215;
	public static final int FORCE_CTRL_STORE = 216;

	public static final int FOLLOWED_BY_FRAME = 217;
	public static final int CUSTOM_SIGNAL_1 = 218;
	public static final int CUSTOM_SIGNAL_2 = 219;
	public static final int CUSTOM_SIGNAL_3 = 220;
	
	public static final String TAINT_FIELD = "PHOSPHOR_TAG";
//	public static final String HAS_TAINT_FIELD = "INVIVO_IS_TAINTED";
//	public static final String IS_TAINT_SEATCHING_FIELD = "INVIVO_IS_TAINT_SEARCHING";

	public static final String METHOD_SUFFIX = "$$PHOSPHORTAGGED";
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
	public static int nextTaintPHOSPHOR_TAG = 0;

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
				if(TaintedPrimitiveWithIntTag.class.isAssignableFrom(c) || TaintedPrimitiveArrayWithIntTag.class.isAssignableFrom(c))
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


	public static int getTaintInt(Object obj) {
		if(obj == null)
			return 0;
		if (obj instanceof TaintedWithIntTag) {
			return ((TaintedWithIntTag) obj).getPHOSPHOR_TAG();
		}
		else if(obj instanceof int[])
		{
			int ret = 0;
			for(int i : ((int[])obj))
			{
				ret |=i;
			}
			return ret;
		}
		else if(obj instanceof Object[])
		{
			int ret = 0;
			for(Object o : ((Object[]) obj))
				ret |= getTaintInt(o);
			return ret;
		}
		else if(obj instanceof MultiDTaintedArrayWithIntTag)
		{
			int ret = 0;
			for(int i : ((MultiDTaintedArrayWithIntTag) obj).taint)
				ret |= i;
			return ret;
		}
//		if(BoxedPrimitiveStoreWithIntTags.tags.containsKey(obj))
//			return BoxedPrimitiveStoreWithIntTags.tags.get(obj);
		return 0;
	}

	public static Object getTaintObj(Object obj) {
		if(obj == null || Taint.IGNORE_TAINTING)
			return null;
		if (obj instanceof TaintedWithObjTag) {
			return ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		}
		else if(ArrayHelper.engaged == 1)
		{
			return ArrayHelper.getTag(obj);
		}
		else if(obj instanceof Taint[])
		{
			Taint ret = new Taint();
			for(Taint t: ((Taint[]) obj))
			{
				if(t != null)
					ret.addDependency(t);
			}
			if(ret.hasNoDependencies())
				return null;
			return ret;
		}
		else if(obj instanceof MultiDTaintedArrayWithObjTag)
		{
			Taint ret = new Taint();
			for(Object t: ((MultiDTaintedArrayWithObjTag) obj).taint)
			{
				if(t != null)
					ret.addDependency((Taint) t);
			}
			if(ret.hasNoDependencies())
				return null;
			return ret;
		}
		else if(obj instanceof Object[])
		{
			Taint ret = new Taint();
			for(Object o : (Object[]) obj)
			{
				Taint t = (Taint) getTaintObj(o);
				if(t != null)
					ret.addDependency(t);
			}
			if(ret.hasNoDependencies())
				return null;
			return ret;
		}
//		if(BoxedPrimitiveStoreWithObjTags.tags.containsKey(obj))
//			return BoxedPrimitiveStoreWithObjTags.tags.get(obj);
		return null;
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
	public static int OKtoDebugPHOSPHOR_TAG;

	public static void arraycopy(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		if(!src.getClass().isArray())
		{
			System.arraycopy(((MultiDTaintedArrayWithIntTag)src).getVal(), srcPos, ((MultiDTaintedArrayWithIntTag)dest).getVal(), destPos, length);
			System.arraycopy(((MultiDTaintedArrayWithIntTag)src).taint, srcPos, ((MultiDTaintedArrayWithIntTag)dest).taint, destPos, length);
		}
		else
			System.arraycopy(src, srcPos, dest, destPos, length);
	}
	
	public static void arraycopy(Object src, Object srcPosTaint, int srcPos, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		if(!src.getClass().isArray())
		{
			System.arraycopy(((MultiDTaintedArrayWithObjTag)src).getVal(), srcPos, ((MultiDTaintedArrayWithObjTag)dest).getVal(), destPos, length);
			System.arraycopy(((MultiDTaintedArrayWithObjTag)src).taint, srcPos, ((MultiDTaintedArrayWithObjTag)dest).taint, destPos, length);
		}
		else
			System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopyVM(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		if(!src.getClass().isArray())
		{
			VMSystem.arraycopy0(((MultiDTaintedArrayWithIntTag)src).getVal(), srcPos, ((MultiDTaintedArrayWithIntTag)dest).getVal(), destPos, length);
			VMSystem.arraycopy0(((MultiDTaintedArrayWithIntTag)src).taint, srcPos, ((MultiDTaintedArrayWithIntTag)dest).taint, destPos, length);
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
		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBooleanWithIntTag()).val && srcTaint != null && destTaint != null) {
			if (srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
				System.arraycopy(srcTaint, srcPos, destTaint, destPos, length);
		}
	}
	public static void arraycopyControlTrack(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		if (VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithIntTag()).val && srcTaint != null && destTaint != null) {
			if (srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
				System.arraycopy(srcTaint, srcPos, destTaint, destPos, length);
		}
	}
	
	public static void arraycopy(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null) {
			if (srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
				System.arraycopy(srcTaint, srcPos, destTaint, destPos, length);
		}
	}
	public static void arraycopyControlTrack(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		if (VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null) {
			if (srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
				System.arraycopy(srcTaint, srcPos, destTaint, destPos, length);
		}
	}
	
	public static void arraycopyVM(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		VMSystem.arraycopy0(src, srcPos, dest, destPos, length);
		
//		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
//			if(srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
//		System.out.println(src);
//		System.out.println(srcTaint);
		if(srcTaint != null && destTaint != null && srcTaint.getClass() == destTaint.getClass())
			VMSystem.arraycopy0(srcTaint, srcPos, destTaint, destPos, length);
//		}

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
////		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
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
			return Configuration.TAINT_TAG_ARRAY_STACK_TYPE;
		if (t.getSort() == Type.ARRAY)
			return null;
		return Configuration.TAINT_TAG_STACK_TYPE;
	}
	public static String getShadowTaintType(String typeDesc) {
		Type t = Type.getType(typeDesc);
		if (t.getSort() == Type.OBJECT || t.getSort() == Type.VOID)
			return null;
		if(t.getSort() == Type.ARRAY && t.getDimensions() > 1)
			return null;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
			return Configuration.TAINT_TAG_ARRAYDESC;
		if (t.getSort() == Type.ARRAY)
			return null;
		return Configuration.TAINT_TAG_DESC;
	}

	public static Type getContainerReturnType(String originalReturnType) {
		return getContainerReturnType(Type.getType(originalReturnType));
	}

	public static Type getContainerReturnType(Type originalReturnType) {
		if(!Configuration.MULTI_TAINTING)
		{
			switch (originalReturnType.getSort()) {
			case Type.BYTE:
				return Type.getType(TaintedByteWithIntTag.class);
			case Type.BOOLEAN:
				return Type.getType(TaintedBooleanWithIntTag.class);
			case Type.CHAR:
				return Type.getType(TaintedCharWithIntTag.class);
			case Type.DOUBLE:
				return Type.getType(TaintedDoubleWithIntTag.class);
			case Type.FLOAT:
				return Type.getType(TaintedFloatWithIntTag.class);
			case Type.INT:
				return Type.getType(TaintedIntWithIntTag.class);
			case Type.LONG:
				return Type.getType(TaintedLongWithIntTag.class);
			case Type.SHORT:
				return Type.getType(TaintedShortWithIntTag.class);
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
						return MultiDTaintedArrayWithIntTag.getTypeForType(originalReturnType);
					case Type.OBJECT:
						return originalReturnType;
					}
				}
				switch (originalReturnType.getElementType().getSort()) {
				case Type.OBJECT:
					return originalReturnType;
				case Type.BYTE:
					return Type.getType(TaintedByteArrayWithIntTag.class);
				case Type.BOOLEAN:
					return Type.getType(TaintedBooleanArrayWithIntTag.class);
				case Type.CHAR:
					return Type.getType(TaintedCharArrayWithIntTag.class);
				case Type.DOUBLE:
					return Type.getType(TaintedDoubleArrayWithIntTag.class);
				case Type.FLOAT:
					return Type.getType(TaintedFloatArrayWithIntTag.class);
				case Type.INT:
					return Type.getType(TaintedIntArrayWithIntTag.class);
				case Type.LONG:
					return Type.getType(TaintedLongArrayWithIntTag.class);
				case Type.SHORT:
					return Type.getType(TaintedShortArrayWithIntTag.class);
				default:
					return Type.getType("[" + getContainerReturnType(originalReturnType.getElementType()).getDescriptor());
				}
			default:
				return originalReturnType;
			}
		}
		else
		{
			switch (originalReturnType.getSort()) {
			case Type.BYTE:
				return Type.getType(TaintedByteWithObjTag.class);
			case Type.BOOLEAN:
				return Type.getType(TaintedBooleanWithObjTag.class);
			case Type.CHAR:
				return Type.getType(TaintedCharWithObjTag.class);
			case Type.DOUBLE:
				return Type.getType(TaintedDoubleWithObjTag.class);
			case Type.FLOAT:
				return Type.getType(TaintedFloatWithObjTag.class);
			case Type.INT:
				return Type.getType(TaintedIntWithObjTag.class);
			case Type.LONG:
				return Type.getType(TaintedLongWithObjTag.class);
			case Type.SHORT:
				return Type.getType(TaintedShortWithObjTag.class);
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
						return MultiDTaintedArrayWithObjTag.getTypeForType(originalReturnType);
					case Type.OBJECT:
						return originalReturnType;
					}
				}
				switch (originalReturnType.getElementType().getSort()) {
				case Type.OBJECT:
					return originalReturnType;
				case Type.BYTE:
					return Type.getType(TaintedByteArrayWithObjTag.class);
				case Type.BOOLEAN:
					return Type.getType(TaintedBooleanArrayWithObjTag.class);
				case Type.CHAR:
					return Type.getType(TaintedCharArrayWithObjTag.class);
				case Type.DOUBLE:
					return Type.getType(TaintedDoubleArrayWithObjTag.class);
				case Type.FLOAT:
					return Type.getType(TaintedFloatArrayWithObjTag.class);
				case Type.INT:
					return Type.getType(TaintedIntArrayWithObjTag.class);
				case Type.LONG:
					return Type.getType(TaintedLongArrayWithObjTag.class);
				case Type.SHORT:
					return Type.getType(TaintedShortArrayWithObjTag.class);
				default:
					return Type.getType("[" + getContainerReturnType(originalReturnType.getElementType()).getDescriptor());
				}
			default:
				return originalReturnType;
			}
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
		if(Configuration.IMPLICIT_TRACKING)
			r += Type.getDescriptor(ControlTaintTagStack.class);
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


	public static Object[] newTaintArray(int len)
	{
		return (Object[]) Array.newInstance(Configuration.TAINT_TAG_OBJ_CLASS, len);
	}
	private static <T> T shallowClone(T obj)
	{
		try{
			Method m =  obj.getClass().getDeclaredMethod("clone");
			m.setAccessible(true);
			return (T) m.invoke(obj);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name) {
		T ret = Enum.valueOf(enumType, name);
		if (name instanceof TaintedWithIntTag) {
			int tag = ((TaintedWithIntTag) name).getPHOSPHOR_TAG();
			if (tag != 0) {
				ret = shallowClone(ret);
				((TaintedWithIntTag) ret).setPHOSPHOR_TAG(tag);
			}
		} else if (name instanceof TaintedWithObjTag) {
			Object tag = ((TaintedWithObjTag) name).getPHOSPHOR_TAG();
			if (tag != null) {
				ret = shallowClone(ret);
				((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
			}
		}
		return ret;
	}
	public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name, ControlTaintTagStack ctrl) {
		T ret = Enum.valueOf(enumType, name);
		Taint tag = (Taint) ((TaintedWithObjTag) name).getPHOSPHOR_TAG();
		tag = Taint.combineTags(tag, ctrl);
		if (tag != null && !(tag.getLabel() == null && tag.hasNoDependencies())) {
			ret = shallowClone(ret);
			((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
		}
		return ret;
	}

	public static Object ensureUnboxed(Object o)
	{
		if(o instanceof MultiDTaintedArrayWithIntTag)
			return ((MultiDTaintedArrayWithIntTag) o).getVal();
		else if(o instanceof MultiDTaintedArrayWithObjTag)
			return ((MultiDTaintedArrayWithObjTag) o).getVal();
		else if(o instanceof Enum<?>)
			return ((Enum) o).valueOf(((Enum) o).getDeclaringClass(), ((Enum) o).name());
		return o;
	}
}
