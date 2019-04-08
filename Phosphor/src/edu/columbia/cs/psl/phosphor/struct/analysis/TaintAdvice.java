package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintPassingMV;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

public abstract class TaintAdvice {
	private final AdviceType adviceType;

	protected TaintAdvice(AdviceType adviceType) {
		this.adviceType = adviceType;
	}

	public AdviceType getAdviceType() {
		return adviceType;
	}

	public abstract void apply(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className);

	public enum AdviceType {FORCE_CONTROL_STORE, FORCE_CONTROL_STORE_LV}
}
