package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.LocalVariablePhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME;

public class MethodArgReindexer extends MethodVisitor {
    /**
     * Set of labels that mark the start of exception handlers.
     */
    private final Set<Label> handlers = new HashSet<>();
    /**
     * Maps an argument's original local variable index to its new index.
     */
    private final int[] localMap;
    /**
     * List containing the types of the arguments in the original uninstrumented method. This includes the argument for
     * the receiver (i.e., {@code this}) if the method is an instance (i.e., non-static) method.
     */
    private final List<Type> originalArgTypes;
    private final MethodNode lvStore;
    /**
     * If the method being visited has a pre-allocated return arguments, the type of that argument. Otherwise,
     * {@code null}.
     */
    private final Type newReturnType;
    /**
     * Number of arguments added to the method being visited to compensate for erased parameters and possibly an erased
     * return type.
     */
    private final int numberOfErasedSentinels;
    /**
     * Index of the local variable for the {@link ControlFlowStack} or {@code -1} if the method being visited
     * does not have a {@link ControlFlowStack} argument.
     */
    private final int stackIndex;
    /**
     * Number of arguments added to the method being visited.
     */
    private final int numberOfAddedParameters;
    /**
     * Maps a parameter's original index to its new index.
     */
    private final int[] parameterMap;
    /**
     * Local variable index for the first added parameter after the remapped parameters.
     */
    private final int nextIndex;
    /**
     * Maps original parameter counts to new parameter counts.
     */
    private final int[] parameterCountMap;
    /**
     * {@code true} if the next frame visited is the frame at the start of an exception handler.
     */
    private boolean isHandler = false;
    /**
     * {@code true} if {@link MethodVisitor#visitParameter(String, int)} was called on this instance.
     */
    private boolean visitedParameter = false;

