package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropogator;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeUnsafePropagator;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.MethodInvoke;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import sun.misc.Unsafe;

public class ReflectionHidingMV extends MethodVisitor implements Opcodes {

	private final String className;
	private final NeverNullArgAnalyzerAdapter analyzer;
	private final String methodName;
	private final boolean disable;
	private final boolean isObjOutputStream;
	private LocalVariableManager lvs;

	public ReflectionHidingMV(MethodVisitor mv, String className, String name, NeverNullArgAnalyzerAdapter analyzer) {
		super(Configuration.ASM_VERSION, mv);
		this.className = className;
		this.analyzer = analyzer;
		this.methodName = name;
		this.disable = shouldDisable(className, name);
		this.isObjOutputStream = (className.equals("java/io/ObjectOutputStream") && name.startsWith("writeObject0")) ||
				(className.equals("java/io/InputStream") && name.startsWith("defaultReadFields"));
	}

	private static boolean shouldDisable(String className, String methodName) {
		if(className.equals("org/codehaus/groovy/vmplugin/v5/Java5") && methodName.equals("makeInterfaceTypes")) {
			return true;
		} else if(Configuration.TAINT_THROUGH_SERIALIZATION && (className.startsWith("java/io/ObjectStreamClass")
				|| className.equals("java/io/ObjectStreamField"))) {
			return true;
		} else {
			return className.startsWith("java/math/BigInteger");
		}
	}

	public void setLvs(LocalVariableManager lvs) {
		this.lvs = lvs;
	}


