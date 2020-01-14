package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

interface ControlFlowPropagationPolicy {
    void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info);
}
