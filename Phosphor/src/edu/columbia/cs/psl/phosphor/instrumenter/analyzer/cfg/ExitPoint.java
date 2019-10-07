package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

/**
 * Represents the single exit point in a control flow graph.
 */
class ExitPoint extends ControlFlowNode {
    @Override
    public String toString() {
        return "ExitPoint";
    }
}
