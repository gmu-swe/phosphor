package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;

/**
 * Record type that stores information about the values used and produced by an instruction.
 */
public class InstructionEffect {

    /**
     * The values used by the instruction, non-null
     */
    final TracedValue[] sources;

    /**
     * The value produced by the instruction, may be null
     */
    final TracedValue product;

    InstructionEffect(TracedValue[] sources, TracedValue product) {
        this.sources = sources;
        this.product = product;
    }

    InstructionEffect(TracedValue source, TracedValue product) {
        this.sources = new TracedValue[1];
        this.sources[0] = source;
        this.product = product;
    }

    InstructionEffect(TracedValue product) {
        this.sources = new TracedValue[0];
        this.product = product;
    }

    @Override
    public String toString() {
        return "Sources: " + Arrays.toString(sources) + " | Product: " + product;
    }
}
