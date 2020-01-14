package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import org.objectweb.asm.MethodVisitor;

import static edu.columbia.cs.psl.phosphor.control.PropagatingControlFlowDelegator.push;
import static org.objectweb.asm.Opcodes.*;

public abstract class BranchStartInfo implements PhosphorInstructionInfo {

    private BranchStartInfo() {

    }

    public static final class JumpStartInfo extends BranchStartInfo {
        private final EdgeInfo notTaken;
        private final EdgeInfo label;

        JumpStartInfo(EdgeInfo notTaken, EdgeInfo label) {
            this.notTaken = notTaken;
            this.label = label;
        }

        public EdgeInfo getNotTaken() {
            return notTaken;
        }

        public EdgeInfo getLabel() {
            return label;
        }
    }

    public static final class SwitchStartInfo extends BranchStartInfo {
        private final EdgeInfo defaultLabel;
        private final EdgeInfo[] labels;

        SwitchStartInfo(EdgeInfo defaultLabel, EdgeInfo[] labels) {
            this.defaultLabel = defaultLabel;
            this.labels = labels;
        }

        public EdgeInfo getDefaultLabel() {
            return defaultLabel;
        }

        public EdgeInfo[] getLabels() {
            return labels;
        }
    }

    public abstract static class EdgeInfo {
        private EdgeInfo() {

        }

        public abstract void pushTag(MethodVisitor mv, int indexOfMasterControlLV, int numberOfBranchIDs);
    }

    static class PropagatingEdgeInfo extends EdgeInfo {
        private final LoopLevel level;
        private final int branchID;

        PropagatingEdgeInfo(LoopLevel level, int branchID) {
            this.level = level;
            this.branchID = branchID;
        }

        @Override
        public void pushTag(MethodVisitor mv, int indexOfMasterControlLV, int numberOfBranchIDs) {
            // T
            mv.visitVarInsn(ALOAD, indexOfMasterControlLV);
            // T T ControlTaintTagStack
            mv.visitInsn(SWAP);
            // T ControlTaintTagStack T
            push(mv, branchID);
            push(mv, numberOfBranchIDs);
            // T ControlTaintTagStack T int int
            level.pushTag(mv);
        }
    }

    static class NonPropagatingEdgeInfo extends EdgeInfo {
        @Override
        public void pushTag(MethodVisitor mv, int indexOfMasterControlLV, int numberOfBranchIDs) {
            mv.visitInsn(POP);
        }
    }
}
