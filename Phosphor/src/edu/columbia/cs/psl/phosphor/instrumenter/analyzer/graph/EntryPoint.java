package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

/**
 * Represents the single point of entry into a control flow graph.
 */
class EntryPoint extends DummyBasicBlock {

    @Override
    public String toString() {
        return "EntryPoint";
    }
}
