package edu.columbia.cs.psl.phosphor.control.graph;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface BasicBlock {

    AbstractInsnNode getFirstInsn();

    AbstractInsnNode getLastInsn();
}
