package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.Objects;

/**
 * Identifies instructions that should be marked as being excluded from control propagation due to revisable branch
 * edges.
 * <p>
 * The following instruction are excluded:
 * <ul>
 *     <li>All ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0,
 *     FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH, and LDC instructions. </li>
 *     <li>All IINC instructions.</li>
 *     <li>Any ISTORE, LSTORE, FSTORE, DSTORE, and ASTORE instructions that store an int value v into variable x where
 *     v is of the form c, c OP x, x OP c, or OP x for some constant value c. </li>
 * </ul>
 */

public class RevisableBranchExclusionInterpreter extends SourceInterpreter {

    private final Set<AbstractInsnNode> exclusions = new HashSet<>();

    public RevisableBranchExclusionInterpreter() {
        super(Configuration.ASM_VERSION);
    }

    @Override
    public SourceValue newOperation(AbstractInsnNode insn) {
        SourceValue result = super.newOperation(insn);
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                // Null load does not need to be marked as an exclusion
                return new ObjectConstantSourceValue(result, null);
            case ICONST_M1:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, -1);
            case ICONST_0:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 0);
            case ICONST_1:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 1);
            case ICONST_2:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 2);
            case ICONST_3:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 3);
            case ICONST_4:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 4);
            case ICONST_5:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, 5);
            case DCONST_0:
                exclusions.add(insn);
                return new DoubleConstantSourceValue(result, 0);
            case DCONST_1:
                exclusions.add(insn);
                return new DoubleConstantSourceValue(result, 1);
            case FCONST_0:
                exclusions.add(insn);
                return new FloatConstantSourceValue(result, 0f);
            case FCONST_1:
                exclusions.add(insn);
                return new FloatConstantSourceValue(result, 1f);
            case FCONST_2:
                exclusions.add(insn);
                return new FloatConstantSourceValue(result, 2f);
            case LCONST_0:
                exclusions.add(insn);
                return new LongConstantSourceValue(result, 0L);
            case LCONST_1:
                exclusions.add(insn);
                return new LongConstantSourceValue(result, 1L);
            case BIPUSH:
            case SIPUSH:
                exclusions.add(insn);
                return new IntegerConstantSourceValue(result, ((IntInsnNode) insn).operand);
            case LDC:
                exclusions.add(insn);
                return new ObjectConstantSourceValue(result, ((LdcInsnNode) insn).cst);
            default:
                return result;
        }
    }

    @Override
    public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
        switch(insn.getOpcode()) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                if(insn instanceof VarInsnNode && isExcludedLocalVariableStore((VarInsnNode) insn, value)) {
                    exclusions.add(insn);
                }
        }
        SourceValue result = super.copyOperation(insn, value);
        if(value instanceof ConstantSourceValue) {
            return ((ConstantSourceValue) value).wrap(result);
        } else {
            return result;
        }
    }

    @Override
    public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
        SourceValue result = super.unaryOperation(insn, value);
        if(value instanceof IntegerConstantSourceValue) {
            switch(insn.getOpcode()) {
                case INEG:
                    return ((IntegerConstantSourceValue) value).negate(result);
                case IINC:
                    exclusions.add(insn);
                    return ((IntegerConstantSourceValue) value).increment(result, ((IincInsnNode) insn).incr);
                case I2L:
                    return ((IntegerConstantSourceValue) value).castToLong(result);
                case I2F:
                    return ((IntegerConstantSourceValue) value).castToFloat(result);
                case I2D:
                    return ((IntegerConstantSourceValue) value).castToDouble(result);
                case I2B:
                    return ((IntegerConstantSourceValue) value).castToByte(result);
                case I2C:
                    return ((IntegerConstantSourceValue) value).castToChar(result);
                case I2S:
                    return ((IntegerConstantSourceValue) value).castToShort(result);
            }
        } else if(value instanceof LongConstantSourceValue) {
            switch(insn.getOpcode()) {
                case LNEG:
                    return ((LongConstantSourceValue) value).negate(result);
                case L2I:
                    return ((LongConstantSourceValue) value).castToInt(result);
                case L2F:
                    return ((LongConstantSourceValue) value).castToFloat(result);
                case L2D:
                    return ((LongConstantSourceValue) value).castToDouble(result);
            }
        } else if(value instanceof FloatConstantSourceValue) {
            switch(insn.getOpcode()) {
                case FNEG:
                    return ((FloatConstantSourceValue) value).negate(result);
                case F2I:
                    return ((FloatConstantSourceValue) value).castToInt(result);
                case F2L:
                    return ((FloatConstantSourceValue) value).castToLong(result);
                case F2D:
                    return ((FloatConstantSourceValue) value).castToDouble(result);
            }
        } else if(value instanceof DoubleConstantSourceValue) {
            switch(insn.getOpcode()) {
                case DNEG:
                    return ((DoubleConstantSourceValue) value).negate(result);
                case D2I:
                    return ((DoubleConstantSourceValue) value).castToInt(result);
                case D2L:
                    return ((DoubleConstantSourceValue) value).castToLong(result);
                case D2F:
                    return ((DoubleConstantSourceValue) value).castToFloat(result);
            }
        }
        return result;
    }

    @Override
    public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2) {
        SourceValue result = super.binaryOperation(insn, value1, value2);
        if(value1 instanceof IntegerConstantSourceValue && value2 instanceof IntegerConstantSourceValue) {
            switch(insn.getOpcode()) {
                case IADD:
                    return ((IntegerConstantSourceValue) value1).add(result, (IntegerConstantSourceValue) value2);
                case ISUB:
                    return ((IntegerConstantSourceValue) value1).subtract(result, (IntegerConstantSourceValue) value2);
                case IMUL:
                    return ((IntegerConstantSourceValue) value1).multiply(result, (IntegerConstantSourceValue) value2);
                case IDIV:
                    return ((IntegerConstantSourceValue) value1).divide(result, (IntegerConstantSourceValue) value2);
                case IREM:
                    return ((IntegerConstantSourceValue) value1).remainder(result, (IntegerConstantSourceValue) value2);
                case ISHL:
                    return ((IntegerConstantSourceValue) value1).shiftLeft(result, (IntegerConstantSourceValue) value2);
                case ISHR:
                    return ((IntegerConstantSourceValue) value1).shiftRight(result, (IntegerConstantSourceValue) value2);
                case IUSHR:
                    return ((IntegerConstantSourceValue) value1).shiftRightUnsigned(result, (IntegerConstantSourceValue) value2);
                case IAND:
                    return ((IntegerConstantSourceValue) value1).bitwiseAnd(result, (IntegerConstantSourceValue) value2);
                case IOR:
                    return ((IntegerConstantSourceValue) value1).bitwiseOr(result, (IntegerConstantSourceValue) value2);
                case IXOR:
                    return ((IntegerConstantSourceValue) value1).bitwiseXor(result, (IntegerConstantSourceValue) value2);
            }
        } else if(value1 instanceof LongConstantSourceValue && value2 instanceof LongConstantSourceValue) {
            switch(insn.getOpcode()) {
                case LADD:
                    return ((LongConstantSourceValue) value1).add(result, (LongConstantSourceValue) value2);
                case LSUB:
                    return ((LongConstantSourceValue) value1).subtract(result, (LongConstantSourceValue) value2);
                case LMUL:
                    return ((LongConstantSourceValue) value1).multiply(result, (LongConstantSourceValue) value2);
                case LDIV:
                    return ((LongConstantSourceValue) value1).divide(result, (LongConstantSourceValue) value2);
                case LREM:
                    return ((LongConstantSourceValue) value1).remainder(result, (LongConstantSourceValue) value2);
                case LSHL:
                    return ((LongConstantSourceValue) value1).shiftLeft(result, (LongConstantSourceValue) value2);
                case LSHR:
                    return ((LongConstantSourceValue) value1).shiftRight(result, (LongConstantSourceValue) value2);
                case LUSHR:
                    return ((LongConstantSourceValue) value1).shiftRightUnsigned(result, (LongConstantSourceValue) value2);
                case LAND:
                    return ((LongConstantSourceValue) value1).bitwiseAnd(result, (LongConstantSourceValue) value2);
                case LOR:
                    return ((LongConstantSourceValue) value1).bitwiseOr(result, (LongConstantSourceValue) value2);
                case LXOR:
                    return ((LongConstantSourceValue) value1).bitwiseXor(result, (LongConstantSourceValue) value2);
                case LCMP:
                    return ((LongConstantSourceValue) value1).compare(result, (LongConstantSourceValue) value2);
            }
        } else if(value1 instanceof FloatConstantSourceValue && value2 instanceof FloatConstantSourceValue) {
            switch(insn.getOpcode()) {
                case FADD:
                    return ((FloatConstantSourceValue) value1).add(result, (FloatConstantSourceValue) value2);
                case FSUB:
                    return ((FloatConstantSourceValue) value1).subtract(result, (FloatConstantSourceValue) value2);
                case FMUL:
                    return ((FloatConstantSourceValue) value1).multiply(result, (FloatConstantSourceValue) value2);
                case FDIV:
                    return ((FloatConstantSourceValue) value1).divide(result, (FloatConstantSourceValue) value2);
                case FREM:
                    return ((FloatConstantSourceValue) value1).remainder(result, (FloatConstantSourceValue) value2);
                case FCMPL:
                    return ((FloatConstantSourceValue) value1).compareL(result, (FloatConstantSourceValue) value2);
                case FCMPG:
                    return ((FloatConstantSourceValue) value1).compareG(result, (FloatConstantSourceValue) value2);
            }
        } else if(value1 instanceof DoubleConstantSourceValue && value2 instanceof DoubleConstantSourceValue) {
            switch(insn.getOpcode()) {
                case DADD:
                    return ((DoubleConstantSourceValue) value1).add(result, (DoubleConstantSourceValue) value2);
                case DSUB:
                    return ((DoubleConstantSourceValue) value1).subtract(result, (DoubleConstantSourceValue) value2);
                case DMUL:
                    return ((DoubleConstantSourceValue) value1).multiply(result, (DoubleConstantSourceValue) value2);
                case DDIV:
                    return ((DoubleConstantSourceValue) value1).divide(result, (DoubleConstantSourceValue) value2);
                case DREM:
                    return ((DoubleConstantSourceValue) value1).remainder(result, (DoubleConstantSourceValue) value2);
                case DCMPL:
                    return ((DoubleConstantSourceValue) value1).compareL(result, (DoubleConstantSourceValue) value2);
                case DCMPG:
                    return ((DoubleConstantSourceValue) value1).compareG(result, (DoubleConstantSourceValue) value2);
            }
        }
        return result;
    }

    @Override
    public SourceValue merge(SourceValue value1, SourceValue value2) {
        if(value1 instanceof ConstantSourceValue && value2 instanceof ConstantSourceValue) {
            if(((ConstantSourceValue) value1).canMerge((ConstantSourceValue) value2)) {
                return value1;
            } else if(((ConstantSourceValue) value2).canMerge((ConstantSourceValue) value1)) {
                return value2;
            }
        }
        return super.merge(value1, value2);
    }

    public static Set<AbstractInsnNode> identifyRevisableBranchExclusions(String owner, MethodNode methodNode) {
        RevisableBranchExclusionInterpreter interpreter = new RevisableBranchExclusionInterpreter();
        Analyzer<SourceValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(interpreter);
        try {
            analyzer.analyze(owner, methodNode);
            return interpreter.exclusions;
        } catch(AnalyzerException e) {
            return Collections.emptySet();
        }
    }

    private static boolean isExcludedLocalVariableStore(VarInsnNode insn, SourceValue value) {
        if(value instanceof ConstantSourceValue) {
            return true; // form x = c;
        } else {
            return false;
        }
    }

    private static abstract class ConstantSourceValue extends SourceValue {
        ConstantSourceValue(SourceValue value) {
            super(value.size, value.insns);
        }

        abstract ConstantSourceValue wrap(SourceValue result);

        abstract boolean canMerge(ConstantSourceValue other);
    }

    private static class ObjectConstantSourceValue extends ConstantSourceValue {
        private final Object constant;

        ObjectConstantSourceValue(SourceValue value, Object constant) {
            super(value);
            this.constant = constant;
        }

        @Override
        ConstantSourceValue wrap(SourceValue result) {
            return new ObjectConstantSourceValue(result, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            return other instanceof ObjectConstantSourceValue && Objects.equals(constant, ((ObjectConstantSourceValue) other).constant);
        }
    }

    private static class IntegerConstantSourceValue extends ConstantSourceValue {

        private final int constant;

        IntegerConstantSourceValue(SourceValue value, int constant) {
            super(value);
            this.constant = constant;
        }

        IntegerConstantSourceValue increment(SourceValue value, int amount) {
            return new IntegerConstantSourceValue(value, constant + amount);
        }

        IntegerConstantSourceValue negate(SourceValue value) {
            return new IntegerConstantSourceValue(value, -constant);
        }

        FloatConstantSourceValue castToFloat(SourceValue value) {
            return new FloatConstantSourceValue(value, constant);
        }

        DoubleConstantSourceValue castToDouble(SourceValue value) {
            return new DoubleConstantSourceValue(value, constant);
        }

        LongConstantSourceValue castToLong(SourceValue value) {
            return new LongConstantSourceValue(value, constant);
        }

        IntegerConstantSourceValue castToByte(SourceValue value) {
            return new IntegerConstantSourceValue(value, (byte) constant);
        }

        IntegerConstantSourceValue castToShort(SourceValue value) {
            return new IntegerConstantSourceValue(value, (short) constant);
        }

        IntegerConstantSourceValue castToChar(SourceValue value) {
            return new IntegerConstantSourceValue(value, (char) constant);
        }

        IntegerConstantSourceValue add(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant + other.constant);
        }

        IntegerConstantSourceValue subtract(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant - other.constant);
        }

        IntegerConstantSourceValue divide(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant / other.constant);
        }

        IntegerConstantSourceValue multiply(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant * other.constant);
        }

        IntegerConstantSourceValue remainder(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant % other.constant);
        }

        IntegerConstantSourceValue shiftLeft(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant << other.constant);
        }

        IntegerConstantSourceValue shiftRight(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant >> other.constant);
        }

        IntegerConstantSourceValue shiftRightUnsigned(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant >>> other.constant);
        }

        IntegerConstantSourceValue bitwiseOr(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant | other.constant);
        }

        IntegerConstantSourceValue bitwiseAnd(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant & other.constant);
        }

        IntegerConstantSourceValue bitwiseXor(SourceValue value, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(value, constant ^ other.constant);
        }

        @Override
        IntegerConstantSourceValue wrap(SourceValue result) {
            return new IntegerConstantSourceValue(result, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            if(other instanceof IntegerConstantSourceValue) {
                return constant == ((IntegerConstantSourceValue) other).constant;
            }
            return false;
        }
    }

    private static class LongConstantSourceValue extends ConstantSourceValue {
        private final long constant;

        LongConstantSourceValue(SourceValue value, long constant) {
            super(value);
            this.constant = constant;
        }

        LongConstantSourceValue negate(SourceValue value) {
            return new LongConstantSourceValue(value, -constant);
        }

        FloatConstantSourceValue castToFloat(SourceValue value) {
            return new FloatConstantSourceValue(value, constant);
        }

        DoubleConstantSourceValue castToDouble(SourceValue value) {
            return new DoubleConstantSourceValue(value, constant);
        }

        IntegerConstantSourceValue castToInt(SourceValue value) {
            return new IntegerConstantSourceValue(value, (int) constant);
        }

        LongConstantSourceValue add(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant + other.constant);
        }

        LongConstantSourceValue subtract(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant - other.constant);
        }

        LongConstantSourceValue divide(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant / other.constant);
        }

        LongConstantSourceValue multiply(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant * other.constant);
        }

        LongConstantSourceValue remainder(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant % other.constant);
        }

        LongConstantSourceValue shiftLeft(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant << other.constant);
        }

        LongConstantSourceValue shiftRight(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant >> other.constant);
        }

        LongConstantSourceValue shiftRightUnsigned(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant >>> other.constant);
        }

        LongConstantSourceValue bitwiseOr(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant | other.constant);
        }

        LongConstantSourceValue bitwiseAnd(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant & other.constant);
        }

        LongConstantSourceValue bitwiseXor(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, constant ^ other.constant);
        }

        LongConstantSourceValue compare(SourceValue value, LongConstantSourceValue other) {
            return new LongConstantSourceValue(value, Long.compare(constant, other.constant));
        }

        @Override
        LongConstantSourceValue wrap(SourceValue result) {
            return new LongConstantSourceValue(result, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            if(other instanceof IntegerConstantSourceValue) {
                return constant == ((IntegerConstantSourceValue) other).constant;
            } else if(other instanceof LongConstantSourceValue) {
                return constant == ((LongConstantSourceValue) other).constant;
            }
            return false;
        }
    }

    private static class FloatConstantSourceValue extends ConstantSourceValue {
        private final float constant;

        FloatConstantSourceValue(SourceValue value, float constant) {
            super(value);
            this.constant = constant;
        }

        FloatConstantSourceValue negate(SourceValue value) {
            return new FloatConstantSourceValue(value, -constant);
        }

        DoubleConstantSourceValue castToDouble(SourceValue value) {
            return new DoubleConstantSourceValue(value, constant);
        }

        IntegerConstantSourceValue castToInt(SourceValue value) {
            return new IntegerConstantSourceValue(value, (int) constant);
        }

        LongConstantSourceValue castToLong(SourceValue value) {
            return new LongConstantSourceValue(value, (long) constant);
        }

        FloatConstantSourceValue add(SourceValue value, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(value, constant + other.constant);
        }

        FloatConstantSourceValue subtract(SourceValue value, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(value, constant - other.constant);
        }

        FloatConstantSourceValue divide(SourceValue value, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(value, constant / other.constant);
        }

        FloatConstantSourceValue multiply(SourceValue value, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(value, constant * other.constant);
        }

        FloatConstantSourceValue remainder(SourceValue value, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(value, constant % other.constant);
        }


        FloatConstantSourceValue compareG(SourceValue value, FloatConstantSourceValue other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValue(value, result);
        }

        FloatConstantSourceValue compareL(SourceValue value, FloatConstantSourceValue other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValue(value, result);
        }

        @Override
        FloatConstantSourceValue wrap(SourceValue result) {
            return new FloatConstantSourceValue(result, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            if(other instanceof IntegerConstantSourceValue) {
                return constant == ((IntegerConstantSourceValue) other).constant;
            } else if(other instanceof LongConstantSourceValue) {
                return constant == ((LongConstantSourceValue) other).constant;
            } else if(other instanceof FloatConstantSourceValue) {
                return constant == ((FloatConstantSourceValue) other).constant;
            }
            return false;
        }
    }

    private static class DoubleConstantSourceValue extends ConstantSourceValue {
        private final double constant;

        DoubleConstantSourceValue(SourceValue value, double constant) {
            super(value);
            this.constant = constant;
        }

        DoubleConstantSourceValue negate(SourceValue value) {
            return new DoubleConstantSourceValue(value, -constant);
        }

        FloatConstantSourceValue castToFloat(SourceValue value) {
            return new FloatConstantSourceValue(value, (float) constant);
        }

        IntegerConstantSourceValue castToInt(SourceValue value) {
            return new IntegerConstantSourceValue(value, (int) constant);
        }

        LongConstantSourceValue castToLong(SourceValue value) {
            return new LongConstantSourceValue(value, (long) constant);
        }

        DoubleConstantSourceValue add(SourceValue value, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(value, constant + other.constant);
        }

        DoubleConstantSourceValue subtract(SourceValue value, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(value, constant - other.constant);
        }

        DoubleConstantSourceValue divide(SourceValue value, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(value, constant / other.constant);
        }

        DoubleConstantSourceValue multiply(SourceValue value, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(value, constant * other.constant);
        }

        DoubleConstantSourceValue remainder(SourceValue value, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(value, constant % other.constant);
        }

        DoubleConstantSourceValue compareG(SourceValue value, DoubleConstantSourceValue other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValue(value, result);
        }

        DoubleConstantSourceValue compareL(SourceValue value, DoubleConstantSourceValue other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValue(value, result);
        }

        @Override
        DoubleConstantSourceValue wrap(SourceValue result) {
            return new DoubleConstantSourceValue(result, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            if(other instanceof IntegerConstantSourceValue) {
                return constant == ((IntegerConstantSourceValue) other).constant;
            } else if(other instanceof LongConstantSourceValue) {
                return constant == ((LongConstantSourceValue) other).constant;
            } else if(other instanceof FloatConstantSourceValue) {
                return constant == ((FloatConstantSourceValue) other).constant;
            } else if(other instanceof DoubleConstantSourceValue) {
                return constant == ((DoubleConstantSourceValue) other).constant;
            }
            return false;
        }
    }
}
