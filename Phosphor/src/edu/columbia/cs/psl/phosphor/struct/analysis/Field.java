package edu.columbia.cs.psl.phosphor.struct.analysis;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;
import java.util.LinkedList;

import static org.objectweb.asm.Opcodes.*;

public class Field extends ForceControlStoreAdvice {
	static int nHelpers;
	public boolean isStatic;
	public String name;
	public String owner;
	public String description;
	public ForceControlStoreAdvice accessPath;
	public Collection<MethodNode> methodCollector;
	private int idxOfGetter = -1;
	private boolean useFlatAccessors;

	public Field(FieldInsnNode fin, ForceControlStoreAdvice lvAccess, Collection<MethodNode> methodCollector, boolean useFlatAccessors) {
		this(fin, lvAccess, methodCollector);
		this.useFlatAccessors = useFlatAccessors;
	}

	public Field(FieldInsnNode fin, ForceControlStoreAdvice lvAccess, Collection<MethodNode> methodCollector) {
		this.isStatic = fin.getOpcode() == GETSTATIC || fin.getOpcode() == PUTSTATIC;
		this.name = fin.name;
		this.owner = fin.owner;
		this.description = fin.desc;
		this.accessPath = lvAccess;
		this.methodCollector = methodCollector;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Field field = (Field) o;

		if (isStatic != field.isStatic) return false;
		if (idxOfGetter != field.idxOfGetter) return false;
		if (useFlatAccessors != field.useFlatAccessors) return false;
		if (name != null ? !name.equals(field.name) : field.name != null) return false;
		if (owner != null ? !owner.equals(field.owner) : field.owner != null) return false;
		if (description != null ? !description.equals(field.description) : field.description != null) return false;
		if (accessPath != null ? !accessPath.equals(field.accessPath) : field.accessPath != null) return false;
		return methodCollector != null ? methodCollector.equals(field.methodCollector) : field.methodCollector == null;
	}

