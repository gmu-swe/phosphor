package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class FloatConstantTracedValue extends ConstantTracedValue {

    private final float constant;

    FloatConstantTracedValue(int size, Set<AbstractInsnNode> instructions, float constant) {
        super(size, instructions);
        this.constant = constant;
    }

    public float getConstant() {
        return constant;
    }

    FloatConstantTracedValue negate(int size, Set<AbstractInsnNode> instructions) {
        return new FloatConstantTracedValue(size, instructions, -constant);
    }

    DoubleConstantTracedValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
        return new DoubleConstantTracedValue(size, instructions, constant);
    }

    IntegerConstantTracedValue castToInt(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (int) constant);
    }

    LongConstantTracedValue castToLong(int size, Set<AbstractInsnNode> instructions) {
        return new LongConstantTracedValue(size, instructions, (long) constant);
    }

    FloatConstantTracedValue add(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, instructions, constant + other.constant);
    }

    FloatConstantTracedValue subtract(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, instructions, constant - other.constant);
    }

    FloatConstantTracedValue divide(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, instructions, constant / other.constant);
    }

    FloatConstantTracedValue multiply(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, instructions, constant * other.constant);
    }

    FloatConstantTracedValue remainder(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, instructions, constant % other.constant);
    }

    FloatConstantTracedValue compareG(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        float result;
        if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
            result = 1;
        } else {
            result = Float.compare(constant, other.constant);
        }
        return new FloatConstantTracedValue(size, instructions, result);
    }

    FloatConstantTracedValue compareL(int size, Set<AbstractInsnNode> instructions, FloatConstantTracedValue other) {
        float result;
        if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
            result = -1;
        } else {
            result = Float.compare(constant, other.constant);
        }
        return new FloatConstantTracedValue(size, instructions, result);
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        if(other instanceof IntegerConstantTracedValue) {
            return constant == ((IntegerConstantTracedValue) other).getConstant();
        } else if(other instanceof LongConstantTracedValue) {
            return constant == ((LongConstantTracedValue) other).getConstant();
        } else if(other instanceof FloatConstantTracedValue) {
            return constant == ((FloatConstantTracedValue) other).getConstant();
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new FloatConstantTracedValue(size, instructions, constant);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof FloatConstantTracedValue) || !super.equals(o)) {
            return false;
        }
        FloatConstantTracedValue that = (FloatConstantTracedValue) o;
        return Float.compare(that.constant, constant) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (constant != +0.0f ? Float.floatToIntBits(constant) : 0);
        return result;
    }
}
