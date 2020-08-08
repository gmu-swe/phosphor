package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropagator;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeUnsafePropagator;
import edu.columbia.cs.psl.phosphor.struct.LazyReferenceArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.MethodInvoke;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class ReflectionHidingMV extends MethodVisitor implements Opcodes {

    private final String className;
    private final String methodName;
    private final boolean disable;
    private final boolean isEnumValueOf;
    private LocalVariableManager lvs;

    public ReflectionHidingMV(MethodVisitor mv, String className, String name, boolean isEnum) {
        super(Configuration.ASM_VERSION, mv);
        this.className = className;
        this.methodName = name;
        this.disable = shouldDisable(className, name);
        this.isEnumValueOf = isEnum && name.equals("valueOf$$PHOSPHORTAGGED");
    }

    public void setLvs(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    private void maskMethodInvoke() {
        // method owner [Args
        // Try the fastpath where we know we don't change the method orig version
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            visit(FIX_ALL_ARGS_METHOD_CONTROL);
        } else {
            visit(FIX_ALL_ARGS_METHOD);
        }
        //B
        super.visitInsn(DUP);
        //B B
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "m", "Ljava/lang/reflect/Method;");
        super.visitInsn(SWAP);
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "m_taint", Configuration.TAINT_TAG_DESC);
        super.visitInsn(SWAP);
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "o", "Ljava/lang/Object;");
        super.visitInsn(SWAP);
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "o_taint", Configuration.TAINT_TAG_DESC);
        super.visitInsn(SWAP);
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a", Type.getDescriptor(LazyReferenceArrayObjTags.class));
        super.visitInsn(SWAP);
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a_taint", Configuration.TAINT_TAG_DESC);
        super.visitInsn(SWAP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "prealloc", Type.getDescriptor(TaintedReferenceWithObjTag.class));
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            super.visitVarInsn(ALOAD, lvs.getIndexOfMasterControlLV());
            super.visitInsn(SWAP);
        }
        super.visitInsn(ACONST_NULL); // for the array descriptor arg
        super.visitInsn(ACONST_NULL); // for the return arg
    }

    private void maskConstructorNewInstance() {
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            visit(FIX_ALL_ARGS_CONSTRUCTOR_CONTROL);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "c", "Ljava/lang/reflect/Constructor;");
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "c_taint", Configuration.TAINT_TAG_DESC);
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a", Type.getDescriptor(LazyReferenceArrayObjTags.class));
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a_taint", Configuration.TAINT_TAG_DESC);
            super.visitInsn(SWAP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "prealloc", Type.getDescriptor(TaintedReferenceWithObjTag.class));
            super.visitVarInsn(ALOAD, lvs.getIndexOfMasterControlLV());
            super.visitInsn(SWAP);
            super.visitInsn(ACONST_NULL); // for the array descriptor arg
            super.visitInsn(ACONST_NULL); // for the return arg
        } else {
            //Method taint array taint prealloc
            visit(FIX_ALL_ARGS_CONSTRUCTOR);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "c", "Ljava/lang/reflect/Constructor;");
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "c_taint", Configuration.TAINT_TAG_DESC);
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a", Type.getDescriptor(LazyReferenceArrayObjTags.class));
            super.visitInsn(SWAP);
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a_taint", Configuration.TAINT_TAG_DESC);
            super.visitInsn(SWAP);
            super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "prealloc", Type.getDescriptor(TaintedReferenceWithObjTag.class));
            super.visitInsn(ACONST_NULL); // for the array descriptor arg
            super.visitInsn(ACONST_NULL); // for the return arg
        }
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
        if(opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            if(args.length < 2 || !args[1].equals(Type.getType(Object.class))) {
                return false;
            }
            switch(nameWithoutSuffix) {
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
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that sets the value of a field of a Java heap object. */
    private boolean isUnsafeFieldSetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if(opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            if(args.length < 2 || !args[1].equals(Type.getType(Object.class))) {
                return false;
            }
            switch(nameWithoutSuffix) {
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
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * for a compareAndSwap method. */
    private boolean isUnsafeCAS(String owner, String name, String nameWithoutSuffix) {
        if(!"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            return "compareAndSwapInt".equals(nameWithoutSuffix)
                    || "compareAndSwapLong".equals(nameWithoutSuffix)
                    || "compareAndSwapObject".equals(nameWithoutSuffix);
        }
    }

    private boolean isUnsafeCopyMemory(String owner, String name, String nameWithoutSuffix) {
        if(!"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            return "copyMemory".equals(nameWithoutSuffix);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(opcode == Opcodes.ARETURN && isEnumValueOf) {
            super.visitVarInsn(ALOAD, 1);
            ENUM_VALUE_OF.delegateVisit(mv);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        String nameWithoutSuffix = name.replace(TaintUtils.METHOD_SUFFIX, "");
        Type returnType = Type.getReturnType(desc);
        if((disable || className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream")) && owner.equals("java/lang/Class") && !owner.equals(className) && name.startsWith("isInstance$$PHOSPHORTAGGED")) {
            // Even if we are ignoring other hiding here, we definitely need to do this.
            if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }
            visit(IS_INSTANCE);
        } else if(disable) {
            if(this.methodName.startsWith("setObjFieldValues") && owner.equals("sun/misc/Unsafe") && (name.startsWith("putObject") || name.startsWith("compareAndSwapObject"))) {
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
            switch(owner) {
                case "java/lang/reflect/Method":
                    if(name.startsWith("invoke")) {
                        maskMethodInvoke();
                    } else if(name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.startsWith("java/lang/Class")) {
                        maskGetter(GET_ORIGINAL_METHOD, args);
                    }
                    break;
                case "java/lang/reflect/Constructor":
                    if(name.startsWith("newInstance")) {
                        maskConstructorNewInstance();
                    } else if(name.startsWith("get") && !className.equals(owner) && !className.startsWith("sun/reflect") && !className.equals("java/lang/Class")) {
                        maskGetter(GET_ORIGINAL_CONSTRUCTOR, args);
                    }
                    break;
                case "java/lang/Class":
                    if(nameWithoutSuffix.equals("getMethod")) {
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitInsn(POP);
                            super.visitInsn(POP);
                            super.visitInsn(SWAP);
                            super.visitInsn(POP);
                            super.visitInsn(ACONST_NULL);
                            super.visitInsn(ACONST_NULL);
                        }
                        visit(GET_METHOD);
                        return;
                    } else if(nameWithoutSuffix.equals("getDeclaredMethod")) {
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitInsn(POP);
                            super.visitInsn(POP);
                            super.visitInsn(SWAP);
                            super.visitInsn(POP);
                            super.visitInsn(ACONST_NULL);
                            super.visitInsn(ACONST_NULL);
                        }
                        visit(GET_DECLARED_METHOD);
                        return;
                    } else if(nameWithoutSuffix.equals("getConstructor") || nameWithoutSuffix.equals("getDeclaredConstructor")) {
                        // Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)

                        super.visitInsn(POP); //for the fake array
                        super.visitInsn(POP); //Null from the extra arg for erased return type
                        int prealloc = lvs.getTmpLV(Type.getType(TaintedReferenceWithObjTag.class));
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            //Class taint array taint ctrl prealloc
                            super.visitInsn(SWAP);
                            super.visitInsn(POP);
                        }
                        super.visitVarInsn(ASTORE, prealloc);
                        //Class taint Array Taint Prealloc
                        //need to save all of these things for later
                        //Class taint array taint
                        super.visitInsn(POP);
                        //Class taint array
                        super.visitInsn(SWAP);
                        super.visitInsn(POP);
                        //Class array
                        super.visitInsn(DUP2);
                        // Class array class array
                        super.visitInsn((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING ? ICONST_1 : ICONST_0));

                        visit(ADD_TYPE_PARAMS); //class taint array taint boolean prealloc
                        //Class badArray array
                        super.visitInsn(SWAP);
                        super.visitInsn(POP);
                        //Class array
                        NEW_EMPTY_TAINT.delegateVisit(mv);
                        super.visitInsn(DUP_X1);
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitVarInsn(ALOAD, lvs.getIndexOfMasterControlLV());
                        }
                        //Class taint array taint
                        super.visitVarInsn(ALOAD, prealloc);
                        lvs.freeTmpLV(prealloc);
                        super.visitInsn(ACONST_NULL); //for the fake array
                        super.visitInsn(ACONST_NULL); //for the erased return
                    }
                    break;
            }
            if(owner.equals("java/lang/reflect/Array") && !owner.equals(className)) {
                owner = Type.getInternalName(ArrayReflectionMasker.class);
            }
            if(owner.equals("java/lang/reflect/Field")
                    && opcode == INVOKEVIRTUAL
                    && (name.equals("get") || name.equals("get$$PHOSPHORTAGGED") || name.equals("set$$PHOSPHORTAGGED") || name.equals("getInt$$PHOSPHORTAGGED") || name.equals("getBoolean$$PHOSPHORTAGGED") || name.equals("getChar$$PHOSPHORTAGGED")
                    || name.equals("getDouble$$PHOSPHORTAGGED") || name.equals("getByte$$PHOSPHORTAGGED") || name.equals("getFloat$$PHOSPHORTAGGED") || name.equals("getLong$$PHOSPHORTAGGED")
                    || name.equals("getShort$$PHOSPHORTAGGED") || name.equals("setAccessible$$PHOSPHORTAGGED") || name.equals("set") || name.equals("setInt$$PHOSPHORTAGGED")
                    || name.equals("setBoolean$$PHOSPHORTAGGED") || name.equals("setChar$$PHOSPHORTAGGED") || name.equals("setDouble$$PHOSPHORTAGGED") || name.equals("setByte$$PHOSPHORTAGGED")
                    || name.equals("setFloat$$PHOSPHORTAGGED") || name.equals("setLong$$PHOSPHORTAGGED") || name.equals("setShort$$PHOSPHORTAGGED") || name.equals("getType") || name.equals("getType$$PHOSPHORTAGGED"))) {
                owner = Type.getInternalName(RuntimeReflectionPropagator.class);
                opcode = INVOKESTATIC;
                desc = "(Ljava/lang/reflect/Field;" + desc.substring(1);
                if(name.equals("get")) {
                    desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;)Ljava/lang/Object;";
                } else if(name.equals("set")) {
                    desc = "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;)V";
                }
            }
            if(isUnsafeFieldGetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    //in control tracking mode, pop the control stack off of the stack to reuse the existing method
                    //but first, pop the null that's there for the erased return type.
                    if(nameWithoutSuffix.equals("getObject") || nameWithoutSuffix.equals("getObjectVolatile")) {
                        super.visitInsn(POP);
                    }
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                    if(nameWithoutSuffix.equals("getObject") || nameWithoutSuffix.equals("getObjectVolatile")) {
                        super.visitInsn(ACONST_NULL);
                    }
                }
                desc = "(Lsun/misc/Unsafe;" + desc.substring(1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), name, desc, false);
                return;
            } else if(isUnsafeFieldSetter(opcode, owner, name, args, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(POP);
                }
                desc = "(Lsun/misc/Unsafe;" + desc.substring(1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), name, desc, false);
                return;
            } else if(isUnsafeCAS(owner, name, nameWithoutSuffix) || isUnsafeCopyMemory(owner, name, nameWithoutSuffix)) {
                if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                    desc = desc.replace(CONTROL_STACK_DESC, "");
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                }
                owner = Type.getInternalName(RuntimeUnsafePropagator.class);
                opcode = INVOKESTATIC;
                desc = "(Lsun/misc/Unsafe;" + desc.substring(1);
                super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if(owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Fields") && !className.equals("java/lang/Class")) {
                if(!Configuration.WITHOUT_FIELD_HIDING) {
                    visit(REMOVE_TAINTED_FIELDS);
                }
            } else if(owner.equals("java/lang/Class") && nameWithoutSuffix.endsWith("Methods") && !className.equals(owner) && desc.equals("(" + Configuration.TAINT_TAG_DESC + controlTrackDescOrNone() + Type.getDescriptor(TaintedReferenceWithObjTag.class) + Type.getDescriptor(Method[].class) + ")" + Type.getDescriptor(TaintedReferenceWithObjTag.class))) {
                super.visitInsn("getMethods".equals(nameWithoutSuffix) ? ICONST_0 : ICONST_1);
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
            } else if(owner.equals("java/lang/reflect/Method") && nameWithoutSuffix.equals("invoke")) {
                visitInsn(DUP);
                visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintedReferenceWithObjTag.class), "unwrapPrimitives", "()V", false);
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
            return Configuration.TAINT_THROUGH_SERIALIZATION && !methodName.equals("getDeclaredSerialFields$$PHOSPHORTAGGED") &&
                    (className.startsWith("java/io/ObjectStreamClass") || className.equals("java/io/ObjectStreamField"));
        }
    }
}
