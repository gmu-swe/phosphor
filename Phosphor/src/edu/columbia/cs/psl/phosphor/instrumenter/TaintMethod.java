package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * Represent some method that is used to ensure that taint tags are correctly propagated. Stores the information needed
 * by ASM method visitors to visit the method.
 */
public enum TaintMethod {

    COMBINE_TAGS_ON_OBJECT(INVOKESTATIC, Taint.class, "combineTagsOnObject", Void.TYPE, false, Object.class, ControlTaintTagStack.class);

    private final int opcode;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;

    /**
     * Constructs a new method.
     *
     * @param opcode the opcode of the type instruction associated with the method
     * @param owner the internal name of the method's owner class
     * @param name the method's name
     * @param returnType the class of the method's return type
     * @param isInterface if the method's owner class is an interface
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
}
