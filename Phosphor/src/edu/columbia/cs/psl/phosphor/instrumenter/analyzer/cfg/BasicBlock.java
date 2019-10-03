package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Arrays;

class BasicBlock extends ControlFlowNode {

    /**
     * Sequence of instructions in this block
     */
    private final AbstractInsnNode[] instructions;

    BasicBlock(final AbstractInsnNode[] instructions, final int start, final int end) {
        if(end <= start) {
            throw new IllegalArgumentException("Invalid range for basic block");
        }
        this.instructions = Arrays.copyOfRange(instructions, start, end);
    }

    AbstractInsnNode getFirstInsn() {
        return instructions[0];
    }

    AbstractInsnNode getLastInsn() {
        return instructions[instructions.length - 1];
    }
}
