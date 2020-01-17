package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.Configuration.TAINT_TAG_OBJ_CLASS;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public enum ControlMethodRecord implements MethodRecord {

    STANDARD_CONTROL_STACK_PUSH_TAG_EXCEPTION(INVOKEVIRTUAL, StandardControlFlowStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class, ExceptionalTaintData.class),
    STANDARD_CONTROL_STACK_PUSH_TAG(INVOKEVIRTUAL, StandardControlFlowStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class),
    STANDARD_CONTROL_STACK_POP_EXCEPTION(INVOKEVIRTUAL, StandardControlFlowStack.class, "pop", Void.TYPE, false, int[].class, int.class, ExceptionalTaintData.class),
    STANDARD_CONTROL_STACK_POP(INVOKEVIRTUAL, StandardControlFlowStack.class, "pop", Void.TYPE, false, int[].class, int.class),
    STANDARD_CONTROL_STACK_POP_ALL_EXCEPTION(INVOKEVIRTUAL, StandardControlFlowStack.class, "pop", Void.TYPE, false, int[].class, ExceptionalTaintData.class),
    STANDARD_CONTROL_STACK_POP_ALL(INVOKEVIRTUAL, StandardControlFlowStack.class, "pop", Void.TYPE, false, int[].class),
    STANDARD_CONTROL_STACK_COPY_TAG(INVOKEVIRTUAL, StandardControlFlowStack.class, "copyTag", TAINT_TAG_OBJ_CLASS, false),
    STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_START(INVOKEVIRTUAL, StandardControlFlowStack.class, "exceptionHandlerStart", EnqueuedTaint.class, false, Throwable.class, Taint.class, EnqueuedTaint.class),
    STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_START_TYPES(INVOKEVIRTUAL, StandardControlFlowStack.class, "exceptionHandlerStart", Void.TYPE, false, Class.class),
    STANDARD_CONTROL_STACK_EXCEPTION_HANDLER_END(INVOKEVIRTUAL, StandardControlFlowStack.class, "exceptionHandlerEnd", Void.TYPE, false, EnqueuedTaint.class),
    STANDARD_CONTROL_STACK_TRY_BLOCK_END(INVOKEVIRTUAL, StandardControlFlowStack.class, "tryBlockEnd", Void.TYPE, false, Class.class),
    STANDARD_CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION(INVOKEVIRTUAL, StandardControlFlowStack.class, "applyPossiblyUnthrownExceptionToTaint", Void.TYPE, false, Class.class),
    STANDARD_CONTROL_STACK_ADD_UNTHROWN_EXCEPTION(INVOKEVIRTUAL, StandardControlFlowStack.class, "addUnthrownException", Void.TYPE, false, ExceptionalTaintData.class, Class.class),
    STANDARD_CONTROL_STACK_FACTORY(INVOKESTATIC, StandardControlFlowStack.class, "factory", StandardControlFlowStack.class, false, boolean.class);

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
    ControlMethodRecord(int opcode, Class<?> owner, String name, Class<?> returnType, boolean isInterface, Class<?>... parameterTypes) {
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
