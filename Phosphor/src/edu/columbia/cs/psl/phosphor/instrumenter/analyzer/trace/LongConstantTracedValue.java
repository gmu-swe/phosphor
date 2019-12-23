package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

final class LongConstantTracedValue extends ConstantTracedValue {

    private final long constant;

    LongConstantTracedValue(int size, Set<AbstractInsnNode> instructions, long constant) {
        super(size, instructions);
        this.constant = constant;
    }

    public long getConstant() {
        return constant;
    }

    LongConstantTracedValue negate(int size, Set<AbstractInsnNode> instructions) {
        return new LongConstantTracedValue(size, instructions, -constant);
    }

    FloatConstantTracedValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
        return new FloatConstantTracedValue(size, instructions, constant);
    }

    DoubleConstantTracedValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
        return new DoubleConstantTracedValue(size, instructions, constant);
    }

    IntegerConstantTracedValue castToInt(int size, Set<AbstractInsnNode> instructions) {
        return new IntegerConstantTracedValue(size, instructions, (int) constant);
    }

    LongConstantTracedValue add(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant + other.constant);
    }

    LongConstantTracedValue subtract(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant - other.constant);
    }

    LongConstantTracedValue divide(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant / other.constant);
    }

    LongConstantTracedValue multiply(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant * other.constant);
    }

    LongConstantTracedValue remainder(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant % other.constant);
    }

    LongConstantTracedValue shiftLeft(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant << other.constant);
    }

    LongConstantTracedValue shiftRight(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant >> other.constant);
    }

    LongConstantTracedValue shiftRightUnsigned(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant >>> other.constant);
    }

    LongConstantTracedValue bitwiseOr(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant | other.constant);
    }

    LongConstantTracedValue bitwiseAnd(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant & other.constant);
    }

    LongConstantTracedValue bitwiseXor(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, constant ^ other.constant);
    }

    LongConstantTracedValue compare(int size, Set<AbstractInsnNode> instructions, LongConstantTracedValue other) {
        return new LongConstantTracedValue(size, instructions, Long.compare(constant, other.constant));
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
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new LongConstantTracedValue(size, instructions, constant);
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
