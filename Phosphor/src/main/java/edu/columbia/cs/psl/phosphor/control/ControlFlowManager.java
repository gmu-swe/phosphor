package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
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

    /**
     * @param className the slash-separated, fully qualified name of a class (e.g., java/lang/Short)
     * @return true if the class with the specified name should not be instrumented by Phosphor
     */
    default boolean isIgnoredClass(String className) {
        return false;
    }

    /**
     * Returns whether a class with the specified name is used by Phosphor for "internal" tainting
     * (e.g., {@link MultiTainter}. Calls to methods in internal tainting classes from instrumented classes are
     * remapped to the appropriate "$$PHOSPHORTAGGED" version even if the internal tainting class is not instrumented
     * by Phosphor. This requires internal tainting classes to provide instrumented versions of any method that may be
     * invoked by a classes that is instrumented by Phosphor.
     *
     * @param className the name of class being checking
     * @return true if a class with the specified name is used by Phosphor for internal tainting
     */
    default boolean isInternalTaintingClass(String className) {
        return false;
    }
}
