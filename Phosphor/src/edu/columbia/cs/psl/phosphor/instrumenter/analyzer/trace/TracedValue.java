package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

/**
 * Represents a value in the operand stack or local variable array of a frame in a method.
 */
abstract class TracedValue implements Value {

    /**
     * The size of this value i.e., 2 for {@code long} and {@code double} values and 1 otherwise
     */
    private final int size;

    /**
     * The instruction that produces this value or null if multiple instructions can produce this value or the value
     * is from a constant or argument value
     *
     */
    private final AbstractInsnNode sourceInsn;

    public TracedValue(int size, AbstractInsnNode sourceInsn) {
        this.size = size;
        this.sourceInsn = sourceInsn;
    }

    /**
     * @return the size of this value i.e., 2 for {@code long} and {@code double} values and 1 otherwise
     */
    @Override
    public int getSize() {
        return size;
    }

    /**
     * @return the instruction that produces this value
     */
    public AbstractInsnNode getInsnSource() {
        return sourceInsn;
    }

    /**
     * @param size the size of the copy
     * @param sourceInsn the source instruction of the copy
     * @return a copy of this value, but with the specified size and source instruction
     */
    abstract TracedValue newInstance(int size, AbstractInsnNode sourceInsn);

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
        return sourceInsn != null ? sourceInsn.equals(that.sourceInsn) : that.sourceInsn == null;
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + (sourceInsn != null ? sourceInsn.hashCode() : 0);
        return result;
    }
}
