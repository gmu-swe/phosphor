package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Annotates a MethodNode's instruction list by inserting {@link LdcInsnNode LdcInsnNode} nodes containing
 * {@link PhosphorInstructionInfo PhosphorInstructionInfo} constants. These PhosphorInstructionInfo constants are used
 * to pass information to a {@link ControlFlowPropagationPolicy ControlFlowPropagationPolicy}.
 */
public interface ControlFlowAnalyzer {
    void annotate(String owner, MethodNode methodNode);
}