	private void maskMethodInvoke() {
		// method owner [Args
		// Try the fastpath where we know we don't change the method orig version
		if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "fixAllArgs", "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;"
					+ Type.getDescriptor(ControlTaintTagStack.class) + ")" + Type.getDescriptor(MethodInvoke.class), false);
		} else {
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
		if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
			super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
	}

	private void maskConstructorNewInstance() {
		if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
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

	private void maskGetter(String owner, Type[] args) {
		String desc = String.format("(L%s;Z)L%s;", owner, owner);
		if(args.length == 0) {
			super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", desc, false);
		} else if(args.length == 1) {
			super.visitInsn(Opcodes.SWAP);
			super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", desc, false);
			super.visitInsn(Opcodes.SWAP);
		} else if(args.length == 2) {
			int lv1 = lvs.getTmpLV();
			super.visitVarInsn(Opcodes.ASTORE, lv1);
			int lv2 = lvs.getTmpLV();
			super.visitVarInsn(Opcodes.ASTORE, lv2);
			super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getOrigMethod", desc, false);
			super.visitVarInsn(Opcodes.ALOAD, lv2);
			super.visitVarInsn(Opcodes.ALOAD, lv1);
			lvs.freeTmpLV(lv1);
			lvs.freeTmpLV(lv2);
		}
	}
	
	/* Return whether a method instruction with the specified information is a phosphor-added getter specified in Unsafe
	 *  for a field of a Java heap object. */
	private boolean isUnsafeHeapObjectGetter(int opcode, String owner, String name, String desc, Type[] args) {
		if(className.equals("sun/misc/Unsafe") || opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
			return false;
		} else if(!(name.startsWith("getByte") || name.startsWith("getBoolean") || name.startsWith("getChar") ||
					name.startsWith("getDouble") || name.startsWith("getFloat") || name.startsWith("getInt") ||
					name.startsWith("getLong") || name.startsWith("getShort") || name.startsWith("getObject"))) {
			return false;
		} else {
			return Type.getReturnType(desc).getSort() != Type.VOID && args.length > 0 &&
					args[0].getClassName().equals("java.lang.Object");
		}
	}

	/* Calls getTagOrOriginalField. */
	private void maskUnsafeHeapObjectGetter(int opcode, String owner, String name, String desc, boolean isInterface, Type[] args) {
		// Store the arguments for the original method call
		int[] localVars = new int[args.length-1];
		for(int i = args.length-1; i >= 1; i--) {
			int lv = lvs.getTmpLV();
			super.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), lv);
			localVars[i-1] = lv;
		}
		// Copy the Unsafe
		super.visitInsn(DUP2);
		for(int i = 0; i < localVars.length; i++) {
			super.visitVarInsn(args[i+1].getOpcode(Opcodes.ILOAD), localVars[i]);
		}
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		super.visitTypeInsn(CHECKCAST,  Type.getInternalName(Object.class));
		super.visitVarInsn(args[2].getOpcode(Opcodes.ILOAD), localVars[1]);
		if(args[2].getSort() == Type.INT) {
			// Cast int offsets to longs
			super.visitInsn(I2L);
		}
		for(int lv : localVars) {
			lvs.freeTmpLV(lv);
		}
		super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), "getTagAndOriginalField", "(Lsun/misc/Unsafe;Ljava/lang/Object;Ljava/lang/Object;J)Ljava/lang/Object;", false);
		super.visitTypeInsn(CHECKCAST,  Type.getReturnType(desc).getInternalName());
	}

	/* Return whether a method instruction with the specified information is a phosphor-added setter specified in Unsafe
	 *  for a field of a Java heap object. */
	private boolean isUnsafeHeapObjectSetter(int opcode, String owner, String name, String desc, Type[] args) {
		if(className.equals("sun/misc/Unsafe") || opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
			return false;
		} else {
			return name.startsWith("put") && Type.getReturnType(desc).getSort() == Type.VOID && args.length > 0 &&
					args[0].getClassName().equals("java.lang.Object");
		}
	}

	/* Calls putTagOrOriginalField to set the field associated with the field about to be set using Unsafe.put. */
	private void maskUnsafeHeapObjectSetter(String name, Type[] args) {
		// Store the arguments for the original method call
		int[] localVars = new int[args.length-1];
		int j = localVars.length - 1;
		for(int i = args.length-1; i >=1; i--) {
			int lv = lvs.getTmpLV();
			super.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), lv);
			localVars[j--] = lv;
		}
		// Copy the Unsafe and Object arguments
		super.visitInsn(DUP2);
		// Put the offset onto the stack
		Type offsetType = args[2];
		super.visitVarInsn(offsetType.getOpcode(Opcodes.ILOAD), localVars[1]);
		if(offsetType.getSort() == Type.INT) {
			// Cast int offsets to longs
			super.visitInsn(I2L);
		}
		// Put the taint tag or object onto the stack
		super.visitVarInsn(args[3].getOpcode(Opcodes.ILOAD), localVars[2]);
		// Call putTag
		String lastArg = Configuration.TAINT_TAG_DESC;
		if(name.contains("Object")) {
			lastArg = "Ljava/lang/Object;";
		}
		String desc = String.format("(Lsun/misc/Unsafe;Ljava/lang/Object;J%s)V", lastArg);
		super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), "putTagOrOriginalField", desc, false);
		// Restore the arguments for the original method call
		for(int i = 1; i < args.length; i++) {
			int lv = localVars[i-1];
			super.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), lv);
			lvs.freeTmpLV(lv);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		Type[] args = Type.getArgumentTypes(desc);
		if(isObjOutputStream && name.equals("getClass")) {
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "getClassOOS", "(Ljava/lang/Object;)Ljava/lang/Class;", false);
		} else if((disable || className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream")) && owner.equals("java/lang/Class") && !owner.equals(className) && name.startsWith("isInstance$$PHOSPHORTAGGED")) {
			// Even if we are ignoring other hiding here, we definitely need to do this.
			String retDesc = "Ledu/columbia/cs/psl/phosphor/struct/TaintedBooleanWith" + (Configuration.MULTI_TAINTING ? "Obj" : "Int") + "Tag;";
			String newDesc = "(Ljava/lang/Class;Ljava/lang/Object;";
			if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
				newDesc += Type.getDescriptor(ControlTaintTagStack.class);
			}
			newDesc += (retDesc + ")" + retDesc);
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "isInstance", newDesc, false);
		} else if(disable) {
			if((this.methodName.startsWith("setObjFieldValues") || this.className.startsWith("java/math/BigInteger")) && owner.equals("sun/misc/Unsafe") && (name.startsWith("putObject") || name.startsWith("compareAndSwapObject"))) {
				owner = Type.getInternalName(ReflectionMasker.class);
				super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
			} else if(this.methodName.startsWith("getObjFieldValues") && owner.equals("sun/misc/Unsafe") && name.startsWith("getObject")) {
				owner = Type.getInternalName(ReflectionMasker.class);
				super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
			} else if((this.methodName.startsWith("getPrimFieldValues") || this.methodName.startsWith("setPrimFieldValues")) && owner.equals("sun/misc/Unsafe") && (name.startsWith("put") || name.startsWith("get"))) {
				name = name + "$$NOUNBOX";
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			} else {
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			}
		} else {
			String nameWithoutSuffix = name.replace(TaintUtils.METHOD_SUFFIX,"");
			if("java/lang/reflect/Method".equals(owner)) {
				if(name.startsWith("invoke")) {
					maskMethodInvoke();
				} else if(name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.startsWith("java/lang/Class")) {
					maskGetter(owner, args);
				}
			} else if("java/lang/reflect/Constructor".equals(owner)) {
				if(name.startsWith("newInstance")) {
					maskConstructorNewInstance();
				} else if(name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.equals("java/lang/Class")) {
					maskGetter(owner, args);
				}
			} else if("java/lang/Class".equals(owner)) {
				if(nameWithoutSuffix.equals("getMethod") || nameWithoutSuffix.equals("getDeclaredMethod")) {
					opcode = Opcodes.INVOKESTATIC;
					owner = Type.getInternalName(ReflectionMasker.class);
					desc = "(Ljava/lang/Class;" + desc.substring(1);
					if(!Configuration.IMPLICIT_TRACKING && !Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
						desc = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;Z)Ljava/lang/reflect/Method;";
						super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					}
				} else if(nameWithoutSuffix.equals("getConstructor") || nameWithoutSuffix.equals("getDeclaredConstructor")) {
					// Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
					if(Configuration.IMPLICIT_TRACKING) {
						super.visitInsn(DUP_X2);
						super.visitInsn(POP);
						super.visitInsn(SWAP);
						super.visitInsn(DUP_X2);
						super.visitInsn(SWAP);
					} else {
						super.visitInsn(SWAP);
						super.visitInsn(DUP_X1);
						super.visitInsn(SWAP);
					}
					super.visitInsn((Configuration.IMPLICIT_TRACKING ? ICONST_1 : ICONST_0));
					super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "addTypeParams", "(Ljava/lang/Class;[Ljava/lang/Class;ZZ)[Ljava/lang/Class;", false);
					if(Configuration.IMPLICIT_TRACKING) {
						super.visitInsn(SWAP);
					}
				}
			}
			if(owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
				owner = Type.getInternalName(ArrayReflectionMasker.class);
			}
			if(owner.equals("java/lang/reflect/Field")
					&& opcode == Opcodes.INVOKEVIRTUAL
					&& (name.equals("get") || name.equals("get$$PHOSPHORTAGGED") || name.equals("set$$PHOSPHORTAGGED") || name.equals("getInt$$PHOSPHORTAGGED") || name.equals("getBoolean$$PHOSPHORTAGGED") || name.equals("getChar$$PHOSPHORTAGGED")
					|| name.equals("getDouble$$PHOSPHORTAGGED") || name.equals("getByte$$PHOSPHORTAGGED") || name.equals("getFloat$$PHOSPHORTAGGED") || name.equals("getLong$$PHOSPHORTAGGED")
					|| name.equals("getShort$$PHOSPHORTAGGED") || name.equals("setAccessible$$PHOSPHORTAGGED") || name.equals("set") || name.equals("setInt$$PHOSPHORTAGGED")
					|| name.equals("setBoolean$$PHOSPHORTAGGED") || name.equals("setChar$$PHOSPHORTAGGED") || name.equals("setDouble$$PHOSPHORTAGGED") || name.equals("setByte$$PHOSPHORTAGGED")
					|| name.equals("setFloat$$PHOSPHORTAGGED") || name.equals("setLong$$PHOSPHORTAGGED") || name.equals("setShort$$PHOSPHORTAGGED") || name.equals("getType") || name.equals("getType$$PHOSPHORTAGGED"))) {
				owner = Type.getInternalName(RuntimeReflectionPropogator.class);
				opcode = Opcodes.INVOKESTATIC;
				desc = "(Ljava/lang/reflect/Field;" + desc.substring(1);
				if(name.equals("get")) {
					desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;Z)Ljava/lang/Object;";
					super.visitInsn((Configuration.MULTI_TAINTING ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
				} else if(name.equals("set")) {
					desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;Z)V";
					super.visitInsn((Configuration.MULTI_TAINTING ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
				}
			}
			if(isUnsafeHeapObjectSetter(opcode, owner, name, desc, args)) {
				maskUnsafeHeapObjectSetter(name, args);
			} else if(isUnsafeHeapObjectGetter(opcode, owner, name, desc, args)) {
				maskUnsafeHeapObjectGetter(opcode, owner, name, desc, isInterface, args);
				return;
			}
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			if(owner.equals("java/lang/Class") && desc.endsWith("[Ljava/lang/reflect/Field;") && !className.equals("java/lang/Class")) {
				if(!Configuration.WITHOUT_FIELD_HIDING)
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintFields", "([Ljava/lang/reflect/Field;)[Ljava/lang/reflect/Field;", false);
			} else if(owner.equals("java/lang/Class") && !className.equals(owner) &&  (desc.equals("()[Ljava/lang/reflect/Method;") || desc.equals("("+Type.getDescriptor(ControlTaintTagStack.class)+")[Ljava/lang/reflect/Method;"))) {
				super.visitInsn("getMethods".equals(nameWithoutSuffix) ? ICONST_0 : ICONST_1);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintMethods", "([Ljava/lang/reflect/Method;Z)[Ljava/lang/reflect/Method;", false);
			} else if(owner.equals("java/lang/Class") && !className.equals(owner) &&  (desc.equals("()[Ljava/lang/reflect/Constructor;") || desc.equals("("+Type.getDescriptor(ControlTaintTagStack.class)+")[Ljava/lang/reflect/Constructor;"))) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintConstructors",
						"([Ljava/lang/reflect/Constructor;)[Ljava/lang/reflect/Constructor;", false);
			} else if(owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintedInterface", "([Ljava/lang/Class;)[Ljava/lang/Class;", false);
			} else if(owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && desc.equals("()" + "[" + Type.getDescriptor(StackTraceElement.class))) {
				String stackTraceElDesc = "[" + Type.getDescriptor(StackTraceElement.class);
				if(className.equals("java/lang/Throwable")) {
					super.visitVarInsn(Opcodes.ALOAD, 0);
					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				} else
					super.visitLdcInsn(Type.getObjectType(className));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeExtraStackTraceElements",
						"(" + stackTraceElDesc + "Ljava/lang/Class;)" + stackTraceElDesc, false);
			} else if(owner.equals("java/lang/Object") && name.equals("getClass") && !isObjOutputStream) {
				super.visitInsn((Configuration.MULTI_TAINTING ? ICONST_1 : ICONST_0));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionMasker.class), "removeTaintClass", "(Ljava/lang/Class;Z)Ljava/lang/Class;", false);
			}
			if((owner.equals("java/lang/reflect/Method") || owner.equals("java/lang/reflect/Constructor")) && !(className.equals("java/lang/Class")) &&
					(name.equals("invoke") || name.equals("newInstance") || name.equals("invoke$$PHOSPHORTAGGED")
							|| name.equals("newInstance$$PHOSPHORTAGGED"))) {
				// Unbox if necessary
				FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
				fn.type = Opcodes.F_NEW;
				super.visitInsn(Opcodes.DUP);
				super.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)));
				Label notPrimitive = new Label();
				super.visitJumpInsn(Opcodes.IFEQ, notPrimitive);
				FrameNode fn2 = TaintAdapter.getCurrentFrameNode(analyzer);
				fn2.type = Opcodes.F_NEW;
				super.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)));
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName((Configuration.MULTI_TAINTING ? TaintedPrimitiveWithObjTag.class : TaintedPrimitiveWithIntTag.class)), "toPrimitiveType", "()Ljava/lang/Object;", false);
				super.visitLabel(notPrimitive);
				fn2.accept(this);
			}
		}
	}
}
