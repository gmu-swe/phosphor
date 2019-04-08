package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

public class LVAccess extends ForceControlStoreAdvice {
	int idx;
	String desc;

	public LVAccess(BasicArrayInterpreter.BasicLVValue lvValue) {
		this.idx = lvValue.getLv();
		if (lvValue.getType() == null)
			this.desc = "Ljava/lang/Object;";
		else
			this.desc = lvValue.getType().getDescriptor();
	}

	public LVAccess(int var, String desc) {
		this.idx = var;
		this.desc = desc;
	}

	@Override
	public String toString() {
		return "LVAccess{" +
				"idx=" + idx +
				", desc='" + desc + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LVAccess lvAccess = (LVAccess) o;
		return idx == lvAccess.idx &&
				Objects.equals(desc, lvAccess.desc);
	}

	@Override
	public int hashCode() {

		return Objects.hash(idx, desc);
	}

	int getShadowVar(LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String expectedDesc) {
		int shadowVar = -1;
		if (!isValid(analyzer, lvs, expectedDesc))
			return -1;
		int remapped = lvs.remap(idx, Type.getType(desc));
		if (idx < lvs.newArgTypes.size() && TaintUtils.getShadowTaintType(lvs.newArgTypes.get(remapped).getDescriptor()) != null) {
			//accessing an arg; remap it
			Type localType = lvs.newArgTypes.get(remapped);
			if (localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
				shadowVar = remapped - 1;
			} else if (localType.getSort() == Type.ARRAY)
				return -1;
		} else {
			if (lvs.varToShadowVar.containsKey(remapped)) {
				shadowVar = lvs.varToShadowVar.get(remapped);
				if (analyzer.locals.get(remapped) instanceof String && ((String) analyzer.locals.get(remapped)).startsWith("["))
					return -1;
				if (shadowVar >= analyzer.locals.size() || analyzer.locals.get(shadowVar) instanceof Integer || ((String) analyzer.locals.get(shadowVar)).startsWith("["))
					return -1;
			}
		}
		return shadowVar;
	}

	@Override
	public void apply(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className) {
		if (!isValid(analyzer, lvs))
			return;
		int shadowVar = getShadowVar(lvs, analyzer, null);
		if (shadowVar == -1)
			return;
		else if (shadowVar >= 0) {
			mv.visitVarInsn(ALOAD, shadowVar);
			mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
			mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
			mv.visitVarInsn(ASTORE, shadowVar);
		} else {
			lvs.visitVarInsn(ALOAD, idx);
			mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
			mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
		}

	}

	public boolean isValid(NeverNullArgAnalyzerAdapter analyzer, LocalVariableManager lvs) {
		return isValid(analyzer, lvs, null);
	}

	public boolean isValid(NeverNullArgAnalyzerAdapter analyzer, LocalVariableManager lvs, String expectedType) {
		int remapped = lvs.remap(idx, Type.getType(desc));

		if (analyzer.locals == null || analyzer.locals.size() <= remapped)
			return false;
		Object an = analyzer.locals.get(remapped);
		if (an == Opcodes.TOP)
			return false;
		//check type
		Type myType = Type.getType(desc);
		if (myType.getSort() == Type.OBJECT || myType.getSort() == Type.ARRAY) {
			if (expectedType == null || expectedType.equals(an))
				return true;
			else
				return false;
		} else if (an instanceof String || an == NULL)
			return false;
		if (expectedType != null && an instanceof Integer) {
			Type expected = Type.getType(expectedType);
			switch (expected.getSort()) {
				case Type.INT:
				case Type.SHORT:
				case Type.CHAR:
				case Type.BOOLEAN:
				case Type.BYTE:
					return an == INTEGER;
				case Type.LONG:
					return an == LONG;
				case Type.FLOAT:
					return an == FLOAT;
				case Type.DOUBLE:
					return an == DOUBLE;
				default:
					throw new UnsupportedOperationException();

			}
		}
		return true;
	}

	@Override
	protected void loadValueAndItsTaintOrNull(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className, String defaultType) {

		int shadowVar = getShadowVar(lvs, analyzer, defaultType);
		boolean shouldHaveTaint = TaintUtils.getShadowTaintType(defaultType) != null;
		if (!shouldHaveTaint) {
			if (!isValid(analyzer, lvs, Type.getType(defaultType).getInternalName()))
				mv.visitInsn(TaintUtils.getNullOrZero(Type.getType(defaultType)));
			else
				lvs.visitVarInsn(Type.getType(defaultType).getOpcode(ILOAD), idx);
		} else {
			if (shadowVar >= 0) {
				mv.visitVarInsn(ALOAD, shadowVar);
				lvs.visitVarInsn(Type.getType(defaultType).getOpcode(ILOAD), idx);
			} else {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(TaintUtils.getNullOrZero(Type.getType(defaultType)));
			}
		}
	}
}
