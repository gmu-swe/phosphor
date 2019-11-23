package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Represents a value in the operand stack or local variable array of a frame in a method that cannot be determined
 * to be one constant value along all execution paths i.e., a non-constant value.
 */
public final class BasicTracedValue extends TracedValue {

    public BasicTracedValue(int size, Set<AbstractInsnNode> instructions) {
        super(size, instructions);
    }

    @Override
    public TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new BasicTracedValue(size, instructions);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof BasicTracedValue)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
