package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.FrameNode;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropogator;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.MethodInvoke;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;

public class ReflectionHidingMV extends MethodVisitor implements Opcodes {

	private String className;
	private LocalVariableManager lvs;
	private NeverNullArgAnalyzerAdapter analyzer;

	public ReflectionHidingMV(MethodVisitor mv, String className, NeverNullArgAnalyzerAdapter analyzer) {
		super(Opcodes.ASM5, mv);
		this.className = className;
		this.analyzer = analyzer;
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
		if ((owner.equals("java/lang/reflect/Method") || owner.equals("java/lang/reflect/Constructor")) && (name.startsWith("invoke") || name.startsWith("newInstance"))) {

			if (owner.equals("java/lang/reflect/Method")) {
				//method owner [Args
				//Try the fastpath where we know we don't change the method
				if (!Instrumenter.IS_ANDROID_INST && !Instrumenter.IS_KAFFE_INST && !Instrumenter.IS_HARMONY_INST) {
					Label slow = new Label();
					Label done = new Label();
					int argsVar = lvs.getTmpLV(Type.getType("[Ljava/lang/Object;"));
					int objVar = lvs.getTmpLV(Type.getType("Ljava/lang/Object;"));
					int methodVar = lvs.getTmpLV(Type.getType("Ljava/lang/reflect/Method;"));

					super.visitVarInsn(ASTORE, argsVar);
					super.visitVarInsn(ASTORE, objVar);
					super.visitInsn(DUP);
					super.visitFieldInsn(GETFIELD, "java/lang/reflect/Method", TaintUtils.TAINT_FIELD + "marked", "Z");
					super.visitJumpInsn(IFEQ, slow);

					super.visitVarInsn(ASTORE, methodVar);
					super.visitVarInsn(ALOAD, methodVar);
					super.visitVarInsn(ALOAD, objVar);

					super.visitVarInsn(ALOAD, methodVar);
					super.visitVarInsn(ALOAD, argsVar);
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgsFast", "(Ljava/lang/reflect/Method;[Ljava/lang/Object;Z)[Ljava/lang/Object;",
							false);
					super.visitJumpInsn(GOTO, done);
					super.visitLabel(slow);
					super.visitVarInsn(ALOAD, objVar);
					super.visitVarInsn(ALOAD, argsVar);
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs",
							"(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;Z)" + Type.getDescriptor(MethodInvoke.class), false);
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
					super.visitLabel(done);

					lvs.freeTmpLV(argsVar);
					lvs.freeTmpLV(objVar);
					lvs.freeTmpLV(methodVar);
				} else {
					//orig version
					if (Configuration.IMPLICIT_TRACKING) {
						super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs", "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;"
								+ Type.getDescriptor(ControlTaintTagStack.class) + ")" + Type.getDescriptor(MethodInvoke.class), false);
					} else
					{
						super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs", "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;Z)"
								+ Type.getDescriptor(MethodInvoke.class), false);
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

				}
			} else {
				if (Configuration.IMPLICIT_TRACKING) {
					super.visitInsn(POP);
					super.visitInsn(Opcodes.SWAP);
					//[A C
					super.visitInsn(Opcodes.DUP_X1);
					//C [A C
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs",
							"([Ljava/lang/Object;Ljava/lang/reflect/Constructor;" + Type.getDescriptor(ControlTaintTagStack.class) + ")[Ljava/lang/Object;", false);
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);

				} else {
					super.visitInsn(Opcodes.SWAP);
					//[A C
					super.visitInsn(Opcodes.DUP_X1);
					//C [A C
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs", "([Ljava/lang/Object;Ljava/lang/reflect/Constructor;Z)[Ljava/lang/Object;",
							false);
				}
			}
		} else if ((owner.equals("java/lang/reflect/Method")) && name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.startsWith("java/lang/Class")) {
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
		} else if ((owner.equals("java/lang/reflect/Constructor")) && name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect")
				&& !className.equals("java/lang/Class")) {
			if (args.length == 0)
			{
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Constructor;Z)Ljava/lang/reflect/Constructor;", false);
			}
			else {
				super.visitInsn(Opcodes.SWAP);
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", "(Ljava/lang/reflect/Constructor;Z)Ljava/lang/reflect/Constructor;", false);
				super.visitInsn(Opcodes.SWAP);
			}
		} else if (owner.equals("java/lang/Class")
				&& (((name.equals("getConstructor") || (name.equals("getDeclaredConstructor"))) && args.length == 1) || ((name.equals("getMethod") || name.equals("getDeclaredMethod")))
						&& args.length == 2)) {
			if (args.length == 2) {

				//				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "addTypeParams", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)" + Type.getDescriptor(Pair.class));
				//				super.visitInsn(Opcodes.DUP);
				//				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Pair.class), "o0", Type.getDescriptor(Class.class));
				//				super.visitInsn(Opcodes.SWAP);
				//				super.visitInsn(Opcodes.DUP);
				//				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Pair.class), "o1", Type.getDescriptor(String.class));
				//				super.visitInsn(Opcodes.SWAP);
				//				super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Pair.class), "o2", Type.getDescriptor(Class[].class));
				opcode = Opcodes.INVOKESTATIC;
				owner = Type.getInternalName(ReflectionMasker.class);
				desc = "(Ljava/lang/Class;" + desc.substring(1);
				if(!Configuration.IMPLICIT_TRACKING)
				{
					desc = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Z)Ljava/lang/reflect/Method;";
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				}
			} else
			{
				super.visitInsn((Configuration.IMPLICIT_TRACKING ? ICONST_1 : ICONST_0));
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "addTypeParams", "([Ljava/lang/Class;ZZ)[Ljava/lang/Class;", false);
			}
		}
		if (owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
			owner = Type.getInternalName(ArrayReflectionMasker.class);
			if(Configuration.MULTI_TAINTING)
				desc = desc.replace(Configuration.TAINT_TAG_DESC, "Ljava/lang/Object;");
		}
		if (owner.equals("java/lang/reflect/Field")
				&& opcode == Opcodes.INVOKEVIRTUAL
				&& (name.equals("get") || name.equals("get$$PHOSPHORTAGGED") || name.equals("set$$PHOSPHORTAGGED") || name.equals("getInt$$PHOSPHORTAGGED") || name.equals("getBoolean$$PHOSPHORTAGGED") || name.equals("getChar$$PHOSPHORTAGGED")
						|| name.equals("getDouble$$PHOSPHORTAGGED") || name.equals("getByte$$PHOSPHORTAGGED") || name.equals("getFloat$$PHOSPHORTAGGED") || name.equals("getLong$$PHOSPHORTAGGED")
						|| name.equals("getShort$$PHOSPHORTAGGED") || name.equals("setAccessible$$PHOSPHORTAGGED") || name.equals("set") || name.equals("setInt$$PHOSPHORTAGGED")
						|| name.equals("setBoolean$$PHOSPHORTAGGED") || name.equals("setChar$$PHOSPHORTAGGED") || name.equals("setDouble$$PHOSPHORTAGGED") || name.equals("setByte$$PHOSPHORTAGGED")
						|| name.equals("setFloa$$PHOSPHORTAGGEDt") || name.equals("setLong$$PHOSPHORTAGGED") || name.equals("setShort$$PHOSPHORTAGGED") || name.equals("getType"))) {
			owner = Type.getInternalName(RuntimeReflectionPropogator.class);
			opcode = Opcodes.INVOKESTATIC;
			desc = "(Ljava/lang/reflect/Field;" + desc.substring(1);
			if(name.equals("get"))
			{
				desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;Z)Ljava/lang/Object;";
				super.visitInsn((Configuration.MULTI_TAINTING ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
			}
			else if(name.equals("set"))
			{
				desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;Z)V";
				super.visitInsn((Configuration.MULTI_TAINTING ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
			}
		}
		if (!Instrumenter.IS_ANDROID_INST && owner.equals("java/lang/Class") && (name.equals("copyMethods") || name.equals("copyFields") || name.equals("copyConstructors")))
		{
			owner = Type.getInternalName(ReflectionMasker.class);
		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
		if (owner.equals("java/lang/Class") && desc.endsWith("[Ljava/lang/reflect/Field;")) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintFields", "([Ljava/lang/reflect/Field;)[Ljava/lang/reflect/Field;", false);
		} else if (owner.equals("java/lang/Class") && (desc.equals("()[Ljava/lang/reflect/Method;") || desc.equals("("+Type.getDescriptor(ControlTaintTagStack.class)+")[Ljava/lang/reflect/Method;"))) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintMethods", "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;", false);
		} else if (owner.equals("java/lang/Class") && (desc.equals("()[Ljava/lang/reflect/Constructor;") || desc.equals("("+Type.getDescriptor(ControlTaintTagStack.class)+")[Ljava/lang/reflect/Constructor;"))) {
			if (Instrumenter.IS_ANDROID_INST)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintConstructors",
						"([Ljava/lang/reflect/Constructor;)[Ljava/lang/reflect/Constructor;", false);
		} else if (owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintedInterface", "([Ljava/lang/Class;)[Ljava/lang/Class;", false);
		} else if (owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && desc.equals("()" + "[" + Type.getDescriptor(StackTraceElement.class))) {
			String stackTraceElDesc = "[" + Type.getDescriptor(StackTraceElement.class);
			if (className.equals("java/lang/Throwable")) {

				super.visitVarInsn(Opcodes.ALOAD, 0);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			} else
				super.visitLdcInsn(Type.getObjectType(className));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeExtraStackTraceElements",
					"(" + stackTraceElDesc + "Ljava/lang/Class;)" + stackTraceElDesc, false);
		} else if (owner.equals("java/lang/Object") && name.equals("getClass")) {
			super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintClass", "(Ljava/lang/Class;Z)Ljava/lang/Class;", false);

		}

		if ((owner.equals("java/lang/reflect/Method") || owner.equals("java/lang/reflect/Constructor")) && !(className.equals("java/lang/Class")) && 
				(name.equals("invoke") || name.equals("newInstance") || name.equals("invoke$$PHOSPHORTAGGED") 
						|| name.equals("newInstance$$PHOSPHORTAGGED"))) {
//			System.out.println(className + "  vs " + owner);
			//Unbox if necessary
			FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
			fn.type = Opcodes.F_NEW;
			super.visitInsn(Opcodes.DUP);
			super.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)));
			Label notPrimitive = new Label();
			Label isOK = new Label();
			//			Label notPrimitiveArray = new Label();

			super.visitJumpInsn(Opcodes.IFEQ, notPrimitive);
			FrameNode fn2 = TaintAdapter.getCurrentFrameNode(analyzer);
			fn2.type = Opcodes.F_NEW;
			super.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)));
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)), "toPrimitiveType", "()Ljava/lang/Object;", false);
			super.visitJumpInsn(Opcodes.GOTO, isOK);
			super.visitLabel(notPrimitive);
			fn2.accept(this);
			super.visitInsn(Opcodes.DUP);
			super.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveArrayWithObjTag.class : TaintedPrimitiveArrayWithIntTag.class)));
			super.visitJumpInsn(Opcodes.IFEQ, isOK);
			super.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveArrayWithObjTag.class : TaintedPrimitiveArrayWithIntTag.class)));
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveArrayWithObjTag.class : TaintedPrimitiveArrayWithIntTag.class)), "toStackType", "()Ljava/lang/Object;", false);
			super.visitLabel(isOK);
			fn.accept(this);
		}
		Instrumenter.IS_ANDROID_INST = origAndroidInst;
	}

}
