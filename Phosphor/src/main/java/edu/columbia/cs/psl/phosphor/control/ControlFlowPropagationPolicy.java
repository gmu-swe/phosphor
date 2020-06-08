package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Implementing classes specify how control flows should propagation in visited methods by delegating instruction visits.
 */
public interface ControlFlowPropagationPolicy {

    ControlFlowAnalyzer getFlowAnalyzer();

    void initialize(MethodVisitor delegate, LocalVariableManager localVariableManager, NeverNullArgAnalyzerAdapter analyzer);

    /**
     * Called before visitCode
     *
     * @param mv method visitor that should be used as the delegate in this method
     */
    void initializeLocalVariables(MethodVisitor mv);

    /**
     * Called before visitMaxs
     */
    void visitingMaxs();

    /**
     * stack_pre = [ControlFlowStack]
     * stack_post = [ControlFlowStack]
     * <p>
     * Called before a MethodInsn or InvokeDynamicInsn that is not ignored by Phosphor and is passed a ControlFlowStack.
     */
    void preparingFrame();

    /**
     * Called before adding a call to to popFrame
     *
     * @param mv method visitor that should be used as the delegate in this method
     */
    void poppingFrame(MethodVisitor mv);

    LocalVariable[] createdLocalVariables();

    /**
     * Called before an IINC instruction.
     *
     * @param var       index of the local variable to be incremented
     * @param shadowVar the index of the taint tag associated with the specified local variable
     */
    void visitingIncrement(int var, int shadowVar);

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
     * Called before a IASTORE, LASTORE, FASTORE, DASTORE, BASTORE, CASTORE, SASTORE, or AASTORE instruction
     * stack_pre = [taint]
     * stack_post = [taint]
     *
     * @param opcode the opcode of the type instruction to being visited, either IASTORE, LASTORE, FASTORE, DASTORE,
     *               BASTORE, CASTORE, SASTORE, or AASTORE
     */
    void visitingArrayStore(int opcode);

    /**
     * Called before a IALOAD, LALOAD, FALOAD, DALOAD, BALOAD, CALOAD, SALOAD, or AALOAD instruction
     * stack_pre = [value, reference-taint, value-taint]
     * stack_post = [value, reference-taint, value-taint]
     * <p>
     * reference-taint is the taint tag of the array reference whose element was loaded.
     * value-taint is the taint tag for the array element that was loaded onto the stack.
     *
     * @param opcode the opcode of the type instruction to being visited, either IALOAD, LALOAD, FALOAD, DALOAD,
     *               BALOAD, CALOAD, SALOAD, or AALOAD
     */
    void visitingArrayLoad(int opcode);

    /**
     * Called before a PUTFIELD or PUTSTATIC instruction.
     * stack_pre = [value taint]
     * stack_post = [value taint]
     *
     * @param opcode     the opcode of the type instruction to being visited, either PUTFIELD or PUTSTATIC
     * @param owner      the internal name of the field's owner class
     * @param name       the field's name.
     * @param descriptor the field's descriptor
     */
    void visitingFieldStore(int opcode, String owner, String name, String descriptor);

    /**
     * Called before a GETFIELD instruction.
     * stack_pre =  [value, reference-taint, value-taint]
     * stack_post = [value, reference-taint, value-taint]
     * <p>
     * reference-taint is the taint tag of the object reference whose field was loaded.
     * value-taint is the taint tag for the value of the field that was loaded onto the stack.
     *
     * @param owner      the internal name of the field's owner class
     * @param name       the field's name.
     * @param descriptor the field's descriptor
     */
    void visitingInstanceFieldLoad(String owner, String name, String descriptor);

    /**
     * Called to create a new taint instance.
     * stack_pre = []
     * stack_post = [taint]
     */
    void generateEmptyTaint();

    /**
     * Called before a return or exceptional return instruction.
     * <p>
     * For  ARETURN, IRETURN, DRETURN, FRETURN, or LRETURN:
     * stack_pre = stack_post = [value taint]
     * <p>
     * For RETURN
     * stack_pre = stack_post = []
     * <p>
     * For ATHROW:
     * stack_pre = stack_post = [exception taint]
     *
     * @param opcode the opcode of the type instruction to being visited. This opcode is either ATHROW, ARETURN,
     *               IRETURN, RETURN, DRETURN, FRETURN, or LRETURN
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
     * Called before a LdcInsn with a PhosphorInstructionInfo constant
     *
     * @param info the constant of the LdcInsn
     */
    void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info);

    /**
     * Called for after an INSTANCEOF instruction. Taint tag on the stack will be taken as the taint tag of the resulting
     * boolean. By default it is the taint tag of the reference that INSTANCEOF was called on.
     * <p>
     * stack_pre = stack_post = [z taint]
     */
    void visitingInstanceOf();

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
