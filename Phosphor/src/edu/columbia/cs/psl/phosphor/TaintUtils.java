package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.SignatureReWriter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import sun.misc.VM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TaintUtils {
	public static final boolean OPT_PURE_METHODS = false;
	public static final boolean OPT_USE_STACK_ONLY = false; //avoid using LVs where possible if true
	public static final int RAW_INSN = 201;
	public static final int NO_TAINT_STORE_INSN = 202;
	public static final int IGNORE_EVERYTHING = 203;
	public static final int TRACKED_LOAD = 204;
	public static final int DUP_TAINT_AT_0 = 205;
	public static final int DUP_TAINT_AT_1 = 206;
	public static final int DUP_TAINT_AT_2 = 207;
	public static final int DUP_TAINT_AT_3 = 208;
	public static final int NEVER_AUTOBOX = 209;
	public static final int ALWAYS_AUTOBOX = 210;
	public static final int ALWAYS_BOX_JUMP = 211;
	public static final int ALWAYS_UNBOX_JUMP = 212;
	public static final int IS_TMP_STORE = 213;
	public static final int BRANCH_START = 214;
	public static final int BRANCH_END = 215;
	public static final int FORCE_CTRL_STORE = 216;
	public static final int FORCE_CTRL_STORE_WIDE = 217;
	public static final int EXCEPTION_HANDLER_START = 205;
	public static final int EXCEPTION_HANDLER_END = 206;
	public static final int UNTHROWN_EXCEPTION = 215; //When we are returning but might have otherwise thrown some exception
	public static final int UNTHROWN_EXCEPTION_CHECK = 214; //When we are returning from a method and are covered directly by a "try"
	public static final int FORCE_CTRL_STORE_SFIELD = 217;
	public static final int FOLLOWED_BY_FRAME = 217;
	public static final int CUSTOM_SIGNAL_1 = 218;
	public static final int CUSTOM_SIGNAL_2 = 219;
	public static final int CUSTOM_SIGNAL_3 = 220;
	public static final int LOOP_HEADER = 221;
	public static final String TAINT_FIELD = "PHOSPHOR_TAG";
	public static final String METHOD_SUFFIX = "$$PHOSPHORTAGGED";
	public static final String ADDED_SVUID_SENTINEL = "$$PHOSPHOR_REMOVE_SVUID";
//	public static final String HAS_TAINT_FIELD = "INVIVO_IS_TAINTED";
//	public static final String IS_TAINT_SEATCHING_FIELD = "INVIVO_IS_TAINT_SEARCHING";
	public static final boolean DEBUG_ALL = false;
	public static final boolean DEBUG_DUPSWAP = DEBUG_ALL;
	public static final boolean DEBUG_FRAMES = DEBUG_ALL;
	public static final boolean DEBUG_FIELDS = DEBUG_ALL;
	public static final boolean DEBUG_LOCAL = DEBUG_ALL;
	public static final boolean DEBUG_CALLS = DEBUG_ALL;
	public static final boolean DEBUG_OPT = false;
	public static final boolean DEBUG_PURE = false;
	public static final boolean ADD_BASIC_ARRAY_CONSTRAINTS = true;
	public static final boolean ADD_HEAVYWEIGHT_ARRAY_TAINTS = ADD_BASIC_ARRAY_CONSTRAINTS;
	public static final int MAX_CONCURRENT_BRANCHES = 500;
	public static final String STR_LDC_WRAPPER = "INVIVO_LDC_STR";
	public static final int UNCONSTRAINED_NEW_STRING = 4;
	public static boolean VERIFY_CLASS_GENERATION = false;
	public static final String METHOD_SUFFIX_UNINST = "$$PHOSPHORUNTAGGED";
	public static int nextTaint = 0;
	public static int nextTaintPHOSPHOR_TAG = 0;
	public static int nextMethodId = 0;
	public static boolean OKtoDebug = false;
	public static int OKtoDebugPHOSPHOR_TAG;
	public static boolean weakHashMapInitialized = false;
	static Object lock = new Object();
	/*
	 * Start: Conversion of method signature from doop format to bytecode format
	 * This can't be here.
	 */
	private static Map<String, String> typeToSymbol = null;

	/*
	private static Map<String, String> typeToSymbol = new HashMap<String, String>();

	static {
		typeToSymbol.put("byte", "B");
		typeToSymbol.put("char", "C");
		typeToSymbol.put("double", "D");
		typeToSymbol.put("float", "F");
		typeToSymbol.put("int", "I");
		typeToSymbol.put("long", "J");
		typeToSymbol.put("short", "S");
		typeToSymbol.put("void", "V");
		typeToSymbol.put("boolean", "Z");
	}	*/
	private static final String processSingleType(String in) {
		if (in.equals("byte"))
			return "B";
		else if (in.equals("char"))
			return "C";
		else if (in.equals("double"))
			return "D";
		else if (in.equals("float"))
			return "F";
		else if (in.equals("int"))
			return "I";
		else if (in.equals("long"))
			return "J";
		else if (in.equals("short"))
			return "S";
		else if (in.equals("void"))
			return "V";
		else if (in.equals("boolean"))
			return "Z";
		return "L" + in.replace('.', '/') + ";";
	}

	private static String processType(String type) {
		StringBuilder typeBuffer = new StringBuilder();
		type = type.trim();
		int firstBracket = type.indexOf('[');
		if (firstBracket >= 0) {
			for (int i = firstBracket; i < type.length(); i += 2)
				typeBuffer.append("[");
			type = type.substring(0, firstBracket);
			typeBuffer.append(processSingleType(type));
		} else
			typeBuffer.append(processSingleType(type));
		return typeBuffer.toString();
	}

	private static String processReverse(String type) {
		type = type.trim();
		if (type.length() == 1) {
			for (Entry<String, String> s : typeToSymbol.entrySet())
				if (s.getValue().equals(type))
					return s.getKey();
			throw new IllegalArgumentException("Invalid type string");
		}

		if (type.startsWith("[")) {
			// is an array
			int idx = 0;
			String suffix = "";
			while (type.charAt(idx) == '[') {
				idx++;
				suffix = suffix + "[]";
			}
			return processReverse(type.substring(idx)) + suffix;

		} else {
			type = type.replaceAll("/", ".");
			type = type.substring(1, type.length() - 1); //remove L and ;
			return type;
		}
	}

	/*
	 * End: Conversion of method signature from doop format to bytecode format
	 */

	public static MethodDescriptor getMethodDesc(String signature) {
		// get return type
		int idxOfColon = signature.indexOf(':');
		String temp = signature.substring(idxOfColon + 2);
		int nameStart = temp.indexOf(' ') + 1;
		int nameEnd = temp.indexOf('(');
		String owner = signature.substring(1, idxOfColon).replace('.', '/');
		String name = temp.substring(nameStart, nameEnd);

		String returnTypeSymbol = processType(temp.substring(0, temp.indexOf(" ")).trim());

		// get args list
		temp = temp.substring(nameEnd + 1, temp.length() - 2);
		StringBuilder argsBuffer = new StringBuilder();

		argsBuffer.append("(");
		if (temp != null && !temp.isEmpty()) {
			for (String arg : temp.split(","))
				argsBuffer.append(processType(arg.trim()));
		}
		argsBuffer.append(")");

		argsBuffer.append(returnTypeSymbol);
		return new MethodDescriptor(name, owner, argsBuffer.toString());
	}

	public static String getMethodDesc(MethodDescriptor desc) {
		String owner = desc.getOwner().replaceAll("/", ".");
		String methodName = desc.getName();
		String returnType = desc.getDesc().substring(desc.getDesc().indexOf(")") + 1);
		String actualReturnType = processReverse(returnType);
		String args = desc.getDesc().substring(desc.getDesc().indexOf("(") + 1, desc.getDesc().indexOf(")"));
		boolean noargs = (args.length() == 0);
		int idx = 0;
		List<String> arguments = new ArrayList<String>();
		while (args.length() > 0) {
			idx = 0;
			if (args.charAt(idx) == 'L') {
				arguments.add(processReverse(args.substring(idx, args.indexOf(";") + 1)));
				idx = args.indexOf(";") + 1;
			} else if (args.charAt(idx) == '[') {
				while (args.charAt(idx) == '[')
					idx++;
				if (args.charAt(idx) == 'L') {
					arguments.add(processReverse(args.substring(0, args.indexOf(";") + 1)));
					idx = args.indexOf(";") + 1;
				} else {
					arguments.add(processReverse(args.substring(0, idx + 1)));
					idx = idx + 1;
				}
			} else {
				arguments.add(processReverse(args.charAt(idx) + ""));
				idx = idx + 1;
			}
			args = args.substring(idx);
		}
		StringBuilder buf = new StringBuilder();
		buf.append("<").append(owner).append(": ").append(actualReturnType).append(" ").append(methodName).append("(");
		for (String s : arguments)
			buf.append(s).append(",");
		if (!noargs)
			buf.setLength(buf.length() - 1);
		buf.append(")>");
		return buf.toString();
	}

	public static void writeToFile(File file, String content) {
		FileOutputStream fop = null;

		try {
			fop = new FileOutputStream(file);
			if (!file.exists())
				file.createNewFile();
			byte[] contentInBytes = content.getBytes();
			fop.write(contentInBytes);
			fop.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isPreAllocReturnType(String methodDescriptor) {
		Type retType = Type.getReturnType(methodDescriptor);
		if (retType.getSort() == Type.OBJECT || retType.getSort() == Type.VOID)
			return false;
		else if (retType.getSort() == Type.ARRAY)
			return false;
		return true;
	}

	public static boolean isNotRealArg(Type t) {
		if (t.equals(Type.getType(TaintSentinel.class)))
			return true;
		if (t.equals(Type.getType(UninstrumentedTaintSentinel.class)))
			return true;
		if (t.getInternalName().startsWith("edu/columbia/cs/psl/")) {
			try {
				Class c = Class.forName(t.getInternalName().replace("/", "."));
				if (TaintedPrimitiveWithIntTag.class.isAssignableFrom(c))
					return true;
			} catch (ClassNotFoundException e) {
			}
		}
		return false;
	}

	/* Returns whether the specified type is for a taint sentinel. */
	public static boolean isTaintSentinel(Type type) {
		return type.equals(Type.getType(TaintSentinel.class)) || type.equals(Type.getType(UninstrumentedTaintSentinel.class));
	}

	/* Returns whether the specified method description contains a taint sentinel. */
	public static boolean containsTaintSentinel(String desc) {
		Type[] types = Type.getArgumentTypes(desc);
		for(Type type : types) {
			if(isTaintSentinel(type)) {
				return true;
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
		if (obj == null)
			return 0;
		if (obj instanceof TaintedWithIntTag) {
			return ((TaintedWithIntTag) obj).getPHOSPHOR_TAG();
		} else if (obj instanceof int[]) {
			int ret = 0;
			for (int i : ((int[]) obj)) {
				ret |= i;
			}
			return ret;
		}

		return 0;
	}

	public static Taint getTaintObj(Object obj) {
		if (obj == null || Taint.IGNORE_TAINTING)
			return null;
		if (obj instanceof TaintedWithObjTag) {
			return (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		} else if (ArrayHelper.engaged == 1) {
			return ArrayHelper.getTag(obj);
		} else if (obj instanceof Taint[]) {
			Taint ret = new Taint();
			for (Taint t : ((Taint[]) obj)) {
				if (t != null)
					ret.addDependency(t);
			}
			if (ret.hasNoDependencies())
				return null;
			return ret;
		} else if (obj instanceof LazyArrayObjTags) {
			return ((LazyArrayObjTags) obj).lengthTaint;
		}
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

	public static void arraycopy(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		try {
			if (src instanceof LazyArrayIntTags) {
				System.arraycopy(((LazyArrayIntTags) src).getVal(), srcPos, ((LazyArrayIntTags) dest).getVal(), destPos, length);
				if (((LazyArrayIntTags) src).taints != null) {
					if (((LazyArrayIntTags) dest).taints == null)
						((LazyArrayIntTags) dest).taints = new int[((LazyArrayIntTags) dest).getLength()];
					System.arraycopy(((LazyArrayIntTags) src).taints, srcPos, ((LazyArrayIntTags) dest).taints, destPos, length);
				}
//			} else if (!dest.getClass().isArray()) {
//				src is a regular array, dest is multidtaintedarraywithinttag
//				System.arraycopy(src, srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
			} else {
				System.arraycopy(src, srcPos, dest, destPos, length);
			}
		} catch (ArrayStoreException ex) {
			System.out.println("Src " + src);
			System.out.println(((Object[]) src)[0]);
			System.out.println("Dest " + dest);
			ex.printStackTrace();
			throw ex;
		}
	}

	public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
		try {
			if (!src.getClass().isArray() && !dest.getClass().isArray()) {
				System.arraycopy(((LazyArrayIntTags) src).getVal(), srcPos, ((LazyArrayIntTags) dest).getVal(), destPos, length);
				if (((LazyArrayIntTags) src).taints != null) {
					if (((LazyArrayIntTags) dest).taints == null) {
						((LazyArrayIntTags) dest).taints = new int[((LazyArrayIntTags) src).taints.length];
					}
					System.arraycopy(((LazyArrayIntTags) src).taints, srcPos, ((LazyArrayIntTags) dest).taints, destPos, length);
				}
			} else if (!dest.getClass().isArray()) {
				//src is a regular array, dest is multidtaintedarraywithinttag
				System.arraycopy(src, srcPos, ((LazyArrayIntTags) dest).getVal(), destPos, length);
			} else
				System.arraycopy(src, srcPos, dest, destPos, length);
		} catch (ArrayStoreException ex) {
			System.out.println("Src " + src);
			System.out.println(((Object[]) src)[0]);
			System.out.println("Dest " + dest);
			ex.printStackTrace();
			throw ex;
		}
	}

	public static void arraycopy(Object src, Object srcPosTaint, int srcPos, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		if (!src.getClass().isArray() && !dest.getClass().isArray()) {
			System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
			if (((LazyArrayObjTags) src).taints != null) {
				if (((LazyArrayObjTags) dest).taints == null) {
					((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).taints.length];
				}
				System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
			}
		} else if (!dest.getClass().isArray()) {
			System.arraycopy(src, srcPos, ((MultiDTaintedArrayWithObjTag) dest).getVal(), destPos, length);
		} else
			System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(Object src, Object srcPosTaint, int srcPos, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length, ControlTaintTagStack ctrl) {
		if (!src.getClass().isArray() && !dest.getClass().isArray()) {
			System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
			if (((LazyArrayObjTags) src).taints != null) {
				if (((LazyArrayObjTags) dest).taints == null) {
					((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).taints.length];
				}
				System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
			}
			if (!ctrl.isEmpty()) {
				if (((LazyArrayObjTags) dest).taints == null) {
					((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).getLength()];
				}
				Taint[] taints = ((LazyArrayObjTags) dest).taints;
				for (int i = 0; i < taints.length; i++) {
					if (taints[i] == null)
						taints[i] = ctrl.copyTag();
					else
						taints[i].addDependency(ctrl.getTag());
				}
			}
		} else if (!dest.getClass().isArray()) {
			System.arraycopy(src, srcPos, ((MultiDTaintedArrayWithObjTag) dest).getVal(), destPos, length);
		} else
			System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopyVM(Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		if (!src.getClass().isArray()) {
			VMSystem.arraycopy0(((LazyArrayIntTags) src).getVal(), srcPos, ((LazyArrayIntTags) dest).getVal(), destPos, length);
			VMSystem.arraycopy0(((LazyArrayIntTags) src).taints, srcPos, ((LazyArrayIntTags) dest).taints, destPos, length);
		} else
			VMSystem.arraycopy0(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		throw new ArrayStoreException("Can't copy from src with taint to dest w/o taint!");
	}

	public static void arraycopy(Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		throw new ArrayStoreException("Can't copy from src w/ no taint to dest w/ taint!!");
	}

	public static void arraycopy(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBooleanWithIntTag()).val && srcTaint != null && destTaint != null) {
			if (((LazyArrayIntTags) srcTaint).taints == null && ((LazyArrayIntTags) destTaint).taints == null) {
				return;
			} else if (((LazyArrayIntTags) srcTaint).taints != null) {
				if (((LazyArrayIntTags) destTaint).taints == null)
					((LazyArrayIntTags) destTaint).taints = new int[Array.getLength(dest)];
				System.arraycopy(((LazyArrayIntTags) srcTaint).taints, srcPos, ((LazyArrayIntTags) destTaint).taints, destPos, length);
			} else {
				if (((LazyArrayIntTags) destTaint).taints == null)
					((LazyArrayIntTags) destTaint).taints = new int[Array.getLength(dest)];
				else {
					int[] empty = new int[length];
					System.arraycopy(empty, 0, ((LazyArrayIntTags) destTaint).taints, destPos, length);
				}
			}
		}
	}

	public static void arraycopy(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBooleanWithObjTag()).val) {
			boolean srcTainted = (srcTaint != null && ((LazyArrayObjTags) srcTaint).taints != null);
			boolean dstTainted = (destTaint != null && ((LazyArrayObjTags) destTaint).taints != null);

			if (!srcTainted && !dstTainted) // Fast path: No taints to copy to/from
				return;

			if (!srcTainted && dstTainted) // Source not tainted, reset dest
			{
				for (int i = destPos; i < destPos + length; i++)
					((LazyArrayObjTags) destTaint).taints[i] = null;
			} else // Source tainted, copy taint over
			{
				if (((LazyArrayObjTags) destTaint).taints == null)
					((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
				System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
			}
		}
	}

	public static void arraycopyControlTrack(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
		try {
			System.arraycopy(src, srcPos, dest, destPos, length);
			if (VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null && ((LazyArrayObjTags) srcTaint).taints != null) {
				if (((LazyArrayObjTags) destTaint).taints == null)
					((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
				System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			Taint t = null;
			if (srcPosTaint != null) {
				t = ((Taint) srcPosTaint).copy();
				t.addDependency((Taint) destPosTaint);
				t.addDependency((Taint) lengthTaint);
			} else if (destPosTaint != null) {

				t = ((Taint) destPosTaint).copy();
				t.addDependency((Taint) lengthTaint);
			} else if (lengthTaint != null) {

				t = ((Taint) lengthTaint).copy();
			}
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(t);
			throw ex;
		}
	}

	public static void arraycopyControlTrack(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length, ControlTaintTagStack ctrl) {
		try {
			System.arraycopy(src, srcPos, dest, destPos, length);

			if (VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null && ((LazyArrayObjTags) srcTaint).taints != null) {
				if (((LazyArrayObjTags) destTaint).taints == null)
					((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
				System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
			}
			if (!ctrl.isEmpty()) {
				if (((LazyArrayObjTags) destTaint).taints == null)
					((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
				Taint[] taints = ((LazyArrayObjTags) destTaint).taints;
				for (int i = destPos; i < destPos + length; i++) {
					if (taints[i] == null)
						taints[i] = ctrl.copyTag();
					else {
						taints[i].addDependency(ctrl.getTag());
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			Taint t = null;
			if (srcPosTaint != null) {
				t = ((Taint) srcPosTaint).copy();
				t.addDependency((Taint) destPosTaint);
				t.addDependency((Taint) lengthTaint);
			} else if (destPosTaint != null) {

				t = ((Taint) destPosTaint).copy();
				t.addDependency((Taint) lengthTaint);
			} else if (lengthTaint != null) {

				t = ((Taint) lengthTaint).copy();
			}
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(t);
			throw ex;
		}
	}

	public static void arraycopyVM(Object srcTaint, Object src, int srcPosTaint, int srcPos, Object destTaint, Object dest, int destPosTaint, int destPos, int lengthTaint, int length) {
		VMSystem.arraycopy0(src, srcPos, dest, destPos, length);

//		if (VM.isBooted$$PHOSPHORTAGGED(new TaintedBoolean()).val && srcTaint != null && destTaint != null) {
//			if(srcPos == 0 && length <= Array.getLength(destTaint) && length <= Array.getLength(srcTaint))
//		System.out.println(src);
//		System.out.println(srcTaint);
		if (srcTaint != null && destTaint != null && srcTaint.getClass() == destTaint.getClass())
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
	public static Object getShadowTaintTypeForFrame(Type t) {
		if (t.getSort() == Type.OBJECT || t.getSort() == Type.VOID)
			return null;
		if (t.getSort() == Type.ARRAY && t.getDimensions() > 1)
			return null;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
			return MultiDTaintedArray.getTypeForType(t).getInternalName();
		if (t.getSort() == Type.ARRAY)
			return null;
		return Configuration.TAINT_TAG_STACK_TYPE;
	}
	public static Object getShadowTaintTypeForFrame(String typeDesc) {
		return getShadowTaintTypeForFrame(Type.getType(typeDesc));
	}

	public static String getShadowTaintType(String typeDesc) {
		Type t = Type.getType(typeDesc);
		if (t.getSort() == Type.OBJECT || t.getSort() == Type.VOID)
			return null;
		if (t.getSort() == Type.ARRAY && t.getDimensions() > 1)
			return null;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
			return MultiDTaintedArray.getTypeForType(Type.getType(typeDesc)).getDescriptor();
		if (t.getSort() == Type.ARRAY)
			return null;
		return Configuration.TAINT_TAG_DESC;
	}

	public static Type getContainerReturnType(String originalReturnType) {
		return getContainerReturnType(Type.getType(originalReturnType));
	}

	public static Type getContainerReturnType(Type originalReturnType) {
		if (!Configuration.MULTI_TAINTING) {
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
				default:
					return originalReturnType;
			}
		} else {
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
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
			r += Type.getDescriptor(ControlTaintTagStack.class);
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static String remapMethodDescWithoutAddingNewArgs(String desc) {
		String r = "(";
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static String remapMethodDescAndIncludeReturnHolder(String desc) {
		String r = "(";
		boolean ctrlAdded = !(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING);
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if(!ctrlAdded && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted"))
			{
				ctrlAdded = true;
				r += Type.getDescriptor(ControlTaintTagStack.class);
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		if (!ctrlAdded)
			r += Type.getDescriptor(ControlTaintTagStack.class);
		if (isPrimitiveType(Type.getReturnType(desc)))
			r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static String remapMethodDescAndIncludeReturnHolderPrimitiveReturnBoxing(String desc) {
		String r = "(";
		boolean ctrlAdded = !(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING);
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if(!ctrlAdded && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted"))
			{
				ctrlAdded = true;
				r += Type.getDescriptor(ControlTaintTagStack.class);
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		if (!ctrlAdded)
			r += Type.getDescriptor(ControlTaintTagStack.class);
		r += ")";
		if (isPrimitiveArrayType(Type.getReturnType(desc)))
			r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		else if (isPrimitiveType(Type.getReturnType(desc)))
			switch (Type.getReturnType(desc).getSort()) {
				case Type.CHAR:
					r += "Ljava/lang/Character;";
					break;
				case Type.BOOLEAN:
					r += "Ljava/lang/Boolean;";
					break;
				case Type.DOUBLE:
					r += "Ljava/lang/Double;";
					break;
				case Type.FLOAT:
					r += "Ljava/lang/Float;";
					break;
				case Type.LONG:
					r += "Ljava/lang/Long;";
					break;
				case Type.INT:
					r += "Ljava/lang/Integer;";
					break;
				case Type.SHORT:
					r += "Ljava/lang/Short;";
					break;
				case Type.BYTE:
					r += "Ljava/lang/Byte;";
			}
		else
			r += Type.getReturnType(desc).getDescriptor();
		return r;
	}

	public static String remapMethodDescAndIncludeReturnHolderInit(String desc) {
		String r = "(";
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
			r += Type.getDescriptor(ControlTaintTagStack.class);
		r += Type.getDescriptor(TaintSentinel.class);
		if (isPrimitiveType(Type.getReturnType(desc)))
			r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static String remapMethodDescAndIncludeReturnHolderNoControlStack(String desc) {
		String r = "(";
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				if (t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1)
					r += getShadowTaintType(t.getDescriptor());
			} else if (t.getSort() != Type.OBJECT) {
				r += getShadowTaintType(t.getDescriptor());
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		if (isPrimitiveType(Type.getReturnType(desc)))
			r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
		return r;
	}

	public static String remapMethodDescForUninst(String desc) {
		String r = "(";
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
				r += MultiDTaintedArray.getTypeForType(t);
			} else
				r += t;
		}
		Type ret = Type.getReturnType(desc);
		if (ret.getSort() == Type.ARRAY && ret.getDimensions() > 1 && ret.getElementType().getSort() != Type.OBJECT)
			r += ")" + MultiDTaintedArrayWithIntTag.getTypeForType(ret).getDescriptor();
		else
			r += ")" + ret.getDescriptor();
		return r;
	}

	public static Object getStackTypeForType(Type t) {
		if (t == null)
			return Opcodes.NULL;
		switch (t.getSort()) {
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
				throw new IllegalArgumentException("Got: " + t);
		}
	}


	public static Object[] newTaintArray(int len) {
		return (Object[]) Array.newInstance(Configuration.TAINT_TAG_OBJ_CLASS, len);
	}

	private static <T> T shallowClone(T obj) {
		try {
			Method m = obj.getClass().getDeclaredMethod("clone");
			m.setAccessible(true);
			return (T) m.invoke(obj);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name) {
		T ret = Enum.valueOf(enumType, name);
		if (((Object) name) instanceof TaintedWithIntTag) {
			int tag = ((TaintedWithIntTag) ((Object) name)).getPHOSPHOR_TAG();
			if (tag != 0) {
				ret = shallowClone(ret);
				((TaintedWithIntTag) ret).setPHOSPHOR_TAG(tag);
			}
		} else if (((Object) name) instanceof TaintedWithObjTag) {
			Object tag = ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
			if (tag != null) {
				ret = shallowClone(ret);
				((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
			}
		}
		return ret;
	}

	public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name, ControlTaintTagStack ctrl) {
		T ret = Enum.valueOf(enumType, name);
		Taint tag = (Taint) ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
		tag = Taint.combineTags(tag, ctrl);
		if (tag != null && !(tag.getLabel() == null && tag.hasNoDependencies())) {
			ret = shallowClone(ret);
			((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
		}
		return ret;
	}

	public static Object ensureUnboxed(Object o) {
		if (!Configuration.MULTI_TAINTING && o instanceof LazyArrayIntTags)
			return ((LazyArrayIntTags) o).getVal();
		else if (Configuration.MULTI_TAINTING && o instanceof LazyArrayObjTags)
			return ((LazyArrayObjTags) o).getVal();
		else if (Configuration.WITH_ENUM_BY_VAL && o instanceof Enum<?>)
			return ((Enum) o).valueOf(((Enum) o).getDeclaringClass(), ((Enum) o).name());
		return o;
	}

	public static boolean isPrimitiveArrayType(Type t) {
		return t != null && t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT;
	}

	public static boolean isPrimitiveType(Type t) {
		return t != null && t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT && t.getSort() != Type.VOID;
	}

	public static boolean isPrimitiveOrPrimitiveArrayType(Type t) {
		return isPrimitiveArrayType(t) || isPrimitiveType(t);
	}

	public static void main(String[] args) {
		LinkedList<String> lst = new LinkedList<>();
		System.out.println(remapSignature("(Ljava/util/stream/AbstractPipeline<TP_OUT;TP_OUT;*>;Ljava/util/stream/PipelineHelper<TP_OUT;>;Ljava/util/Spliterator<TP_IN;>;Ljava/util/function/IntFunction<[TP_OUT;>;JJ)V", lst));
	}

	public static String remapSignature(String sig, final List<String> extraArgs) {
		if (sig == null)
			return null;
//		System.out.println(sig);
		SignatureReWriter sw = new SignatureReWriter() {
			int isInArray = 0;
			boolean isInReturnType;
			boolean isInParam;

			@Override
			public SignatureVisitor visitArrayType() {
				isInArray++;
				return super.visitArrayType();
			}

			@Override
			public void visitBaseType(char descriptor) {
				if (descriptor == 'V') {
					super.visitBaseType(descriptor);
					return;
				}
				if (isInParam) {
					if (isInArray == 0) {
						if (Configuration.MULTI_TAINTING) {
							super.visitClassType(Configuration.TAINT_TAG_INTERNAL_NAME);
							super.visitEnd();
						} else
							super.visitBaseType('I');
						super.visitParameterType();
						super.visitBaseType(descriptor);
					} else if (isInArray == 1) {
						super.pop();
						super.visitClassType(MultiDTaintedArray.getTypeForType(Type.getType("[" + descriptor)).getInternalName());
						super.visitEnd();
						super.visitArrayType();
						super.visitParameterType();
						super.visitBaseType(descriptor);
					} else {
						super.pop();
						super.visitClassType(MultiDTaintedArray.getTypeForType(Type.getType("[" + descriptor)).getInternalName());
						super.visitEnd();
					}
				} else {
					if (isInArray > 0) {
						super.pop();//reduce dimensions by 1
						super.visitClassType(TaintUtils.getContainerReturnType("[" + descriptor).getInternalName());
						super.visitEnd();
					} else {
						super.visitClassType(TaintUtils.getContainerReturnType("" + descriptor).getInternalName());
						super.visitEnd();
					}
				}
				isInParam = false;
				isInArray = 0;
			}

			@Override
			public SignatureVisitor visitReturnType() {
				//Add in extra stuff as needed.
				for (String s : extraArgs) {
					super.visitParameterType();
					super.visitClassType(s);
					super.visitEnd();
				}
				isInReturnType = true;
				return super.visitReturnType();
			}

			@Override
			public void visitTypeVariable(String name) {
				isInParam = false;
				isInArray = 0;
				super.visitTypeVariable(name);
			}

			@Override
			public void visitClassType(String name) {
				isInArray = 0;
				isInParam = false;
				super.visitClassType(name);
			}

			@Override
			public SignatureVisitor visitParameterType() {
				isInParam = true;
				return super.visitParameterType();
			}
		};
		SignatureReader sr = new SignatureReader(sig);
		sr.accept(sw);
		sig = sw.toString();
//		System.out.println(">"+sig);
		return sig;
	}

	/* Returns whether the specified opcode is for a return instruction. */
	public static boolean isReturnOpcode(int opcode) {
		switch(opcode) {
			case Opcodes.ARETURN:
			case Opcodes.IRETURN:
			case Opcodes.RETURN:
			case Opcodes.DRETURN:
			case Opcodes.FRETURN:
			case Opcodes.LRETURN:
				return true;
			default:
				return false;
		}
	}
}
