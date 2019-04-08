package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public abstract  class ForceControlStoreAdvice extends TaintAdvice {
	public ForceControlStoreAdvice() {
		super(AdviceType.FORCE_CONTROL_STORE);
	}

	public AbstractInsnNode getNewForceCtrlStoreNode() {
		return new LdcInsnNode(this);
	}

	protected abstract void loadValueAndItsTaintOrNull(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className, String defaultTypeDesc);
}
