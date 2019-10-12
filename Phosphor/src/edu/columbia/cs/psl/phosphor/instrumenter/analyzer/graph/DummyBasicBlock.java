package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

/**
 * A node in a control flow graph that does not contain any real instructions
 */
public class DummyBasicBlock implements BasicBlock {

    private final InsnNode insn = new InsnNode(Opcodes.NOP);

    @Override
    public AbstractInsnNode getFirstInsn() {
        return insn;
    }

    @Override
    public AbstractInsnNode getLastInsn() {
        return insn;
    }
}