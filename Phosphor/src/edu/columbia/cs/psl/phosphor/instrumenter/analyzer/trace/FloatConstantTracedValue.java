package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.tree.AbstractInsnNode;

final class FloatConstantTracedValue extends ConstantTracedValue {

    private final float constant;

    FloatConstantTracedValue(int size, AbstractInsnNode insnSource, float constant) {
        super(size, insnSource);
        this.constant = constant;
    }

    public float getConstant() {
        return constant;
    }

    FloatConstantTracedValue negate(int size, AbstractInsnNode insnSource) {
        return new FloatConstantTracedValue(size, insnSource, -constant);
    }

    DoubleConstantTracedValue castToDouble(int size, AbstractInsnNode insnSource) {
        return new DoubleConstantTracedValue(size, insnSource, constant);
    }

    IntegerConstantTracedValue castToInt(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (int) constant);
    }

    LongConstantTracedValue castToLong(int size, AbstractInsnNode insnSource) {
        return new LongConstantTracedValue(size, insnSource, (long) constant);
    }

    FloatConstantTracedValue add(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, insnSource, constant + other.constant);
    }

    FloatConstantTracedValue subtract(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, insnSource, constant - other.constant);
    }

    FloatConstantTracedValue divide(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, insnSource, constant / other.constant);
    }

    FloatConstantTracedValue multiply(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, insnSource, constant * other.constant);
    }

    FloatConstantTracedValue remainder(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        return new FloatConstantTracedValue(size, insnSource, constant % other.constant);
    }

    FloatConstantTracedValue compareG(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        float result;
        if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
            result = 1;
        } else {
            result = Float.compare(constant, other.constant);
        }
        return new FloatConstantTracedValue(size, insnSource, result);
    }

    FloatConstantTracedValue compareL(int size, AbstractInsnNode insnSource, FloatConstantTracedValue other) {
        float result;
        if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
            result = -1;
        } else {
            result = Float.compare(constant, other.constant);
        }
        return new FloatConstantTracedValue(size, insnSource, result);
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
    TracedValue newInstance(int size, AbstractInsnNode insnSource) {
        return new FloatConstantTracedValue(size, insnSource, constant);
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
