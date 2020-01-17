package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public enum BindingMethodRecord implements MethodRecord {

    BINDING_CONTROL_STACK_START_FRAME(INVOKEVIRTUAL, BindingControlFlowStack.class, "startFrame", BindingControlFlowStack.class, false, int.class, int.class),
    BINDING_CONTROL_STACK_SET_ARG_CONSTANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "setNextFrameArgConstant", BindingControlFlowStack.class, false),
    BINDING_CONTROL_STACK_SET_ARG_DEPENDENT(INVOKEVIRTUAL, BindingControlFlowStack.class, "setNextFrameArgDependent", BindingControlFlowStack.class, false, int[].class),
    BINDING_CONTROL_STACK_SET_ARG_VARIANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "setNextFrameArgVariant", BindingControlFlowStack.class, false, int.class),
    BINDING_CONTROL_STACK_COPY_TAG_CONSTANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "copyTagConstant", Taint.class, false),
    BINDING_CONTROL_STACK_COPY_TAG_DEPENDENT(INVOKEVIRTUAL, BindingControlFlowStack.class, "copyTagDependent", Taint.class, false, int[].class),
    BINDING_CONTROL_STACK_COPY_TAG_VARIANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "copyTagVariant", Taint.class, false, int.class),
    BINDING_CONTROL_STACK_PUSH_CONSTANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "pushConstant", Void.TYPE, false, int.class, int.class),
    BINDING_CONTROL_STACK_PUSH_DEPENDENT(INVOKEVIRTUAL, BindingControlFlowStack.class, "pushDependent", Void.TYPE, false, int.class, int.class, int[].class),
    BINDING_CONTROL_STACK_PUSH_VARIANT(INVOKEVIRTUAL, BindingControlFlowStack.class, "pushVariant", Void.TYPE, false, int.class, int.class, int.class),
    BINDING_CONTROL_STACK_SET_NEXT_BRANCH_TAG(INVOKEVIRTUAL, BindingControlFlowStack.class, "setNextBranchTag", Void.TYPE, false, Taint.class),
    BINDING_CONTROL_STACK_EXIT_LOOP_LEVEL(INVOKEVIRTUAL, BindingControlFlowStack.class, "exitLoopLevel", Void.TYPE, false, int.class),
    BINDING_CONTROL_STACK_POP(INVOKEVIRTUAL, BindingControlFlowStack.class, "pop", Void.TYPE, false, int.class),
    BINDING_CONTROL_STACK_FACTORY(INVOKESTATIC, BindingControlFlowStack.class, "factory", BindingControlFlowStack.class, false, boolean.class);

    private final int opcode;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;
    private final Class<?> returnType;

    /**
     * Constructs a new method.
     *
     * @param opcode         the opcode of the type instruction associated with the method
     * @param owner          the internal name of the method's owner class
     * @param name           the method's name
     * @param returnType     the class of the method's return type
     * @param isInterface    if the method's owner class is an interface
     * @param parameterTypes the types of the parameters of the method
     */
    BindingMethodRecord(int opcode, Class<?> owner, String name, Class<?> returnType, boolean isInterface, Class<?>... parameterTypes) {
        this.opcode = opcode;
        this.owner = Type.getInternalName(owner);
        this.name = name;
        this.isInterface = isInterface;
        this.descriptor = MethodRecord.createDescriptor(returnType, parameterTypes);
        this.returnType = returnType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return returnType;
    }
}
