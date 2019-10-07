package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

/**
 * Represents the single point of entry into a control flow graph.
 */
class EntryPoint extends ControlFlowNode {
    @Override
    public String toString() {
        return "EntryPoint";
    }
}
