package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Represents a value in the operand stack or local variable array of a frame in a method that has been determined
 * to be equal to exactly one constant value along all execution paths.
 */
abstract class ConstantTracedValue extends TracedValue {

    ConstantTracedValue(int size, Set<AbstractInsnNode> instructions) {
        super(size, instructions);
    }

    /**
     * @param other the value whose constant is to be compared to this value's constant
     * @return true if the specified other value's constant is equal to this value's constant
     */
    abstract boolean canMerge(ConstantTracedValue other);
}
