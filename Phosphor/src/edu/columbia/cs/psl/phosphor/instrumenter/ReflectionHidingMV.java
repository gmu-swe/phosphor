package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeReflectionPropagator;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeUnsafePropagator;
import edu.columbia.cs.psl.phosphor.struct.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

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
        } else {
            return Configuration.TAINT_THROUGH_SERIALIZATION &&
                    (className.startsWith("java/io/ObjectStreamClass") || className.equals("java/io/ObjectStreamField"));
        }
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
        //B M
        super.visitInsn(SWAP);
        //M B
        super.visitInsn(DUP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "o", "Ljava/lang/Object;");
        super.visitInsn(SWAP);
        super.visitFieldInsn(GETFIELD, Type.getInternalName(MethodInvoke.class), "a", "[Ljava/lang/Object;");
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
        }
    }

    private void maskConstructorNewInstance() {
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            super.visitInsn(POP);
            super.visitInsn(SWAP);
            //[A C
            super.visitInsn(DUP_X1);
            //C [A C
            super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
            visit(FIX_ALL_ARGS_CONSTRUCTOR_CONTROL);
            super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
        } else {
            super.visitInsn(SWAP);
            //[A C
            super.visitInsn(DUP_X1);
            //C [A C
            visit(FIX_ALL_ARGS_CONSTRUCTOR);
        }
    }

    private void maskGetter(TaintMethodRecord mask, Type[] args) {
        if(args.length == 0) {
            visit(mask);
        } else if(args.length == 1) {
            super.visitInsn(SWAP);
            visit(mask);
            super.visitInsn(SWAP);
        } else if(args.length == 2) {
            int lv1 = lvs.getTmpLV();
            super.visitVarInsn(ASTORE, lv1);
            int lv2 = lvs.getTmpLV();
            super.visitVarInsn(ASTORE, lv2);
            visit(mask);
            super.visitVarInsn(ALOAD, lv2);
            super.visitVarInsn(ALOAD, lv1);
            lvs.freeTmpLV(lv1);
            lvs.freeTmpLV(lv2);
        }
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that retrieves the value of a field of a Java heap object. */
    private boolean isUnsafeFieldGetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if(className.equals("sun/misc/Unsafe") || opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            if(args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
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

    /* Changes calls to methods added to Unsafe by Phosphor which retrieve the value of a field of a Java heap object to
     * instead call a method in RuntimeUnsafePropagator. */
    private void maskUnsafeFieldGetter(Type retType, String nameWithoutSuffix, Type[] args) {
        SinglyLinkedList<Type> argStack = new SinglyLinkedList<>();
        for(Type arg : args) {
            argStack.push(arg);
        }
        popControlTaintTagStack(argStack);
        if(!TaintUtils.isTaintedPrimitiveType(argStack.peek())) {
            // Put a null value onto the stack in place of the prealloc
            super.visitInsn(ACONST_NULL);
            argStack.push(Type.getType(Object.class));
        }
        removeOffsetTagAndCastOffset(argStack);
        String name = nameWithoutSuffix.contains("Volatile") ? "getVolatile" : "get";
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), name,
                "(Lsun/misc/Unsafe;Ljava/lang/Object;JLjava/lang/Object;)Ljava/lang/Object;", false);
        super.visitTypeInsn(CHECKCAST, retType.getInternalName());
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * that sets the value of a field of a Java heap object. */
    private boolean isUnsafeFieldSetter(int opcode, String owner, String name, Type[] args, String nameWithoutSuffix) {
        if(className.equals("sun/misc/Unsafe") || opcode != INVOKEVIRTUAL || !"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX)) {
            return false;
        } else {
            if(args.length < 1 || !args[0].equals(Type.getType(Object.class))) {
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

    /* Changes calls to methods added to Unsafe by Phosphor which set the value of a field of a Java heap object to instead
     * call a method in RuntimeUnsafePropagator. */
    private void maskUnsafeFieldSetter(String nameWithoutSuffix, Type[] args) {
        SinglyLinkedList<Type> argStack = new SinglyLinkedList<>();
        for(Type arg : args) {
            argStack.push(arg);
        }
        popControlTaintTagStack(argStack);
        wrapPrimitive(argStack);
        removeOffsetTagAndCastOffset(argStack);
        String name = nameWithoutSuffix.contains("Volatile") ? "putVolatile" : (nameWithoutSuffix.contains("Ordered") ? "putOrdered" : "put");
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), name,
                "(Lsun/misc/Unsafe;Ljava/lang/Object;JLjava/lang/Object;)V", false);
    }

    /* Returns whether a method instruction with the specified information is for a method added to Unsafe by Phosphor
     * for a compareAndSwap method. */
    private boolean isUnsafeCAS(String owner, String name, String nameWithoutSuffix) {
        if(!"sun/misc/Unsafe".equals(owner) || !name.endsWith(TaintUtils.METHOD_SUFFIX) || className.equals("sun/misc/Unsafe")) {
            return false;
        } else {
            return "compareAndSwapInt".equals(nameWithoutSuffix) || "compareAndSwapLong".equals(nameWithoutSuffix)
                    || "compareAndSwapObject".equals(nameWithoutSuffix);
        }
    }

    /* Changes calls to methods added to Unsafe by Phosphor for compareAndSwap to instead call a method in
     * RuntimeUnsafePropagator. */
    private void maskUnsafeCAS(Type[] args) {
        SinglyLinkedList<Type> argStack = new SinglyLinkedList<>();
        for(Type arg : args) {
            argStack.push(arg);
        }
        popControlTaintTagStack(argStack);
        // Store the prealloc into a local variable
        int preallocLV = lvs.getTmpLV();
        super.visitVarInsn(ASTORE, preallocLV);
        argStack.pop();
        // If a primitive and its tag are at the top of the stack, wrap them together
        wrapPrimitive(argStack);
        int valLV = lvs.getTmpLV();
        super.visitVarInsn(ASTORE, valLV);
        argStack.pop();
        // If a primitive and its tag are at the top of the stack, wrap them together
        wrapPrimitive(argStack);
        removeOffsetTagAndCastOffset(argStack);
        super.visitVarInsn(ALOAD, valLV);
        lvs.freeTmpLV(valLV);
        super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(RuntimeUnsafePropagator.class), "compareAndSwap",
                "(Lsun/misc/Unsafe;Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z", false);
        // Set the prealloc's val field to be the returned boolean, and taint field be null/0
        super.visitVarInsn(ALOAD, preallocLV);
        super.visitInsn(SWAP);
        String fieldOwner = Type.getInternalName(TaintedBooleanWithObjTag.class);
        super.visitFieldInsn(PUTFIELD, fieldOwner, "val", "Z");
        super.visitVarInsn(ALOAD, preallocLV);
        lvs.freeTmpLV(preallocLV);
        super.visitInsn(DUP);
        super.visitInsn(ACONST_NULL);
        super.visitFieldInsn(PUTFIELD, fieldOwner, "taint", Configuration.TAINT_TAG_DESC);
    }

    /* Swaps the top stack value of the specified type with the value below it of the specified other type. */
    private void swap(Type top, Type below) {
        if(top.getSize() == 1) {
            if(below.getSize() == 1) {
                super.visitInsn(SWAP);
            } else {
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
            }
        } else {
            super.visitInsn((below.getSize() == 1) ? DUP2_X1 : DUP2_X2);
            super.visitInsn(POP2);
        }
    }

    /* If the the type at the top of the specified stack is a ControlTaintTagStack type or the top type is a tainted
     * primitive container type and the type under it is a ControlTaintTagStack type pops the ControlTaintTagStack off of
     * the stack and updates the stack to reflect the changes made. */
    private void popControlTaintTagStack(SinglyLinkedList<Type> argStack) {
        if(!argStack.isEmpty() && argStack.peek().equals(Type.getType(ControlTaintTagStack.class))) {
            super.visitInsn(POP);
            argStack.pop();
        } else if(argStack.size() >= 2 && TaintUtils.isTaintedPrimitiveType(argStack.peek())) {
            Type top = argStack.pop();
            if(argStack.peek().equals(Type.getType(ControlTaintTagStack.class))) {
                swap(top, argStack.peek());
                super.visitInsn(POP);
                argStack.pop();
            }
            argStack.push(top);
        }
    }

    /* If the type at the top of the specified stack is a primitive type wraps that primitive into a
     * TaintedPrimitiveWithObjTag instance. */
    private void wrapPrimitive(SinglyLinkedList<Type> argStack) {
        int sort = argStack.peek().getSort();
        if(sort != Type.ARRAY && sort != Type.OBJECT) {
            // Store the primitive into a local variable
            int lv = lvs.getTmpLV();
            super.visitVarInsn(argStack.peek().getOpcode(ISTORE), lv);
            Type containerType = TaintUtils.getContainerReturnType(argStack.peek());
            super.visitTypeInsn(NEW, containerType.getInternalName());
            super.visitInsn(DUP_X1);
            super.visitInsn(DUP_X1);
            super.visitInsn(POP);
            // Load the primitive from the local variable
            super.visitVarInsn(argStack.peek().getOpcode(ILOAD), lv);
            lvs.freeTmpLV(lv);
            super.visitMethodInsn(INVOKESPECIAL, containerType.getInternalName(), "<init>", "(" + Configuration.TAINT_TAG_DESC +
                    argStack.peek().getDescriptor() + ")V", false);
            argStack.pop();
            argStack.pop();
            argStack.push(Type.getType(Object.class));
        }
    }

    /* The stack when entering this method should have a Object followed by either an int or a long and then the taint tag
     * for that int or long. Removes the taint tag from the stack and ensures that the second value is always a long. */
    private void removeOffsetTagAndCastOffset(SinglyLinkedList<Type> argStack) {
        Type top = argStack.pop();
        Type second = argStack.pop();
        swap(top, second);
        // Cast the second value if necessary
        if(second.getSort() == Type.INT) {
            super.visitInsn(I2L);
        }
        // Store the long into a local variable
        int lv = lvs.getTmpLV();
        super.visitVarInsn(LSTORE, lv);
        // Pop the taint tag off of the stack
        super.visitInsn(SWAP);
        super.visitInsn(POP);
        // Load the long from the local variable
        super.visitVarInsn(LLOAD, lv);
        lvs.freeTmpLV(lv);
        // Swap the long and the object so that the object is back on the top of the stack
        swap(Type.LONG_TYPE, top);
        // Update argStack
        argStack.pop();
        argStack.push(Type.LONG_TYPE);
        argStack.push(top);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        String nameWithoutSuffix = name.replace(TaintUtils.METHOD_SUFFIX, "");
        Type returnType = Type.getReturnType(desc);
        if(owner.equals("java/lang/Object") && nameWithoutSuffix.equals("getClass") && isObjOutputStream) {
            visit(GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM);
        } else if((disable || className.equals("java/io/ObjectOutputStream") || className.equals("java/io/ObjectInputStream")) && owner.equals("java/lang/Class") && !owner.equals(className) && name.startsWith("isInstance$$PHOSPHORTAGGED")) {
            // Even if we are ignoring other hiding here, we definitely need to do this.
            if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
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
                        if(Configuration.IMPLICIT_TRACKING && !Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitInsn(POP);
                        }
                        visit(GET_METHOD);
                        return;
                    } else if(nameWithoutSuffix.equals("getDeclaredMethod")) {
                        if(Configuration.IMPLICIT_TRACKING && !Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitInsn(POP);
                        }
                        visit(GET_DECLARED_METHOD);
                        return;
                    } else if(nameWithoutSuffix.equals("getConstructor") || nameWithoutSuffix.equals("getDeclaredConstructor")) {
                        // Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
                        if(Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitInsn(POP);
                        }
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
                        super.visitInsn((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING ? ICONST_1 : ICONST_0));
                        visit(ADD_TYPE_PARAMS);
                        if(Configuration.IMPLICIT_TRACKING) {
                            super.visitInsn(SWAP);
                        } else if(Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                        }
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
                maskUnsafeFieldGetter(returnType, nameWithoutSuffix, args);
                return;
            } else if(isUnsafeFieldSetter(opcode, owner, name, args, nameWithoutSuffix)) {
                maskUnsafeFieldSetter(nameWithoutSuffix, args);
                return;
            } else if(isUnsafeCAS(owner, name, nameWithoutSuffix)) {
                maskUnsafeCAS(args);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
            if(owner.equals("java/lang/Class") && desc.endsWith("[Ljava/lang/reflect/Field;") && !className.equals("java/lang/Class")) {
                if(!Configuration.WITHOUT_FIELD_HIDING) {
                    visit(REMOVE_TAINTED_FIELDS);
                }
            } else if(owner.equals("java/lang/Class") && !className.equals(owner) && (desc.equals("()[Ljava/lang/reflect/Method;") || desc.equals("(" + Type.getDescriptor(ControlTaintTagStack.class) + ")[Ljava/lang/reflect/Method;"))) {
                super.visitInsn("getMethods".equals(nameWithoutSuffix) ? ICONST_0 : ICONST_1);
                visit(REMOVE_TAINTED_METHODS);
            } else if(owner.equals("java/lang/Class") && !className.equals(owner) && (desc.equals("()[Ljava/lang/reflect/Constructor;") || desc.equals("(" + Type.getDescriptor(ControlTaintTagStack.class) + ")[Ljava/lang/reflect/Constructor;"))) {
                visit(REMOVE_TAINTED_CONSTRUCTORS);
            } else if(owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
                visit(REMOVE_TAINTED_INTERFACES);
            } else if(owner.equals("java/lang/Throwable") && (name.equals("getOurStackTrace") || name.equals("getStackTrace")) && desc.equals("()" + "[" + Type.getDescriptor(StackTraceElement.class))) {
                if(className.equals("java/lang/Throwable")) {
                    super.visitVarInsn(ALOAD, 0);
                    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                } else {
                    super.visitLdcInsn(Type.getObjectType(className));
                }
                visit(REMOVE_EXTRA_STACK_TRACE_ELEMENTS);
            } else if(owner.equals("java/lang/Object") && name.equals("getClass") && !isObjOutputStream) {
                visit(GET_ORIGINAL_CLASS);
            }
            if((owner.equals("java/lang/reflect/Method") || owner.equals("java/lang/reflect/Constructor")) && !(className.equals("java/lang/Class")) &&
                    (name.equals("invoke") || name.equals("newInstance") || name.equals("invoke$$PHOSPHORTAGGED")
                            || name.equals("newInstance$$PHOSPHORTAGGED"))) {
                // Unbox if necessary
                FrameNode fn = TaintAdapter.getCurrentFrameNode(analyzer);
                fn.type = F_NEW;
                super.visitInsn(DUP);
                super.visitTypeInsn(INSTANCEOF, Type.getInternalName(TaintedPrimitiveWithObjTag.class));
                Label notPrimitive = new Label();
                super.visitJumpInsn(IFEQ, notPrimitive);
                FrameNode fn2 = TaintAdapter.getCurrentFrameNode(analyzer);
                fn2.type = F_NEW;
                super.visitTypeInsn(CHECKCAST, Type.getInternalName(TaintedPrimitiveWithObjTag.class));
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintedPrimitiveWithObjTag.class), "toPrimitiveType", "()Ljava/lang/Object;", false);
                super.visitLabel(notPrimitive);
                fn2.accept(this);
            }
        }
    }

    /**
     * Visits a method instruction for the specified method.
     *
     * @param method the method to be visited
     */
    private void visit(TaintMethodRecord method) {
        super.visitMethodInsn(method.getOpcode(), method.getOwner(), method.getName(), method.getDescriptor(), method.isInterface());
    }
}
