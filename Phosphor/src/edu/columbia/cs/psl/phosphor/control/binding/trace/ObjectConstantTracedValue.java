package edu.columbia.cs.psl.phosphor.control.binding.trace;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Objects;

final class ObjectConstantTracedValue extends ConstantTracedValue {

    private final Object constant;

    ObjectConstantTracedValue(int size, AbstractInsnNode insnSource, Object constant) {
        super(size, insnSource);
        this.constant = constant;
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        return other instanceof ObjectConstantTracedValue && Objects.equals(constant, ((ObjectConstantTracedValue) other).constant);
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode insnSource) {
        return new ObjectConstantTracedValue(size, insnSource, constant);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof ObjectConstantTracedValue) || !super.equals(o)) {
            return false;
        }
        ObjectConstantTracedValue that = (ObjectConstantTracedValue) o;
        return constant != null ? constant.equals(that.constant) : that.constant == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (constant != null ? constant.hashCode() : 0);
        return result;
    }
}