	@Override
	public int hashCode() {
		int result = (isStatic ? 1 : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (owner != null ? owner.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (accessPath != null ? accessPath.hashCode() : 0);
		result = 31 * result + idxOfGetter;
		result = 31 * result + (useFlatAccessors ? 1 : 0);
		result = 31 * result + (methodCollector != null ? methodCollector.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Field{" +
				"isStatic=" + isStatic +
				", name='" + name + '\'' +
				", owner='" + owner + '\'' +
				", description='" + description + '\'' +
				", accessPath='" + accessPath + '\'' +
				'}';
	}

	@Override
	public void apply(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className) {
		Type descType = Type.getType(description);
		if (!isStatic) {
			//Load the LV, or whatever else we need onto the stack, might be null though
			if (!loadEverythingUpUntilThisField(mv, lvs, analyzer, className))
				return;

			FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
			fn.type = F_NEW;
			Label bail = new Label();
			mv.visitInsn(DUP);
			mv.visitJumpInsn(IFNULL, bail);
			mv.visitInsn(DUP);


			if (descType.getSort() == Type.OBJECT || descType.getSort() == Type.ARRAY) {
				if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
					description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
				}

				mv.visitFieldInsn(GETFIELD, owner, name, description);
				mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V",
						false);
			} else {
				mv.visitInsn(DUP);
				mv.visitFieldInsn(GETFIELD, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
				mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
						+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
				mv.visitFieldInsn(PUTFIELD, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

			}

			mv.visitLabel(bail);
			fn.accept(mv);
			mv.visitInsn(POP);
		} else {
			//static field, hit it directly
			if (descType.getSort() == Type.OBJECT || descType.getSort() == Type.ARRAY) {
				if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
					description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
				}
				mv.visitFieldInsn(GETSTATIC, owner, name, description);
				mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V",
						false);
			} else {
				mv.visitFieldInsn(GETSTATIC, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
				mv.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				mv.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
						+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
				mv.visitFieldInsn(PUTSTATIC, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

			}

		}
	}


	public void fixMethodCollector(LinkedList<MethodNode> methodCollector) {
		if (this.methodCollector == null) {
			this.methodCollector = methodCollector;
			if (this.accessPath instanceof Field)
				((Field) this.accessPath).fixMethodCollector(methodCollector);
		}
	}

	private boolean loadEverythingUpUntilThisField(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className) {
		Type descType = Type.getType(description);
		if (isStatic)
			return true;
		else if (accessPath instanceof LVAccess && ((LVAccess) accessPath).idx == 0) {
			int lv = ((LVAccess) accessPath).idx;
			if (!((LVAccess) accessPath).isValid(analyzer, lvs, owner))
				return false;
			lvs.visitVarInsn(ALOAD, lv);
		} else {
			//Need to generate the helper method
			//Find what local variables are needed
			ForceControlStoreAdvice fv = this;
			LVAccess neededLV = null;
			LinkedList<Field> fieldsToLoadInOrder = new LinkedList<>();
			Field prev = this;
			fv = ((Field) fv).accessPath;
			while (fv != null) {
				if (fv instanceof Field) {
					prev = (Field) fv;
					if (((Field) fv).isStatic) {
						break;
					}
					fieldsToLoadInOrder.add((Field) fv);
					fv = ((Field) fv).accessPath;
				} else if (fv instanceof LVAccess) {
					neededLV = (LVAccess) fv;
					neededLV.desc = "L" + prev.owner + ";";
					break;
				}
			}
			String argDesc = null;
			if (neededLV != null) {
				//LV base
				if (!neededLV.isValid(analyzer, lvs, Type.getType(neededLV.desc).getInternalName()))
					return false;
				argDesc = neededLV.desc;
				lvs.visitVarInsn(ALOAD, neededLV.idx);
				if(accessPath == neededLV)
					return true;
			} else {
				//static field base
				Field sf = (Field) fv;
				argDesc = sf.description;
				mv.visitFieldInsn(GETSTATIC, sf.owner, sf.name, sf.description);
				if(accessPath == sf)
					return true;
			}
			int idx = methodCollector.size();
			if (this.idxOfGetter > 0)
				idx = this.idxOfGetter;
			mv.visitMethodInsn(INVOKESTATIC, className, "$$$phosphorFieldHelper" + idx, "(" + argDesc + ")L" + owner + ";", false);


			if (idx != this.idxOfGetter) {
				MethodNode helperMethod = new MethodNode(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, "$$$phosphorFieldHelper" + idx, "(" + argDesc + ")L" + owner + ";", null, null);

				helperMethod.visitCode();
				Label start = new Label();
				Label end = new Label();
				Label bail = new Label();
				Label handler = new Label();
				helperMethod.visitTryCatchBlock(start, end, handler, null);
				helperMethod.visitLabel(start);
				helperMethod.visitVarInsn(ALOAD, 0);
				helperMethod.visitInsn(DUP);
				helperMethod.visitJumpInsn(IFNULL, bail);
				for (Field each : fieldsToLoadInOrder) {
					if (each != this) {
						helperMethod.visitFieldInsn(GETFIELD, each.owner, each.name, each.description);
						helperMethod.visitInsn(DUP);
						helperMethod.visitJumpInsn(IFNULL, bail);
					}
				}
				helperMethod.visitInsn(ARETURN);
				helperMethod.visitLabel(bail);
				Object[] local = new Object[2];
				local[0] = Type.getType(argDesc).getInternalName();
				local[1] = Type.getInternalName(ControlTaintTagStack.class);
				helperMethod.visitFrame(F_NEW, 2, local, 1, new Object[]{"java/lang/Object"});
				helperMethod.visitInsn(POP);
				helperMethod.visitInsn(ACONST_NULL);
				helperMethod.visitInsn(ARETURN);
				helperMethod.visitLabel(end);

				helperMethod.visitLabel(handler);
				helperMethod.visitFrame(F_NEW, 2, local, 1, new Object[]{"java/lang/Throwable"});
				helperMethod.visitInsn(POP);
				helperMethod.visitInsn(ACONST_NULL);
				helperMethod.visitInsn(ARETURN);

				helperMethod.visitEnd();
				helperMethod.visitMaxs(0, 0);
				helperMethod.visitLocalVariable("phosphorFieldOwner", argDesc, null, start, end, 0);
				this.idxOfGetter = idx;
				methodCollector.add(helperMethod);
			}
		}
		return true;

	}

	@Override
	protected void loadValueAndItsTaintOrNull(MethodVisitor mv, LocalVariableManager lvs, NeverNullArgAnalyzerAdapter analyzer, String className, String defaultTypeDesc) {
		Type descType = Type.getType(description);
		String taintType = TaintUtils.getShadowTaintType(defaultTypeDesc);
		boolean shouldHaveTaint = taintType != null;
		if(!defaultTypeDesc.equals(this.description))
		{
			//bail
			if(shouldHaveTaint)
				mv.visitInsn(ACONST_NULL);
			mv.visitInsn(TaintUtils.getNullOrZero(Type.getType(defaultTypeDesc)));
			return;
		}

		if (!isStatic) {
			//Load the LV, or whatever else we need onto the stack, might be null though
			if (!loadEverythingUpUntilThisField(mv, lvs, analyzer, className)) {
				if (shouldHaveTaint)
					mv.visitInsn(ACONST_NULL);
				mv.visitInsn(TaintUtils.getNullOrZero(Type.getType(description)));
				return;
			}

			FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
			fn.type = F_NEW;
			Label bail = new Label();
			mv.visitInsn(DUP);
			mv.visitJumpInsn(IFNULL, bail);
			if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
				description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
			}
			if (shouldHaveTaint) {
				if(Instrumenter.isIgnoredClass(owner))
				{
					mv.visitInsn(ACONST_NULL);
				}
				else {
					mv.visitInsn(DUP);
					mv.visitFieldInsn(GETFIELD, owner, name + TaintUtils.TAINT_FIELD, taintType);
				}
				mv.visitInsn(SWAP);
				mv.visitFieldInsn(GETFIELD, owner, name, description);
			} else {
				mv.visitFieldInsn(GETFIELD, owner, name, description);
			}
			FrameNode fn2 = TaintAdapter.getCurrentFrameNode(analyzer);
			fn2.type = F_NEW;

			Label end = new Label();
			mv.visitJumpInsn(GOTO, end);
			mv.visitLabel(bail);
			fn.accept(mv);
			mv.visitInsn(POP);
			if (shouldHaveTaint) {
				mv.visitInsn(ACONST_NULL);
			}
			mv.visitInsn(TaintUtils.getNullOrZero(Type.getType(description)));
			mv.visitLabel(end);
			fn2.accept(mv);

//			mv.visitInsn(POP);
		} else {
			//static field, hit it directly
			if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
				description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
			}
			if (shouldHaveTaint) {
				if(Instrumenter.isIgnoredClass(owner)){
					mv.visitInsn(ACONST_NULL);
				}else {
					mv.visitFieldInsn(GETSTATIC, owner, name + TaintUtils.TAINT_FIELD, taintType);
				}
				mv.visitFieldInsn(GETSTATIC, owner, name, description);
			}
			else{
				mv.visitFieldInsn(GETSTATIC, owner, name, description);
			}
		}
	}
}
