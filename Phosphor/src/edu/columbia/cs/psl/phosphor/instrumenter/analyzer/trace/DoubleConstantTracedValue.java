package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.tree.AbstractInsnNode;

final class DoubleConstantTracedValue extends ConstantTracedValue {

    private final double constant;

    DoubleConstantTracedValue(int size, AbstractInsnNode insnSource, double constant) {
        super(size, insnSource);
        this.constant = constant;
    }

    public double getConstant() {
        return constant;
    }

    DoubleConstantTracedValue negate(int size, AbstractInsnNode insnSource) {
        return new DoubleConstantTracedValue(size, insnSource, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, AbstractInsnNode insnSource) {
        return new FloatConstantTracedValue(size, insnSource, (float) constant);
    }

    IntegerConstantTracedValue castToInt(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (int) constant);
    }

    LongConstantTracedValue castToLong(int size, AbstractInsnNode insnSource) {
        return new LongConstantTracedValue(size, insnSource, (long) constant);
    }

    DoubleConstantTracedValue add(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, insnSource, constant + other.constant);
    }

    DoubleConstantTracedValue subtract(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, insnSource, constant - other.constant);
    }

    DoubleConstantTracedValue divide(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, insnSource, constant / other.constant);
    }

    DoubleConstantTracedValue multiply(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, insnSource, constant * other.constant);
    }

    DoubleConstantTracedValue remainder(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        return new DoubleConstantTracedValue(size, insnSource, constant % other.constant);
    }

    DoubleConstantTracedValue compareG(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        double result;
        if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
            result = 1;
        } else {
            result = Double.compare(constant, other.constant);
        }
        return new DoubleConstantTracedValue(size, insnSource, result);
    }

    DoubleConstantTracedValue compareL(int size, AbstractInsnNode insnSource, DoubleConstantTracedValue other) {
        double result;
        if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
            result = -1;
        } else {
            result = Double.compare(constant, other.constant);
        }
        return new DoubleConstantTracedValue(size, insnSource, result);
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
    TracedValue newInstance(int size, AbstractInsnNode insnSource) {
        return new DoubleConstantTracedValue(size, insnSource, constant);
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
