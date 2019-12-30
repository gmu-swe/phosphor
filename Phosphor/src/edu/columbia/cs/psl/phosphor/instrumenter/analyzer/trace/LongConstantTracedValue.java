package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.tree.AbstractInsnNode;

final class LongConstantTracedValue extends ConstantTracedValue {

    private final long constant;

    LongConstantTracedValue(int size, AbstractInsnNode insnSource, long constant) {
        super(size, insnSource);
        this.constant = constant;
    }

    public long getConstant() {
        return constant;
    }

    LongConstantTracedValue negate(int size, AbstractInsnNode insnSource) {
        return new LongConstantTracedValue(size, insnSource, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, AbstractInsnNode insnSource) {
        return new FloatConstantTracedValue(size, insnSource, constant);
    }

    DoubleConstantTracedValue castToDouble(int size, AbstractInsnNode insnSource) {
        return new DoubleConstantTracedValue(size, insnSource, constant);
    }

    IntegerConstantTracedValue castToInt(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (int) constant);
    }

    LongConstantTracedValue add(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant + other.constant);
    }

    LongConstantTracedValue subtract(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant - other.constant);
    }

    LongConstantTracedValue divide(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant / other.constant);
    }

    LongConstantTracedValue multiply(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant * other.constant);
    }

    LongConstantTracedValue remainder(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant % other.constant);
    }

    LongConstantTracedValue shiftLeft(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant << other.constant);
    }

    LongConstantTracedValue shiftRight(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant >> other.constant);
    }

    LongConstantTracedValue shiftRightUnsigned(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant >>> other.constant);
    }

    LongConstantTracedValue bitwiseOr(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant | other.constant);
    }

    LongConstantTracedValue bitwiseAnd(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant & other.constant);
    }

    LongConstantTracedValue bitwiseXor(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, constant ^ other.constant);
    }

    LongConstantTracedValue compare(int size, AbstractInsnNode insnSource, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, insnSource, Long.compare(constant, other.constant));
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        if(other instanceof IntegerConstantTracedValue) {
            return constant == ((IntegerConstantTracedValue) other).getConstant();
        } else if(other instanceof LongConstantTracedValue) {
            return constant == ((LongConstantTracedValue) other).getConstant();
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode insnSource) {
        return new LongConstantTracedValue(size, insnSource, constant);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof LongConstantTracedValue) || !super.equals(o)) {
            return false;
        }
        LongConstantTracedValue that = (LongConstantTracedValue) o;
        return constant == that.constant;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (constant ^ (constant >>> 32));
        return result;
    }
}
