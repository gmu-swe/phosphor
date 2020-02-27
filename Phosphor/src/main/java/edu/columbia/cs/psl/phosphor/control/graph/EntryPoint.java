package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;

/**
 * Represents the single point of entry into a control flow graph.
 */
public class EntryPoint extends DummyBasicBlock {

    @Override
    public String toString() {
        return "EntryPoint";
    }

    @Override
    public String toDotString(Map<Label, String> labelNames) {
        return "ENTRY";
    }
}
