package edu.columbia.cs.psl.phosphor;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;

public abstract class SourceSinkManager {
	public abstract boolean isSourceOrSinkOrTaintThrough(Class<?> clazz);

	public abstract boolean isSource(String str);

	public abstract boolean isTaintThrough(String str);
	
	public Object getLabel(String owner, String name, String taintedDesc)
	{
		if (name.endsWith("$$PHOSPHORTAGGED") || TaintUtils.containsTaintSentinel(taintedDesc))
			return getLabel(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return getLabel(owner + "." + name + taintedDesc);
	}
	public abstract Object getLabel(String str);
	public boolean isSource(MethodInsnNode insn) {
		return isSource(insn.owner + "." + insn.name + insn.desc);
	}

	public abstract boolean isSink(String str);

	public boolean isSink(MethodInsnNode insn) {
		return isSink(insn.owner + "." + insn.name + insn.desc);
	}

	public static String remapMethodDescToRemoveTaints(String desc) {
		String r = "(";
		boolean isSkipping = false;
		for (Type t : Type.getArgumentTypes(desc)) {
			if (t.getSort() == Type.ARRAY) {
				r += remapReturnType(t);
			} else if (t.getSort() != Type.OBJECT) {
				if (!isSkipping)
					isSkipping = true;
				else {
					r += t.getDescriptor();
					isSkipping = !isSkipping;
				}
			} else if (t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
				r += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(t.getDescriptor()).getDescriptor();
			} else if (t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct") || TaintUtils.isTaintSentinel(t)) {
				//ignore
			} else if(t.getDescriptor().equals(Configuration.TAINT_TAG_DESC))
				isSkipping = true;
			else
				r += t;
		}
		r += ")" + remapReturnType(Type.getReturnType(desc));
		if(Type.getReturnType(desc).getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct"))
			r = r.replace(Type.getReturnType(desc).getDescriptor(), "");
		return r;
	}

	public static String remapReturnType(Type returnType) {
		if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
			if (returnType.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
				return MultiDTaintedArray.getPrimitiveTypeForWrapper(returnType.getInternalName()).getDescriptor();
			}
			if(Configuration.MULTI_TAINTING)
			{
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithObjTag.class)))
					return "B";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyByteArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyByteArrayObjTags.class), "[B");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithObjTag.class)))
					return "Z";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyBooleanArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyBooleanArrayObjTags.class), "[Z");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithObjTag.class)))
					return "C";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyCharArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyCharArrayObjTags.class), "[C");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithObjTag.class)))
					return "D";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyDoubleArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyDoubleArrayObjTags.class), "[D");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithObjTag.class)))
					return "I";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyIntArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyIntArrayObjTags.class), "[I");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithObjTag.class)))
					return "F";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyFloatArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyFloatArrayObjTags.class), "[F");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithObjTag.class)))
					return "J";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyLongArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyLongArrayObjTags.class), "[J");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithObjTag.class)))
					return "S";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyShortArrayObjTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyShortArrayObjTags.class), "[S");
			}
			else
			{
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithIntTag.class)))
					return "B";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyByteArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyByteArrayIntTags.class), "[B");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithIntTag.class)))
					return "Z";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyBooleanArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyBooleanArrayIntTags.class), "[Z");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithIntTag.class)))
					return "C";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyCharArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyCharArrayIntTags.class), "[C");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithIntTag.class)))
					return "D";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyDoubleArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyDoubleArrayIntTags.class), "[D");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithIntTag.class)))
					return "I";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyIntArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyIntArrayIntTags.class), "[I");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithIntTag.class)))
					return "F";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyFloatArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyFloatArrayIntTags.class), "[F");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithIntTag.class)))
					return "J";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyLongArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyLongArrayIntTags.class), "[J");
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithIntTag.class)))
					return "S";
				if (returnType.getDescriptor().contains(Type.getDescriptor(LazyShortArrayIntTags.class)))
					return returnType.getDescriptor().replace(Type.getDescriptor(LazyShortArrayIntTags.class), "[S");
			}
		}
		return returnType.getDescriptor();
	}

	public boolean isTaintThrough(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED") || TaintUtils.containsTaintSentinel(taintedDesc))
			return isTaintThrough(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return isTaintThrough(owner + "." + name + taintedDesc);
	}

	
	public boolean isSink(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED") || TaintUtils.containsTaintSentinel(taintedDesc))
			return isSink(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return isSink(owner + "." + name + taintedDesc);
	}

	public boolean isSource(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED") || TaintUtils.containsTaintSentinel(taintedDesc))
			return isSource(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return isSource(owner + "." + name + taintedDesc);
	}

	/* Returns the name of sink method from which the specified method inherited its sink property or null if the specified
	 * method is not a sink. */
	public String getBaseSink(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED") || TaintUtils.containsTaintSentinel(taintedDesc))
			return getBaseSink(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return getBaseSink(owner + "." + name + taintedDesc);
	}

	public abstract String getBaseSink(String str);
}
