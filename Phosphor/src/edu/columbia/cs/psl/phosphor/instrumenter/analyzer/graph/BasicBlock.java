package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface BasicBlock {

    AbstractInsnNode getFirstInsn();

    AbstractInsnNode getLastInsn();
}
