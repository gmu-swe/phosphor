package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;
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
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public abstract class SourceSinkManager {
	public abstract boolean isSource(String str);

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
				r += MultiDTaintedArray.getPrimitiveTypeForWrapper(t.getDescriptor()).getDescriptor();
			} else if (t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct")) {
				//ignore
			} else
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
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedByte.class)))
				return "B";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteArray.class)))
				return "B";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedBoolean.class)))
				return "Z";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanArray.class)))
				return "[Z";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedChar.class)))
				return "C";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharArray.class)))
				return "[C";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedDouble.class)))
				return "D";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleArray.class)))
				return "[D";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedInt.class)))
				return "I";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntArray.class)))
				return "[I";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloat.class)))
				return "F";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatArray.class)))
				return "[F";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedLong.class)))
				return "J";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongArray.class)))
				return "[J";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedShort.class)))
				return "S";
			if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortArray.class)))
				return "[S";
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
