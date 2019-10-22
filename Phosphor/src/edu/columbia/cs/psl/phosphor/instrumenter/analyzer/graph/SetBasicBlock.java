package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class SetBasicBlock extends SimpleBasicBlock{

    /**
     * Unmodifiable set containing all of the instructions in this block
     */
    private final Set<AbstractInsnNode> instructions;

    /**
     * Constructs a new basic block that represents the specified instruction sequence.
     *
     * @param instructions the sequence of instructions in the basic block being constructed
     * @param identifier   a number used to identify the basic block being constructed
     */
    SetBasicBlock(AbstractInsnNode[] instructions, int identifier) {
        super(instructions, identifier);
        HashSet<AbstractInsnNode> tempInstructions = new HashSet<>();
        for(AbstractInsnNode instruction : instructions) {
            tempInstructions.add(instruction);
        }
        this.instructions = Collections.unmodifiableSet(tempInstructions);
    }

    public boolean containsInstruction(AbstractInsnNode instruction) {
        return instructions.contains(instruction);
    }
}
