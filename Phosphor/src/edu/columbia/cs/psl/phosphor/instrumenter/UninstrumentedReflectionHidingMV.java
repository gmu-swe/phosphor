package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropogator;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.MethodInvoke;

public class UninstrumentedReflectionHidingMV extends MethodVisitor implements Opcodes {

	private String className;
	private LocalVariableManager lvs;

	public UninstrumentedReflectionHidingMV(MethodVisitor mv, String className) {
		super(Opcodes.ASM5, mv);
		this.className = className;
	}

	public void setLvs(LocalVariableManager lvs) {
		this.lvs = lvs;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		Type[] args = Type.getArgumentTypes(desc);
//TESTING
		boolean origAndroidInst = Instrumenter.IS_ANDROID_INST;
		Instrumenter.IS_ANDROID_INST = true;

		if (owner.equals("java/lang/reflect/Field")
				&& opcode == Opcodes.INVOKEVIRTUAL
				&& (name.equals("get") || name.equals("set"))){
			owner = Type.getInternalName(RuntimeReflectionPropogator.class);
			opcode = Opcodes.INVOKESTATIC;
			name = name + "UNINST";
			desc = "(Ljava/lang/reflect/Field;" + desc.substring(1);
		}
		if ((owner.equals("java/lang/reflect/Method") || owner.equals("java/lang/reflect/Constructor")) && (name.startsWith("invoke") || name.startsWith("newInstance"))) {

			if (owner.equals("java/lang/reflect/Method")) {
				//method owner [Args
				if (Configuration.IMPLICIT_TRACKING) {
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgsUninst",
							"(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;" + Type.getDescriptor(ControlTaintTagStack.class) + ")" + Type.getDescriptor(MethodInvoke.class), false);
				} else {
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgsUninst",
							"(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;Z)" + Type.getDescriptor(MethodInvoke.class), false);
				}
				//B
				super.visitInsn(Opcodes.DUP);
				//B B
				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(MethodInvoke.class), "m", "Ljava/lang/reflect/Method;");
				//B M
				super.visitInsn(Opcodes.SWAP);
				//M B
				super.visitInsn(Opcodes.DUP);
				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(MethodInvoke.class), "o", "Ljava/lang/Object;");
				super.visitInsn(Opcodes.SWAP);
				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(MethodInvoke.class), "a", "[Ljava/lang/Object;");
				if (Configuration.IMPLICIT_TRACKING)
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);

			} else {
				if (Configuration.IMPLICIT_TRACKING) {
					super.visitInsn(POP);
					super.visitInsn(Opcodes.SWAP);
					//[A C
					super.visitInsn(Opcodes.DUP_X1);
					//C [A C
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgsUninst",
							"([Ljava/lang/Object;Ljava/lang/reflect/Constructor;" + Type.getDescriptor(ControlTaintTagStack.class) + ")[Ljava/lang/Object;", false);
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);

				} else {
					super.visitInsn(Opcodes.SWAP);
					//[A C
					super.visitInsn(Opcodes.DUP_X1);
					//C [A C
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgsUninst", "([Ljava/lang/Object;Ljava/lang/reflect/Constructor;Z)[Ljava/lang/Object;",
							false);
				}
			}
		}
		else if (!Instrumenter.IS_ANDROID_INST && owner.equals("java/lang/Class") && (name.equals("copyMethods") || name.equals("copyFields") || name.equals("copyConstructors")))
			owner = Type.getInternalName(ReflectionMasker.class);
		else if ((owner.equals("java/lang/reflect/Method")) && name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.startsWith("java/lang/Class")) {
			if (args.length == 0)
			{
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Method;Z)Ljava/lang/reflect/Method;", false);
			}
			else if (args.length == 1) {
				super.visitInsn(Opcodes.SWAP);
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Method;Z)Ljava/lang/reflect/Method;", false);
				super.visitInsn(Opcodes.SWAP);
			}
			else if(args.length == 2)
			{
				int lv1 = lvs.getTmpLV();
				super.visitVarInsn(Opcodes.ASTORE, lv1);
				int lv2 = lvs.getTmpLV();
				super.visitVarInsn(Opcodes.ASTORE, lv2);
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Method;Z)Ljava/lang/reflect/Method;", false);
				super.visitVarInsn(Opcodes.ALOAD, lv2);
				super.visitVarInsn(Opcodes.ALOAD, lv1);
				lvs.freeTmpLV(lv1);
				lvs.freeTmpLV(lv2);
			}
		} else if ((owner.equals("java/lang/reflect/Constructor")) && name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect")
				&& !className.equals("java/lang/Class")) {
			if (args.length == 0)
			{
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Constructor;Z)Ljava/lang/reflect/Constructor;", false);
			}
			else if(args.length == 1){
				super.visitInsn(Opcodes.SWAP);
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Constructor;Z)Ljava/lang/reflect/Constructor;", false);
				super.visitInsn(Opcodes.SWAP);
			}
			else if(args.length == 2)
			{
				int lv1 = lvs.getTmpLV();
				super.visitVarInsn(Opcodes.ASTORE, lv1);
				int lv2 = lvs.getTmpLV();
				super.visitVarInsn(Opcodes.ASTORE, lv2);
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Constructor;Z)Ljava/lang/reflect/Constructor;", false);
				super.visitVarInsn(Opcodes.ALOAD, lv2);
				super.visitVarInsn(Opcodes.ALOAD, lv1);
				lvs.freeTmpLV(lv1);
				lvs.freeTmpLV(lv2);
			}
		}
		else if (owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
			owner = Type.getInternalName(ArrayReflectionMasker.class);
			if(Configuration.MULTI_TAINTING)
				desc = desc.replace(Configuration.TAINT_TAG_DESC, "Ljava/lang/Object;");
		}
		super.visitMethodInsn(opcode, owner, name, desc,itfc);
		if (owner.equals("java/lang/Class") && desc.equals("()[Ljava/lang/reflect/Field;")) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintFields", "([Ljava/lang/reflect/Field;)[Ljava/lang/reflect/Field;",false);
		} else if (owner.equals("java/lang/Class") && desc.equals("()[Ljava/lang/reflect/Method;")) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintMethods", "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;",false);
		} else if (owner.equals("java/lang/Class") && desc.equals("()[Ljava/lang/reflect/Constructor;")) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintConstructors",
						"([Ljava/lang/reflect/Constructor;)[Ljava/lang/reflect/Constructor;",false);
		} else if (owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintedInterface", "([Ljava/lang/Class;)[Ljava/lang/Class;",false);
		} else if (owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && desc.equals("()" + "[" + Type.getDescriptor(StackTraceElement.class))) {
			String stackTraceElDesc = "[" + Type.getDescriptor(StackTraceElement.class);
			if (className.equals("java/lang/Throwable")) {

				super.visitVarInsn(Opcodes.ALOAD, 0);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;",false);
			} else
				super.visitLdcInsn(Type.getObjectType(className));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeExtraStackTraceElements", "(" + stackTraceElDesc + "Ljava/lang/Class;)" + stackTraceElDesc,false);
		} else if (owner.equals("java/lang/Object") && name.equals("getClass")) {
			super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintClass", "(Ljava/lang/Class;Z)Ljava/lang/Class;", false);

		}

		Instrumenter.IS_ANDROID_INST = origAndroidInst;
	}

}
