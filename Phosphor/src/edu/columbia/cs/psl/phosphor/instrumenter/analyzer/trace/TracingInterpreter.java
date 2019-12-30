package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.DependentLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.analysis.Analyzer;
import edu.columbia.cs.psl.phosphor.struct.BitSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.Iterator;
import java.util.List;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.ConstantLoopLevel.CONSTANT_LOOP_LEVEL;
import static org.objectweb.asm.Opcodes.*;

public final class TracingInterpreter extends Interpreter<TracedValue> {

    private final Map<AbstractInsnNode, InstructionEffect> effectMap = new HashMap<>();
    private final InsnList instructions;
    private final Map<AbstractInsnNode, Set<NaturalLoop<BasicBlock>>> containingLoopMap;
    private final Frame<TracedValue>[] frames;
    private int paramNumber = 0;
    private int currentInsnIndex = -1;

    public TracingInterpreter(String owner, MethodNode methodNode, Map<AbstractInsnNode, Set<NaturalLoop<BasicBlock>>> containingLoopMap) throws AnalyzerException {
        super(Configuration.ASM_VERSION);
        this.instructions = methodNode.instructions;
        this.containingLoopMap = containingLoopMap;
        Analyzer<TracedValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(this);
        this.frames = analyzer.analyze(owner, methodNode);
    }

    @Override
    public TracedValue newValue(Type type) {
        if(type == Type.VOID_TYPE) {
            return null;
        }
        return new VariantTracedValue(type == null ? 1 : type.getSize(), new HashSet<>(), Collections.emptySet());
    }

