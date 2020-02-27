package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;

/**
 * Represents the single point of exit in a control flow graph.
 */
public class ExitPoint extends DummyBasicBlock {

    @Override
    public String toString() {
        return "ExitPoint";
    }

    @Override
    public String toDotString(Map<Label, String> labelNames) {
        return "EXIT";
    }
}
