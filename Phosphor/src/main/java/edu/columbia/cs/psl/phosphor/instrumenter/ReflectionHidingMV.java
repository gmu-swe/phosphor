package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.CharacterUtils;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.jdk.unsupported.RuntimeSunMiscUnsafePropagator;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;

//TODO need to find all other unsafe methods that are intrinsic candidates and make sure that we wrap them, too.
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
        this.patchAnonymousClasses = className.equals("java/lang/invoke/InnerClassLambdaMetafactory");
        this.isEnumValueOf = isEnum && name.equals("valueOf");
    }

    public void setLvs(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    private int[] storeToLocals(int n) {
        int[] ret = new int[n];
        for(int i = 0; i < n; i++) {
            ret[i] = lvs.getTmpLV();
            super.visitVarInsn(ASTORE, ret[i]);
        }
        return ret;
    }

    private void loadAndFree(int[] r) {
        for(int i = r.length - 1; i >= 0; i--) {
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
        if(opcode != INVOKEVIRTUAL || !Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if(args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
                return false;
            }
            if(Configuration.IS_JAVA_8) {
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
            } else{
                switch (nameWithoutSuffix) {
                    case "getInt":
                    case "getReference":
                    case "getBoolean":
                    case "getByte":
                    case "getShort":
                    case "getChar":
                    case "getLong":
                    case "getFloat":
                    case "getDouble":
                    case "getUncompressedObject":
                    case "getReferenceVolatile":
                    case "getIntVolatile":
                    case "getBooleanVolatile":
                    case "getByteVolatile":
                    case "getShortVolatile":
                    case "getCharVolatile":
                    case "getLongVolatile":
                    case "getFloatVolatile":
                    case "getDoubleVolatile":
                    case "getReferenceAcquire":
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that sets the value of a field of a Java heap object. */
    private boolean isUnsafeFieldSetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if(opcode != INVOKEVIRTUAL || !Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if(args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
                return false;
            }
            if(Configuration.IS_JAVA_8) {
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
            } else{
                switch (nameWithoutSuffix) {
                    case "putInt":
                    case "putReference":
                    case "putBoolean":
                    case "putByte":
                    case "putShort":
                    case "putChar":
                    case "putLong":
                    case "putFloat":
                    case "putDouble":
                    case "putReferenceVolatile":
                    case "putIntVolatile":
                    case "putBooleanVolatile":
                    case "putByteVolatile":
                    case "putShortVolatile":
                    case "putCharVolatile":
                    case "putLongVolatile":
                    case "putFloatVolatile":
                    case "putDoubleVolatile":
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * for a compareAndSwap method. */
    private boolean isUnsafeCAS(String owner, String name, String nameWithoutSuffix) {
        if(!Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            if(Configuration.IS_JAVA_8){
                return "compareAndSwapInt".equals(nameWithoutSuffix)
                        || "compareAndSwapLong".equals(nameWithoutSuffix)
                        || "compareAndSwapObject".equals(nameWithoutSuffix);
            } else{
                return "compareAndSetReference".equals(nameWithoutSuffix)
                        || "compareAndExchangeReference".equals(nameWithoutSuffix)
                        || "compareAndSetInt".equals(nameWithoutSuffix)
                        || "compareAndExchangeInt".equals(nameWithoutSuffix)
                        || "compareAndSetLong".equals(nameWithoutSuffix)
                        || "compareAndExchangeLong".equals(nameWithoutSuffix);
            }
        }
    }

    private boolean isUnsafeCopyMemory(String owner, String name, String nameWithoutSuffix) {
        if(!Instrumenter.isUnsafeClass(owner)) {
            return false;
        } else {
            return "copyMemory".equals(nameWithoutSuffix);
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if(this.className.equals("java/lang/invoke/MethodHandles$Lookup") && this.methodName.startsWith("defineHiddenClass")){
            super.visitVarInsn(ALOAD, 1);
            INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
            super.visitVarInsn(ASTORE, 1);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        String nameWithoutSuffix = name;
        Type returnType = Type.getReturnType(desc);
        if((disable || className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream")) && owner.equals("java/lang/Class") && !owner.equals(className) && name.startsWith("isInstance")) {
            // Even if we are ignoring other hiding here, we definitely need to do this.
            super.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
            visit(IS_INSTANCE);
        } else if(disable) {
            if(this.methodName.startsWith("setObjFieldValues") && owner.equals("sun/misc/Unsafe") && (name.startsWith("putObject") || name.startsWith("compareAndSwapObject"))) {
                owner = getRuntimeUnsafePropogatorClassName();
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv);
                super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
            } else if(this.methodName.startsWith("getObjFieldValues") && owner.equals("sun/misc/Unsafe") && name.startsWith("getObject")) {
                owner = getRuntimeUnsafePropogatorClassName();
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv);
                super.visitMethodInsn(INVOKESTATIC, owner, name, "(Lsun/misc/Unsafe;" + desc.substring(1), isInterface);
            } else if((this.methodName.startsWith("getPrimFieldValues") || this.methodName.startsWith("setPrimFieldValues")) && owner.equals("sun/misc/Unsafe") && (name.startsWith("put") || name.startsWith("get"))) {
                //name = name + "$$NOUNBOX";
                //TODO
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            }
        } else {
            if(patchAnonymousClasses && name.equals("defineAnonymousClass") && Instrumenter.isUnsafeClass(owner) && desc.equals("(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;")){
                super.visitInsn(SWAP);
                INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
                super.visitInsn(SWAP);
            }
            if(owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
                owner = Type.getInternalName(ArrayReflectionMasker.class);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, this);
            }
            if (name.equals("allocateUninitializedArray") && Instrumenter.isUnsafeClass(owner)) {
                desc = "(L" + owner + ";" + desc.substring(1);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, this);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if (isUnsafeFieldGetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    //in control tracking mode, pop the control stack off of the stack to reuse the existing method
                    //but first, pop the null that's there for the erased return type.
                    if(isUnsafeReferenceFieldGetter(nameWithoutSuffix)) {
                        super.visitInsn(POP);
                    }
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                    if(isUnsafeReferenceFieldGetter(nameWithoutSuffix)) {
                        super.visitInsn(ACONST_NULL);
                    }
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, this);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if(isUnsafeFieldSetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(POP);
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, this);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, getRuntimeUnsafePropogatorClassName(), name, desc, false);
                return;
            } else if(isUnsafeCAS(owner, name, nameWithoutSuffix) || isUnsafeCopyMemory(owner, name, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                }
                desc = "(L" + owner + ";" + desc.substring(1);
                owner = getRuntimeUnsafePropogatorClassName();
                opcode = INVOKESTATIC;
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, this);
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                return;
            } else if(owner.equals(Type.getInternalName(Character.class)) && (name.equals("codePointAt")
                    || name.equals("toChars") || name.equals("codePointBefore") || name.equals("reverseBytes")
                    || name.equals("toLowerCase") || name.equals("toTitleCase") || name.equals("toUpperCase"))) {
                desc = lvs.patchDescToAcceptPhosphorStackFrameAndPushIt(desc, mv);
                owner = Type.getInternalName(CharacterUtils.class);
            } else if(owner.equals("java/lang/reflect/Method") && name.equals("invoke")){
            } else if(owner.equals("java/lang/reflect/Constructor") && name.equals("newInstance")){
            } else if((owner.equals("sun/reflect/NativeMethodAccessorImpl") || owner.equals("jdk/internal/reflect/NativeMethodAccessorImpl")) && name.equals("invoke0")){
                //Stack: Method Receiver Args
                mv.visitInsn(SWAP);
                mv.visitInsn(DUP_X1);
                //Stack: Method Receiver Args Receiver
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                FIX_ALL_ARGS.delegateVisit(mv);

                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);
                mv.visitInsn(DUP_X2);
                super.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                PREPARE_FOR_CALL_REFLECTIVE.delegateVisit(this);
            } else if((owner.equals("sun/reflect/NativeConstructorAccessorImpl") || owner.equals("jdk/internal/reflect/NativeConstructorAccessorImpl")) && name.equals("newInstance0")){
                mv.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                FIX_ALL_ARGS_CONSTRUCTOR.delegateVisit(mv);

                mv.visitInsn(SWAP);
                mv.visitInsn(DUP);
                super.visitVarInsn(ALOAD, lvs.getLocalVariableAdder().getIndexOfPhosphorStackData());
                PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR.delegateVisit(this);
                mv.visitInsn(SWAP);
            }
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if(owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Fields") && !className.equals("java/lang/Class")) {
                if(!Configuration.WITHOUT_FIELD_HIDING) {
                    visit(REMOVE_TAINTED_FIELDS);
                }
            } else if(owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Methods") && !className.equals(owner) && desc.equals("()"+ Type.getDescriptor(Method[].class))) {
                visit(REMOVE_TAINTED_METHODS);
            } else if(owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Constructors") && !className.equals(owner) &&
                    desc.equals("(" + Configuration.TAINT_TAG_DESC + controlTrackDescOrNone() + Type.getDescriptor(TaintedReferenceWithObjTag.class) + Type.getDescriptor(Constructor[].class) + ")" + Type.getDescriptor(TaintedReferenceWithObjTag.class))) {
                visit(REMOVE_TAINTED_CONSTRUCTORS);
            } else if(owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
                visit(REMOVE_TAINTED_INTERFACES);
            } else if(owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && desc.equals("(" + Configuration.TAINT_TAG_DESC + controlTrackDescOrNone() + Type.getDescriptor(TaintedReferenceWithObjTag.class)  + Type.getDescriptor(StackTraceElement[].class) + ")" + Type.getDescriptor(TaintedReferenceWithObjTag.class))) {
                if(className.equals("java/lang/Throwable")) {
                    super.visitVarInsn(ALOAD, 0);
                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                } else {
                    super.visitLdcInsn(Type.getObjectType(className));
                }
                visit(REMOVE_EXTRA_STACK_TRACE_ELEMENTS);
            } else if(owner.equals("java/lang/reflect/Method") && name.equals("invoke")){
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
        if(className.equals("org/codehaus/groovy/vmplugin/v5/Java5") && methodName.equals("makeInterfaceTypes")) {
            return true;
        } else {
            if(className.equals("jdk/internal/reflect/ReflectionFactory") || className.equals("java/lang/reflect/ReflectAccess")){
                //Java >= 9
                //TODO keep?
                return true;
            }
            return Configuration.TAINT_THROUGH_SERIALIZATION && !methodName.equals("getDeclaredSerialFields$$PHOSPHORTAGGED") &&
                    (className.startsWith("java/io/ObjectStreamClass") || className.equals("java/io/ObjectStreamField"));
        }
    }

    private static String getRuntimeUnsafePropogatorClassName(){
        if(Configuration.IS_JAVA_8){
            return Type.getInternalName(RuntimeSunMiscUnsafePropagator.class);
        }
        return Type.getInternalName(RuntimeJDKInternalUnsafePropagator.class);
    }

    private static boolean isUnsafeReferenceFieldGetter(String methodName){
        if(Configuration.IS_JAVA_8){
            return "getObject".equals(methodName) || "getObjectVolatile".equals(methodName);
        }
        return "getReference".equals(methodName) || "getReferenceVolatile".equals(methodName);
    }
}
