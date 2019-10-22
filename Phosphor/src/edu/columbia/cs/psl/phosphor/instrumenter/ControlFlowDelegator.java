package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Type;

/**
 * Implementing classes specify a control flow propagation policy for a method by delegating instruction visits.
 */
interface ControlFlowDelegator {

    /**
     * Called as a result of entering the method. Maintains the stack.
     */
    void visitedCode();

    /**
     * Called as a result of a visiting a try-catch block. Maintains the stack
     */
    void visitedTryCatch();

    /**
     * Called before an IINC instruction. Maintains the stack.
     *
     * @param var index of the local variable to be incremented
     */
    void visitingIncrement(int var);

    /**
     * Called before a BRANCH_START instruction. Maintains the stack
     *
     * @param branchID the identifier of the "branch" location that is starting
     */
    void visitingBranchStart(int branchID);

    /**
     * Called before a BRANCH_END instruction. Maintains the stack.
     *
     * @param branchID the identifier of the "branch" location that is ending
     */
    void visitingBranchEnd(int branchID);

    /**
     * Called before a DSTORE, LSTORE, FSTORE, ISTORE, ASTORE, or FORCE_CTRL_STORE instruction. Maintains the
     * stack.
     * <p>
     * If visiting a DSTORE, LSTORE, FSTORE, or ISTORE instruction: stack_pre = taint val
     * <p>
     * If visiting an ASTORE instruction: stack_pre = objectref
     *
     * @param opcode the opcode of the instruction being visited
     * @param var    the operand of the instruction to be visited
     */
    void storingTaintedValue(int opcode, int var);

    /**
     * Called before a FORCE_CTRL_STORE_SFIELD or FORCE_CTRL_STORE field instruction. Maintains the stack.
     *
     * @param field the field that is being marked as needing to be force control stored
     */
    void visitingForceControlStoreField(Field field);

    /**
     * Called before a PUTFIELD or PUTSTATIC instruction. Maintains the stack.
     * <p>
     * If visiting a PUTFIELD instruction for a tainted primitive field: stack_pre = objectref taint val
     * <p>
     * If visiting a PUTFIELD instruction for a reference type field: stack_pre = objectref objectref
     * <p>
     * If visiting a PUTSTATIC instruction for a primitive field: stack_pre = taint val
     * <p>
     * If visiting a PUTSTATIC instruction for a reference type field: stack_pre = objectref
     *
     * @param isStatic        if the a PUTSTATIC instruction is being visited
     * @param type            the formal type of the field
     * @param topCarriesTaint true if the top value on the stack is tainted
     */
    void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint);

    /**
     * Called to create a new taint instance. stack_post = taint
     */
    void generateEmptyTaint();

    /**
     * Called before an EXCEPTION_HANDLER_START instruction. Maintains the stack.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingExceptionHandlerStart(String type);

    /**
     * Called before an EXCEPTION_HANDLER_END instruction. Maintains the stack.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingExceptionHandlerEnd(String type);

    /**
     * Called before an UNTHROWN_EXCEPTION instruction. Maintains the stack.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingUnthrownException(String type);

    /**
     * Called before an UNTHROWN_EXCEPTION_CHECK instruction. Maintains the stack.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingUnthrownExceptionCheck(String type);

    /**
     * Called as a result of an INSTANCE_OF instruction. stack_pre = objectref, stack_post = taint objectref
     */
    void visitingTrackedInstanceOf();

    /**
     * Called before visiting the maximum stack size and the maximum number of local variables of the method. Maintains
     * the stack.
     */
    void visitingMaxs(int maxStack, int maxLocals);

    /**
     * Call before a return or exceptional return instruction. Maintains the stack.
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either ATHROW, ARETURN, IRETURN,
     *               RETURN, DRETURN, FRETURN, or LRETURN
     */
    void onMethodExit(int opcode);

    /**
     * Called before a FORCE_CTRL_STORE instruction. Maintains the stack.
     */
    void visitingForceControlStore(Type stackTop);

    /**
     * Called before a jump operation.
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either IFEQ,
     *               IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
     *               IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     */
    void visitingJump(int opcode);

    /**
     * Called before a LOOKUPSWITCH or TABLESWITCH instruction. If propagating control flows then stack_pre = taint val,
     * otherwise stack_pre = taint val. stack_post = val
     */
    void visitingSwitch();

    /**
     * Called before an AASTORE instruction. Maintains the stack. stack_pre = objectref
     */
    void storingReferenceInArray();

    /**
     * Called before a EXCLUDE_BRANCH instruction. Maintains the stack.
     *
     * @param branchID the identifier of the "branch" location that is to be excluded
     */
    void visitingExcludeBranch(int branchID);
}
