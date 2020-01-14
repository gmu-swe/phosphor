package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

    /**
     * Called before an IINC instruction.
     *
     * @param var       index of the local variable to be incremented
     * @param shadowVar the index of the taint tag associated with the specified local variable
     */
    void visitingIncrement(int var, int shadowVar);

    /**
     * Called before a IASTORE, LASTORE, FASTORE, DASTORE, BASTORE, CASTORE, SASTORE, or AASTORE instruction
     * stack_pre = [taint]
     * stack_post = [taint]
     */
    void visitingArrayStore();

    /**
     * Called before a DSTORE, LSTORE, FSTORE, ISTORE, or ASTORE instruction.
     * stack_pre = [value taint]
     * stack_post = [value taint]
     *
     * @param opcode the opcode of the instruction being visited
     * @param var    the operand of the instruction to be visited
     */
    void visitingLocalVariableStore(int opcode, int var);

    /**
     * Called before a PUTFIELD or PUTSTATIC instruction.
     * stack_pre = [value taint]
     * stack_post = [value taint]
     *
     * @param isStatic        if a PUTSTATIC instruction is being visited
     * @param type            the formal type of the field
     * @param topCarriesTaint true if the top value on the stack is tainted
     */
    void visitingFieldStore(boolean isStatic, Type type, boolean topCarriesTaint);

    /**
     * Called to create a new taint instance.
     * stack_pre = []
     * stack_post = [taint]
     */
    void generateEmptyTaint();

    /**
     * Call before a return or exceptional return instruction.
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either ATHROW, ARETURN, IRETURN,
     *               RETURN, DRETURN, FRETURN, or LRETURN
     */
    void onMethodExit(int opcode);


    /**
     * Called for a jump operation.
     * If visiting a IF_ACMP<cond> or IF_ICMP<cond>: stack_pre = [value1 taint1 value2 taint2], stack_post=[value1 value2]
     * If visiting a IF<cond>, IFNULL, or IFNONULL: stack_pre = [value1 taint1], stack_post = [value1]
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either IFEQ,
     *               IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
     *               IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     * @param label  the instruction to which the jump instruction may jump
     */
    void visitingJump(int opcode, Label label);

    /**
     * Called for a TABLESWITCH instruction.
     * stack_pre = [value taint]
     * stack_post = [value]
     *
     * @param min          the minimum key value.
     * @param max          the maximum key value.
     * @param defaultLabel beginning of the default handler block.
     * @param labels       beginnings of the handler blocks. {@code labels[i]} is the beginning of the
     *                     handler block for the {@code min + i} key.
     */
    void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels);

    /**
     * Called for a LOOKUPSWITCH instruction.
     * stack_pre = [value taint]
     * stack_post = [value]
     *
     * @param defaultLabel beginning of the default handler block.
     * @param keys         the values of the keys.
     * @param labels       beginnings of the handler blocks. {@code labels[i]} is the beginning of the
     *                     handler block for the {@code keys[i]} key.
     */
    void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels);

    /**
     * Loads the specified value onto the stack.
     *
     * @param delegate the method visitor that should be used to load the specified value onto the stack
     * @param value    the value to be pushed onto the stack
     */
    static void push(MethodVisitor delegate, int value) {
        if(value >= -1 && value <= 5) {
            delegate.visitInsn(Opcodes.ICONST_0 + value);
        } else if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            delegate.visitIntInsn(Opcodes.BIPUSH, value);
        } else if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            delegate.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            delegate.visitLdcInsn(value);
        }
    }
}
