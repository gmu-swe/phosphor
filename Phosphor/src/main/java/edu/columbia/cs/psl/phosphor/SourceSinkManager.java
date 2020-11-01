package edu.columbia.cs.psl.phosphor;

import org.objectweb.asm.tree.MethodInsnNode;

public abstract class SourceSinkManager {
    public abstract boolean isSourceOrSinkOrTaintThrough(Class<?> clazz);

    public abstract boolean isSource(String str);

    public abstract boolean isTaintThrough(String str);

    public Object getLabel(String owner, String name, String taintedDesc) {
        return getLabel(TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, taintedDesc));
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
        return isTaintThrough(TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, taintedDesc));
    }

    public boolean isSink(String owner, String name, String taintedDesc) {
        return isSink(TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, taintedDesc));
    }

    public boolean isSource(String owner, String name, String taintedDesc) {
        return isSource(TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, taintedDesc));
    }

    /* Returns the name of sink method from which the specified method inherited its sink property or null if the specified
     * method is not a sink. */
    public String getBaseSink(String owner, String name, String taintedDesc) {
        return getBaseSink(TaintUtils.getOriginalMethodSignatureWithoutReturn(owner, name, taintedDesc));
    }

    public abstract String getBaseSink(String str);
}
