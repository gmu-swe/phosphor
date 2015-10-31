package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropogator;
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
		if (!Instrumenter.IS_ANDROID_INST && owner.equals("java/lang/Class") && (name.equals("copyMethods") || name.equals("copyFields") || name.equals("copyConstructors")))
			owner = Type.getInternalName(ReflectionMasker.class);
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
