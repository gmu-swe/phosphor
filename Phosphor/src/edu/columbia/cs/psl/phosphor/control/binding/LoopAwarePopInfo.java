package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public final class LoopAwarePopInfo implements PhosphorInstructionInfo {

    private final int branchID;

    LoopAwarePopInfo(int branchID) {
        this.branchID = branchID;
    }

    public int getBranchID() {
        return branchID;
    }

    @Override
    public String toString() {
        return String.format("LoopAwarePop(%d)", branchID);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof LoopAwarePopInfo)) {
            return false;
        }
        LoopAwarePopInfo that = (LoopAwarePopInfo) o;
        return branchID == that.branchID;
    }

    @Override
    public int hashCode() {
        return branchID;
    }
}
