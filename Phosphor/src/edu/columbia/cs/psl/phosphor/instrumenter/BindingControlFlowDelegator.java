package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.*;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.EdgeInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.JumpStartInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo.SwitchStartInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;

import java.util.Arrays;

import static edu.columbia.cs.psl.phosphor.Configuration.TAINT_TAG_INTERNAL_NAME;
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

    /**
     * A mapping between new branch targets and edge information for the original edge
     */
    private Map<Label, EdgeInfo> trailingBlocks = new HashMap<>();

    /**
     * A mapping between the new branch target and the old branch target that they replace
     */
    private Map<Label, Label> branchTargetMap = new HashMap<>();

    /**
     * A mapping between labels and the frame that should be assigned after a new branch target whose old target
     * was the specified label
     */
    private Map<Label, FrameNode> targetFrameMap = new HashMap<>();

    /**
     * The labels visited without a following visit to a frame
     */
    private SinglyLinkedList<Label> visitedLabels = new SinglyLinkedList<>();

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

    @Override
    public void visitingLabel(Label label) {
        visitedLabels.push(label);
    }

    @Override
    public void visitingFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        for(Label label : visitedLabels) {
            Object[] newStack = Arrays.copyOf(stack, stack.length + 1);
            newStack[stack.length] = TAINT_TAG_INTERNAL_NAME;
            FrameNode newTargetFrame = new FrameNode(F_NEW, numLocal, local, numStack + 1, newStack);
            targetFrameMap.put(label, newTargetFrame);
        }
        visitedLabels.clear();
        super.visitingFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitingMaxs() {
        for(Label newTarget : trailingBlocks.keySet()) {
            addTrailingBlock(newTarget, trailingBlocks.get(newTarget));
        }
    }

    @Override
    public void visitingJump(int opcode, Label label) {
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
                pushBranchStart(opcode, label, false);
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
                pushBranchStart(opcode, label, true);
                break;
            default:
                delegate.visitJumpInsn(opcode, label);
        }
    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
        defaultLabel = pushBranchStart(defaultLabel, labels);
        delegate.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
        defaultLabel = pushBranchStart(defaultLabel, labels);
        delegate.visitLookupSwitchInsn(defaultLabel, keys, labels);
    }

    // stack_pre = [taint]
    private void pushBranchStart(int opcode, Label label, boolean twoOperand) {
        if(nextBranchStartInfo instanceof JumpStartInfo) {
            JumpStartInfo jumpInfo = ((JumpStartInfo) nextBranchStartInfo);
            Label newLabel = new Label();
            branchTargetMap.put(newLabel, label);
            trailingBlocks.put(newLabel, jumpInfo.getLabel());
            if(twoOperand) {
                // v1 v2 t
                delegate.visitInsn(DUP_X2);
                // t v1 v2 t
                delegate.visitInsn(POP);
            } else {
                // v1 t1
                delegate.visitInsn(SWAP);
            }
            delegate.visitJumpInsn(opcode, newLabel);
            jumpInfo.getNotTaken().pushTag(delegate, localVariableManager.getIndexOfMasterControlLV(), numberOfBranchIDs);
        } else {
            delegate.visitInsn(POP); // Pop the taint tag off the stack
            delegate.visitJumpInsn(opcode, label);
        }
        nextBranchStartInfo = null;
    }

    private Label pushBranchStart(Label defaultLabel, Label[] labels) {
        if(nextBranchStartInfo instanceof SwitchStartInfo) {
            SwitchStartInfo switchInfo = ((SwitchStartInfo) nextBranchStartInfo);
            Map<Label, Label> oldNewMap = new HashMap<>();
            Label newDefault = new Label();
            branchTargetMap.put(newDefault, defaultLabel);
            oldNewMap.put(defaultLabel, newDefault);
            defaultLabel = newDefault;
            trailingBlocks.put(newDefault, switchInfo.getDefaultLabel());
            EdgeInfo[] labelInfo = switchInfo.getLabels();
            for(int i = 0; i < labels.length; i++) {
                if(!oldNewMap.containsKey(labels[i])) {
                    Label newLabel = new Label();
                    branchTargetMap.put(newLabel, labels[i]);
                    oldNewMap.put(labels[i], newLabel);
                    trailingBlocks.put(newLabel, labelInfo[i]);
                }
                Label newLabel = oldNewMap.get(labels[i]);
                labels[i] = newLabel;
            }
            delegate.visitInsn(SWAP);
        } else {
            delegate.visitInsn(POP); // Pop the taint tag off the stack
        }
        nextBranchStartInfo = null;
        return defaultLabel;
    }

    private void addTrailingBlock(Label newTarget, EdgeInfo edge) {
        delegate.visitLabel(newTarget);
        Label oldTarget = branchTargetMap.get(newTarget);
        FrameNode frame = targetFrameMap.get(oldTarget);
        delegate.visitFrame(frame.type, frame.local.size(), frame.local.toArray(), frame.stack.size(), frame.stack.toArray());
        edge.pushTag(delegate, localVariableManager.getIndexOfMasterControlLV(), numberOfBranchIDs);
        delegate.visitJumpInsn(GOTO, oldTarget);
    }
}
