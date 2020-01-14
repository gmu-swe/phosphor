package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.control.binding.*;
import edu.columbia.cs.psl.phosphor.control.binding.LoopLevel.VariantLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy.push;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static org.objectweb.asm.Opcodes.*;

public class BindingControlFlowDelegator extends AbstractControlFlowDelegator {

    private static LoopLevel defaultLevel = new VariantLoopLevel(0);

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private final LocalVariableManager localVariableManager;

    /**
     * The number of unique identifiers for branch locations in the method being visited.
     */
    private final int numberOfBranchIDs;

    /**
     * The loop level information associated with the next instruction to be visited or null if no information
     * is associated with the instruction or there are no remaining instructions to be visited
     */
    private LoopLevel nextCopyTagInfo = defaultLevel;

    public BindingControlFlowDelegator(MethodVisitor delegate, LocalVariableManager localVariableManager, int numberOfBranchIDs) {
        this.delegate = delegate;
        this.localVariableManager = localVariableManager;
        this.numberOfBranchIDs = numberOfBranchIDs;
    }

    @Override
    public void visitingIncrement(int var, int shadowVar) {
        delegate.visitVarInsn(ALOAD, shadowVar); // Current tag
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
        delegate.visitVarInsn(ASTORE, shadowVar);
    }

    @Override
    public void storingTaintedValue(int opcode, int var) {
        switch(opcode) {
            case ISTORE:
            case FSTORE:
            case DSTORE:
            case LSTORE:
            case ASTORE:
                copyTag();
                COMBINE_TAGS.delegateVisit(delegate);
        }
    }

    @Override
    public void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint) {
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
    }

    @Override
    public void generateEmptyTaint() {
        copyTag();
    }

    @Override
    public void visitingArrayStore() {
        copyTag();
        COMBINE_TAGS.delegateVisit(delegate);
    }

    // stack_pre = []
    // stack_post = [taint]
    private void copyTag() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        nextCopyTagInfo.copyTag(delegate);
        nextCopyTagInfo = defaultLevel;
    }

    @Override
    public void visitingJump(int opcode, Label label) {
        switch(opcode) {
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ACMPEQ:
                // v1 t1 v2 t2
                delegate.visitInsn(DUP2_X1);
                // v1 v2 t2 t1 v2 t2
                delegate.visitInsn(POP2);
                // v1 v2 t2 t1
                COMBINE_TAGS.delegateVisit(delegate);
                // v1 v2 t
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                // t
                setNextBranchTag();
        }
    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
        setNextBranchTag();
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
        setNextBranchTag();
    }

    /**
     * stack_pre = [taint]
     * stack_post = []
     */
    private void setNextBranchTag() {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        delegate.visitInsn(SWAP);
        CONTROL_STACK_SET_NEXT_BRANCH_TAG.delegateVisit(delegate);
    }

    @Override
    public void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info) {
        if(info instanceof ExitLoopLevelInfo) {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            push(delegate, ((ExitLoopLevelInfo) info).getLevelOffset());
            CONTROL_STACK_EXIT_LOOP_LEVEL.delegateVisit(delegate);
        } else if(info instanceof LoopAwarePopInfo) {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            push(delegate, ((LoopAwarePopInfo) info).getBranchID());
            CONTROL_STACK_LOOP_AWARE_POP.delegateVisit(delegate);
        } else if(info instanceof BranchStartInfo) {
            delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            push(delegate, ((BranchStartInfo) info).getBranchID());
            push(delegate, numberOfBranchIDs);
            ((BranchStartInfo) info).getLevel().pushTag(delegate);
        } else if(info instanceof CopyTagInfo) {
            nextCopyTagInfo = ((CopyTagInfo) info).getLevel();
        }
    }
}
