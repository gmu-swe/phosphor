package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Stores the information needed by an ASM method visitor to add a call to a method.
 */
public interface MethodRecord {

    /**
     * @return the opcode of the type instruction associated with this method
     */
    int getOpcode();

    /**
     * @return the internal name of this method's owner class
     */
    String getOwner();

    /**
     * @return this method's name
     */
    String getName();

    /**
     * @return this method's descriptor
     */
    String getDescriptor();

    /**
     * @return true if this method's owner class is an interface
     */
    boolean isInterface();

    /**
     * @return this method's return type
     */
    Class<?> getReturnType();

    /**
     * Tells the specified method visitor to visit a method instruction for this method.
     *
     * @param methodVisitor the method visitor that should visit this method
     */
    default void delegateVisit(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(getOpcode(), getOwner(), getName(), getDescriptor(), isInterface());
    }

    static String createDescriptor(Class<?> returnType, Class<?>... parameterTypes) {
        Type[] parameters = new Type[parameterTypes.length];
        for(int i = 0; i < parameters.length; i++) {
            parameters[i] = Type.getType(parameterTypes[i]);
        }
        return Type.getMethodDescriptor(Type.getType(returnType), parameters);
    }
}
