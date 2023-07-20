package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.runtime.*;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.RuntimeSunMiscUnsafePropagator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;

public class ReflectionHidingMV extends MethodVisitor implements Opcodes {

    private final String className;
    private final String methodName;
    private final boolean disable;
    private final boolean isEnumValueOf;
    private final boolean patchAnonymousClasses;
    private LocalVariableManager lvs;

    public ReflectionHidingMV(MethodVisitor mv, String className, String name, boolean isEnum) {
        super(Configuration.ASM_VERSION, mv);
        this.className = className;
        this.methodName = name;
        this.disable = shouldDisable(className, name);
        this.patchAnonymousClasses = className.equals("java/lang/invoke/InnerClassLambdaMetafactory") || className.equals("sun/reflect/ClassDefiner");
        this.isEnumValueOf = isEnum && name.equals("valueOf");
    }

    public void setLvs(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    private int[] storeToLocals(int n) {
        int[] ret = new int[n];
        for (int i = 0; i < n; i++) {
            ret[i] = lvs.getTmpLV();
            super.visitVarInsn(ASTORE, ret[i]);
        }
        return ret;
    }

    private void loadAndFree(int[] r) {
        for (int i = r.length - 1; i >= 0; i--) {
            super.visitVarInsn(ALOAD, r[i]);
            lvs.freeTmpLV(r[i]);
        }
    }

    private void maskGetter(TaintMethodRecord mask, Type[] args) {
        int[] tmps = storeToLocals(args.length);
        visit(mask);
        loadAndFree(tmps);
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that retrieves the value of a field of a Java heap object. */
    private boolean isUnsafeFieldGetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if (opcode != INVOKEVIRTUAL || !Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if (args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
                return false;
            }
            if (Configuration.IS_JAVA_8) {
                switch (nameWithoutSuffix) {
                    case "getBoolean":
                    case "getByte":
                    case "getChar":
                    case "getDouble":
                    case "getFloat":
                    case "getInt":
                    case "getLong":
                    case "getObject":
                    case "getShort":
                    case "getBooleanVolatile":
                    case "getByteVolatile":
                    case "getCharVolatile":
                    case "getDoubleVolatile":
                    case "getFloatVolatile":
                    case "getLongVolatile":
                    case "getIntVolatile":
                    case "getObjectVolatile":
                    case "getShortVolatile":
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that sets the value of a field of a Java heap object. */
    private boolean isUnsafeFieldSetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if (opcode != INVOKEVIRTUAL || !Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if (args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
                return false;
            }
            if (Configuration.IS_JAVA_8) {
                switch (nameWithoutSuffix) {
                    case "putBoolean":
                    case "putByte":
                    case "putChar":
                    case "putDouble":
                    case "putFloat":
                    case "putInt":
                    case "putLong":
                    case "putObject":
                    case "putShort":
                    case "putBooleanVolatile":
                    case "putByteVolatile":
                    case "putCharVolatile":
                    case "putDoubleVolatile":
                    case "putFloatVolatile":
                    case "putIntVolatile":
                    case "putLongVolatile":
                    case "putObjectVolatile":
                    case "putShortVolatile":
                    case "putOrderedInt":
                    case "putOrderedLong":
                    case "putOrderedObject":
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * for a compareAndSwap method. */
    private boolean isUnsafeCAS(String owner, String name, String nameWithoutSuffix) {
        if (!Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if (Configuration.IS_JAVA_8) {
                return "compareAndSwapInt".equals(nameWithoutSuffix)
                        || "compareAndSwapLong".equals(nameWithoutSuffix)
                        || "compareAndSwapObject".equals(nameWithoutSuffix);
            }
            return false;
        }
    }

    private boolean isUnsafeIntrinsic(String owner, String name, String desc) {
        if(Configuration.IS_JAVA_8){
            return false; //These intrinsics are only for 9+
        }
        if (!Instrumenter.isUnsafeClass(owner)) {
            return false;
        }
        // Java 11 uses get/putObject instead of Reference
        name = name.replace("Object", "Reference");
        switch (desc) {
            case "(Ljava/lang/Object;JLjava/lang/Object;)V":
                switch (name) {
                    case "putReference":
                    case "putReferenceVolatile":
                    case "putReferenceOpaque":
                    case "putReferenceRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JZ)V":
                switch (name) {
                    case "putBoolean":
                    case "putBooleanVolatile":
                    case "putBooleanOpaque":
                    case "putBooleanRelease":
                        return true;
                }
            case "(Ljava/lang/Object;J)B":
                switch (name) {
                    case "getByte":
                    case "getByteVolatile":
                    case "getByteOpaque":
                    case "getByteAcquire":
                        return true;
                }
            case "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z":
                switch (name) {
                    case "compareAndSetReference":
                    case "weakCompareAndSetReferencePlain":
                    case "weakCompareAndSetReferenceAcquire":
                    case "weakCompareAndSetReferenceRelease":
                    case "weakCompareAndSetReference":
                        return true;
                }
            case "(Ljava/lang/Object;JJJ)J":
                switch (name) {
                    case "compareAndExchangeLong":
                    case "compareAndExchangeLongAcquire":
                    case "compareAndExchangeLongRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JB)B":
                switch (name) {
                    case "getAndAddByte":
                    case "getAndSetByte":
                        return true;
                }
            case "(Ljava/lang/Object;JJJ)Z":
                switch (name) {
                    case "compareAndSetLong":
                    case "weakCompareAndSetLongPlain":
                    case "weakCompareAndSetLongAcquire":
                    case "weakCompareAndSetLongRelease":
                    case "weakCompareAndSetLong":
                        return true;
                }
            case "(Ljava/lang/Object;JBB)Z":
                switch (name) {
                    case "compareAndSetByte":
                    case "weakCompareAndSetBytePlain":
                    case "weakCompareAndSetByteAcquire":
                    case "weakCompareAndSetByteRelease":
                    case "weakCompareAndSetByte":
                        return true;
                }
            case "(Ljava/lang/Object;JI)I":
                switch (name) {
                    case "getAndAddInt":
                    case "getAndSetInt":
                        return true;
                }
            case "(Ljava/lang/Object;JSS)S":
                switch (name) {
                    case "compareAndExchangeShort":
                    case "compareAndExchangeShortAcquire":
                    case "compareAndExchangeShortRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JSS)Z":
                switch (name) {
                    case "compareAndSetShort":
                    case "weakCompareAndSetShortPlain":
                    case "weakCompareAndSetShortAcquire":
                    case "weakCompareAndSetShortRelease":
                    case "weakCompareAndSetShort":
                        return true;
                }
            case "(Ljava/lang/Object;JJ)J":
                switch (name) {
                    case "getAndAddLong":
                    case "getAndSetLong":
                        return true;
                }
            case "(Ljava/lang/Object;J)Ljava/lang/Object;":
                switch (name) {
                    case "getReference":
                    case "getReferenceVolatile":
                    case "getReferenceOpaque":
                    case "getReferenceAcquire":
                        return true;
                }
            case "(Ljava/lang/Object;JD)V":
                switch (name) {
                    case "putDouble":
                    case "putDoubleVolatile":
                    case "putDoubleOpaque":
                    case "putDoubleRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JII)I":
                switch (name) {
                    case "compareAndExchangeInt":
                    case "compareAndExchangeIntAcquire":
                    case "compareAndExchangeIntRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JLjava/lang/Object;)Ljava/lang/Object;":
                switch (name) {
                    case "getAndSetReference":
                        return true;
                }
            case "(Ljava/lang/Object;JC)V":
                switch (name) {
                    case "putChar":
                    case "putCharVolatile":
                    case "putCharOpaque":
                    case "putCharRelease":
                    case "putCharUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;J)Z":
                switch (name) {
                    case "getBoolean":
                    case "getBooleanVolatile":
                    case "getBooleanOpaque":
                    case "getBooleanAcquire":
                        return true;
                }
            case "(Ljava/lang/Object;JB)V":
                switch (name) {
                    case "putByte":
                    case "putByteVolatile":
                    case "putByteOpaque":
                    case "putByteRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;":
                switch (name) {
                    case "compareAndExchangeReference":
                    case "compareAndExchangeReferenceAcquire":
                    case "compareAndExchangeReferenceRelease":
                        return true;
                }
            case "(Ljava/lang/Object;J)S":
                switch (name) {
                    case "getShort":
                    case "getShortVolatile":
                    case "getShortOpaque":
                    case "getShortAcquire":
                    case "getShortUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;JF)V":
                switch (name) {
                    case "putFloat":
                    case "putFloatVolatile":
                    case "putFloatOpaque":
                    case "putFloatRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JJ)V":
                switch (name) {
                    case "putLong":
                    case "putLongVolatile":
                    case "putLongOpaque":
                    case "putLongRelease":
                    case "putLongUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;JI)V":
                switch (name) {
                    case "putInt":
                    case "putIntVolatile":
                    case "putIntOpaque":
                    case "putIntRelease":
                    case "putIntUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;JBB)B":
                switch (name) {
                    case "compareAndExchangeByte":
                    case "compareAndExchangeByteAcquire":
                    case "compareAndExchangeByteRelease":
                        return true;
                }
            case "(Ljava/lang/Object;JS)S":
                switch (name) {
                    case "getAndAddShort":
                    case "getAndSetShort":
                        return true;
                }
            case "(Ljava/lang/Object;JS)V":
                switch (name) {
                    case "putShort":
                    case "putShortVolatile":
                    case "putShortOpaque":
                    case "putShortRelease":
                    case "putShortUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;JII)Z":
                switch (name) {
                    case "compareAndSetInt":
                    case "weakCompareAndSetIntPlain":
                    case "weakCompareAndSetIntAcquire":
                    case "weakCompareAndSetIntRelease":
                    case "weakCompareAndSetInt":
                        return true;
                }
            case "(Ljava/lang/Object;J)J":
                switch (name) {
                    case "getLong":
                    case "getLongVolatile":
                    case "getLongOpaque":
                    case "getLongAcquire":
                    case "getLongUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;J)I":
                switch (name) {
                    case "getInt":
                    case "getIntVolatile":
                    case "getIntOpaque":
                    case "getIntAcquire":
                    case "getIntUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;J)D":
                switch (name) {
                    case "getDouble":
                    case "getDoubleVolatile":
                    case "getDoubleOpaque":
                    case "getDoubleAcquire":
                        return true;
                }
            case "(Ljava/lang/Object;J)C":
                switch (name) {
                    case "getChar":
                    case "getCharVolatile":
                    case "getCharOpaque":
                    case "getCharAcquire":
                    case "getCharUnaligned":
                        return true;
                }
            case "(Ljava/lang/Object;J)F":
                switch (name) {
                    case "getFloat":
                    case "getFloatVolatile":
                    case "getFloatOpaque":
                    case "getFloatAcquire":
                        return true;
                }
        }
        return false;
    }

    private boolean isUnsafeCopyMemory(String owner, String name, String nameWithoutSuffix) {
        if (Instrumenter.isUnsafeClass(owner)) {
            switch (nameWithoutSuffix) {
                case "copyMemory":
                case "copySwapMemory":
                    return true;
            }

        }
        return false;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (this.className.equals("java/lang/invoke/MethodHandles$Lookup") && this.methodName.startsWith("defineHiddenClass")) {
            super.visitVarInsn(ALOAD, 1);
            INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
            super.visitVarInsn(ASTORE, 1);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        String nameWithoutSuffix = name;
        String descWithoutStackFrame = desc.replace(PhosphorStackFrame.DESCRIPTOR, "");
        if ((disable || className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream")) && owner.equals("java/lang/Class") && !owner.equals(className) && name.startsWith("isInstance")) {
            // Even if we are ignoring other hiding here, we definitely need to do this.
            visit(IS_INSTANCE);
        } else if (disable) {
            if (this.methodName.startsWith("setObjFieldValues") && owner.equals("sun/misc/Unsafe") && (name.startsWith("putObject") || name.startsWith("compareAndSwapObject"))) {
                owner = getRuntimeUnsafePropogatorClassName();
                super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
            } else if (this.methodName.startsWith("getObjFieldValues") && owner.equals("sun/misc/Unsafe") && name.startsWith("getObject")) {
                owner = getRuntimeUnsafePropogatorClassName();
                super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
            } else if ((this.methodName.startsWith("getPrimFieldValues") || this.methodName.startsWith("setPrimFieldValues")) && owner.equals("sun/misc/Unsafe") && (name.startsWith("put") || name.startsWith("get"))) {
                //name = name + "$$NOUNBOX";
                //TODO
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            }
        } else {
            if (patchAnonymousClasses && name.equals("defineAnonymousClass") && Instrumenter.isUnsafeClass(owner) && descWithoutStackFrame.equals("(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;")) {
                super.visitInsn(POP);
                super.visitInsn(SWAP);
                INSTRUMENT_CLASS_BYTES_ANONYMOUS.delegateVisit(mv);
                super.visitInsn(SWAP);
                desc = descWithoutStackFrame; // Go directly to the native call
            } else if (patchAnonymousClasses && name.equals("defineClass")
                    && Configuration.IS_JAVA_8 && "sun/misc/Unsafe".equals(owner)
                    && descWithoutStackFrame.equals("(Ljava/lang/String;[BIILjava/lang/ClassLoader;Ljava/security/ProtectionDomain;)Ljava/lang/Class;")) {
                desc = "(L" + owner + ";" + desc.substring(1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            }
            /*
             * Fix for #188 - if we are in a wrapped method, and called by the wrapper, we need to get the caller class
             * of the wrapper, not of this stack frame
             */
            if(owner.equals("jdk/internal/reflect/Reflection") && name.equals("getCallerClass")){
                int phosphorStackFrame = lvs.getLocalVariableAdder().getIndexOfPhosphorStackData();
                super.visitVarInsn(ALOAD, phosphorStackFrame);
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                super.visitLdcInsn(Type.getObjectType(className));
                GET_CALLER_CLASS_WRAPPER.delegateVisit(mv);
                return;
            }
            if (owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
                owner = Type.getInternalName(ArrayReflectionMasker.class);
            }
            if (name.equals("allocateUninitializedArray") && Instrumenter.isUnsafeClass(owner)) {
                desc = "(L" + owner + ";" + desc.substring(1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if (isUnsafeIntrinsic(owner, name, descWithoutStackFrame) || isUnsafeFieldGetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    //in control tracking mode, pop the control stack off of the stack to reuse the existing method
                    //but first, pop the null that's there for the erased return type.
                    if (isUnsafeReferenceFieldGetter(nameWithoutSuffix)) {
                        super.visitInsn(POP);
                    }
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                    if (isUnsafeReferenceFieldGetter(nameWithoutSuffix)) {
                        super.visitInsn(ACONST_NULL);
                    }
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                // Java 11 uses get/putObject instead of Reference
                name = name.replace("Object", "Reference");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if (isUnsafeFieldSetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(POP);
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if (isUnsafeCAS(owner, name, nameWithoutSuffix) || isUnsafeCopyMemory(owner, name, nameWithoutSuffix)) {
                if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                owner = getRuntimeUnsafePropogatorClassName();
                opcode = INVOKESTATIC;
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                return;
            } else if (owner.equals(Type.getInternalName(Character.class)) && (name.equals("codePointAt")
                    || name.equals("toChars") || name.equals("codePointBefore") || name.equals("reverseBytes")
                    || name.equals("toLowerCase") || name.equals("toTitleCase") || name.equals("toUpperCase"))) {
                owner = Type.getInternalName(CharacterUtils.class);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv);
            } else if ((owner.equals("sun/reflect/NativeMethodAccessorImpl") || owner.equals("jdk/internal/reflect/NativeMethodAccessorImpl")) && name.equals("invoke0")) {
                //Stack: Method Receiver Args StackData
                PREPARE_FOR_CALL_REFLECTIVE.delegateVisit(this);
                String methodTupleInternalName = Type.getInternalName(ReflectionMasker.MethodInvocationTuple.class);
                mv.visitInsn(DUP);
                mv.visitFieldInsn(GETFIELD, methodTupleInternalName, "method", Type.getType(Method.class).getDescriptor());
                mv.visitInsn(SWAP);
                mv.visitInsn(DUP);
                mv.visitFieldInsn(GETFIELD, methodTupleInternalName, "receiver", "Ljava/lang/Object;");
                mv.visitInsn(SWAP);
                mv.visitFieldInsn(GETFIELD, methodTupleInternalName, "args", "[Ljava/lang/Object;");
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
            } else if ((owner.equals("sun/reflect/NativeConstructorAccessorImpl") || owner.equals("jdk/internal/reflect/NativeConstructorAccessorImpl")) && name.equals("newInstance0")) {
                PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR.delegateVisit(this);
                String constructorInvocationPairInternalName = Type.getInternalName(ReflectionMasker.ConstructorInvocationPair.class);
                mv.visitInsn(DUP);
                mv.visitFieldInsn(GETFIELD, constructorInvocationPairInternalName, "constructor", Type.getDescriptor(Constructor.class));
                mv.visitInsn(SWAP);
                mv.visitFieldInsn(GETFIELD, constructorInvocationPairInternalName, "args", "[Ljava/lang/Object;");
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
            }
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if (owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Fields") && !className.equals("java/lang/Class")) {
                if (!Configuration.WITHOUT_FIELD_HIDING) {
                    visit(REMOVE_TAINTED_FIELDS);
                }
            } else if (owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Methods") && !className.equals(owner) && descWithoutStackFrame.equals("()" + Type.getDescriptor(Method[].class))) {
                visit(REMOVE_TAINTED_METHODS);
            } else if (owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Constructors") && !className.equals(owner) && descWithoutStackFrame.equals("()" + Type.getDescriptor(Constructor[].class))) {
                visit(REMOVE_TAINTED_CONSTRUCTORS);
            } else if (owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
                visit(REMOVE_TAINTED_INTERFACES);
            } else if (owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && descWithoutStackFrame.equals("()" + Type.getDescriptor(StackTraceElement[].class))) {
                //if (className.equals("java/lang/Throwable")) {
                //    super.visitVarInsn(ALOAD, 0);
                //    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                //} else {
                //    super.visitLdcInsn(Type.getObjectType(className));
                //}
                //visit(REMOVE_EXTRA_STACK_TRACE_ELEMENTS);
            } else if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                UNWRAP_RETURN.delegateVisit(mv);
            }
        }
    }

    private String controlTrackDescOrNone() {
        return (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING ? CONTROL_STACK_DESC : "");
    }

    /**
     * Visits a method instruction for the specified method.
     *
     * @param method the method to be visited
     */
    private void visit(TaintMethodRecord method) {
        super.visitMethodInsn(method.getOpcode(), method.getOwner(), method.getName(), method.getDescriptor(), method.isInterface());
    }

    private static boolean shouldDisable(String className, String methodName) {
        if (className.equals("org/codehaus/groovy/vmplugin/v5/Java5") && methodName.equals("makeInterfaceTypes")) {
            return true;
        } else {
            if (className.equals("jdk/internal/reflect/ReflectionFactory") || className.equals("java/lang/reflect/ReflectAccess")) {
                //Java >= 9
                //TODO keep?
                return true;
            }
            return Configuration.TAINT_THROUGH_SERIALIZATION && !methodName.equals("getDeclaredSerialFields$$PHOSPHORTAGGED") &&
                    (className.startsWith("java/io/ObjectStreamClass") || className.equals("java/io/ObjectStreamField"));
        }
    }

    private static String getRuntimeUnsafePropogatorClassName() {
        if (Configuration.IS_JAVA_8) {
            return Type.getInternalName(RuntimeSunMiscUnsafePropagator.class);
        }
        return Type.getInternalName(RuntimeJDKInternalUnsafePropagator.class);
    }

    private static boolean isUnsafeReferenceFieldGetter(String methodName) {
        if (Configuration.IS_JAVA_8) {
            return "getObject".equals(methodName) || "getObjectVolatile".equals(methodName);
        }
        // TODO Java 11?
        return "getReference".equals(methodName) || "getReferenceVolatile".equals(methodName);
    }
}
