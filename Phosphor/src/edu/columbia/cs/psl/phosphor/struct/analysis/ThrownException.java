package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class ThrownException extends ForceControlStoreAdvice {
	public ThrownException(String exceptionInternalName) {
		this.exceptionInternalName = exceptionInternalName;
	}

	String exceptionInternalName;

	@Override
	protected void loadValueAndItsTaintOrNull(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className, String defaultTypeDesc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void apply(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className) {
		mv.visitVarInsn(ALOAD,lvs.getIdxOfMasterControlLV());
		mv.visitVarInsn(ALOAD,lvs.getIdxOfMasterExceptionLV());
		mv.visitLdcInsn(Type.getObjectType(exceptionInternalName));
		mv.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack","addUnthrownException","("+Type.getDescriptor(ExceptionalTaintData.class)+"Ljava/lang/Class;)V",false);
		mv.visitVarInsn(ALOAD,lvs.getIdxOfMasterControlLV());
		mv.visitLdcInsn(Type.getObjectType(exceptionInternalName));
		mv.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack","applyPossiblyUnthrownExceptionToTaint","(Ljava/lang/Class;)V",false);

	}
}
