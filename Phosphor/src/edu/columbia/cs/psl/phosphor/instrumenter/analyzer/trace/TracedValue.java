package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

/**
 * Represents a value in the operand stack or local variable array of a frame in a method.
 */
public abstract class TracedValue implements Value {

    /**
     * The size of this value i.e., 2 for {@code long} and {@code double} values and 1 otherwise
     */
    private final int size;

    /**
     * An unmodifiable set containing the instructions that can produce this value for some execution
     *
     * @see org.objectweb.asm.tree.analysis.SourceValue
     */
    private final Set<AbstractInsnNode> sources;

    public TracedValue(int size, Set<AbstractInsnNode> sources) {
        this.size = size;
        this.sources = Collections.unmodifiableSet(new HashSet<>(sources));
    }

    /**
     * @return the size of this value i.e., 2 for {@code long} and {@code double} values and 1 otherwise
     */
    @Override
    public int getSize() {
        return size;
    }

    /**
     * @return an unmodifiable set containing the instructions that can produce this value for some execution
     */
    public Set<AbstractInsnNode> getSources() {
        return sources;
    }

    /**
     * @param size the size of the copy
     * @param instructions the instructions of the copy
     * @return a copy of this value, but with the specified size and instructions
     */
    abstract TracedValue newInstance(int size, Set<AbstractInsnNode> instructions);

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof TracedValue)) {
            return false;
        }
        TracedValue that = (TracedValue) o;
        if(size != that.size) {
            return false;
        }
        return sources.equals(that.sources);
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + sources.hashCode();
        return result;
    }

}
