package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;
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
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;

public abstract class SourceSinkManager {
	public abstract boolean isSource(String str);

	public Object getLabel(String owner, String name, String taintedDesc)
	{
		if (name.endsWith("$$PHOSPHORTAGGED"))
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
				if (!isSkipping)
					isSkipping = true;
				else {
					r += t.getDescriptor();
					isSkipping = !isSkipping;
				}
			} else if (t.getSort() != Type.OBJECT) {
				if (!isSkipping)
					isSkipping = true;
				else {
					r += t.getDescriptor();
					isSkipping = !isSkipping;
				}
			} else if (t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
				r += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(t.getDescriptor()).getDescriptor();
			} else if (t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct")) {
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

	private static String remapReturnType(Type returnType) {
		if (returnType.getSort() == Type.OBJECT) {
			if (returnType.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
				return MultiDTaintedArray.getPrimitiveTypeForWrapper(returnType.getInternalName()).getDescriptor();
			}
			if(Configuration.MULTI_TAINTING)
			{
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithObjTag.class)))
					return "B";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteArrayWithObjTag.class)))
					return "[B";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithObjTag.class)))
					return "Z";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanArrayWithObjTag.class)))
					return "[Z";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithObjTag.class)))
					return "C";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharArrayWithObjTag.class)))
					return "[C";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithObjTag.class)))
					return "D";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleArrayWithObjTag.class)))
					return "[D";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithObjTag.class)))
					return "I";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntArrayWithObjTag.class)))
					return "[I";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithObjTag.class)))
					return "F";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatArrayWithObjTag.class)))
					return "[F";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithObjTag.class)))
					return "J";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongArrayWithObjTag.class)))
					return "[J";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithObjTag.class)))
					return "S";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortArrayWithObjTag.class)))
					return "[S";
			}
			else
			{
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithIntTag.class)))
					return "B";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteArrayWithIntTag.class)))
					return "[B";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithIntTag.class)))
					return "Z";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanArrayWithIntTag.class)))
					return "[Z";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithIntTag.class)))
					return "C";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharArrayWithIntTag.class)))
					return "[C";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithIntTag.class)))
					return "D";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleArrayWithIntTag.class)))
					return "[D";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithIntTag.class)))
					return "I";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntArrayWithIntTag.class)))
					return "[I";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithIntTag.class)))
					return "F";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatArrayWithIntTag.class)))
					return "[F";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithIntTag.class)))
					return "J";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongArrayWithIntTag.class)))
					return "[J";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithIntTag.class)))
					return "S";
				if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortArrayWithIntTag.class)))
					return "[S";
			}
		}
		return returnType.getDescriptor();
	}

	public boolean isSink(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED"))
			return isSink(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return isSink(owner + "." + name + taintedDesc);
	}

	public boolean isSource(String owner, String name, String taintedDesc) {
		if (name.endsWith("$$PHOSPHORTAGGED"))
			return isSource(owner + "." + name.replace("$$PHOSPHORTAGGED", "") + remapMethodDescToRemoveTaints(taintedDesc));
		else
			return isSource(owner + "." + name + taintedDesc);
	}
}
