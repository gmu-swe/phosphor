package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.Iterator;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class TracingInterpreter extends Interpreter<TracedValue> {

    private final Map<AbstractInsnNode, InstructionEffect> effectMap = new HashMap<>();
    private final Frame<TracedValue>[] frames;
    private final InsnList instructions;
    private final Map<TracedValue, ParamValue> paramMap = new HashMap<>();

    public TracingInterpreter(String owner, MethodNode methodNode) throws AnalyzerException {
        super(Configuration.ASM_VERSION);
        Analyzer<TracedValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(this);
        this.frames = analyzer.analyze(owner, methodNode);
        this.instructions = methodNode.instructions;
    }

    @Override
    public TracedValue newValue(Type type) {
        if(type == Type.VOID_TYPE) {
            return null;
        }
        return new BasicTracedValue(type == null ? 1 : type.getSize(), new HashSet<>());
    }

    @Override
    public TracedValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
        TracedValue result = newValue(type);
        paramMap.put(result, new ParamValue(isInstanceMethod, local, type));
        return result;
    }

    @Override
    public TracedValue newOperation(AbstractInsnNode insn) {
        TracedValue result;
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                result = new ObjectConstantTracedValue(size, set, null);
                break;
            case ICONST_M1:
                result = new IntegerConstantTracedValue(size, set, -1);
                break;
            case ICONST_0:
                result = new IntegerConstantTracedValue(size, set, 0);
                break;
            case ICONST_1:
                result = new IntegerConstantTracedValue(size, set, 1);
                break;
            case ICONST_2:
                result = new IntegerConstantTracedValue(size, set, 2);
                break;
            case ICONST_3:
                result = new IntegerConstantTracedValue(size, set, 3);
                break;
            case ICONST_4:
                result = new IntegerConstantTracedValue(size, set, 4);
                break;
            case ICONST_5:
                result = new IntegerConstantTracedValue(size, set, 5);
                break;
            case DCONST_0:
                result = new DoubleConstantTracedValue(size, set, 0);
                break;
            case DCONST_1:
                result = new DoubleConstantTracedValue(size, set, 1);
                break;
            case FCONST_0:
                result = new FloatConstantTracedValue(size, set, 0f);
                break;
            case FCONST_1:
                result = new FloatConstantTracedValue(size, set, 1f);
                break;
            case FCONST_2:
                result = new FloatConstantTracedValue(size, set, 2f);
                break;
            case LCONST_0:
                result = new LongConstantTracedValue(size, set, 0L);
                break;
            case LCONST_1:
                result = new LongConstantTracedValue(size, set, 1L);
                break;
            case BIPUSH:
            case SIPUSH:
                result = new IntegerConstantTracedValue(size, set, ((IntInsnNode) insn).operand);
                break;
            case LDC:
                result = new ObjectConstantTracedValue(size, set, ((LdcInsnNode) insn).cst);
                break;
            default:
                result = new BasicTracedValue(size, set);
                break;
        }
        effectMap.put(insn, new InstructionEffect(result));
        return result;
    }

    @Override
    public TracedValue copyOperation(AbstractInsnNode insn, TracedValue value) {
        TracedValue result;
        int size = value.getSize();
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
                result = value.newInstance(size, set);
                break;
            default:
                result = value;
                break;
        }
        effectMap.put(insn, new InstructionEffect(value, result));
        return result;
    }

    @Override
    public TracedValue unaryOperation(AbstractInsnNode insn, TracedValue value) {
        TracedValue result = null;
        boolean finished = false;
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        if(value instanceof IntegerConstantTracedValue) {
            switch(insn.getOpcode()) {
                case INEG:
                    result = ((IntegerConstantTracedValue) value).negate(size, set);
                    finished = true;
                    break;
                case IINC:
                    result = ((IntegerConstantTracedValue) value).increment(size, set, ((IincInsnNode) insn).incr);
                    finished = true;
                    break;
                case I2L:
                    result = ((IntegerConstantTracedValue) value).castToLong(size, set);
                    finished = true;
                    break;
                case I2F:
                    result = ((IntegerConstantTracedValue) value).castToFloat(size, set);
                    finished = true;
                    break;
                case I2D:
                    result = ((IntegerConstantTracedValue) value).castToDouble(size, set);
                    finished = true;
                    break;
                case I2B:
                    result = ((IntegerConstantTracedValue) value).castToByte(size, set);
                    finished = true;
                    break;
                case I2C:
                    result = ((IntegerConstantTracedValue) value).castToChar(size, set);
                    finished = true;
                    break;
                case I2S:
                    result = ((IntegerConstantTracedValue) value).castToShort(size, set);
                    finished = true;
                    break;
            }
        } else if(value instanceof LongConstantTracedValue) {
            switch(insn.getOpcode()) {
                case LNEG:
                    result = ((LongConstantTracedValue) value).negate(size, set);
                    finished = true;
                    break;
                case L2I:
                    result = ((LongConstantTracedValue) value).castToInt(size, set);
                    finished = true;
                    break;
                case L2F:
                    result = ((LongConstantTracedValue) value).castToFloat(size, set);
                    finished = true;
                    break;
                case L2D:
                    result = ((LongConstantTracedValue) value).castToDouble(size, set);
                    finished = true;
                    break;
            }
        } else if(value instanceof FloatConstantTracedValue) {
            switch(insn.getOpcode()) {
                case FNEG:
                    result = ((FloatConstantTracedValue) value).negate(size, set);
                    finished = true;
                    break;
                case F2I:
                    result = ((FloatConstantTracedValue) value).castToInt(size, set);
                    finished = true;
                    break;
                case F2L:
                    result = ((FloatConstantTracedValue) value).castToLong(size, set);
                    finished = true;
                    break;
                case F2D:
                    result = ((FloatConstantTracedValue) value).castToDouble(size, set);
                    finished = true;
                    break;
            }
        } else if(value instanceof DoubleConstantTracedValue) {
            switch(insn.getOpcode()) {
                case DNEG:
                    result = ((DoubleConstantTracedValue) value).negate(size, set);
                    finished = true;
                    break;
                case D2I:
                    result = ((DoubleConstantTracedValue) value).castToInt(size, set);
                    finished = true;
                    break;
                case D2L:
                    result = ((DoubleConstantTracedValue) value).castToLong(size, set);
                    finished = true;
                    break;
                case D2F:
                    result = ((DoubleConstantTracedValue) value).castToFloat(size, set);
                    finished = true;
                    break;
            }
        }
        if(!finished) {
            result = new BasicTracedValue(size, set);
        }
        effectMap.put(insn, new InstructionEffect(value, result));
        return result;
    }

    @Override
    public TracedValue binaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2) {
        TracedValue result = null;
        boolean finished = false;
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        if(value1 instanceof IntegerConstantTracedValue && value2 instanceof IntegerConstantTracedValue) {
            switch(insn.getOpcode()) {
                case IADD:
                    result = ((IntegerConstantTracedValue) value1).add(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISUB:
                    result = ((IntegerConstantTracedValue) value1).subtract(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IMUL:
                    result = ((IntegerConstantTracedValue) value1).multiply(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IDIV:
                    result = ((IntegerConstantTracedValue) value1).divide(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IREM:
                    result = ((IntegerConstantTracedValue) value1).remainder(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISHL:
                    result = ((IntegerConstantTracedValue) value1).shiftLeft(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISHR:
                    result = ((IntegerConstantTracedValue) value1).shiftRight(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IUSHR:
                    result = ((IntegerConstantTracedValue) value1).shiftRightUnsigned(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IAND:
                    result = ((IntegerConstantTracedValue) value1).bitwiseAnd(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IOR:
                    result = ((IntegerConstantTracedValue) value1).bitwiseOr(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IXOR:
                    result = ((IntegerConstantTracedValue) value1).bitwiseXor(size, set, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof LongConstantTracedValue && value2 instanceof LongConstantTracedValue) {
            switch(insn.getOpcode()) {
                case LADD:
                    result = ((LongConstantTracedValue) value1).add(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSUB:
                    result = ((LongConstantTracedValue) value1).subtract(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LMUL:
                    result = ((LongConstantTracedValue) value1).multiply(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LDIV:
                    result = ((LongConstantTracedValue) value1).divide(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LREM:
                    result = ((LongConstantTracedValue) value1).remainder(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSHL:
                    result = ((LongConstantTracedValue) value1).shiftLeft(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSHR:
                    result = ((LongConstantTracedValue) value1).shiftRight(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LUSHR:
                    result = ((LongConstantTracedValue) value1).shiftRightUnsigned(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LAND:
                    result = ((LongConstantTracedValue) value1).bitwiseAnd(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LOR:
                    result = ((LongConstantTracedValue) value1).bitwiseOr(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LXOR:
                    result = ((LongConstantTracedValue) value1).bitwiseXor(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LCMP:
                    result = ((LongConstantTracedValue) value1).compare(size, set, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof FloatConstantTracedValue && value2 instanceof FloatConstantTracedValue) {
            switch(insn.getOpcode()) {
                case FADD:
                    result = ((FloatConstantTracedValue) value1).add(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FSUB:
                    result = ((FloatConstantTracedValue) value1).subtract(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FMUL:
                    result = ((FloatConstantTracedValue) value1).multiply(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FDIV:
                    result = ((FloatConstantTracedValue) value1).divide(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FREM:
                    result = ((FloatConstantTracedValue) value1).remainder(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FCMPL:
                    result = ((FloatConstantTracedValue) value1).compareL(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FCMPG:
                    result = ((FloatConstantTracedValue) value1).compareG(size, set, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof DoubleConstantTracedValue && value2 instanceof DoubleConstantTracedValue) {
            switch(insn.getOpcode()) {
                case DADD:
                    result = ((DoubleConstantTracedValue) value1).add(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DSUB:
                    result = ((DoubleConstantTracedValue) value1).subtract(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DMUL:
                    result = ((DoubleConstantTracedValue) value1).multiply(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DDIV:
                    result = ((DoubleConstantTracedValue) value1).divide(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DREM:
                    result = ((DoubleConstantTracedValue) value1).remainder(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DCMPL:
                    result = ((DoubleConstantTracedValue) value1).compareL(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DCMPG:
                    result = ((DoubleConstantTracedValue) value1).compareG(size, set, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        }
        if(!finished) {
            result = new BasicTracedValue(size, set);
        }
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2}, result));
        return result;
    }

    @Override
    public TracedValue ternaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2, TracedValue value3) {
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        TracedValue result = new BasicTracedValue(1, set);
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2, value3}, result));
        return result;
    }

    @Override
    public TracedValue naryOperation(AbstractInsnNode insn, List<? extends TracedValue> values) {
        int size = getSize(insn);
        Set<AbstractInsnNode> set = new HashSet<>();
        set.add(insn);
        TracedValue result = new BasicTracedValue(size, set);
        effectMap.put(insn, new InstructionEffect(values.toArray(new TracedValue[0]), result));
        return result;
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, TracedValue value, TracedValue expected) {
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value}, null));
    }

    @Override
    public TracedValue merge(TracedValue value1, TracedValue value2) {
        if(value1 == value2) {
            return value1;
        } else if(value1 instanceof BasicTracedValue) {
            if(value1.getSize() != value2.getSize() || !value1.getSources().containsAll(value2.getSources())) {
                Set<AbstractInsnNode> setUnion = new HashSet<>(value1.getSources());
                setUnion.addAll(value2.getSources());
                int size = Math.min(value1.getSize(), value2.getSize());
                return new BasicTracedValue(size, setUnion);
            }
            return value1;
        }
        Set<AbstractInsnNode> setUnion = new HashSet<>(value1.getSources());
        setUnion.addAll(value2.getSources());
        int size = Math.min(value1.getSize(), value2.getSize());
        if(value1 instanceof ConstantTracedValue && value2 instanceof ConstantTracedValue) {
            if(((ConstantTracedValue) value1).canMerge((ConstantTracedValue) value2)) {
                return value1.newInstance(size, setUnion);
            } else if(((ConstantTracedValue) value2).canMerge((ConstantTracedValue) value1)) {
                return value2.newInstance(size, setUnion);
            }
        }
        return new BasicTracedValue(size, setUnion);
    }

    /**
     * @param insn the instruction whose associated type's size is being calculated
     * @return the size of the type associated with the specified instruction
     */
    private int getSize(AbstractInsnNode insn) {
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

    /**
     * @param insn the instruction to be checked
     * @return true if the source of each operand or value used by the specified instruction is constant
     */
    public boolean hasConstantSources(AbstractInsnNode insn) {
        if(effectMap.containsKey(insn)) {
            InstructionEffect effect = effectMap.get(insn);
            for(TracedValue source : effect.sources) {
                if(!(source instanceof ConstantTracedValue)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public Set<AbstractInsnNode> identifyRevisionExcludedInstructions() {
        Set<AbstractInsnNode> exclusions = new HashSet<>();
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(shouldExclude(insn)) {
                exclusions.add(insn);
            }
        }
        return exclusions;
    }

    private boolean shouldExclude(AbstractInsnNode insn) {
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
                int var = ((VarInsnNode) insn).var;
                return checkSources(var, insn, new HashSet<>());
            default:
                return false;
        }
    }

    private boolean checkSources(int var, AbstractInsnNode insn, Set<AbstractInsnNode> visited) {
        visited.add(insn);
        if(insn.getOpcode() >= ILOAD && insn.getOpcode() <= ALOAD && ((VarInsnNode) insn).var == var) {
            return true;
        }
        if(effectMap.containsKey(insn)) {
            InstructionEffect effect = effectMap.get(insn);
            if(effect.product instanceof ConstantTracedValue) {
                return true;
            }
            for(TracedValue source : effect.sources) {
                if(!(source instanceof ConstantTracedValue)) {
                    if(source.getSources().size() != 1) {
                        return false;
                    }
                    AbstractInsnNode sourceInsn = source.getSources().iterator().next();
                    if(visited.contains(sourceInsn) || !checkSources(var, sourceInsn, visited)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private Set<SourceLeaf> walkToLeaves(AbstractInsnNode insn) {
        Set<SourceLeaf> leaves = new HashSet<>();
        gatherLeaves(insn, new HashSet<>(), leaves);
        return leaves;
    }

    private void gatherLeaves(AbstractInsnNode insn, Set<AbstractInsnNode> visited, Set<SourceLeaf> leaves) {
        if(visited.add(insn)) {
            InstructionEffect effect = effectMap.get(insn);
            SourceLeaf l = SourceLeaf.newLeaf(insn, effect, paramMap);
            if(l != null) {
                leaves.add(l);
            } else {
                for(TracedValue val : effect.sources) {
                    if(val.getSources().size() == 1) {
                        gatherLeaves(val.getSources().iterator().next(), visited, leaves);
                    } else {
                        leaves.add(new SourceLeaf.SplitSource(val.getSources()));
                    }
                }
            }
        }
    }
}
