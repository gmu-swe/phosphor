package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

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

public class RevisableBranchExclusionInterpreter extends Interpreter<RevisableBranchExclusionInterpreter.SourceValueWrapper> {

    private final SourceInterpreter interpreter;

    public RevisableBranchExclusionInterpreter() {
        super(Configuration.ASM_VERSION);
        interpreter = new SourceInterpreter();
    }

    @Override
    public SourceValueWrapper newValue(Type type) {
        SourceValue result = interpreter.newValue(type);
        return result == null ? null : new BasicSourceValueWrapper(result);
    }

    @Override
    public SourceValueWrapper newOperation(AbstractInsnNode insn) {
        SourceValue result = interpreter.newOperation(insn);
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                return new ObjectConstantSourceValueWrapper(result, null);
            case ICONST_M1:
                return new IntegerConstantSourceValueWrapper(result, -1);
            case ICONST_0:
                return new IntegerConstantSourceValueWrapper(result, 0);
            case ICONST_1:
                return new IntegerConstantSourceValueWrapper(result, 1);
            case ICONST_2:
                return new IntegerConstantSourceValueWrapper(result, 2);
            case ICONST_3:
                return new IntegerConstantSourceValueWrapper(result, 3);
            case ICONST_4:
                return new IntegerConstantSourceValueWrapper(result, 4);
            case ICONST_5:
                return new IntegerConstantSourceValueWrapper(result, 5);
            case DCONST_0:
                return new DoubleConstantSourceValueWrapper(result, 0);
            case DCONST_1:
                return new DoubleConstantSourceValueWrapper(result, 1);
            case FCONST_0:
                return new FloatConstantSourceValueWrapper(result, 0f);
            case FCONST_1:
                return new FloatConstantSourceValueWrapper(result, 1f);
            case FCONST_2:
                return new FloatConstantSourceValueWrapper(result, 2f);
            case LCONST_0:
                return new LongConstantSourceValueWrapper(result, 0L);
            case LCONST_1:
                return new LongConstantSourceValueWrapper(result, 1L);
            case BIPUSH:
            case SIPUSH:
                return new IntegerConstantSourceValueWrapper(result, ((IntInsnNode) insn).operand);
            case LDC:
                return new ObjectConstantSourceValueWrapper(result, ((LdcInsnNode) insn).cst);
            default:
                return new BasicSourceValueWrapper(result);
        }
    }

    @Override
    public SourceValueWrapper copyOperation(AbstractInsnNode insn, SourceValueWrapper value) {
        switch(insn.getOpcode()) {
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                SourceValue result = interpreter.copyOperation(insn, value.wrappedValue);
                return value.wrap(result);
            default:
                return value;
        }
    }

    @Override
    public SourceValueWrapper unaryOperation(AbstractInsnNode insn, SourceValueWrapper value) {
        SourceValue result = interpreter.unaryOperation(insn, value.wrappedValue);
        if(value instanceof IntegerConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case INEG:
                    return ((IntegerConstantSourceValueWrapper) value).negate(result);
                case IINC:
                    return ((IntegerConstantSourceValueWrapper) value).increment(result, ((IincInsnNode) insn).incr);
                case I2L:
                    return ((IntegerConstantSourceValueWrapper) value).castToLong(result);
                case I2F:
                    return ((IntegerConstantSourceValueWrapper) value).castToFloat(result);
                case I2D:
                    return ((IntegerConstantSourceValueWrapper) value).castToDouble(result);
                case I2B:
                    return ((IntegerConstantSourceValueWrapper) value).castToByte(result);
                case I2C:
                    return ((IntegerConstantSourceValueWrapper) value).castToChar(result);
                case I2S:
                    return ((IntegerConstantSourceValueWrapper) value).castToShort(result);
            }
        } else if(value instanceof LongConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case LNEG:
                    return ((LongConstantSourceValueWrapper) value).negate(result);
                case L2I:
                    return ((LongConstantSourceValueWrapper) value).castToInt(result);
                case L2F:
                    return ((LongConstantSourceValueWrapper) value).castToFloat(result);
                case L2D:
                    return ((LongConstantSourceValueWrapper) value).castToDouble(result);
            }
        } else if(value instanceof FloatConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case FNEG:
                    return ((FloatConstantSourceValueWrapper) value).negate(result);
                case F2I:
                    return ((FloatConstantSourceValueWrapper) value).castToInt(result);
                case F2L:
                    return ((FloatConstantSourceValueWrapper) value).castToLong(result);
                case F2D:
                    return ((FloatConstantSourceValueWrapper) value).castToDouble(result);
            }
        } else if(value instanceof DoubleConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case DNEG:
                    return ((DoubleConstantSourceValueWrapper) value).negate(result);
                case D2I:
                    return ((DoubleConstantSourceValueWrapper) value).castToInt(result);
                case D2L:
                    return ((DoubleConstantSourceValueWrapper) value).castToLong(result);
                case D2F:
                    return ((DoubleConstantSourceValueWrapper) value).castToFloat(result);
            }
        }
        return new BasicSourceValueWrapper(result);
    }

    @Override
    public SourceValueWrapper binaryOperation(AbstractInsnNode insn, SourceValueWrapper value1, SourceValueWrapper value2) {
        SourceValue result = interpreter.binaryOperation(insn, value1.wrappedValue, value2.wrappedValue);
        if(value1 instanceof IntegerConstantSourceValueWrapper && value2 instanceof IntegerConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case IADD:
                    return ((IntegerConstantSourceValueWrapper) value1).add(result, (IntegerConstantSourceValueWrapper) value2);
                case ISUB:
                    return ((IntegerConstantSourceValueWrapper) value1).subtract(result, (IntegerConstantSourceValueWrapper) value2);
                case IMUL:
                    return ((IntegerConstantSourceValueWrapper) value1).multiply(result, (IntegerConstantSourceValueWrapper) value2);
                case IDIV:
                    return ((IntegerConstantSourceValueWrapper) value1).divide(result, (IntegerConstantSourceValueWrapper) value2);
                case IREM:
                    return ((IntegerConstantSourceValueWrapper) value1).remainder(result, (IntegerConstantSourceValueWrapper) value2);
                case ISHL:
                    return ((IntegerConstantSourceValueWrapper) value1).shiftLeft(result, (IntegerConstantSourceValueWrapper) value2);
                case ISHR:
                    return ((IntegerConstantSourceValueWrapper) value1).shiftRight(result, (IntegerConstantSourceValueWrapper) value2);
                case IUSHR:
                    return ((IntegerConstantSourceValueWrapper) value1).shiftRightUnsigned(result, (IntegerConstantSourceValueWrapper) value2);
                case IAND:
                    return ((IntegerConstantSourceValueWrapper) value1).bitwiseAnd(result, (IntegerConstantSourceValueWrapper) value2);
                case IOR:
                    return ((IntegerConstantSourceValueWrapper) value1).bitwiseOr(result, (IntegerConstantSourceValueWrapper) value2);
                case IXOR:
                    return ((IntegerConstantSourceValueWrapper) value1).bitwiseXor(result, (IntegerConstantSourceValueWrapper) value2);
            }
        } else if(value1 instanceof LongConstantSourceValueWrapper && value2 instanceof LongConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case LADD:
                    return ((LongConstantSourceValueWrapper) value1).add(result, (LongConstantSourceValueWrapper) value2);
                case LSUB:
                    return ((LongConstantSourceValueWrapper) value1).subtract(result, (LongConstantSourceValueWrapper) value2);
                case LMUL:
                    return ((LongConstantSourceValueWrapper) value1).multiply(result, (LongConstantSourceValueWrapper) value2);
                case LDIV:
                    return ((LongConstantSourceValueWrapper) value1).divide(result, (LongConstantSourceValueWrapper) value2);
                case LREM:
                    return ((LongConstantSourceValueWrapper) value1).remainder(result, (LongConstantSourceValueWrapper) value2);
                case LSHL:
                    return ((LongConstantSourceValueWrapper) value1).shiftLeft(result, (LongConstantSourceValueWrapper) value2);
                case LSHR:
                    return ((LongConstantSourceValueWrapper) value1).shiftRight(result, (LongConstantSourceValueWrapper) value2);
                case LUSHR:
                    return ((LongConstantSourceValueWrapper) value1).shiftRightUnsigned(result, (LongConstantSourceValueWrapper) value2);
                case LAND:
                    return ((LongConstantSourceValueWrapper) value1).bitwiseAnd(result, (LongConstantSourceValueWrapper) value2);
                case LOR:
                    return ((LongConstantSourceValueWrapper) value1).bitwiseOr(result, (LongConstantSourceValueWrapper) value2);
                case LXOR:
                    return ((LongConstantSourceValueWrapper) value1).bitwiseXor(result, (LongConstantSourceValueWrapper) value2);
                case LCMP:
                    return ((LongConstantSourceValueWrapper) value1).compare(result, (LongConstantSourceValueWrapper) value2);
            }
        } else if(value1 instanceof FloatConstantSourceValueWrapper && value2 instanceof FloatConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case FADD:
                    return ((FloatConstantSourceValueWrapper) value1).add(result, (FloatConstantSourceValueWrapper) value2);
                case FSUB:
                    return ((FloatConstantSourceValueWrapper) value1).subtract(result, (FloatConstantSourceValueWrapper) value2);
                case FMUL:
                    return ((FloatConstantSourceValueWrapper) value1).multiply(result, (FloatConstantSourceValueWrapper) value2);
                case FDIV:
                    return ((FloatConstantSourceValueWrapper) value1).divide(result, (FloatConstantSourceValueWrapper) value2);
                case FREM:
                    return ((FloatConstantSourceValueWrapper) value1).remainder(result, (FloatConstantSourceValueWrapper) value2);
                case FCMPL:
                    return ((FloatConstantSourceValueWrapper) value1).compareL(result, (FloatConstantSourceValueWrapper) value2);
                case FCMPG:
                    return ((FloatConstantSourceValueWrapper) value1).compareG(result, (FloatConstantSourceValueWrapper) value2);
            }
        } else if(value1 instanceof DoubleConstantSourceValueWrapper && value2 instanceof DoubleConstantSourceValueWrapper) {
            switch(insn.getOpcode()) {
                case DADD:
                    return ((DoubleConstantSourceValueWrapper) value1).add(result, (DoubleConstantSourceValueWrapper) value2);
                case DSUB:
                    return ((DoubleConstantSourceValueWrapper) value1).subtract(result, (DoubleConstantSourceValueWrapper) value2);
                case DMUL:
                    return ((DoubleConstantSourceValueWrapper) value1).multiply(result, (DoubleConstantSourceValueWrapper) value2);
                case DDIV:
                    return ((DoubleConstantSourceValueWrapper) value1).divide(result, (DoubleConstantSourceValueWrapper) value2);
                case DREM:
                    return ((DoubleConstantSourceValueWrapper) value1).remainder(result, (DoubleConstantSourceValueWrapper) value2);
                case DCMPL:
                    return ((DoubleConstantSourceValueWrapper) value1).compareL(result, (DoubleConstantSourceValueWrapper) value2);
                case DCMPG:
                    return ((DoubleConstantSourceValueWrapper) value1).compareG(result, (DoubleConstantSourceValueWrapper) value2);
            }
        }
        return new BasicSourceValueWrapper(result);
    }

    @Override
    public SourceValueWrapper ternaryOperation(AbstractInsnNode insn, SourceValueWrapper value1, SourceValueWrapper value2, SourceValueWrapper value3) {
        SourceValue result = interpreter.ternaryOperation(insn, value1.wrappedValue, value2.wrappedValue, value3.wrappedValue);
        return result == null ? null : new BasicSourceValueWrapper(result);
    }

    @Override
    public SourceValueWrapper naryOperation(AbstractInsnNode insn, List<? extends SourceValueWrapper> values) {
        List<SourceValue> unwrappedValues = new ArrayList<>(values.size());
        for(SourceValueWrapper value : values) {
            unwrappedValues.add(value.wrappedValue);
        }
        SourceValue result = interpreter.naryOperation(insn, unwrappedValues);
        return result == null ? null : new BasicSourceValueWrapper(result);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, SourceValueWrapper value, SourceValueWrapper expected) {
        interpreter.returnOperation(insn, value.wrappedValue, expected.wrappedValue);
    }

    @Override
    public SourceValueWrapper merge(SourceValueWrapper value1, SourceValueWrapper value2) {
        if(value1 == value2) {
            return value1;
        }
        SourceValue merge = interpreter.merge(value1.wrappedValue, value2.wrappedValue);
        if(value1 instanceof ConstantSourceValueWrapper && value2 instanceof ConstantSourceValueWrapper) {
            if(((ConstantSourceValueWrapper) value1).canMerge((ConstantSourceValueWrapper) value2)) {
                return value1.wrap(merge);
            } else if(((ConstantSourceValueWrapper) value2).canMerge((ConstantSourceValueWrapper) value1)) {
                return value2.wrap(merge);
            }
        }
        return new BasicSourceValueWrapper(merge);
    }

    public static Set<AbstractInsnNode> identifyRevisableBranchExclusions(String owner, MethodNode methodNode) {
        RevisableBranchExclusionInterpreter interpreter = new RevisableBranchExclusionInterpreter();
        Analyzer<SourceValueWrapper> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(interpreter);
        try {
            Set<AbstractInsnNode> result = new HashSet<>();
            Frame<SourceValueWrapper>[] frames = analyzer.analyze(owner, methodNode);
            Iterator<AbstractInsnNode> itr = methodNode.instructions.iterator();
            for(Frame<SourceValueWrapper> frame : frames) {
                AbstractInsnNode insn = itr.next();
                if(frame != null && shouldExclude(insn, frame, methodNode.instructions, frames)) {
                    result.add(insn);
                }
            }
            return result;
        } catch(AnalyzerException e) {
            return Collections.emptySet();
        }
    }

    private static boolean shouldExclude(AbstractInsnNode insn, Frame<SourceValueWrapper> frame, InsnList insnList, Frame<SourceValueWrapper>[] allFrames) {
        switch(insn.getOpcode()) {
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case LCONST_0:
            case LCONST_1:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case DCONST_0:
            case DCONST_1:
            case BIPUSH:
            case SIPUSH:
            case LDC:
            case IINC:
                return true;
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                SourceValueWrapper top = frame.getStack(0);
                if(top instanceof ConstantSourceValueWrapper) {
                    return true;
                } else {
                    int var = ((VarInsnNode) insn).var;
                    java.util.Set<AbstractInsnNode> sources = top.wrappedValue.insns;
                    if(sources.size() == 1) {
                        AbstractInsnNode source = sources.iterator().next();
                        Frame<SourceValueWrapper> sourceFrame = getPreviousFrame(insn, insnList, allFrames);
                        return sourceFrame != null && checkSource(source, sourceFrame, var);
                    }
                }
            default:
                return false;
        }
    }

    private static boolean checkSource(AbstractInsnNode source, Frame<SourceValueWrapper> sourceFrame, int var) {
        switch(source.getOpcode()) {
            case IADD:
            case LADD:
            case FADD:
            case DADD:
            case ISUB:
            case LSUB:
            case FSUB:
            case DSUB:
            case IMUL:
            case LMUL:
            case FMUL:
            case DMUL:
            case IDIV:
            case LDIV:
            case FDIV:
            case DDIV:
            case IREM:
            case LREM:
            case FREM:
            case DREM:
            case ISHL:
            case LSHL:
            case ISHR:
            case LSHR:
            case IUSHR:
            case LUSHR:
            case IAND:
            case LAND:
            case IOR:
            case LOR:
            case IXOR:
            case LXOR:
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return checkStackValue(var, sourceFrame.getStack(0)) && checkStackValue(var, sourceFrame.getStack(1));
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
            case I2L:
            case I2F:
            case I2D:
            case L2I:
            case L2F:
            case L2D:
            case F2I:
            case F2L:
            case F2D:
            case D2I:
            case D2L:
            case D2F:
            case I2B:
            case I2C:
            case I2S:
                return checkStackValue(var, sourceFrame.getStack(0));
        }
        return false;
    }

    private static boolean checkStackValue(int var, SourceValueWrapper stackValue) {
        if(stackValue instanceof ConstantSourceValueWrapper) {
            return true;
        } else if(stackValue.wrappedValue.insns.size() == 1) {
            AbstractInsnNode insn = stackValue.wrappedValue.insns.iterator().next();
            switch(insn.getOpcode()) {
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                    return ((VarInsnNode) insn).var == var;
            }
        }
        return false;
    }

    private static Frame<SourceValueWrapper> getPreviousFrame(AbstractInsnNode insnNode, InsnList insnList, Frame<SourceValueWrapper>[] allFrames) {
        int index = insnList.indexOf(insnNode) - 1;
        if(index < 0 || index > allFrames.length) {
            return null;
        } else {
            return allFrames[index];
        }
    }

    public static abstract class SourceValueWrapper implements Value {
        SourceValue wrappedValue;

        SourceValueWrapper(SourceValue wrappedValue) {
            this.wrappedValue = wrappedValue;
        }

        @Override
        public int getSize() {
            return wrappedValue.getSize();
        }

        abstract SourceValueWrapper wrap(SourceValue result);
    }

    private static class BasicSourceValueWrapper extends SourceValueWrapper {

        BasicSourceValueWrapper(SourceValue wrappedValue) {
            super(wrappedValue);
        }

        @Override
        BasicSourceValueWrapper wrap(SourceValue value) {
            return new BasicSourceValueWrapper((value));
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof BasicSourceValueWrapper)) {
                return false;
            } else {
                return wrappedValue.equals(((SourceValueWrapper) o).wrappedValue);
            }
        }

        @Override
        public int hashCode() {
            return wrappedValue.hashCode();
        }

    }

    private static abstract class ConstantSourceValueWrapper extends SourceValueWrapper {
        ConstantSourceValueWrapper(SourceValue value) {
            super(value);
        }

        abstract boolean canMerge(ConstantSourceValueWrapper other);
    }

    private static class ObjectConstantSourceValueWrapper extends ConstantSourceValueWrapper {
        private final Object constant;

        ObjectConstantSourceValueWrapper(SourceValue value, Object constant) {
            super(value);
            this.constant = constant;
        }

        @Override
        ConstantSourceValueWrapper wrap(SourceValue value) {
            return new ObjectConstantSourceValueWrapper(value, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValueWrapper other) {
            return other instanceof ObjectConstantSourceValueWrapper && Objects.equals(constant, ((ObjectConstantSourceValueWrapper) other).constant);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof ObjectConstantSourceValueWrapper)) {
                return false;
            }
            if(!wrappedValue.equals(((SourceValueWrapper) o).wrappedValue)) {
                return false;
            }
            ObjectConstantSourceValueWrapper that = (ObjectConstantSourceValueWrapper) o;
            return constant != null ? constant.equals(that.constant) : that.constant == null;
        }

        @Override
        public int hashCode() {
            int result = wrappedValue.hashCode();
            result = 31 * result + (constant != null ? constant.hashCode() : 0);
            return result;
        }
    }

    private static class IntegerConstantSourceValueWrapper extends ConstantSourceValueWrapper {

        private final int constant;

        IntegerConstantSourceValueWrapper(SourceValue value, int constant) {
            super(value);
            this.constant = constant;
        }

        IntegerConstantSourceValueWrapper increment(SourceValue value, int amount) {
            return new IntegerConstantSourceValueWrapper(value, constant + amount);
        }

        IntegerConstantSourceValueWrapper negate(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, -constant);
        }

        FloatConstantSourceValueWrapper castToFloat(SourceValue value) {
            return new FloatConstantSourceValueWrapper(value, constant);
        }

        DoubleConstantSourceValueWrapper castToDouble(SourceValue value) {
            return new DoubleConstantSourceValueWrapper(value, constant);
        }

        LongConstantSourceValueWrapper castToLong(SourceValue value) {
            return new LongConstantSourceValueWrapper(value, constant);
        }

        IntegerConstantSourceValueWrapper castToByte(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (byte) constant);
        }

        IntegerConstantSourceValueWrapper castToShort(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (short) constant);
        }

        IntegerConstantSourceValueWrapper castToChar(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (char) constant);
        }

        IntegerConstantSourceValueWrapper add(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant + other.constant);
        }

        IntegerConstantSourceValueWrapper subtract(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant - other.constant);
        }

        IntegerConstantSourceValueWrapper divide(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant / other.constant);
        }

        IntegerConstantSourceValueWrapper multiply(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant * other.constant);
        }

        IntegerConstantSourceValueWrapper remainder(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant % other.constant);
        }

        IntegerConstantSourceValueWrapper shiftLeft(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant << other.constant);
        }

        IntegerConstantSourceValueWrapper shiftRight(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant >> other.constant);
        }

        IntegerConstantSourceValueWrapper shiftRightUnsigned(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant >>> other.constant);
        }

        IntegerConstantSourceValueWrapper bitwiseOr(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant | other.constant);
        }

        IntegerConstantSourceValueWrapper bitwiseAnd(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant & other.constant);
        }

        IntegerConstantSourceValueWrapper bitwiseXor(SourceValue value, IntegerConstantSourceValueWrapper other) {
            return new IntegerConstantSourceValueWrapper(value, constant ^ other.constant);
        }

        @Override
        IntegerConstantSourceValueWrapper wrap(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValueWrapper other) {
            if(other instanceof IntegerConstantSourceValueWrapper) {
                return constant == ((IntegerConstantSourceValueWrapper) other).constant;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof IntegerConstantSourceValueWrapper)) {
                return false;
            }
            if(!wrappedValue.equals(((SourceValueWrapper) o).wrappedValue)) {
                return false;
            }
            IntegerConstantSourceValueWrapper that = (IntegerConstantSourceValueWrapper) o;
            return constant == that.constant;
        }

        @Override
        public int hashCode() {
            int result = wrappedValue.hashCode();
            result = 31 * result + constant;
            return result;
        }
    }

    private static class LongConstantSourceValueWrapper extends ConstantSourceValueWrapper {
        private final long constant;

        LongConstantSourceValueWrapper(SourceValue value, long constant) {
            super(value);
            this.constant = constant;
        }

        LongConstantSourceValueWrapper negate(SourceValue value) {
            return new LongConstantSourceValueWrapper(value, -constant);
        }

        FloatConstantSourceValueWrapper castToFloat(SourceValue value) {
            return new FloatConstantSourceValueWrapper(value, constant);
        }

        DoubleConstantSourceValueWrapper castToDouble(SourceValue value) {
            return new DoubleConstantSourceValueWrapper(value, constant);
        }

        IntegerConstantSourceValueWrapper castToInt(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (int) constant);
        }

        LongConstantSourceValueWrapper add(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant + other.constant);
        }

        LongConstantSourceValueWrapper subtract(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant - other.constant);
        }

        LongConstantSourceValueWrapper divide(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant / other.constant);
        }

        LongConstantSourceValueWrapper multiply(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant * other.constant);
        }

        LongConstantSourceValueWrapper remainder(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant % other.constant);
        }

        LongConstantSourceValueWrapper shiftLeft(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant << other.constant);
        }

        LongConstantSourceValueWrapper shiftRight(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant >> other.constant);
        }

        LongConstantSourceValueWrapper shiftRightUnsigned(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant >>> other.constant);
        }

        LongConstantSourceValueWrapper bitwiseOr(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant | other.constant);
        }

        LongConstantSourceValueWrapper bitwiseAnd(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant & other.constant);
        }

        LongConstantSourceValueWrapper bitwiseXor(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, constant ^ other.constant);
        }

        LongConstantSourceValueWrapper compare(SourceValue value, LongConstantSourceValueWrapper other) {
            return new LongConstantSourceValueWrapper(value, Long.compare(constant, other.constant));
        }

        @Override
        LongConstantSourceValueWrapper wrap(SourceValue value) {
            return new LongConstantSourceValueWrapper(value, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValueWrapper other) {
            if(other instanceof IntegerConstantSourceValueWrapper) {
                return constant == ((IntegerConstantSourceValueWrapper) other).constant;
            } else if(other instanceof LongConstantSourceValueWrapper) {
                return constant == ((LongConstantSourceValueWrapper) other).constant;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof LongConstantSourceValueWrapper)) {
                return false;
            }
            if(!wrappedValue.equals(((SourceValueWrapper) o).wrappedValue)) {
                return false;
            }
            LongConstantSourceValueWrapper that = (LongConstantSourceValueWrapper) o;
            return constant == that.constant;
        }

        @Override
        public int hashCode() {
            int result = wrappedValue.hashCode();
            result = 31 * result + (int) (constant ^ (constant >>> 32));
            return result;
        }
    }

    private static class FloatConstantSourceValueWrapper extends ConstantSourceValueWrapper {
        private final float constant;

        FloatConstantSourceValueWrapper(SourceValue value, float constant) {
            super(value);
            this.constant = constant;
        }

        FloatConstantSourceValueWrapper negate(SourceValue value) {
            return new FloatConstantSourceValueWrapper(value, -constant);
        }

        DoubleConstantSourceValueWrapper castToDouble(SourceValue value) {
            return new DoubleConstantSourceValueWrapper(value, constant);
        }

        IntegerConstantSourceValueWrapper castToInt(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (int) constant);
        }

        LongConstantSourceValueWrapper castToLong(SourceValue value) {
            return new LongConstantSourceValueWrapper(value, (long) constant);
        }

        FloatConstantSourceValueWrapper add(SourceValue value, FloatConstantSourceValueWrapper other) {
            return new FloatConstantSourceValueWrapper(value, constant + other.constant);
        }

        FloatConstantSourceValueWrapper subtract(SourceValue value, FloatConstantSourceValueWrapper other) {
            return new FloatConstantSourceValueWrapper(value, constant - other.constant);
        }

        FloatConstantSourceValueWrapper divide(SourceValue value, FloatConstantSourceValueWrapper other) {
            return new FloatConstantSourceValueWrapper(value, constant / other.constant);
        }

        FloatConstantSourceValueWrapper multiply(SourceValue value, FloatConstantSourceValueWrapper other) {
            return new FloatConstantSourceValueWrapper(value, constant * other.constant);
        }

        FloatConstantSourceValueWrapper remainder(SourceValue value, FloatConstantSourceValueWrapper other) {
            return new FloatConstantSourceValueWrapper(value, constant % other.constant);
        }

        FloatConstantSourceValueWrapper compareG(SourceValue value, FloatConstantSourceValueWrapper other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValueWrapper(value, result);
        }

        FloatConstantSourceValueWrapper compareL(SourceValue value, FloatConstantSourceValueWrapper other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValueWrapper(value, result);
        }

        @Override
        FloatConstantSourceValueWrapper wrap(SourceValue value) {
            return new FloatConstantSourceValueWrapper(value, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValueWrapper other) {
            if(other instanceof IntegerConstantSourceValueWrapper) {
                return constant == ((IntegerConstantSourceValueWrapper) other).constant;
            } else if(other instanceof LongConstantSourceValueWrapper) {
                return constant == ((LongConstantSourceValueWrapper) other).constant;
            } else if(other instanceof FloatConstantSourceValueWrapper) {
                return constant == ((FloatConstantSourceValueWrapper) other).constant;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof FloatConstantSourceValueWrapper)) {
                return false;
            }
            if(!wrappedValue.equals(((SourceValueWrapper) o).wrappedValue)) {
                return false;
            }
            FloatConstantSourceValueWrapper that = (FloatConstantSourceValueWrapper) o;
            return Float.compare(that.constant, constant) == 0;
        }

        @Override
        public int hashCode() {
            int result = wrappedValue.hashCode();
            result = 31 * result + (constant != +0.0f ? Float.floatToIntBits(constant) : 0);
            return result;
        }
    }

    private static class DoubleConstantSourceValueWrapper extends ConstantSourceValueWrapper {
        private final double constant;

        DoubleConstantSourceValueWrapper(SourceValue value, double constant) {
            super(value);
            this.constant = constant;
        }

        DoubleConstantSourceValueWrapper negate(SourceValue value) {
            return new DoubleConstantSourceValueWrapper(value, -constant);
        }

        FloatConstantSourceValueWrapper castToFloat(SourceValue value) {
            return new FloatConstantSourceValueWrapper(value, (float) constant);
        }

        IntegerConstantSourceValueWrapper castToInt(SourceValue value) {
            return new IntegerConstantSourceValueWrapper(value, (int) constant);
        }

        LongConstantSourceValueWrapper castToLong(SourceValue value) {
            return new LongConstantSourceValueWrapper(value, (long) constant);
        }

        DoubleConstantSourceValueWrapper add(SourceValue value, DoubleConstantSourceValueWrapper other) {
            return new DoubleConstantSourceValueWrapper(value, constant + other.constant);
        }

        DoubleConstantSourceValueWrapper subtract(SourceValue value, DoubleConstantSourceValueWrapper other) {
            return new DoubleConstantSourceValueWrapper(value, constant - other.constant);
        }

        DoubleConstantSourceValueWrapper divide(SourceValue value, DoubleConstantSourceValueWrapper other) {
            return new DoubleConstantSourceValueWrapper(value, constant / other.constant);
        }

        DoubleConstantSourceValueWrapper multiply(SourceValue value, DoubleConstantSourceValueWrapper other) {
            return new DoubleConstantSourceValueWrapper(value, constant * other.constant);
        }

        DoubleConstantSourceValueWrapper remainder(SourceValue value, DoubleConstantSourceValueWrapper other) {
            return new DoubleConstantSourceValueWrapper(value, constant % other.constant);
        }

        DoubleConstantSourceValueWrapper compareG(SourceValue value, DoubleConstantSourceValueWrapper other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValueWrapper(value, result);
        }

        DoubleConstantSourceValueWrapper compareL(SourceValue value, DoubleConstantSourceValueWrapper other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValueWrapper(value, result);
        }

        @Override
        DoubleConstantSourceValueWrapper wrap(SourceValue value) {
            return new DoubleConstantSourceValueWrapper(value, constant);
        }

        @Override
        boolean canMerge(ConstantSourceValueWrapper other) {
            if(other instanceof IntegerConstantSourceValueWrapper) {
                return constant == ((IntegerConstantSourceValueWrapper) other).constant;
            } else if(other instanceof LongConstantSourceValueWrapper) {
                return constant == ((LongConstantSourceValueWrapper) other).constant;
            } else if(other instanceof FloatConstantSourceValueWrapper) {
                return constant == ((FloatConstantSourceValueWrapper) other).constant;
            } else if(other instanceof DoubleConstantSourceValueWrapper) {
                return constant == ((DoubleConstantSourceValueWrapper) other).constant;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof DoubleConstantSourceValueWrapper)) {
                return false;
            }
            if(!wrappedValue.equals(((SourceValueWrapper) o).wrappedValue)) {
                return false;
            }
            DoubleConstantSourceValueWrapper that = (DoubleConstantSourceValueWrapper) o;
            return Double.compare(that.constant, constant) == 0;
        }

        @Override
        public int hashCode() {
            int result = wrappedValue.hashCode();
            long temp;
            temp = Double.doubleToLongBits(constant);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
