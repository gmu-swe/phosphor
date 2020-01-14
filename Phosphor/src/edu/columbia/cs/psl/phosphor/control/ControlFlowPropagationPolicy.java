package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public interface ControlFlowPropagationPolicy {

    /**
     * Called before visitCode
     */
    void visitingCode();

    /**
     * Called before visitMaxs
     */
    void visitingMaxs();

    /**
     * Called before a LdcInsn with a PhosphorInstructionInfo constant
     *
     * @param info the constant of the LdcInsn
     */
    void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info);

    /**
     * stack_pre = [ControlFlowStack]
     * stack_post = [ControlFlowStack]
     * <p>
     * Called before a MethodInsn or InvokeDynamicInsn that is not ignored by Phosphor and is passed a ControlFlowStack.
     */
    void prepareFrame();
}
