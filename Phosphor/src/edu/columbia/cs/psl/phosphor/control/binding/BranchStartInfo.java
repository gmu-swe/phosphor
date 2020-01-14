package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public class BranchStartInfo implements PhosphorInstructionInfo {

    private final LoopLevel level;
    private final int branchID;

    BranchStartInfo(LoopLevel level, int branchID) {
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
