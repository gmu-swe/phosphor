package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.tree.AbstractInsnNode;

final class IntegerConstantTracedValue extends ConstantTracedValue {

    private final int constant;

    IntegerConstantTracedValue(int size, AbstractInsnNode insnSource, int constant) {
        super(size, insnSource);
        this.constant = constant;
    }

    public int getConstant() {
        return constant;
    }

    IntegerConstantTracedValue increment(int size, AbstractInsnNode insnSource, int amount) {
        return new IntegerConstantTracedValue(size, insnSource, constant + amount);
    }

    IntegerConstantTracedValue negate(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, AbstractInsnNode insnSource) {
        return new FloatConstantTracedValue(size, insnSource, constant);
    }

    DoubleConstantTracedValue castToDouble(int size, AbstractInsnNode insnSource) {
        return new DoubleConstantTracedValue(size, insnSource, constant);
    }

    LongConstantTracedValue castToLong(int size, AbstractInsnNode insnSource) {
        return new LongConstantTracedValue(size, insnSource, constant);
    }

    IntegerConstantTracedValue castToByte(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (byte) constant);
    }

    IntegerConstantTracedValue castToShort(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (short) constant);
    }

    IntegerConstantTracedValue castToChar(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, (char) constant);
    }

    IntegerConstantTracedValue add(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant + other.constant);
    }

    IntegerConstantTracedValue subtract(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant - other.constant);
    }

    IntegerConstantTracedValue divide(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant / other.constant);
    }

    IntegerConstantTracedValue multiply(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant * other.constant);
    }

    IntegerConstantTracedValue remainder(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant % other.constant);
    }

    IntegerConstantTracedValue shiftLeft(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant << other.constant);
    }

    IntegerConstantTracedValue shiftRight(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant >> other.constant);
    }

    IntegerConstantTracedValue shiftRightUnsigned(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant >>> other.constant);
    }

    IntegerConstantTracedValue bitwiseOr(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant | other.constant);
    }

    IntegerConstantTracedValue bitwiseAnd(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant & other.constant);
    }

    IntegerConstantTracedValue bitwiseXor(int size, AbstractInsnNode insnSource, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, insnSource, constant ^ other.constant);
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        if(other instanceof IntegerConstantTracedValue) {
            return constant == ((IntegerConstantTracedValue) other).constant;
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode insnSource) {
        return new IntegerConstantTracedValue(size, insnSource, constant);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof IntegerConstantTracedValue) || !super.equals(o)) {
            return false;
        }
        IntegerConstantTracedValue that = (IntegerConstantTracedValue) o;
        return constant == that.constant;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + constant;
        return result;
    }
}
