package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.CopyTagInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ExitLoopLevelInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopAwarePopInfo;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Type;

/**
 * Implementing classes specify a control flow propagation policy for a method by delegating instruction visits.
 */
interface ControlFlowDelegator {

    /**
     * Called before an IINC instruction.
     *
     * @param var       index of the local variable to be incremented
     * @param shadowVar the index of the taint tag associated with the specified local variable
     */
    void visitingIncrement(int var, int shadowVar);

    /**
     * Called before a BRANCH_START instruction.
     *
     * @param branchID the identifier of the "branch" location that is starting
     */
    void visitingBranchStart(int branchID);

    /**
     * Called before a BRANCH_END instruction.
     *
     * @param branchID the identifier of the "branch" location that is ending
     */
    void visitingBranchEnd(int branchID);

    /**
     * Called before a DSTORE, LSTORE, FSTORE, ISTORE, ASTORE, or FORCE_CTRL_STORE instruction.
     * stack_pre = [value taint]
     * stack_post = [value taint]
     *
     * @param opcode the opcode of the instruction being visited
     * @param var    the operand of the instruction to be visited
     */
    void storingTaintedValue(int opcode, int var);

    /**
     * Called before a FORCE_CTRL_STORE_FIELD or FORCE_CTRL_STORE field instruction.
     *
     * @param field the field that is being marked as needing to be force control stored
     */
    void visitingForceControlStoreField(Field field);

    /**
     * Called before a PUTFIELD or PUTSTATIC instruction.
     * stack_pre = [value taint]
     * stack_post = [value taint]
     *
     * @param isStatic        if a PUTSTATIC instruction is being visited
     * @param type            the formal type of the field
     * @param topCarriesTaint true if the top value on the stack is tainted
     */
    void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint);

    /**
     * Called to create a new taint instance.
     * stack_pre = []
     * stack_post = [taint]
     */
    void generateEmptyTaint();

    /**
     * Called before an EXCEPTION_HANDLER_START instruction.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingExceptionHandlerStart(String type);

    /**
     * Called before an EXCEPTION_HANDLER_END instruction.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingExceptionHandlerEnd(String type);

    /**
     * Called before an UNTHROWN_EXCEPTION instruction.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingUnthrownException(String type);

    /**
     * Called before an UNTHROWN_EXCEPTION_CHECK instruction.
     *
     * @param type the operand of the instruction to being visited
     */
    void visitingUnthrownExceptionCheck(String type);

    /**
     * Call before a return or exceptional return instruction.
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either ATHROW, ARETURN, IRETURN,
     *               RETURN, DRETURN, FRETURN, or LRETURN
     */
    void onMethodExit(int opcode);

    /**
     * Called before a FORCE_CTRL_STORE instruction.
     */
    void visitingForceControlStore(Type stackTop);

    /**
     * Called before a jump operation.
     * If visiting a IF_ACMP<cond> or IF_ICMP<cond>:
     *      stack_pre = [value1 taint1 value2 taint2]
     *      stack_post = [value1 value2]
     * if visiting a IF<cond>, IFNULL, or IFNONULL:
     *      stack_pre = [value1 taint1]
     *      stack_post = [value1]
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either IFEQ,
     *               IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
     *               IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     */
    void visitingJump(int opcode);

    /**
     * Called before a LOOKUPSWITCH or TABLESWITCH instruction.
     * stack_pre = [value taint]
     * stack_post = [value]
     */
    void visitingSwitch();

    /**
     * Called before a IASTORE, LASTORE, FASTORE, DASTORE, BASTORE, CASTORE, SASTORE, or AASTORE instruction
     * stack_pre = [taint]
     * stack_post = [taint]
     */
    void visitingArrayStore();

    /**
     * Called before a LDC ExitLoopLevelInfo instruction.
     *
     * @param info the constant of the LDC ExitLoopLevelInfo instruction
     */
    void visitingExitLoopLevelInfo(ExitLoopLevelInfo info);

    /**
     * Called before a LDC LoopAwarePopInfo instruction
     *
     * @param info the constant of the LDC LoopAwarePopInfo.
     */
    void visitingLoopAwarePop(LoopAwarePopInfo info);

    /**
     * Called before a LDC BranchStartInfo instruction
     *
     * @param info the constant of the LDC BranchStartInfo.
     */
    void visitingBranchStart(BranchStartInfo info);

    /**
     * Called before a LDC CopyTagInfo instruction
     *
     * @param info the constant of the LDC CopyTagInfo.
     */
    void visitingCopyTagInfo(CopyTagInfo info);
}
