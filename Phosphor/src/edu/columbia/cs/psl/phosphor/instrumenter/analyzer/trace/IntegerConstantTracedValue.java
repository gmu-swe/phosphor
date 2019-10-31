package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class IntegerConstantTracedValue extends ConstantTracedValue {

    private final int constant;

    IntegerConstantTracedValue(int size, Set<AbstractInsnNode> instructions, int constant) {
        super(size, instructions);
        this.constant = constant;
    }

    public int getConstant() {
        return constant;
    }

    IntegerConstantTracedValue increment(int size, Set<AbstractInsnNode> instructions, int amount) {
        return new IntegerConstantTracedValue(size, instructions, constant + amount);
    }

    IntegerConstantTracedValue negate(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
        return new FloatConstantTracedValue(size, instructions, constant);
    }

    DoubleConstantTracedValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
        return new DoubleConstantTracedValue(size, instructions, constant);
    }

    LongConstantTracedValue castToLong(int size, Set<AbstractInsnNode> instructions) {
        return new LongConstantTracedValue(size, instructions, constant);
    }

    IntegerConstantTracedValue castToByte(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (byte) constant);
    }

    IntegerConstantTracedValue castToShort(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (short) constant);
    }

    IntegerConstantTracedValue castToChar(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (char) constant);
    }

    IntegerConstantTracedValue add(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant + other.constant);
    }

    IntegerConstantTracedValue subtract(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant - other.constant);
    }

    IntegerConstantTracedValue divide(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant / other.constant);
    }

    IntegerConstantTracedValue multiply(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant * other.constant);
    }

    IntegerConstantTracedValue remainder(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant % other.constant);
    }

    IntegerConstantTracedValue shiftLeft(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant << other.constant);
    }

    IntegerConstantTracedValue shiftRight(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant >> other.constant);
    }

    IntegerConstantTracedValue shiftRightUnsigned(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant >>> other.constant);
    }

    IntegerConstantTracedValue bitwiseOr(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant | other.constant);
    }

    IntegerConstantTracedValue bitwiseAnd(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant & other.constant);
    }

    IntegerConstantTracedValue bitwiseXor(int size, Set<AbstractInsnNode> instructions, IntegerConstantTracedValue other) {
        return new IntegerConstantTracedValue(size, instructions, constant ^ other.constant);
    }

    @Override
    boolean canMerge(ConstantTracedValue other) {
        if(other instanceof IntegerConstantTracedValue) {
            return constant == ((IntegerConstantTracedValue) other).constant;
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, constant);
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
