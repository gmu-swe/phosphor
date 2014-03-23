package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodInsnNode;

public abstract class SourceSinkManager {
	public abstract boolean isSource(String str);
	public boolean isSource(MethodInsnNode insn)
	{
		return isSource(insn.owner+"."+insn.name+insn.desc);
	}
}
