package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

public class RevisionExclusionInterpreter extends Interpreter<RevisionExclusionInterpreter.SourceValue> {

    public RevisionExclusionInterpreter() {
        super(Configuration.ASM_VERSION);
    }

    @Override
    public SourceValue newValue(Type type) {
        if (type == Type.VOID_TYPE) {
            return null;
        }
        return new BasicSourceValue(type == null ? 1 : type.getSize(), new HashSet<>());
    }

    @Override
    public SourceValue newOperation(AbstractInsnNode insn) {
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                return new ObjectConstantSourceValue(size, set, null);
            case ICONST_M1:
                return new IntegerConstantSourceValue(size, set, -1);
            case ICONST_0:
                return new IntegerConstantSourceValue(size, set, 0);
            case ICONST_1:
                return new IntegerConstantSourceValue(size, set, 1);
            case ICONST_2:
                return new IntegerConstantSourceValue(size, set, 2);
            case ICONST_3:
                return new IntegerConstantSourceValue(size, set, 3);
            case ICONST_4:
                return new IntegerConstantSourceValue(size, set, 4);
            case ICONST_5:
                return new IntegerConstantSourceValue(size, set, 5);
            case DCONST_0:
                return new DoubleConstantSourceValue(size, set, 0);
            case DCONST_1:
                return new DoubleConstantSourceValue(size, set, 1);
            case FCONST_0:
                return new FloatConstantSourceValue(size, set, 0f);
            case FCONST_1:
                return new FloatConstantSourceValue(size, set, 1f);
            case FCONST_2:
                return new FloatConstantSourceValue(size, set, 2f);
            case LCONST_0:
                return new LongConstantSourceValue(size, set, 0L);
            case LCONST_1:
                return new LongConstantSourceValue(size, set, 1L);
            case BIPUSH:
            case SIPUSH:
                return new IntegerConstantSourceValue(size, set, ((IntInsnNode) insn).operand);
            case LDC:
                return new ObjectConstantSourceValue(size, set, ((LdcInsnNode) insn).cst);
            default:
                return new BasicSourceValue(size, set);
        }
    }

    @Override
    public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
        int size = value.size;
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
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
                return value.makeNew(size, set);
            default:
                return value;
        }
    }

    @Override
    public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        if(value instanceof IntegerConstantSourceValue) {
            switch(insn.getOpcode()) {
                case INEG:
                    return ((IntegerConstantSourceValue) value).negate(size, set);
                case IINC:
                    return ((IntegerConstantSourceValue) value).increment(size, set, ((IincInsnNode) insn).incr);
                case I2L:
                    return ((IntegerConstantSourceValue) value).castToLong(size, set);
                case I2F:
                    return ((IntegerConstantSourceValue) value).castToFloat(size, set);
                case I2D:
                    return ((IntegerConstantSourceValue) value).castToDouble(size, set);
                case I2B:
                    return ((IntegerConstantSourceValue) value).castToByte(size, set);
                case I2C:
                    return ((IntegerConstantSourceValue) value).castToChar(size, set);
                case I2S:
                    return ((IntegerConstantSourceValue) value).castToShort(size, set);
            }
        } else if(value instanceof LongConstantSourceValue) {
            switch(insn.getOpcode()) {
                case LNEG:
                    return ((LongConstantSourceValue) value).negate(size, set);
                case L2I:
                    return ((LongConstantSourceValue) value).castToInt(size, set);
                case L2F:
                    return ((LongConstantSourceValue) value).castToFloat(size, set);
                case L2D:
                    return ((LongConstantSourceValue) value).castToDouble(size, set);
            }
        } else if(value instanceof FloatConstantSourceValue) {
            switch(insn.getOpcode()) {
                case FNEG:
                    return ((FloatConstantSourceValue) value).negate(size, set);
                case F2I:
                    return ((FloatConstantSourceValue) value).castToInt(size, set);
                case F2L:
                    return ((FloatConstantSourceValue) value).castToLong(size, set);
                case F2D:
                    return ((FloatConstantSourceValue) value).castToDouble(size, set);
            }
        } else if(value instanceof DoubleConstantSourceValue) {
            switch(insn.getOpcode()) {
                case DNEG:
                    return ((DoubleConstantSourceValue) value).negate(size, set);
                case D2I:
                    return ((DoubleConstantSourceValue) value).castToInt(size, set);
                case D2L:
                    return ((DoubleConstantSourceValue) value).castToLong(size, set);
                case D2F:
                    return ((DoubleConstantSourceValue) value).castToFloat(size, set);
            }
        }
        return new BasicSourceValue(size, set);
    }

    @Override
    public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2) {
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        if(value1 instanceof IntegerConstantSourceValue && value2 instanceof IntegerConstantSourceValue) {
            switch(insn.getOpcode()) {
                case IADD:
                    return ((IntegerConstantSourceValue) value1).add(size, set, (IntegerConstantSourceValue) value2);
                case ISUB:
                    return ((IntegerConstantSourceValue) value1).subtract(size, set, (IntegerConstantSourceValue) value2);
                case IMUL:
                    return ((IntegerConstantSourceValue) value1).multiply(size, set, (IntegerConstantSourceValue) value2);
                case IDIV:
                    return ((IntegerConstantSourceValue) value1).divide(size, set, (IntegerConstantSourceValue) value2);
                case IREM:
                    return ((IntegerConstantSourceValue) value1).remainder(size, set, (IntegerConstantSourceValue) value2);
                case ISHL:
                    return ((IntegerConstantSourceValue) value1).shiftLeft(size, set, (IntegerConstantSourceValue) value2);
                case ISHR:
                    return ((IntegerConstantSourceValue) value1).shiftRight(size, set, (IntegerConstantSourceValue) value2);
                case IUSHR:
                    return ((IntegerConstantSourceValue) value1).shiftRightUnsigned(size, set, (IntegerConstantSourceValue) value2);
                case IAND:
                    return ((IntegerConstantSourceValue) value1).bitwiseAnd(size, set, (IntegerConstantSourceValue) value2);
                case IOR:
                    return ((IntegerConstantSourceValue) value1).bitwiseOr(size, set, (IntegerConstantSourceValue) value2);
                case IXOR:
                    return ((IntegerConstantSourceValue) value1).bitwiseXor(size, set, (IntegerConstantSourceValue) value2);
            }
        } else if(value1 instanceof LongConstantSourceValue && value2 instanceof LongConstantSourceValue) {
            switch(insn.getOpcode()) {
                case LADD:
                    return ((LongConstantSourceValue) value1).add(size, set, (LongConstantSourceValue) value2);
                case LSUB:
                    return ((LongConstantSourceValue) value1).subtract(size, set, (LongConstantSourceValue) value2);
                case LMUL:
                    return ((LongConstantSourceValue) value1).multiply(size, set, (LongConstantSourceValue) value2);
                case LDIV:
                    return ((LongConstantSourceValue) value1).divide(size, set, (LongConstantSourceValue) value2);
                case LREM:
                    return ((LongConstantSourceValue) value1).remainder(size, set, (LongConstantSourceValue) value2);
                case LSHL:
                    return ((LongConstantSourceValue) value1).shiftLeft(size, set, (LongConstantSourceValue) value2);
                case LSHR:
                    return ((LongConstantSourceValue) value1).shiftRight(size, set, (LongConstantSourceValue) value2);
                case LUSHR:
                    return ((LongConstantSourceValue) value1).shiftRightUnsigned(size, set, (LongConstantSourceValue) value2);
                case LAND:
                    return ((LongConstantSourceValue) value1).bitwiseAnd(size, set, (LongConstantSourceValue) value2);
                case LOR:
                    return ((LongConstantSourceValue) value1).bitwiseOr(size, set, (LongConstantSourceValue) value2);
                case LXOR:
                    return ((LongConstantSourceValue) value1).bitwiseXor(size, set, (LongConstantSourceValue) value2);
                case LCMP:
                    return ((LongConstantSourceValue) value1).compare(size, set, (LongConstantSourceValue) value2);
            }
        } else if(value1 instanceof FloatConstantSourceValue && value2 instanceof FloatConstantSourceValue) {
            switch(insn.getOpcode()) {
                case FADD:
                    return ((FloatConstantSourceValue) value1).add(size, set, (FloatConstantSourceValue) value2);
                case FSUB:
                    return ((FloatConstantSourceValue) value1).subtract(size, set, (FloatConstantSourceValue) value2);
                case FMUL:
                    return ((FloatConstantSourceValue) value1).multiply(size, set, (FloatConstantSourceValue) value2);
                case FDIV:
                    return ((FloatConstantSourceValue) value1).divide(size, set, (FloatConstantSourceValue) value2);
                case FREM:
                    return ((FloatConstantSourceValue) value1).remainder(size, set, (FloatConstantSourceValue) value2);
                case FCMPL:
                    return ((FloatConstantSourceValue) value1).compareL(size, set, (FloatConstantSourceValue) value2);
                case FCMPG:
                    return ((FloatConstantSourceValue) value1).compareG(size, set, (FloatConstantSourceValue) value2);
            }
        } else if(value1 instanceof DoubleConstantSourceValue && value2 instanceof DoubleConstantSourceValue) {
            switch(insn.getOpcode()) {
                case DADD:
                    return ((DoubleConstantSourceValue) value1).add(size, set, (DoubleConstantSourceValue) value2);
                case DSUB:
                    return ((DoubleConstantSourceValue) value1).subtract(size, set, (DoubleConstantSourceValue) value2);
                case DMUL:
                    return ((DoubleConstantSourceValue) value1).multiply(size, set, (DoubleConstantSourceValue) value2);
                case DDIV:
                    return ((DoubleConstantSourceValue) value1).divide(size, set, (DoubleConstantSourceValue) value2);
                case DREM:
                    return ((DoubleConstantSourceValue) value1).remainder(size, set, (DoubleConstantSourceValue) value2);
                case DCMPL:
                    return ((DoubleConstantSourceValue) value1).compareL(size, set, (DoubleConstantSourceValue) value2);
                case DCMPG:
                    return ((DoubleConstantSourceValue) value1).compareG(size, set, (DoubleConstantSourceValue) value2);
            }
        }
        return new BasicSourceValue(size, set);
    }

    @Override
    public SourceValue ternaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2, SourceValue value3) {
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        return new BasicSourceValue(1, set);
    }

    @Override
    public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        return new BasicSourceValue(size, set);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, SourceValue value, SourceValue expected) {
        //
    }

    @Override
    public SourceValue merge(SourceValue value1, SourceValue value2) {
        if(value1 == value2) {
            return value1;
        } else if(value1 instanceof BasicSourceValue) {
            if(value1.size != value2.size || !value1.instructions.containsAll(value2.instructions)) {
                Set<AbstractInsnNode> setUnion = new HashSet<>(value1.instructions);
                setUnion.addAll(value2.instructions);
                int size = Math.min(value1.size, value2.size);
                return new BasicSourceValue(size, setUnion);
            }
            return value1;
        }
        Set<AbstractInsnNode> setUnion = new HashSet<>(value1.instructions);
        setUnion.addAll(value2.instructions);
        int size = Math.min(value1.size, value2.size);
        if(value1 instanceof ConstantSourceValue && value2 instanceof ConstantSourceValue) {
            if(((ConstantSourceValue) value1).canMerge((ConstantSourceValue) value2)) {
                return value1.makeNew(size, setUnion);
            } else if(((ConstantSourceValue) value2).canMerge((ConstantSourceValue) value1)) {
                return value2.makeNew(size, setUnion);
            }
        }
        return new BasicSourceValue(size, setUnion);
    }

    public static Set<AbstractInsnNode> identifyRevisionExcludedInstructions(String owner, MethodNode methodNode) {
        RevisionExclusionInterpreter interpreter = new RevisionExclusionInterpreter();
        Analyzer<SourceValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(interpreter);
        try {
            Set<AbstractInsnNode> result = new HashSet<>();
            Frame<SourceValue>[] frames = analyzer.analyze(owner, methodNode);
            Iterator<AbstractInsnNode> itr = methodNode.instructions.iterator();
            for(Frame<SourceValue> frame : frames) {
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

    private static boolean shouldExclude(AbstractInsnNode insn, Frame<SourceValue> frame, InsnList insnList, Frame<SourceValue>[] allFrames) {
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
                SourceValue top = frame.getStack(0);
                if(top instanceof ConstantSourceValue) {
                    return true;
                } else {
                    int var = ((VarInsnNode) insn).var;
                    Set<AbstractInsnNode> sources = top.instructions;
                    if(sources.size() == 1) {
                        AbstractInsnNode source = sources.iterator().next();
                        Frame<SourceValue> sourceFrame = getPreviousFrame(insn, insnList, allFrames);
                        return sourceFrame != null && checkSource(source, sourceFrame, var);
                    }
                }
            default:
                return false;
        }
    }

    private static boolean checkSource(AbstractInsnNode source, Frame<SourceValue> sourceFrame, int var) {
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

    private static boolean checkStackValue(int var, SourceValue stackValue) {
        if(stackValue instanceof ConstantSourceValue) {
            return true;
        } else if(stackValue.instructions.size() == 1) {
            AbstractInsnNode insn = stackValue.instructions.iterator().next();
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

    private static Frame<SourceValue> getPreviousFrame(AbstractInsnNode insnNode, InsnList insnList, Frame<SourceValue>[] allFrames) {
        int index = insnList.indexOf(insnNode) - 1;
        if(index < 0 || index > allFrames.length) {
            return null;
        } else {
            return allFrames[index];
        }
    }

    private static int getSize(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        switch(opcode) {
            case LCONST_0:
            case LCONST_1:
            case DCONST_0:
            case DCONST_1:
            case LALOAD:
            case DALOAD:
            case LADD:
            case DADD:
            case LSUB:
            case DSUB:
            case LMUL:
            case DMUL:
            case LDIV:
            case DDIV:
            case LREM:
            case DREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
            case LNEG:
            case DNEG:
            case I2L:
            case I2D:
            case L2D:
            case F2L:
            case F2D:
            case D2L:
                return 2;
            case LDC:
                Object value = ((LdcInsnNode) insn).cst;
                return value instanceof Long || value instanceof Double ? 2 : 1;
            case GETSTATIC:
            case GETFIELD:
                return Type.getType(((FieldInsnNode) insn).desc).getSize();
            case INVOKEDYNAMIC:
                return Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
                return Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
            default:
                return 1;
        }
    }

    public static abstract class SourceValue implements Value {
        final int size;
        final Set<AbstractInsnNode> instructions;

        SourceValue(int size, Set<AbstractInsnNode> instructions) {
            this.size = size;
            this.instructions = instructions;
        }

        @Override
        public int getSize() {
            return size;
        }

        abstract SourceValue makeNew(int size, Set<AbstractInsnNode> instructions);
    }

    private static class BasicSourceValue extends SourceValue {

        BasicSourceValue(int size, Set<AbstractInsnNode> instructions) {
            super(size, instructions);
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new BasicSourceValue(size, instructions);
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            return 31 * result + size;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof BasicSourceValue)) {
                return false;
            }
            return instructions.equals(((SourceValue) o).instructions) && size == ((SourceValue) o).size;
        }
    }

    private static abstract class ConstantSourceValue extends SourceValue {
        ConstantSourceValue(int size, Set<AbstractInsnNode> instructions) {
            super(size, instructions);
        }

        abstract boolean canMerge(ConstantSourceValue other);
    }

    private static class ObjectConstantSourceValue extends ConstantSourceValue {
        private final Object constant;

        ObjectConstantSourceValue(int size, Set<AbstractInsnNode> instructions, Object constant) {
            super(size, instructions);
            this.constant = constant;
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            return other instanceof ObjectConstantSourceValue && Objects.equals(constant, ((ObjectConstantSourceValue) other).constant);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof ObjectConstantSourceValue)) {
                return false;
            }
            if(!instructions.equals(((SourceValue) o).instructions) || size != ((SourceValue) o).size) {
                return false;
            }
            ObjectConstantSourceValue that = (ObjectConstantSourceValue) o;
            return constant != null ? constant.equals(that.constant) : that.constant == null;
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            result = 31 * result + size;
            result = 31 * result + (constant != null ? constant.hashCode() : 0);
            return result;
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new ObjectConstantSourceValue(size, instructions, constant);
        }
    }

    private static class IntegerConstantSourceValue extends ConstantSourceValue {

        private final int constant;

        IntegerConstantSourceValue(int size, Set<AbstractInsnNode> instructions, int constant) {
            super(size, instructions);
            this.constant = constant;
        }

        IntegerConstantSourceValue increment(int size, Set<AbstractInsnNode> instructions, int amount) {
            return new IntegerConstantSourceValue(size, instructions, constant + amount);
        }

        IntegerConstantSourceValue negate(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, -constant);
        }

        FloatConstantSourceValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
            return new FloatConstantSourceValue(size, instructions, constant);
        }

        DoubleConstantSourceValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
            return new DoubleConstantSourceValue(size, instructions, constant);
        }

        LongConstantSourceValue castToLong(int size, Set<AbstractInsnNode> instructions) {
            return new LongConstantSourceValue(size, instructions, constant);
        }

        IntegerConstantSourceValue castToByte(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (byte) constant);
        }

        IntegerConstantSourceValue castToShort(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (short) constant);
        }

        IntegerConstantSourceValue castToChar(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (char) constant);
        }

        IntegerConstantSourceValue add(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant + other.constant);
        }

        IntegerConstantSourceValue subtract(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant - other.constant);
        }

        IntegerConstantSourceValue divide(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant / other.constant);
        }

        IntegerConstantSourceValue multiply(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant * other.constant);
        }

        IntegerConstantSourceValue remainder(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant % other.constant);
        }

        IntegerConstantSourceValue shiftLeft(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant << other.constant);
        }

        IntegerConstantSourceValue shiftRight(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant >> other.constant);
        }

        IntegerConstantSourceValue shiftRightUnsigned(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant >>> other.constant);
        }

        IntegerConstantSourceValue bitwiseOr(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant | other.constant);
        }

        IntegerConstantSourceValue bitwiseAnd(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant & other.constant);
        }

        IntegerConstantSourceValue bitwiseXor(int size, Set<AbstractInsnNode> instructions, IntegerConstantSourceValue other) {
            return new IntegerConstantSourceValue(size, instructions, constant ^ other.constant);
        }

        @Override
        boolean canMerge(ConstantSourceValue other) {
            if(other instanceof IntegerConstantSourceValue) {
                return constant == ((IntegerConstantSourceValue) other).constant;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof IntegerConstantSourceValue)) {
                return false;
            }
            if(!instructions.equals(((SourceValue) o).instructions) || size != ((SourceValue) o).size) {
                return false;
            }
            IntegerConstantSourceValue that = (IntegerConstantSourceValue) o;
            return constant == that.constant;
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            result = 31 * result + size;
            result = 31 * result + constant;
            return result;
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, constant);
        }
    }

    private static class LongConstantSourceValue extends ConstantSourceValue {
        private final long constant;

        LongConstantSourceValue(int size, Set<AbstractInsnNode> instructions, long constant) {
            super(size, instructions);
            this.constant = constant;
        }

        LongConstantSourceValue negate(int size, Set<AbstractInsnNode> instructions) {
            return new LongConstantSourceValue(size, instructions, -constant);
        }

        FloatConstantSourceValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
            return new FloatConstantSourceValue(size, instructions, constant);
        }

        DoubleConstantSourceValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
            return new DoubleConstantSourceValue(size, instructions, constant);
        }

        IntegerConstantSourceValue castToInt(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (int) constant);
        }

        LongConstantSourceValue add(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant + other.constant);
        }

        LongConstantSourceValue subtract(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant - other.constant);
        }

        LongConstantSourceValue divide(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant / other.constant);
        }

        LongConstantSourceValue multiply(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant * other.constant);
        }

        LongConstantSourceValue remainder(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant % other.constant);
        }

        LongConstantSourceValue shiftLeft(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant << other.constant);
        }

        LongConstantSourceValue shiftRight(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant >> other.constant);
        }

        LongConstantSourceValue shiftRightUnsigned(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant >>> other.constant);
        }

        LongConstantSourceValue bitwiseOr(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant | other.constant);
        }

        LongConstantSourceValue bitwiseAnd(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant & other.constant);
        }

        LongConstantSourceValue bitwiseXor(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, constant ^ other.constant);
        }

        LongConstantSourceValue compare(int size, Set<AbstractInsnNode> instructions, LongConstantSourceValue other) {
            return new LongConstantSourceValue(size, instructions, Long.compare(constant, other.constant));
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

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof LongConstantSourceValue)) {
                return false;
            }
            if(!instructions.equals(((SourceValue) o).instructions) || size != ((SourceValue) o).size) {
                return false;
            }
            LongConstantSourceValue that = (LongConstantSourceValue) o;
            return constant == that.constant;
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            result = 31 * result + size;
            result = 31 * result + (int) (constant ^ (constant >>> 32));
            return result;
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new LongConstantSourceValue(size, instructions, constant);
        }
    }

    private static class FloatConstantSourceValue extends ConstantSourceValue {
        private final float constant;

        FloatConstantSourceValue(int size, Set<AbstractInsnNode> instructions, float constant) {
            super(size, instructions);
            this.constant = constant;
        }

        FloatConstantSourceValue negate(int size, Set<AbstractInsnNode> instructions) {
            return new FloatConstantSourceValue(size, instructions, -constant);
        }

        DoubleConstantSourceValue castToDouble(int size, Set<AbstractInsnNode> instructions) {
            return new DoubleConstantSourceValue(size, instructions, constant);
        }

        IntegerConstantSourceValue castToInt(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (int) constant);
        }

        LongConstantSourceValue castToLong(int size, Set<AbstractInsnNode> instructions) {
            return new LongConstantSourceValue(size, instructions, (long) constant);
        }

        FloatConstantSourceValue add(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(size, instructions, constant + other.constant);
        }

        FloatConstantSourceValue subtract(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(size, instructions, constant - other.constant);
        }

        FloatConstantSourceValue divide(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(size, instructions, constant / other.constant);
        }

        FloatConstantSourceValue multiply(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(size, instructions, constant * other.constant);
        }

        FloatConstantSourceValue remainder(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            return new FloatConstantSourceValue(size, instructions, constant % other.constant);
        }

        FloatConstantSourceValue compareG(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValue(size, instructions, result);
        }

        FloatConstantSourceValue compareL(int size, Set<AbstractInsnNode> instructions, FloatConstantSourceValue other) {
            float result;
            if(Float.isNaN(constant) || Float.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Float.compare(constant, other.constant);
            }
            return new FloatConstantSourceValue(size, instructions, result);
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

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof FloatConstantSourceValue)) {
                return false;
            }
            if(!instructions.equals(((SourceValue) o).instructions) || size != ((SourceValue) o).size) {
                return false;
            }
            FloatConstantSourceValue that = (FloatConstantSourceValue) o;
            return Float.compare(that.constant, constant) == 0;
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            result = 31 * result + size;
            result = 31 * result + (constant != +0.0f ? Float.floatToIntBits(constant) : 0);
            return result;
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new FloatConstantSourceValue(size, instructions, constant);
        }
    }

    private static class DoubleConstantSourceValue extends ConstantSourceValue {
        private final double constant;

        DoubleConstantSourceValue(int size, Set<AbstractInsnNode> instructions, double constant) {
            super(size, instructions);
            this.constant = constant;
        }

        DoubleConstantSourceValue negate(int size, Set<AbstractInsnNode> instructions) {
            return new DoubleConstantSourceValue(size, instructions, -constant);
        }

        FloatConstantSourceValue castToFloat(int size, Set<AbstractInsnNode> instructions) {
            return new FloatConstantSourceValue(size, instructions, (float) constant);
        }

        IntegerConstantSourceValue castToInt(int size, Set<AbstractInsnNode> instructions) {
            return new IntegerConstantSourceValue(size, instructions, (int) constant);
        }

        LongConstantSourceValue castToLong(int size, Set<AbstractInsnNode> instructions) {
            return new LongConstantSourceValue(size, instructions, (long) constant);
        }

        DoubleConstantSourceValue add(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(size, instructions, constant + other.constant);
        }

        DoubleConstantSourceValue subtract(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(size, instructions, constant - other.constant);
        }

        DoubleConstantSourceValue divide(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(size, instructions, constant / other.constant);
        }

        DoubleConstantSourceValue multiply(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(size, instructions, constant * other.constant);
        }

        DoubleConstantSourceValue remainder(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            return new DoubleConstantSourceValue(size, instructions, constant % other.constant);
        }

        DoubleConstantSourceValue compareG(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = 1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValue(size, instructions, result);
        }

        DoubleConstantSourceValue compareL(int size, Set<AbstractInsnNode> instructions, DoubleConstantSourceValue other) {
            double result;
            if(Double.isNaN(constant) || Double.isNaN(other.constant)) {
                result = -1;
            } else {
                result = Double.compare(constant, other.constant);
            }
            return new DoubleConstantSourceValue(size, instructions, result);
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

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof DoubleConstantSourceValue)) {
                return false;
            }
            if(!instructions.equals(((SourceValue) o).instructions) || size != ((SourceValue) o).size) {
                return false;
            }
            DoubleConstantSourceValue that = (DoubleConstantSourceValue) o;
            return Double.compare(that.constant, constant) == 0;
        }

        @Override
        public int hashCode() {
            int result = instructions.hashCode();
            result = 31 * result + size;
            long temp;
            temp = Double.doubleToLongBits(constant);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        SourceValue makeNew(int size, Set<AbstractInsnNode> instructions) {
            return new DoubleConstantSourceValue(size, instructions, constant);
        }
    }
}
