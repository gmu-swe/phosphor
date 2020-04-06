package edu.columbia.cs.psl.phosphor.control;

import org.objectweb.asm.MethodVisitor;

public interface ControlFlowManager {

    Class<? extends ControlFlowStack> getControlStackClass();

    void visitCreateStack(MethodVisitor mv, boolean disabled);

    ControlFlowStack getStack(boolean disabled);

    /**
     * @param access     the access flags of the method
     * @param owner      the internal name of the owner class of the method
     * @param name       the name of the method
     * @param descriptor the descriptor of the method
     * @return a new ControlFlowPropagationPolicy for the specified method
     */
    ControlFlowPropagationPolicy createPropagationPolicy(int access, String owner, String name,
                                                         String descriptor);

    boolean isIgnoredFromControlTrack(String className, String methodName);
}