    @Override
    public TracedValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
        BitSet dependencies = new BitSet(paramNumber + 1);
        dependencies.add(paramNumber++);
        return new DependentTracedValue(type == null ? 1 : type.getSize(), new HashSet<>(), dependencies);
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
                result = new VariantTracedValue(size, set, containingLoopMap.get(insn));
                break;
        }
        effectMap.put(insn, new InstructionEffect(result));
        return result;
    }

    @Override
    public TracedValue copyOperation(AbstractInsnNode insn, TracedValue value) {
        TracedValue result;
        int size = value.getSize();
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
                result = value.newInstance(size, Collections.singleton(insn));
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
            result = value.newInstance(size, Collections.singleton(insn));
        }
        effectMap.put(insn, new InstructionEffect(value, result));
        return result;
    }

    @Override
    public TracedValue binaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2) {
        TracedValue result = null;
        boolean finished = false;
        int size = getSize(insn);
        Set<AbstractInsnNode> set = Collections.singleton(insn);
        switch(insn.getOpcode()) {
            case IALOAD:
            case LALOAD:
            case FALOAD:
            case DALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                result = new VariantTracedValue(1, Collections.singleton(insn), containingLoopMap.get(insn));
                effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2}, result));
                return result;
        }
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
        } else if(value1 instanceof DependentTracedValue && value2 instanceof DependentTracedValue) {
            result = new DependentTracedValue(size, set, (DependentTracedValue) value1, (DependentTracedValue) value2);
            finished = true;
        } else if(value1 instanceof DependentTracedValue && value2 instanceof ConstantTracedValue) {
            result = value1.newInstance(size, set);
            finished = true;
        } else if(value1 instanceof ConstantTracedValue && value2 instanceof DependentTracedValue) {
            result = value2.newInstance(size, set);
            finished = true;
        }
        if(!finished) {
            Set<NaturalLoop<BasicBlock>> variantLoops = new HashSet<>();
            if(value1 instanceof VariantTracedValue) {
                variantLoops.addAll(((VariantTracedValue) value1).getVariantLoops());
            }
            if(value2 instanceof VariantTracedValue) {
                variantLoops.addAll(((VariantTracedValue) value2).getVariantLoops());
            }
            Set<NaturalLoop<BasicBlock>> containingLoops = new HashSet<>(containingLoopMap.get(insn));
            containingLoops.retainAll(variantLoops);
            result = new VariantTracedValue(size, set, containingLoops);
        }
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2}, result));
        return result;
    }

    @Override
    public TracedValue ternaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2, TracedValue value3) {
        TracedValue result = new VariantTracedValue(1, Collections.singleton(insn), containingLoopMap.get(insn));
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2, value3}, result));
        return result;
    }

    @Override
    public TracedValue naryOperation(AbstractInsnNode insn, List<? extends TracedValue> values) {
        TracedValue result = new VariantTracedValue(getSize(insn), Collections.singleton(insn), containingLoopMap.get(insn));
        effectMap.put(insn, new InstructionEffect(values.toArray(new TracedValue[0]), result));
        return result;
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, TracedValue value, TracedValue expected) {
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value}, null));
    }

    public void preMerge(int insnIndex) {
        currentInsnIndex = insnIndex;
    }

    public void postMerge() {
        currentInsnIndex = -1;
    }

    @Override
    public TracedValue merge(TracedValue value1, TracedValue value2) {
        if(value1 == value2) {
            return value1;
        } else if(value1 instanceof DependentTracedValue && value1.equals(value2)) {
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
        if(currentInsnIndex == -1) {
            return new VariantTracedValue(size, setUnion, new HashSet<>());
        } else {
            Set<NaturalLoop<BasicBlock>> containingLoops = new HashSet<>(containingLoopMap.get(instructions.get(currentInsnIndex)));
            return new VariantTracedValue(size, setUnion, containingLoops);
        }
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

    /**
     * @return a mapping from the instructions analyzed by this interpreter to their loop levels
     */
    Map<AbstractInsnNode, LoopLevel> calculateLoopLevelMap() {
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = new HashMap<>();
        for(AbstractInsnNode insn : effectMap.keySet()) {
            Set<TracedValue> sources = getSources(insn, effectMap.get(insn));
            if(atConstantLevel(insn, sources)) {
                loopLevelMap.put(insn, CONSTANT_LOOP_LEVEL);
            } else if(atDependentLevel(sources)) {
                loopLevelMap.put(insn, createDependentLoopLevel(sources));
            } else {
                loopLevelMap.put(insn, createVariantLoopLevel(insn, sources, containingLoopMap));
            }
        }
        return loopLevelMap;
    }

    /**
     * The "sources" of an instructions are the values used from local variables and the runtime stack by the
     * instruction. For operations that store a value to a local variable, field, or array any source which
     * is the current definition of the memory location being written to by the store operation is excluded from the
     * instruction's sources. Specifically, for a ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, IASTORE, LASTORE, FASTORE,
     * DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, PUTSTATIC, or PUTFIELD  instruction that stores a value v into a
     * memory location x where v can be expressed as an arithmetic or logical expression e, any term in e that is
     * loaded from memory location x is excluded.
     *
     * @param insn   the instruction whose sources are to be calculated
     * @param effect the effect associated with the specified instruction
     * @return the set of source values that should be considered when calculating the loop level of the specified
     * instructions
     */
    private Set<TracedValue> getSources(AbstractInsnNode insn, InstructionEffect effect) {
        if(isLocalVariableStoreInsn(insn)) {
            Frame<TracedValue> currentFrame = frames[instructions.indexOf(insn)];
            int local = ((VarInsnNode) insn).var;
            TracedValue currentDefinition = currentFrame.getLocal(local);
            Set<TracedValue> sources = new HashSet<>();
            SameLocationChecker checker = new SameLocalVariableChecker(effectMap, local, currentDefinition);
            gatherSources(checker, effect.sources[0], sources, new HashSet<>());
            return sources;
        } else if(isFieldStoreInsn(insn)) {
            SameLocationChecker checker = new SameFieldChecker(effectMap, (FieldInsnNode) insn);
            Set<TracedValue> sources = new HashSet<>();
            if(insn.getOpcode() == PUTSTATIC) {
                gatherSources(checker, effect.sources[0], sources, new HashSet<>());
            } else {
                sources.add(effect.sources[0]); // Add the receiver as a source
                gatherSources(checker, effect.sources[1], sources, new HashSet<>(sources));
            }
            return sources;
        } else if(isArrayStoreInsn(insn)) {
            SameLocationChecker checker = new SameArrayLocationChecker(effectMap, (InsnNode) insn);
            Set<TracedValue> sources = new HashSet<>();
            sources.add(effect.sources[0]); // Add the array reference as a source
            sources.add(effect.sources[1]); // Add the index as a source
            gatherSources(checker, effect.sources[2], sources, new HashSet<>(sources));
            return sources;
        }
        return new HashSet<>(Arrays.asList(effect.sources));
    }

    private void gatherSources(SameLocationChecker checker, TracedValue current, Set<TracedValue> sources, Set<TracedValue> visited) {
        if(visited.add(current)) {
            if(current.getSources().size() != 1) {
                sources.add(current);
                return;
            }
            AbstractInsnNode insn = current.getSources().iterator().next();
            if(!checker.loadsValueFromSameLocation(insn) && !sources.contains(current)) {
                if(isArithmeticOrLogicalInsn(insn) && effectMap.containsKey(insn)) {
                    InstructionEffect effect = effectMap.get(insn);
                    for(TracedValue source : effect.sources) {
                        gatherSources(checker, source, sources, visited);
                    }
                } else {
                    sources.add(current);
                }
            }
        }
    }

    /**
     * An instruction is considered to be at the constant loop level if one of the following conditions is met:
     * <ul>
     *     <li>The instruction is an ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
     *       LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH, or LDC instruction.</li>
     *     <li>The instruction is an IINC instruction.</li>
     *     <li>All of the sources of the instruction can be identified as having the same constant value
     *     along all execution paths to the instruction or, in the case of operations that store a value to a local
     *     variable, field, or array, as using the current value of the memory location being written to. </li>
     * </ul>
     *
     * @param insn    the instruction being checked
     * @param sources the local variables and runtime stack values used by the specified instruction
     * @return true if the specified instruction should be marked as being at the constant loop level
     */
    private static boolean atConstantLevel(AbstractInsnNode insn, Set<TracedValue> sources) {
        if(isPushConstantInsn(insn) || insn.getOpcode() == IINC) {
            return true;
        }
        for(TracedValue source : sources) {
            if(!(source instanceof ConstantTracedValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param sources the local variables and runtime stack values used by an instruction
     * @return true if the specified sources does not contains a VariantTracedValue
     */
    private static boolean atDependentLevel(Set<TracedValue> sources) {
        for(TracedValue source : sources) {
            if(source instanceof VariantTracedValue) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param sources the local variables and runtime stack values used by an instruction, none of the sources can be
     *                variant and at least one must be argument dependent
     * @return a dependent loop level instance that represents the argument dependencies in the specified sources
     */
    private static DependentLoopLevel createDependentLoopLevel(Set<TracedValue> sources) {
        BitSet dependencies = new BitSet(0);
        for(TracedValue source : sources) {
            if(source instanceof DependentTracedValue) {
                dependencies = BitSet.union(dependencies, ((DependentTracedValue) source).getDependencies());
            }
        }
        return new DependentLoopLevel(dependencies.toList().toArray());
    }

    /**
     * @param insn              the instruction for which a loop level instance is to be created
     * @param sources           the local variables and runtime stack values used by the specified instruction
     * @param containingLoopMap a mapping from instructions to the set of naturals that contain them
     * @return a variant loop level instance that notes the number of loops that contain the specified instruction for
     * which on the of sources of the instruction varies
     */
    private static VariantLoopLevel createVariantLoopLevel(AbstractInsnNode insn, Set<TracedValue> sources,
                                                           Map<AbstractInsnNode, Set<NaturalLoop<BasicBlock>>> containingLoopMap) {
        Set<NaturalLoop<BasicBlock>> variantLoops = new HashSet<>();
        for(TracedValue source : sources) {
            if(source instanceof VariantTracedValue) {
                variantLoops.addAll(((VariantTracedValue) source).getVariantLoops());
            }
        }
        Set<NaturalLoop<BasicBlock>> containingVariantLoops = containingLoopMap.get(insn);
        containingVariantLoops.retainAll(variantLoops);
        return new VariantLoopLevel(containingVariantLoops.size());
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into a field
     */
    private static boolean isFieldStoreInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case PUTSTATIC:
            case PUTFIELD:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into an array
     */
    private static boolean isArrayStoreInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case IASTORE:
            case LASTORE:
            case FASTORE:
            case DASTORE:
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction loads a value from an array
     */
    private static boolean isArrayLoadInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case IALOAD:
            case LALOAD:
            case FALOAD:
            case DALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into a local variable
     */
    private static boolean isLocalVariableStoreInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction loads a value from a local variable onto the runtime stack
     */
    private static boolean isLocalVariableLoadInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction pushes a constant onto the runtime stack
     */
    private static boolean isPushConstantInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
            case ACONST_NULL:
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
                return true;
            default:
                return false;
        }
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction performs an arithmetic or logical computation using values on the
     * runtime stack and pushes the result onto the runtime stack
     */
    private static boolean isArithmeticOrLogicalInsn(AbstractInsnNode insn) {
        switch(insn.getOpcode()) {
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
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
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
            case IINC:
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
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return true;
            default:
                return false;
        }
    }

    private static boolean isSameValue(TracedValue value1, TracedValue value2, Map<AbstractInsnNode, InstructionEffect> effectMap) {
        if(value1.equals(value2)) {
            return true;
        } else if(value1 instanceof ConstantTracedValue && value2 instanceof ConstantTracedValue) {
            return ((ConstantTracedValue) value1).canMerge((ConstantTracedValue) value2);
        }
        if(value1.getSources().size() == 1) {
            AbstractInsnNode s1 = value1.getSources().iterator().next();
            if(isLocalVariableLoadInsn(s1) && effectMap.containsKey(s1)) {
                return isSameValue(effectMap.get(s1).sources[0], value2, effectMap);
            }
        }
        if(value2.getSources().size() == 1) {
            AbstractInsnNode s2 = value2.getSources().iterator().next();
            if(isLocalVariableLoadInsn(s2) && effectMap.containsKey(s2)) {
                return isSameValue(value1, effectMap.get(s2).sources[0], effectMap);
            }
        }
        // TODO check if is same field value or same array value
        return false;
    }

    private abstract static class SameLocationChecker {
        final Map<AbstractInsnNode, InstructionEffect> effectMap;

        SameLocationChecker(Map<AbstractInsnNode, InstructionEffect> effectMap) {
            this.effectMap = effectMap;
        }

        abstract boolean loadsValueFromSameLocation(AbstractInsnNode insn);
    }

    private static class SameLocalVariableChecker extends SameLocationChecker {

        private final int local;
        private final TracedValue currentDefinition;

        SameLocalVariableChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, int local, TracedValue currentDefinition) {
            super(effectMap);
            this.local = local;
            this.currentDefinition = currentDefinition;
        }

        @Override
        boolean loadsValueFromSameLocation(AbstractInsnNode insn) {
            return isLocalVariableLoadInsn(insn) && ((VarInsnNode) insn).var == local
                    && effectMap.containsKey(insn) && currentDefinition.equals(effectMap.get(insn).sources[0]);
        }
    }

    private static class SameFieldChecker extends SameLocationChecker {

        private final FieldInsnNode fieldInsn;
        private final TracedValue receiver;

        SameFieldChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, FieldInsnNode fieldInsn) {
            super(effectMap);
            this.fieldInsn = fieldInsn;
            if(fieldInsn.getOpcode() == PUTSTATIC) {
                receiver = null;
            } else {
                receiver = effectMap.get(fieldInsn).sources[0];
            }
        }

        @Override
        boolean loadsValueFromSameLocation(AbstractInsnNode insn) {
            InstructionEffect effect = effectMap.get(insn);
            if(effect != null && insn instanceof FieldInsnNode && ((FieldInsnNode) insn).name.equals(fieldInsn.name)
                    && ((FieldInsnNode) insn).owner.equals(fieldInsn.owner)) {
                if(fieldInsn.getOpcode() == PUTSTATIC && insn.getOpcode() == GETSTATIC) {
                    return true;
                } else if(fieldInsn.getOpcode() == PUTFIELD && insn.getOpcode() == GETFIELD) {
                    TracedValue otherReceiver = effect.sources[0];
                    return isSameValue(receiver, otherReceiver, effectMap);
                }
            }
            return false;
        }
    }

    private static class SameArrayLocationChecker extends SameLocationChecker {

        private final TracedValue arrayReference;
        private final TracedValue index;

        SameArrayLocationChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, InsnNode insn) {
            super(effectMap);
            InstructionEffect effect = effectMap.get(insn);
            this.arrayReference = effect.sources[0];
            this.index = effect.sources[1];
        }

        @Override
        boolean loadsValueFromSameLocation(AbstractInsnNode insn) {
            if(isArrayLoadInsn(insn) && effectMap.containsKey(insn)) {
                InstructionEffect effect = effectMap.get(insn);
                TracedValue otherArrayReference = effect.sources[0];
                TracedValue otherIndex = effect.sources[1];
                return isSameValue(arrayReference, otherArrayReference, effectMap) && isSameValue(index, otherIndex, effectMap);
            }
            return false;
        }
    }
}
