package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

/**
 * Represents the single point of exit in a control flow graph.
 */
class ExitPoint extends DummyBasicBlock{

    @Override
    public String toString() {
        return "ExitPoint";
    }
}
