package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;
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

    @Override
    public String toDotString(Map<Label, String> labelNames) {
        return "NOP";
    }

    @Override
    public String toString() {
        return "DummyBasicBlock";
    }
}