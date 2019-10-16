package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.Configuration.TAINT_TAG_OBJ_CLASS;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

/**
 * Represent some method that is used to ensure that taint tags are correctly propagated. Stores the information needed
 * by ASM method visitors to visit the method.
 */
public enum TaintMethod {

    COMBINE_TAGS_ON_OBJECT(INVOKESTATIC, Taint.class, "combineTagsOnObject", Void.TYPE, false, Object.class, ControlTaintTagStack.class),
    COMBINE_TAGS_STACK(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, ControlTaintTagStack.class),
    GET_TAINT_OBJECT(INVOKESTATIC, TaintUtils.class, "getTaintObj", TAINT_TAG_OBJ_CLASS, false, Object.class),
    CONTROL_PUSH_TAG_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class, ExceptionalTaintData.class),
    CONTROL_PUSH_TAG(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class),
    PUSH_OBJECT_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, Object.class, int[].class, int.class, int.class, ExceptionalTaintData.class),
    CONTROL_PUSH_OBJECT(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, Object.class, int[].class, int.class, int.class),
    CONTROL_POP_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, int.class, ExceptionalTaintData.class),
    CONTROL_POP(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, int.class),
    CONTROL_POP_ALL_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, ExceptionalTaintData.class),
    CONTROL_POP_ALL(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class),
    COPY_TAINT(INVOKESTATIC, Taint.class, "copyTaint", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS),
    BOX_IF_NECESSARY(INVOKESTATIC, MultiDTaintedArray.class, "boxIfNecessary", Object.class, false, Object.class);

    private final int opcode;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;

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
    TaintMethod(int opcode, Class<?> owner, String name, Class<?> returnType, boolean isInterface, Class<?>... parameterTypes) {
        this.opcode = opcode;
        this.owner = Type.getInternalName(owner);
        this.name = name;
        this.isInterface = isInterface;
        Type[] parameters = new Type[parameterTypes.length];
        for(int i = 0; i < parameters.length; i++) {
            parameters[i] = Type.getType(parameterTypes[i]);
        }
        this.descriptor = Type.getMethodDescriptor(Type.getType(returnType), parameters);
    }

    /**
     * @return the opcode of the type instruction associated with this method
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * @return the internal name of this method's owner class
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return this method's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return this method's descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * @return true if this method's owner class is an interface
     */
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * Tells the specified method visitor to visit a method instruction for this method.
     *
     * @param methodVisitor the method visitor that should visit this method
     */
    public void delegateVisit(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
