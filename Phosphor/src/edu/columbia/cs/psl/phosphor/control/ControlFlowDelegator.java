package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.control.binding.BranchStartInfo;
import edu.columbia.cs.psl.phosphor.control.binding.CopyTagInfo;
import edu.columbia.cs.psl.phosphor.control.binding.ExitLoopLevelInfo;
import edu.columbia.cs.psl.phosphor.control.binding.LoopAwarePopInfo;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

/**
 * Implementing classes specify a control flow propagation policy for a method by delegating instruction visits.
 */
public interface ControlFlowDelegator {

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
     * Called for a jump operation. The delegator is responsible for visiting the jump instruction itself.
     * If visiting a IF_ACMP<cond> or IF_ICMP<cond>: stack_pre = [value1 taint1 value2 taint2].
     * If visiting a IF<cond>, IFNULL, or IFNONULL: stack_pre = [value1 taint1].
     * stack_post = []
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either IFEQ,
     *               IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
     *               IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     * @param label  the instruction to which the jump instruction may jump
     */
    void visitingJump(int opcode, Label label);

    /**
     * Called for a TABLESWITCH instruction. The delegator is responsible for visiting the switch instruction itself.
     * stack_pre = [value taint]
     * stack_post = []
     *
     * @param min          the minimum key value.
     * @param max          the maximum key value.
     * @param defaultLabel beginning of the default handler block.
     * @param labels       beginnings of the handler blocks. {@code labels[i]} is the beginning of the
     *                     handler block for the {@code min + i} key.
     */
    void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels);

    /**
     * Called for a LOOKUPSWITCH instruction. The delegator is responsible for visiting the switch instruction itself.
     * stack_pre = [value taint]
     * stack_post = []
     *
     * @param defaultLabel beginning of the default handler block.
     * @param keys         the values of the keys.
     * @param labels       beginnings of the handler blocks. {@code labels[i]} is the beginning of the
     *                     handler block for the {@code keys[i]} key.
     */
    void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels);

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

    /**
     * Called before visitMaxs
     */
    void visitingMaxs();

    /**
     * Called before visitLabel
     *
     * @param label the label about to be visited
     */
    void visitingLabel(Label label);

    /**
     * Called before visitFrame
     *
     * @param type     the type of this stack map frame
     * @param numLocal the number of local variables in the visited frame
     * @param local    the local variable types in this frame
     * @param numStack the number of operand stack elements in the visited frame.
     * @param stack    the operand stack types in this frame
     */
    void visitingFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack);
}
