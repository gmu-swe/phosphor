package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.*;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.BranchEdge;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.MultiIDBranch;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.PropagatingBranchEdge;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.SingleIDBranch;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.PropagatingControlFlowDelegator.push;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static org.objectweb.asm.Opcodes.*;

public class BindingControlFlowDelegator extends AbstractControlFlowDelegator {

    private static LoopLevel defaultLevel = new VariantLoopLevel(1);

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    /**
     * Visitor to which instruction visiting that needs to "pass through" the primary delegate is delegated.
     */
    private final MethodVisitor passThroughDelegate;

    /**
     * Tracks the current stack and local variable bindings.
     */
    private final NeverNullArgAnalyzerAdapter analyzer;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private final LocalVariableManager localVariableManager;

    /**
     * The number of unique identifiers for branch locations in the method being visited.
     */
    private final int numberOfBranchIDs;

    /**
     * The branch information associated with the next branch instruction to be visited
     */
    private BranchStartInfo nextBranchStartInfo = null;

    /**
     * The loop level information associated with the next instruction to be visited or null if no information
     * is associated with the instruction or there are no remaining instructions to be visited
     */
    private LoopLevel nextCopyTagInfo = defaultLevel;

    public BindingControlFlowDelegator(MethodVisitor delegate, MethodVisitor passThroughDelegate, NeverNullArgAnalyzerAdapter analyzer,
                                       LocalVariableManager localVariableManager, int numberOfBranchIDs) {
        this.delegate = delegate;
        this.passThroughDelegate = passThroughDelegate;
        this.analyzer = analyzer;
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
    public void visitingTrackedInstanceOf() {
        // TODO
    }

    @Override
    public void visitingJump(int opcode) {
        switch(opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                // v1 t1
                pushBranchStarts();
                break;
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
                pushBranchStarts();
                break;
        }
    }

    @Override
    public void visitingSwitch() {
        pushBranchStarts();
    }

    @Override
    public void visitingExitLoopLevelInfo(ExitLoopLevelInfo info) {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        push(delegate, info.getLevelOffset());
        CONTROL_STACK_EXIT_LOOP_LEVEL.delegateVisit(delegate);
    }

    @Override
    public void visitingLoopAwarePop(LoopAwarePopInfo info) {
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        push(delegate, info.getBranchID());
        CONTROL_STACK_LOOP_AWARE_POP.delegateVisit(delegate);
    }

    @Override
    public void visitingBranchStart(BranchStartInfo info) {
        nextBranchStartInfo = info;
    }

    @Override
    public void visitingCopyTagInfo(CopyTagInfo info) {
        nextCopyTagInfo = info.getLevel();
    }

    // stack_pre = [taint]
    // stack_post =  []
    private void pushBranchStarts() {
        if(nextBranchStartInfo instanceof SingleIDBranch) {
            pushBranchStart(((SingleIDBranch) nextBranchStartInfo).getBranchID(), ((SingleIDBranch) nextBranchStartInfo).getLevel());
        } else if(nextBranchStartInfo instanceof MultiIDBranch) {
            // Issue is here
            for(BranchEdge edge : ((MultiIDBranch) nextBranchStartInfo).getEdges()) {
                if(edge instanceof PropagatingBranchEdge) {
                    pushBranchStart(((PropagatingBranchEdge) edge).getBranchID(), ((PropagatingBranchEdge) edge).getLevel());
                }
            }
        }
        delegate.visitInsn(POP);
        nextBranchStartInfo = null;
    }

    private void pushBranchStart(int branchID, LoopLevel level) {
        // T
        delegate.visitInsn(DUP);
        // T T
        delegate.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        // T T ControlTaintTagStack
        delegate.visitInsn(SWAP);
        // T ControlTaintTagStack T
        push(delegate, branchID);
        push(delegate, numberOfBranchIDs);
        // T ControlTaintTagStack T int int
        level.pushTag(delegate);
    }
}
