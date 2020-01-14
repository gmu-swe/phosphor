package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public final class CopyTagInfo implements PhosphorInstructionInfo {

    private final LoopLevel level;

    CopyTagInfo(LoopLevel level) {
        this.level = level;
    }

    public LoopLevel getLevel() {
        return level;
    }
}
