package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Collections;

import static org.objectweb.asm.Opcodes.*;

public class OpcodesUtil {

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode stores a value into a field
     */
    public static boolean isFieldStoreInsn(int opcode) {
        return opcode == PUTFIELD || opcode == PUTSTATIC;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode loads a value from a field
     */
    public static boolean isFieldLoadInsn(int opcode) {
        return opcode == GETFIELD || opcode == GETSTATIC;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode stores a value into an array
     */
    public static boolean isArrayStore(int opcode) {
        return opcode >= IASTORE && opcode <= SASTORE;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode loads a value from an array
     */
    public static boolean isArrayLoad(int opcode) {
        return opcode >= IALOAD && opcode <= SALOAD;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode stores a value into a local variable
     */
    public static boolean isLocalVariableStoreInsn(int opcode) {
        return opcode >= ISTORE && opcode <= ASTORE;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode pushes a constant onto the runtime stack
     */
    public static boolean isPushConstantOpcode(int opcode) {
        return opcode >= ACONST_NULL && opcode <= LDC;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the operation associated with the specified opcode performs an arithmetic or logical computation
     * runtime stack and pushes the result onto the runtime stack
     */
    public static boolean isArithmeticOrLogicalInsn(int opcode) {
        return opcode >= IADD && opcode <= DCMPG;
    }

    /**
     * @param opcode the opcode to be checked
     * @return true if the specified opcode is associated with a return operation
     */
    public static boolean isReturnOpcode(int opcode) {
        switch(opcode) {
            case ARETURN:
            case IRETURN:
            case RETURN:
            case DRETURN:
            case FRETURN:
            case LRETURN:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param instruction the instruction to be checked
     * @return true if the specified instruction triggers a method exit
     */
    public static boolean isExitInstruction(AbstractInsnNode instruction) {
        return instruction.getOpcode() == ATHROW || isReturnOpcode(instruction.getOpcode());
    }

    /**
     * @param insn the instruction whose associated type's size is being calculated
     * @return the size of the type associated with the specified instruction
     */
    public static int getSize(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case LCONST_0:
            case LCONST_1:
            case DCONST_0:
            case DCONST_1:
            case LALOAD:
            case DALOAD:
            case LADD:
            case DADD:
            case LSUB:
            case DSUB:
            case LMUL:
            case DMUL:
            case LDIV:
            case DDIV:
            case LREM:
            case DREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
            case LNEG:
            case DNEG:
            case I2L:
            case I2D:
            case L2D:
            case F2L:
            case F2D:
            case D2L:
                return 2;
            case LDC:
                Object value = ((LdcInsnNode) insn).cst;
                return value instanceof Long || value instanceof Double ? 2 : 1;
            case GETSTATIC:
            case GETFIELD:
                return Type.getType(((FieldInsnNode) insn).desc).getSize();
            case INVOKEDYNAMIC:
                return Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
                return Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
            default:
                return 1;
        }
    }

    /**
     * Returns true if the execution of the specified instruction could directly result in an exception being throw due
     * to an abnormal execution condition that the specified handler can catch according to: <br>
     * The Java Virtual Machine Specification, Java SE 8 Edition<br>
     * Chapter 6: The Java Virtual Machine Instruction Set<br>
     * https://docs.oracle.com/javase/specs/jvms/se8/jvms8.pdf
     * <p>
     * Only considers exceptions resulting from abnormal execution conditions "synchronously detected by the
     * Java Virtual Machine" (e.g., NullPointerExceptions thrown due to an operation de-referencing a null array
     * reference or object reference) that did not occur due to
     * loading, linking, or exceeding resource limitations.
     * <p>
     * In this case, directly means by the specific instruction itself. So, exceptions thrown by the method called
     * by INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, and INVOKEDYNAMIC instructions are not
     * considered.
     *
     * @param insn    the instruction to be checked
     * @param handler the exception handler
     * @return true if the execution of the specified instruction could directly result in an exception being throw due
     * to an abnormal execution condition that the specified handler can catch
     * @throws NullPointerException if insn or handler are null
     */
    public static boolean couldImplicitlyThrowException(AbstractInsnNode insn, TryCatchBlockNode handler) {
        if(insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode) {
            return false;
        }
        String handledExceptionType = handler.type == null ? "java/lang/Throwable" : handler.type;
        int opcode = insn.getOpcode();
        switch(handledExceptionType) {
            case "java/lang/Throwable":
            case "java/lang/Error":
                return true;
            case "java/lang/Exception":
            case "java/lang/RuntimeException":
                switch(opcode) {
                    case ANEWARRAY:
                    case MULTIANEWARRAY:
                    case NEWARRAY:
                    case ARRAYLENGTH:
                    case ATHROW:
                    case CHECKCAST:
                    case GETFIELD:
                    case IDIV:
                    case IREM:
                    case LDIV:
                    case LREM:
                    case INVOKEINTERFACE:
                    case INVOKESPECIAL:
                    case INVOKEVIRTUAL:
                    case MONITORENTER:
                    case MONITOREXIT:
                    case PUTFIELD:
                        return true;
                    default:
                        return isReturnOpcode(opcode) || isArrayStore(opcode) || isArrayLoad(opcode);
                }
            case "java/lang/NullPointerException":
                switch(opcode) {
                    case ARRAYLENGTH:
                    case ATHROW:
                    case GETFIELD:
                    case INVOKEINTERFACE:
                    case INVOKESPECIAL:
                    case INVOKEVIRTUAL:
                    case MONITORENTER:
                    case MONITOREXIT:
                    case PUTFIELD:
                        return true;
                    default:
                        return isArrayStore(opcode) || isArrayLoad(opcode);
                }
            case "java/lang/IndexOutOfBoundsException":
            case "java/lang/ArrayIndexOutOfBoundsException":
                return isArrayStore(opcode) || isArrayLoad(opcode);
            case "java/lang/IllegalMonitorStateException":
                return isReturnOpcode(opcode) || opcode == ATHROW || opcode == MONITOREXIT;
            case "java/lang/invoke/WrongMethodTypeException":
                return opcode == INVOKEVIRTUAL;
            case "java/lang/ArrayStoreException":
                return opcode == AASTORE;
            case "java/lang/ArithmeticException":
                return opcode == IDIV || opcode == IREM || opcode == LDIV || opcode == LREM;
            case "java/lang/NegativeArraySizeException":
                return opcode == ANEWARRAY || opcode == MULTIANEWARRAY || opcode == NEWARRAY;
            case "java/lang/ClassCastException":
                return opcode == CHECKCAST;
            default:
                return false;
        }
    }

    /**
     * Returns true if the execution of the specified instruction could result in an exception being throw that the
     * specified handler can catch according to: <br>
     * The Java Virtual Machine Specification, Java SE 8 Edition<br>
     * Chapter 6: The Java Virtual Machine Instruction Set<br>
     * https://docs.oracle.com/javase/specs/jvms/se8/jvms8.pdf
     * <p>
     * Only considers exceptions that were explicitly thrown by an ATHROW instructions and exceptions resulting from
     * abnormal execution conditions "synchronously detected by the Java Virtual Machine" (e.g., NullPointerExceptions thrown due
     * to an operation de-referencing a null array reference or object reference) that did not occur due to
     * loading, linking, or exceeding resource limitations.
     *
     * @param insn                          the instruction to be checked
     * @param handler                       the exception handler
     * @param explicitlyThrownExceptionType the name of the class of the explicitly thrown if the specified instruction
     *                                      is an ATHROW exception (as returned by Class.getName(), but with the '.'s
     *                                      replaced with '/'s), otherwise null
     * @return true if the execution of the specified instruction could result in an exception being throw that the
     * specified handler can catch
     * @throws NullPointerException if insn or handler are null
     */
    public static boolean couldThrowHandledException(AbstractInsnNode insn, TryCatchBlockNode handler,
                                                     String explicitlyThrownExceptionType) {
        if(couldImplicitlyThrowException(insn, handler)) {
            return true;
        } else if(!(insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode
                || insn.getOpcode() == ATHROW)) {
            return false;
        }
        String handledExceptionType = handler.type == null ? "java/lang/Throwable" : handler.type;
        if((insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode) &&
                !disjointTypes("java/lang/RuntimeException", handledExceptionType)) {
            // Handled exception could be RuntimeException, a superclass of RuntimeException, or a subclass of
            // RuntimeException.
            // Since methods can throw RuntimeExceptions without declaring them, return true.
            return true;
        }
        Iterable<String> thrownExceptionTypes;
        if(insn instanceof MethodInsnNode || insn instanceof InvokeDynamicInsnNode) {
            MethodNode methodNode;
            if(insn instanceof MethodInsnNode) {
                methodNode = findMethod(((MethodInsnNode) insn).owner, ((MethodInsnNode) insn).name,
                        ((MethodInsnNode) insn).desc);
            } else {
                methodNode = findMethod(((InvokeDynamicInsnNode) insn).bsm.getOwner(), ((InvokeDynamicInsnNode) insn).name,
                        ((InvokeDynamicInsnNode) insn).desc);
            }
            if(methodNode == null) {
                // Method information is unavailable.
                // Conservatively say that the instruction could throw a handled exception.
                return true;
            }
            thrownExceptionTypes = methodNode.exceptions;
        } else {
            // ATHROW
            if(explicitlyThrownExceptionType == null) {
                // Thrown exception type is unavailable.
                // Conservatively say that the instruction could throw a handled exception.
                return true;
            } else {
                thrownExceptionTypes = Collections.singletonList(explicitlyThrownExceptionType);
            }
        }
        Set<String> handledExceptionSuperClasses = getSuperClasses(handledExceptionType);
        for(String thrownExceptionType : thrownExceptionTypes) {
            if(!disjointTypes(handledExceptionType, thrownExceptionType, handledExceptionSuperClasses)) {
                return true;
            }
        }
        // Thrown exceptions were all provably disjoint from handled exceptions
        return false;
    }

    /**
     * Returns true if an object cannot be an instance of both of the specified classes.
     * For example, {@code disjointTypes("java/lang/NullPointerException", "java/lang/RuntimeException")} returns false,
     * but {@code disjointTypes("java/lang/NullPointerException", "java/lang/IndexOutOfBoundsException")} returns true.
     * May return false because ClassNode information is not available for one of the classes.
     *
     * @param className1 the name of the first class
     * @param className2 the name of the second class
     * @return true if an object cannot be an instance of both of the specified classes or ClassNode information is
     * not available for one of the classes
     * @throws NullPointerException if className1 is null or className2 is null
     */
    private static boolean disjointTypes(String className1, String className2) {
        Set<String> superClasses1 = getSuperClasses(className1);
        return disjointTypes(className1, className2, superClasses1);
    }

    private static boolean disjointTypes(String className1, String className2, Set<String> superClasses1) {
        if(className1 == null || className2 == null) {
            throw new NullPointerException();
        } else if(className1.equals(className2)) {
            return false;
        }
        if(superClasses1 == null || superClasses1.contains(className2)) {
            // Either super class information for the first class was unavailable
            // or the second class is a superclass of the first class
            return false;
        }
        Set<String> superClasses2 = getSuperClasses(className2);
        // Return true if super class information for the second class was available
        // and the first class is not a superclass of the second class
        return superClasses2 != null && !superClasses2.contains(className1);
    }

    /**
     * If a MethodNode representing the method with the specified owner, name, descriptor can be found using ClassNodes
     * created by {@link Instrumenter}, returns it. Otherwise, returns null.
     *
     * @param owner      the declaring class of the method whose MethodNode should be searched for
     * @param name       the name of the method whose MethodNode should be searched for
     * @param descriptor the descriptor of the method whose MethodNode should be searched for
     * @return the MethodNode representing the method with the specified owner, name, descriptor if it can be found,
     * otherwise null
     * @throws NullPointerException if owner, name, or descriptor is null
     */
    private static MethodNode findMethod(String owner, String name, String descriptor) {
        if(owner == null || name == null || descriptor == null) {
            throw new NullPointerException();
        }
        ClassNode cn = Instrumenter.getClassNode(owner);
        if(cn != null) {
            for(MethodNode methodNode : cn.methods) {
                if(descriptor.equals(methodNode.desc) && name.equals(methodNode.name)) {
                    return methodNode;
                }
            }
        }
        return null;
    }

    /**
     * Creates a set that contains the specified class name and the names of classes that are super classes of the class
     * with the specified name using ClassNodes created by {@link Instrumenter}.
     * If a ClassNode is not available for the class with specified name or any of its super classes, returns null.
     *
     * @param className the name of the class whose super classes are to be gathered
     * @return a set that contains the specified class name and the names of classes that are super classes of the class
     * with the specified name if this information can be found for the class, otherwise null
     * @throws NullPointerException if className is null
     */
    private static Set<String> getSuperClasses(String className) {
        if(className == null) {
            throw new NullPointerException();
        }
        LinkedList<String> ancestors = new LinkedList<>();
        ancestors.add(className);
        while(true) {
            String currentClass = ancestors.getLast();
            ClassNode classNode = Instrumenter.getClassNode(currentClass);
            if(classNode == null) {
                return null;
            } else if(classNode.superName == null) {
                return new HashSet<>(ancestors);
            } else {
                ancestors.add(classNode.superName);
            }
        }
    }
}