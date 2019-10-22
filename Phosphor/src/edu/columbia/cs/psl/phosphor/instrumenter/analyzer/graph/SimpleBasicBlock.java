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

    /**
     * An identifying number for this basic block
     */
    private final int identifier;

    /**
     * Constructs a new basic block that represents the specified instruction sequence.
     *
     * @param instructions the sequence of instructions in the basic block being constructed
     * @param identifier   a number used to identify the basic block being constructed
     */
    SimpleBasicBlock(final AbstractInsnNode[] instructions, int identifier) {
        if(instructions.length == 0) {
            this.firstInsn = this.lastInsn = new InsnNode(Opcodes.NOP);
        } else {
            this.firstInsn = instructions[0];
            this.lastInsn = instructions[instructions.length - 1];
        }
        this.identifier = identifier;
    }

    @Override
    public AbstractInsnNode getFirstInsn() {
        return firstInsn;
    }

    @Override
    public AbstractInsnNode getLastInsn() {
        return lastInsn;
    }

    @Override
    public String toString() {
        return String.format("BasicBlock{#%d: %s - %s}", identifier, firstInsn.getClass().getSimpleName(), lastInsn.getClass().getSimpleName());
    }
}
