package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

public class SimpleBasicBlock implements BasicBlock {

    /**
     * The first instruction in this block
     */
    private final AbstractInsnNode firstInsn;

    /**
     * The last instruction in this block
     */
    private final AbstractInsnNode lastInsn;

    SimpleBasicBlock(final AbstractInsnNode[] instructions) {
        if(instructions.length == 0) {
            this.firstInsn = this.lastInsn = new InsnNode(Opcodes.NOP);
        } else {
            this.firstInsn = instructions[0];
            this.lastInsn = instructions[instructions.length - 1];
        }
    }

    @Override
    public AbstractInsnNode getFirstInsn() {
        return firstInsn;
    }

    @Override
    public AbstractInsnNode getLastInsn() {
        return lastInsn;
    }
}