    MethodArgReindexer(MethodVisitor mv, int access, String name, String desc, String originalDesc, MethodNode lvStore,
                       boolean isLambda) {
        super(Configuration.ASM_VERSION, mv);
        boolean isStatic = (Opcodes.ACC_STATIC & access) != 0;
        this.lvStore = lvStore;
        lvStore.localVariables = new java.util.ArrayList<>();
        Type[] originalParameters = Type.getArgumentTypes(originalDesc);
        this.parameterMap = computeParameterMap(originalParameters, isStatic, isLambda);
        this.parameterCountMap = computeParameterCountMap(originalParameters, isStatic, isLambda);
        this.localMap = new int[sumSizes(originalParameters) + (isStatic ? 0 : 1)];
        this.nextIndex = computeLocalMap(localMap, originalParameters, isStatic, isLambda);
        this.originalArgTypes = getFullArguments(isStatic, originalParameters);
        if (addControlStack(name)) {
            this.stackIndex = nextIndex;
        } else {
            this.stackIndex = -1;
        }
        this.newReturnType = TaintUtils.isPreAllocReturnType(originalDesc) ? Type.getReturnType(desc) : null;
        this.numberOfErasedSentinels = countErasedSentinels(originalParameters, Type.getReturnType(originalDesc));
        this.numberOfAddedParameters = Type.getArgumentTypes(desc).length - originalParameters.length;
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        visitedParameter = true;
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        super.visitAnnotableParameterCount(parameterCountMap[parameterCount], visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        return super.visitParameterAnnotation(parameterMap[parameter], descriptor, visible);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        if (type != Opcodes.F_NEW) { // Uncompressed frame.
            throw new IllegalArgumentException(MethodArgReindexer.class.getName() + " only accepts expanded frames");
        }
        Object[] newLocal = remapLocals(nLocal, local).toArray();
        if (isHandler) {
            isHandler = false;
            super.visitFrame(type, newLocal.length, newLocal, nStack, stack);
        } else {
            List<Object> newStack = new ArrayList<>();
            for (int i = 0; i < nStack; i++) {
                newStack.add(mapStackType(stack[i]));
                newStack.add(Configuration.TAINT_TAG_INTERNAL_NAME);
            }
            super.visitFrame(type, newLocal.length, newLocal, newStack.size(), newStack.toArray());
        }
    }

    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, remap(var));
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        isHandler |= handlers.contains(label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof LocalVariablePhosphorInstructionInfo) {
            LocalVariablePhosphorInstructionInfo info = (LocalVariablePhosphorInstructionInfo) cst;
            super.visitLdcInsn(info.setLocalVariableIndex(remap(info.getLocalVariableIndex())));
        } else {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(remap(var), increment);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        handlers.add(handler);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        Type t = Type.getType(desc);
        if (TaintUtils.isWrappedType(t)) {
            desc = TaintUtils.getWrapperType(t).getDescriptor();
        }
        if (index < localMap.length) {
            updateLocalVariableStore(name, desc, signature, index);
            String shadow = TaintUtils.getShadowTaintType(desc);
            if (shadow != null) {
                super.visitLocalVariable(name + TaintUtils.METHOD_SUFFIX, shadow, null, start, end,
                                         localMap[index] + 1);
            }
            super.visitLocalVariable(name, desc, signature, start, end, localMap[index]);
            if (index == 0) {
                int x = nextIndex;
                if (stackIndex != -1) {
                    super.visitLocalVariable("Phopshor$$ImplicitTaintTrackingFromParent", CONTROL_STACK_DESC, null,
                                             start, end, x++);
                }
                if (newReturnType != null) {
                    super.visitLocalVariable("Phosphor$$ReturnPreAllocated", newReturnType.getDescriptor(), null, start,
                                             end, x++);
                }
                for (int i = 0; i < numberOfErasedSentinels; i++) {
                    super.visitLocalVariable("PhosphorMethodDescriptorHelper" + i, "Ljava/lang/Object;", null, start,
                                             end, x++);
                }
            }
        } else {
            super.visitLocalVariable(name, desc, signature, start, end, index + numberOfAddedParameters);
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (visitedParameter) {
            // Add fake params
            for (int i = 0; i < numberOfAddedParameters; i++) {
                super.visitParameter("Phosphor$$Param$$" + i, 0);
            }
        }
    }

    private void updateLocalVariableStore(String name, String desc, String signature, int index) {
        for (LocalVariableNode lv : lvStore.localVariables) {
            if (lv != null && name.equals(lv.name) && lv.index == index) {
                return;
            }
        }
        lvStore.localVariables.add(new LocalVariableNode(name, desc, signature, null, null, index));
    }

    private List<Object> remapLocals(int nLocal, Object[] local) {
        List<Object> result = new ArrayList<>();
        int argIndex = 0;
        int i;
        for (i = 0; i < nLocal && argIndex < originalArgTypes.size(); i++) {
            Object frameType = local[i];
            Type originalType = originalArgTypes.get(argIndex++);
            if (frameType == Opcodes.TOP) {
                result.add(Opcodes.TOP);
                if (originalType.getSize() == 2 && i + 1 < local.length) {
                    // Wide argument was set to two TOPs
                    if (local[++i] != Opcodes.TOP) {
                        throw new IllegalArgumentException();
                    }
                    result.add(Opcodes.TOP);
                }
                // Add the tag
                result.add(Opcodes.TOP);
            } else {
                if (TaintUtils.isWrappedType(originalType)) {
                    result.add(TaintUtils.getWrapperType(originalType).getInternalName());
                } else {
                    result.add(frameType);
                }
                // Add the tag
                result.add(Configuration.TAINT_TAG_INTERNAL_NAME);
            }
        }
        while (argIndex < originalArgTypes.size()) {
            Type originalType = originalArgTypes.get(argIndex++);
            result.add(Opcodes.TOP);
            if (originalType.getSize() == 2) {
                result.add(Opcodes.TOP);
            }
            // Add the tag
            result.add(Opcodes.TOP);
        }
        if (stackIndex != -1) {
            result.add(CONTROL_STACK_INTERNAL_NAME);
        }
        if (newReturnType != null) {
            result.add(newReturnType.getInternalName());
        }
        for (int j = 0; j < numberOfErasedSentinels; j++) {
            result.add(Opcodes.TOP);
        }
        for (; i < nLocal; i++) {
            Object frameType = local[i];
            Type t = getTypeForStackTypeTOPAsNull(frameType);
            if (TaintUtils.isWrappedType(t)) {
                result.add(TaintUtils.getWrapperType(Type.getObjectType((String) frameType)).getInternalName());
            } else {
                result.add(frameType);
            }
        }
        return result;
    }

    private int remap(int var) {
        return var < localMap.length ? localMap[var] : var + numberOfAddedParameters;
    }

    private static Object mapStackType(Object stackType) {
        Type type = TaintAdapter.getTypeForStackType(stackType);
        if (TaintUtils.isWrappedType(type)) {
            return TaintUtils.getWrapperType(type).getInternalName();
        } else {
            return stackType;
        }
    }

    private static boolean addControlStack(String name) {
        return (Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) &&
                !"<clinit>".equals(name);
    }

    private static int countErasedSentinels(Type[] parameters, Type returnType) {
        int count = 0;
        for (Type parameter : parameters) {
            if (TaintUtils.isWrappedTypeWithErasedType(parameter)) {
                count++;
            }
        }
        if (TaintUtils.isErasedReturnType(returnType)) {
            count++;
        }
        return count;
    }

    private static List<Type> getFullArguments(boolean isStatic, Type[] parameters) {
        List<Type> result = new ArrayList<>();
        if (!isStatic) {
            result.add(Type.getType("Lthis;"));
        }
        for (Type t : parameters) {
            result.add(t);
        }
        return result;
    }

    private static Type getTypeForStackTypeTOPAsNull(Object obj) {
        if (obj == Opcodes.TOP) {
            return Type.getType("Ljava/lang/Object;");
        } else if (obj instanceof String) {
            if (!(((String) obj).charAt(0) == '[') && ((String) obj).length() > 1) {
                return Type.getType("L" + obj + ";");
            }
        }
        return TaintAdapter.getTypeForStackType(obj);
    }

    private static int sumSizes(Type[] types) {
        int result = 0;
        for (Type argumentType : types) {
            result += argumentType.getSize();
        }
        return result;
    }

    private static int[] computeParameterCountMap(Type[] parameters, boolean isStatic, boolean isLambda) {
        int[] map = new int[parameters.length + 1];
        int added = isStatic ? 0 : 1;
        for (int i = 1; i < map.length; i++) {
            if (!isLambda && TaintUtils.isShadowedType(parameters[i - 1])) {
                added++;
            }
            map[i] = i + added;
        }
        return map;
    }

    private static int[] computeParameterMap(Type[] parameters, boolean isStatic, boolean isLambda) {
        int[] map = new int[parameters.length];
        int added = isStatic ? 0 : 1;
        for (int i = 0; i < map.length; i++) {
            map[i] = i + added;
            if (!isLambda && TaintUtils.isShadowedType(parameters[i])) {
                added++;
            }
        }
        return map;
    }

    private static int computeLocalMap(int[] map, Type[] parameters, boolean isStatic, boolean isLambda) {
        int added = 0;
        int index = 0;
        if (!isStatic) {
            map[index++] = 0;
            added++;
        }
        for (Type parameter : parameters) {
            map[index] = added + index;
            index++;
            if (parameter.getSize() == 2) {
                map[index] = added + index;
                index++;
            }
            if (!isLambda && TaintUtils.isShadowedType(parameter)) {
                added++;
            }
        }
        return index + added;
    }
}