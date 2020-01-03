package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collection;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import org.objectweb.asm.Label;

public abstract class BranchStartInfo implements PhosphorInstructionInfo {

    private BranchStartInfo() {

    }

    public static final class SingleIDBranch extends BranchStartInfo {
        private final LoopLevel level;
        private final int branchID;

        SingleIDBranch(LoopLevel level, int branchID) {
            if(level == null) {
                throw new NullPointerException();
            }
            this.level = level;
            this.branchID = branchID;
        }

        public LoopLevel getLevel() {
            return level;
        }

        public int getBranchID() {
            return branchID;
        }
    }

    public static final class MultiIDBranch extends BranchStartInfo {

        private final List<BranchEdge> edges;

        MultiIDBranch(Collection<? extends BranchEdge> edges) {
            this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        }

        public List<BranchEdge> getEdges() {
            return edges;
        }
    }

    public static class BranchEdge {
        // Target of the branch or null if the edge is the branch no taken case
        private final Label target;

        BranchEdge(Label target) {
            this.target = target;
        }

        public Label getTarget() {
            return target;
        }
    }

    public static final class PropagatingBranchEdge extends BranchEdge {
        private final LoopLevel level;
        private final int branchID;

        PropagatingBranchEdge(LoopLevel level, int branchID, Label target) {
            super(target);
            if(level == null) {
                throw new NullPointerException();
            }
            this.level = level;
            this.branchID = branchID;
        }

        public LoopLevel getLevel() {
            return level;
        }

        public int getBranchID() {
            return branchID;
        }
    }
}
