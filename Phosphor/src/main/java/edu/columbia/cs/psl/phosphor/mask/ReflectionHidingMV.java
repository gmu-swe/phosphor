package edu.columbia.cs.psl.phosphor.mask;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.CharacterUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;

class ReflectionHidingMV extends ReflectionMV implements Opcodes {
    private final String className;
    private final String methodName;
    private final boolean patchAnonymousClasses;
    private LocalVariableManager lvs;

    ReflectionHidingMV(MethodVisitor mv, String className, String name) {
        super(Configuration.ASM_VERSION, mv);
        this.className = className;
        this.methodName = name;
        this.patchAnonymousClasses = className.equals("java/lang/invoke/InnerClassLambdaMetafactory")
                || className.equals("sun/reflect/ClassDefiner");
    }

    @Override
    public void setLvs(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that retrieves the value of a field of a Java heap object. */
    private boolean isUnsafeFieldGetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if (opcode != INVOKEVIRTUAL || !Phosphor.isUnsafeClass(owner)) {
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
        if (opcode != INVOKEVIRTUAL || !Phosphor.isUnsafeClass(owner)) {
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
    private boolean isUnsafeCAS(String owner, String nameWithoutSuffix) {
        if (Phosphor.isUnsafeClass(owner)) {
            if (Configuration.IS_JAVA_8) {
                return "compareAndSwapInt".equals(nameWithoutSuffix)
                        || "compareAndSwapLong".equals(nameWithoutSuffix)
                        || "compareAndSwapObject".equals(nameWithoutSuffix);
            }
        }
        return false;
    }

    private boolean isUnsafeIntrinsic(String owner, String name, String desc) {
        if (Configuration.IS_JAVA_8) {
            return false; // These intrinsics are only for 9+
        }
        if (!Phosphor.isUnsafeClass(owner)) {
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
        if (Phosphor.isUnsafeClass(owner)) {
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
        if (this.className.equals("java/lang/invoke/MethodHandles$Lookup")
                && this.methodName.startsWith("defineHiddenClass")) {
            super.visitVarInsn(ALOAD, 1);
            INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
            super.visitVarInsn(ASTORE, 1);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        String descWithoutStackFrame = desc.replace(PhosphorStackFrame.DESCRIPTOR, "");
        if ((className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream"))
                && owner.equals("java/lang/Class")
                && name.startsWith("isInstance")) {
            IS_INSTANCE.delegateVisit(mv);
            return;
        }
        switch (owner) {
            case "jdk/internal/reflect/Reflection":
                maskReflection(opcode, owner, name, desc, isInterface);
                return;
            case "java/lang/reflect/Array":
                maskArray(opcode, owner, name, desc, isInterface);
                return;
            case "java/lang/Character":
                maskCharacter(opcode, owner, name, desc, isInterface);
                return;
            case "sun/reflect/NativeMethodAccessorImpl":
            case "jdk/internal/reflect/NativeMethodAccessorImpl":
                maskMethodAccessor(opcode, owner, name, desc, isInterface);
                return;
            case "sun/reflect/NativeConstructorAccessorImpl":
            case "jdk/internal/reflect/NativeConstructorAccessorImpl":
                maskConstructorAccessor(opcode, owner, name, desc, isInterface);
                return;
            default:
                if (Phosphor.isUnsafeClass(owner)) {
                    patchUnsafe(opcode, owner, name, desc, isInterface, descWithoutStackFrame, args, name);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                    fixReturn(owner, name, name, descWithoutStackFrame);
                }
        }
    }

    private void patchUnsafe(
            int opcode,
            String owner,
            String name,
            String desc,
            boolean isInterface,
            String descWithoutStackFrame,
            Type[] args,
            String nameWithoutSuffix) {
        if (patchAnonymousClasses
                && name.equals("defineAnonymousClass")
                && descWithoutStackFrame.equals("(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;")) {
            super.visitInsn(POP);
            super.visitInsn(SWAP);
            INSTRUMENT_CLASS_BYTES_ANONYMOUS.delegateVisit(mv);
            super.visitInsn(SWAP);
            desc = descWithoutStackFrame; // Go directly to the native call
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        } else if (patchAnonymousClasses
                && name.equals("defineClass")
                && Configuration.IS_JAVA_8
                && descWithoutStackFrame.equals(
                        "(Ljava/lang/String;[BIILjava/lang/ClassLoader;Ljava/security/ProtectionDomain;)Ljava/lang/Class;")) {
            desc = "(Lsun/misc/Unsafe;" + desc.substring(1);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC, ReflectionMVFactory.getRuntimeUnsafePropagatorClassName(), name, desc, false);
        } else if (name.equals("allocateUninitializedArray")) {
            desc = "(L" + owner + ";" + desc.substring(1);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC, ReflectionMVFactory.getRuntimeUnsafePropagatorClassName(), name, desc, false);
        } else if (isUnsafeIntrinsic(owner, name, descWithoutStackFrame)
                || isUnsafeFieldGetter(opcode, owner, name, args, nameWithoutSuffix)) {
            if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                desc = desc.replace(CONTROL_STACK_DESC, "");
                // in control tracking mode, pop the control stack off of the stack to reuse the existing method
                // but first, pop the null that's there for the erased return type.
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
            if (isUnsafeIntrinsic(owner, name, descWithoutStackFrame)) {
                // Java 11 uses get/putObject instead of Reference
                name = name.replace("Object", "Reference");
            }
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC, ReflectionMVFactory.getRuntimeUnsafePropagatorClassName(), name, desc, false);
        } else if (isUnsafeFieldSetter(opcode, owner, name, args, nameWithoutSuffix)) {
            if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                desc = desc.replace(CONTROL_STACK_DESC, "");
                super.visitInsn(POP);
            }
            desc = "(L" + owner + ";" + desc.substring(1);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC, ReflectionMVFactory.getRuntimeUnsafePropagatorClassName(), name, desc, false);
        } else if (isUnsafeCAS(owner, nameWithoutSuffix) || isUnsafeCopyMemory(owner, name, nameWithoutSuffix)) {
            if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                desc = desc.replace(CONTROL_STACK_DESC, "");
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }
            desc = "(L" + owner + ";" + desc.substring(1);
            super.visitMethodInsn(
                    INVOKESTATIC, ReflectionMVFactory.getRuntimeUnsafePropagatorClassName(), name, desc, isInterface);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        }
    }

    private void maskReflection(int opcode, String owner, String name, String desc, boolean isInterface) {
        // If we in a wrapped method and called by the wrapper
        // get the caller class of the wrapper not this stack frame
        if (name.equals("getCallerClass")) {
            int phosphorStackFrame = lvs.getLocalVariableAdder().getIndexOfPhosphorStackData();
            super.visitVarInsn(ALOAD, phosphorStackFrame);
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            super.visitLdcInsn(Type.getObjectType(className));
            GET_CALLER_CLASS_WRAPPER.delegateVisit(mv);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        }
    }

    private void maskArray(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (!owner.equals(className)) {
            owner = Type.getInternalName(ArrayReflectionMasker.class);
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }

    private void maskCharacter(int opcode, String owner, String name, String desc, boolean isInterface) {
        switch (name) {
            case "codePointAt":
            case "toChars":
            case "codePointBefore":
            case "reverseBytes":
            case "toLowerCase":
            case "toTitleCase":
            case "toUpperCase":
                owner = Type.getInternalName(CharacterUtils.class);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv);
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }

    private void maskConstructorAccessor(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (name.equals("newInstance0")) {
            PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR.delegateVisit(this);
            String constructorInvocationPairInternalName =
                    Type.getInternalName(ReflectionMasker.ConstructorInvocationPair.class);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(
                    GETFIELD,
                    constructorInvocationPairInternalName,
                    "constructor",
                    Type.getDescriptor(Constructor.class));
            mv.visitInsn(SWAP);
            mv.visitFieldInsn(GETFIELD, constructorInvocationPairInternalName, "args", "[Ljava/lang/Object;");
            mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }

    private void maskMethodAccessor(int opcode, String owner, String name, String desc, boolean isInterface) {
        if (name.equals("invoke0")) {
            // Stack: Method Receiver Args StackData
            PREPARE_FOR_CALL_REFLECTIVE.delegateVisit(this);
            String methodTupleInternalName = Type.getInternalName(ReflectionMasker.MethodInvocationTuple.class);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(
                    GETFIELD,
                    methodTupleInternalName,
                    "method",
                    Type.getType(Method.class).getDescriptor());
            mv.visitInsn(SWAP);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, methodTupleInternalName, "receiver", "Ljava/lang/Object;");
            mv.visitInsn(SWAP);
            mv.visitFieldInsn(GETFIELD, methodTupleInternalName, "args", "[Ljava/lang/Object;");
            mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }

    private void fixReturn(String owner, String name, String nameWithoutSuffix, String descWithoutStackFrame) {
        if (owner.equals("java/lang/Class") && !className.equals("java/lang/Class")) {
            if (nameWithoutSuffix.endsWith("Fields")) {
                if (!Configuration.WITHOUT_FIELD_HIDING) {
                    REMOVE_TAINTED_FIELDS.delegateVisit(mv);
                }
            } else if (nameWithoutSuffix.endsWith("Methods")
                    && descWithoutStackFrame.equals("()" + Type.getDescriptor(Method[].class))) {
                REMOVE_TAINTED_METHODS.delegateVisit(mv);
            } else if (nameWithoutSuffix.endsWith("Constructors")
                    && descWithoutStackFrame.equals("()" + Type.getDescriptor(Constructor[].class))) {
                REMOVE_TAINTED_CONSTRUCTORS.delegateVisit(mv);
            } else if (name.equals("getInterfaces")) {
                REMOVE_TAINTED_INTERFACES.delegateVisit(mv);
            }
        } else if (owner.equals("java/lang/Throwable")) {
            if (name.equals("getOurStackTrace") || name.equals("getStackTrace")) {
                if (descWithoutStackFrame.equals("()" + Type.getDescriptor(StackTraceElement[].class))) {
                    // TODO REMOVE_EXTRA_STACK_TRACE_ELEMENTS?
                }
            }
        } else if (owner.equals("java/lang/reflect/Method")) {
            if (name.equals("invoke")) {
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                UNWRAP_RETURN.delegateVisit(mv);
            }
        }
    }

    private static boolean isUnsafeReferenceFieldGetter(String methodName) {
        if (Configuration.IS_JAVA_8) {
            return "getObject".equals(methodName) || "getObjectVolatile".equals(methodName);
        }
        // TODO Java 11?
        return "getReference".equals(methodName) || "getReferenceVolatile".equals(methodName);
    }
}
