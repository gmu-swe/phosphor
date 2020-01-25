package edu.columbia.cs.psl.phosphor.control.binding.trace;

import edu.columbia.cs.psl.phosphor.struct.BitSet;
import org.objectweb.asm.tree.AbstractInsnNode;

final class DependentTracedValue extends TracedValue {

    BitSet dependencies;

    DependentTracedValue(int size, AbstractInsnNode sourceInsn, DependentTracedValue value1, DependentTracedValue value2) {
        super(size, sourceInsn);
        this.dependencies = BitSet.union(value1.dependencies, value2.dependencies);
    }

    DependentTracedValue(int size, AbstractInsnNode instruction, BitSet dependencies) {
        super(size, instruction);
        this.dependencies = dependencies.copy();
    }

    public BitSet getDependencies() {
        return dependencies;
    }

    @Override
    public TracedValue newInstance(int size, AbstractInsnNode instruction) {
        return new DependentTracedValue(size, instruction, dependencies);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof DependentTracedValue) || !super.equals(o)) {
            return false;
        }
        DependentTracedValue that = (DependentTracedValue) o;
        return dependencies != null ? dependencies.equals(that.dependencies) : that.dependencies == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
        return result;
    }
}
