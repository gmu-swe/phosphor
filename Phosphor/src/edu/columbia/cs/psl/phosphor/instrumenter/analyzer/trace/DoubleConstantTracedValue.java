package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class DoubleConstantTracedValue extends ConstantTracedValue {

    private final double constant;

    DoubleConstantTracedValue(int size, Set<AbstractInsnNode> instructions, double constant) {
        super(size, instructions);
        this.constant = constant;
    }

    public double getConstant() {
        return constant;
    }

    DoubleConstantTracedValue negate(int size, Set<AbstractInsnNode> instructions) {
        return new DoubleConstantTracedValue(size, instructions, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
        return new FloatConstantTracedValue(size, instructions, (float) constant);
    }

    IntegerConstantTracedValue castToInt(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (int) constant);
    }

    LongConstantTracedValue castToLong(int size, Set<AbstractInsnNode> instructions) {
        return new LongConstantTracedValue(size, instructions, (long) constant);
    }

    DoubleConstantTracedValue add(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, instructions, constant + other.constant);
    }

    DoubleConstantTracedValue subtract(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, instructions, constant - other.constant);
    }

    DoubleConstantTracedValue divide(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, instructions, constant / other.constant);
    }

    DoubleConstantTracedValue multiply(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, instructions, constant * other.constant);
    }

    DoubleConstantTracedValue remainder(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, instructions, constant % other.constant);
    }

    DoubleConstantTracedValue compareG(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        double result;
        if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
            result = 1;
        } else {
            result = Double.compare(constant, other.constant);
        }
        return new DoubleConstantTracedValue(size, instructions, result);
    }

    DoubleConstantTracedValue compareL(int size, Set<AbstractInsnNode> instructions, DoubleConstantTracedValue other) {
        double result;
        if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
            result = -1;
        } else {
            result = Double.compare(constant, other.constant);
        }
        return new DoubleConstantTracedValue(size, instructions, result);
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        if(other instanceof IntegerConstantTracedValue) {
            return constant == ((IntegerConstantTracedValue) other).getConstant();
        } else if(other instanceof LongConstantTracedValue) {
            return constant == ((LongConstantTracedValue) other).getConstant();
        } else if(other instanceof FloatConstantTracedValue) {
            return constant == ((FloatConstantTracedValue) other).getConstant();
        } else if(other instanceof DoubleConstantTracedValue) {
            return constant == ((DoubleConstantTracedValue) other).getConstant();
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new DoubleConstantTracedValue(size, instructions, constant);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof DoubleConstantTracedValue) || !super.equals(o)) {
            return false;
        }
        DoubleConstantTracedValue that = (DoubleConstantTracedValue) o;
        return Double.compare(that.constant, constant) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(constant);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
