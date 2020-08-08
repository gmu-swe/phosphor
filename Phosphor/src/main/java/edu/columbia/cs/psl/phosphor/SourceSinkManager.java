package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public abstract class SourceSinkManager {
    public abstract boolean isSourceOrSinkOrTaintThrough(Class<?> clazz);

    public abstract boolean isSource(String str);

    public abstract boolean isTaintThrough(String str);

    public Object getLabel(String owner, String name, String taintedDesc) {
        return getLabel(getOriginalMethodSignature(owner, name, taintedDesc));
    }

    public abstract Object getLabel(String str);

    public boolean isSource(MethodInsnNode insn) {
        return isSource(insn.owner + "." + insn.name + insn.desc);
    }

    public abstract boolean isSink(String str);

    public boolean isSink(MethodInsnNode insn) {
        return isSink(insn.owner + "." + insn.name + insn.desc);
    }

    public boolean isTaintThrough(String owner, String name, String taintedDesc) {
        return isTaintThrough(getOriginalMethodSignature(owner, name, taintedDesc));
    }

    public boolean isSink(String owner, String name, String taintedDesc) {
        return isSink(getOriginalMethodSignature(owner, name, taintedDesc));
    }

    public boolean isSource(String owner, String name, String taintedDesc) {
        return isSource(getOriginalMethodSignature(owner, name, taintedDesc));
    }

    /* Returns the name of sink method from which the specified method inherited its sink property or null if the specified
     * method is not a sink. */
    public String getBaseSink(String owner, String name, String taintedDesc) {
        return getBaseSink(getOriginalMethodSignature(owner, name, taintedDesc));
    }

    public abstract String getBaseSink(String str);

    public static String remapMethodDescToRemoveTaintsAndReturnType(String desc) {
        String r = "(";
        boolean isSkipping = false;
        LinkedList<String> wrappedTypes = new LinkedList<>();
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                wrappedTypes.add(t.getDescriptor());
            }
        }
        Type[] args = Type.getArgumentTypes(desc);
        int lastArgToAdd = args.length;
        if(Type.getReturnType(desc).getDescriptor().equals("Ledu/columbia/cs/psl/phosphor/struct/TaintedReferenceWithObjTag;")){
            lastArgToAdd--;
        }
        for(int i = 0; i < lastArgToAdd; i++) {
            Type t = args[i];
            if(t.getDescriptor().equals(TaintTrackingClassVisitor.CONTROL_STACK_DESC)) {
                //
            } else if(t.getDescriptor().equals("Ledu/columbia/cs/psl/phosphor/struct/LazyReferenceArrayObjTags;")) {
                r += wrappedTypes.remove();
            } else if(t.getSort() == Type.ARRAY) {
                //
            } else if(t.getSort() != Type.OBJECT) {
                if(!isSkipping) {
                    isSkipping = true;
                } else {
                    r += t.getDescriptor();
                    isSkipping = !isSkipping;
                }
            } else if(t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")
                    || t.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct")) {
                r += TaintUtils.getUnwrappedType(t).getDescriptor();
            } else if(t.getDescriptor().equals(Configuration.TAINT_TAG_DESC)) {
                isSkipping = true;
            } else {
                r += t;
            }
        }
        r += ")";
        if(Type.getReturnType(desc).getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct")) {
            r = r.replace(Type.getReturnType(desc).getDescriptor(), "");
        }
        return r;
    }

    public static String remapReturnType(Type returnType) {
        if(returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            if(returnType.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
                return MultiDTaintedArray.getPrimitiveTypeForWrapper(returnType.getInternalName()).getDescriptor();
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithObjTag.class))) {
                return "B";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyByteArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyByteArrayObjTags.class), "[B");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithObjTag.class))) {
                return "Z";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyBooleanArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyBooleanArrayObjTags.class), "[Z");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithObjTag.class))) {
                return "C";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyCharArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyCharArrayObjTags.class), "[C");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithObjTag.class))) {
                return "D";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyDoubleArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyDoubleArrayObjTags.class), "[D");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithObjTag.class))) {
                return "I";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyIntArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyIntArrayObjTags.class), "[I");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithObjTag.class))) {
                return "F";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyFloatArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyFloatArrayObjTags.class), "[F");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithObjTag.class))) {
                return "J";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyLongArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyLongArrayObjTags.class), "[J");
            }
            if(returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithObjTag.class))) {
                return "S";
            }
            if(returnType.getDescriptor().contains(Type.getDescriptor(LazyShortArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyShortArrayObjTags.class), "[S");
            }
        }
        return returnType.getDescriptor();
    }

    /* Constructs and returns the bytecode method signature from the specified pieces; removes any phosphor-added suffixes and tainted types from the
     * signature. */
    public static String getOriginalMethodSignature(String owner, String name, String desc) {
        if(name.endsWith(TaintUtils.METHOD_SUFFIX) || TaintUtils.containsTaint(desc)) {
            return owner + "." + name.replace(TaintUtils.METHOD_SUFFIX, "") + remapMethodDescToRemoveTaintsAndReturnType(desc);
        } else {
            return owner + "." + name + desc;
        }
    }
}
