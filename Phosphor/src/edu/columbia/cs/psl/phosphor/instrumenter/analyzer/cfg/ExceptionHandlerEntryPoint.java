package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Represents a point of entry into an exception handler's code.
 */
public class ExceptionHandlerEntryPoint extends EntryPoint {

    private final TryCatchBlockNode node;

    public ExceptionHandlerEntryPoint(TryCatchBlockNode node) {
        this.node = node;
    }

    LabelNode getHandlerStart() {
        return node.handler;
    }

    @Override
    public String toString() {
        return "ExceptionHandlerEntryPoint{node = " + node + "}";
    }
}
