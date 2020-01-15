package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public class BranchEnd implements PhosphorInstructionInfo {

    private final int branchID;

    public BranchEnd(int branchID) {
        this.branchID = branchID;
    }

    public int getBranchID() {
        return branchID;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof BranchEnd)) {
            return false;
        }
        BranchEnd that = (BranchEnd) o;
        return branchID == that.branchID;
    }

    @Override
    public int hashCode() {
        return branchID;
    }
}
