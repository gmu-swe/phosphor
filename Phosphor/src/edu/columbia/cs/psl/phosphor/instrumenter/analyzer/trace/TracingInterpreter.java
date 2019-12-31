package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.ConstantLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.DependentLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace.MergePointTracedValue.MergePointValueCache;
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
    private final MergePointValueCache mergePointCache = new MergePointValueCache();
    private final InsnList instructions;
    private final Map<AbstractInsnNode, Set<NaturalLoop<BasicBlock>>> containingLoopMap;
    private final Frame<TracedValue>[] frames;
    private int paramNumber = 0;
    private int currentInsnIndex = -1;
    private int varIndex = -1;

    public TracingInterpreter(String owner, MethodNode methodNode, Map<AbstractInsnNode,
            Set<NaturalLoop<BasicBlock>>> containingLoopMap) throws AnalyzerException {
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
        return new VariantTracedValueImpl(type == null ? 1 : type.getSize(), null, Collections.emptySet());
    }

    @Override
    public TracedValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
        BitSet dependencies = new BitSet(paramNumber + 1);
        dependencies.add(paramNumber++);
        return new DependentTracedValue(type == null ? 1 : type.getSize(), null, dependencies);
    }

    @Override
    public TracedValue newOperation(AbstractInsnNode insn) {
        TracedValue result;
        int size = getSize(insn);
        switch(insn.getOpcode()) {
            case ACONST_NULL:
                result = new ObjectConstantTracedValue(size, insn, null);
                break;
            case ICONST_M1:
                result = new IntegerConstantTracedValue(size, insn, -1);
                break;
            case ICONST_0:
                result = new IntegerConstantTracedValue(size, insn, 0);
                break;
            case ICONST_1:
                result = new IntegerConstantTracedValue(size, insn, 1);
                break;
            case ICONST_2:
                result = new IntegerConstantTracedValue(size, insn, 2);
                break;
            case ICONST_3:
                result = new IntegerConstantTracedValue(size, insn, 3);
                break;
            case ICONST_4:
                result = new IntegerConstantTracedValue(size, insn, 4);
                break;
            case ICONST_5:
                result = new IntegerConstantTracedValue(size, insn, 5);
                break;
            case DCONST_0:
                result = new DoubleConstantTracedValue(size, insn, 0);
                break;
            case DCONST_1:
                result = new DoubleConstantTracedValue(size, insn, 1);
                break;
            case FCONST_0:
                result = new FloatConstantTracedValue(size, insn, 0f);
                break;
            case FCONST_1:
                result = new FloatConstantTracedValue(size, insn, 1f);
                break;
            case FCONST_2:
                result = new FloatConstantTracedValue(size, insn, 2f);
                break;
            case LCONST_0:
                result = new LongConstantTracedValue(size, insn, 0L);
                break;
            case LCONST_1:
                result = new LongConstantTracedValue(size, insn, 1L);
                break;
            case BIPUSH:
            case SIPUSH:
                result = new IntegerConstantTracedValue(size, insn, ((IntInsnNode) insn).operand);
                break;
            case LDC:
                result = new ObjectConstantTracedValue(size, insn, ((LdcInsnNode) insn).cst);
                break;
            default:
                result = new VariantTracedValueImpl(size, insn, new HashSet<>(containingLoopMap.get(insn)));
                break;
        }
        effectMap.put(insn, new InstructionEffect(result));
        return result;
    }

    @Override
    public TracedValue copyOperation(AbstractInsnNode insn, TracedValue value) {
        effectMap.put(insn, new InstructionEffect(value, value));
        return value;
    }

    @Override
    public TracedValue unaryOperation(AbstractInsnNode insn, TracedValue value) {
        TracedValue result = null;
        boolean finished = false;
        int size = getSize(insn);
        switch(insn.getOpcode()) {
            case NEWARRAY:
            case ANEWARRAY:
            case GETFIELD:
                result = new VariantTracedValueImpl(size, insn, new HashSet<>(containingLoopMap.get(insn)));
                effectMap.put(insn, new InstructionEffect(value, result));
                return result;

        }
        if(value instanceof IntegerConstantTracedValue) {
            switch(insn.getOpcode()) {
                case INEG:
                    result = ((IntegerConstantTracedValue) value).negate(size, insn);
                    finished = true;
                    break;
                case IINC:
                    result = ((IntegerConstantTracedValue) value).increment(size, insn, ((IincInsnNode) insn).incr);
                    finished = true;
                    break;
                case I2L:
                    result = ((IntegerConstantTracedValue) value).castToLong(size, insn);
                    finished = true;
                    break;
                case I2F:
                    result = ((IntegerConstantTracedValue) value).castToFloat(size, insn);
                    finished = true;
                    break;
                case I2D:
                    result = ((IntegerConstantTracedValue) value).castToDouble(size, insn);
                    finished = true;
                    break;
                case I2B:
                    result = ((IntegerConstantTracedValue) value).castToByte(size, insn);
                    finished = true;
                    break;
                case I2C:
                    result = ((IntegerConstantTracedValue) value).castToChar(size, insn);
                    finished = true;
                    break;
                case I2S:
                    result = ((IntegerConstantTracedValue) value).castToShort(size, insn);
                    finished = true;
                    break;
            }
        } else if(value instanceof LongConstantTracedValue) {
            switch(insn.getOpcode()) {
                case LNEG:
                    result = ((LongConstantTracedValue) value).negate(size, insn);
                    finished = true;
                    break;
                case L2I:
                    result = ((LongConstantTracedValue) value).castToInt(size, insn);
                    finished = true;
                    break;
                case L2F:
                    result = ((LongConstantTracedValue) value).castToFloat(size, insn);
                    finished = true;
                    break;
                case L2D:
                    result = ((LongConstantTracedValue) value).castToDouble(size, insn);
                    finished = true;
                    break;
            }
        } else if(value instanceof FloatConstantTracedValue) {
            switch(insn.getOpcode()) {
                case FNEG:
                    result = ((FloatConstantTracedValue) value).negate(size, insn);
                    finished = true;
                    break;
                case F2I:
                    result = ((FloatConstantTracedValue) value).castToInt(size, insn);
                    finished = true;
                    break;
                case F2L:
                    result = ((FloatConstantTracedValue) value).castToLong(size, insn);
                    finished = true;
                    break;
                case F2D:
                    result = ((FloatConstantTracedValue) value).castToDouble(size, insn);
                    finished = true;
                    break;
            }
        } else if(value instanceof DoubleConstantTracedValue) {
            switch(insn.getOpcode()) {
                case DNEG:
                    result = ((DoubleConstantTracedValue) value).negate(size, insn);
                    finished = true;
                    break;
                case D2I:
                    result = ((DoubleConstantTracedValue) value).castToInt(size, insn);
                    finished = true;
                    break;
                case D2L:
                    result = ((DoubleConstantTracedValue) value).castToLong(size, insn);
                    finished = true;
                    break;
                case D2F:
                    result = ((DoubleConstantTracedValue) value).castToFloat(size, insn);
                    finished = true;
                    break;
            }
        }
        if(!finished) {
            result = value.newInstance(size, insn);
        }
        effectMap.put(insn, new InstructionEffect(value, result));
        return result;
    }

    @Override
    public TracedValue binaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2) {
        TracedValue result = null;
        boolean finished = false;
        int size = getSize(insn);
        switch(insn.getOpcode()) {
            case IALOAD:
            case LALOAD:
            case FALOAD:
            case DALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                result = new VariantTracedValueImpl(1, insn, new HashSet<>(containingLoopMap.get(insn)));
                effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2}, result));
                return result;
        }
        if(value1 instanceof IntegerConstantTracedValue && value2 instanceof IntegerConstantTracedValue) {
            switch(insn.getOpcode()) {
                case IADD:
                    result = ((IntegerConstantTracedValue) value1).add(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISUB:
                    result = ((IntegerConstantTracedValue) value1).subtract(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IMUL:
                    result = ((IntegerConstantTracedValue) value1).multiply(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IDIV:
                    result = ((IntegerConstantTracedValue) value1).divide(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IREM:
                    result = ((IntegerConstantTracedValue) value1).remainder(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISHL:
                    result = ((IntegerConstantTracedValue) value1).shiftLeft(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case ISHR:
                    result = ((IntegerConstantTracedValue) value1).shiftRight(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IUSHR:
                    result = ((IntegerConstantTracedValue) value1).shiftRightUnsigned(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IAND:
                    result = ((IntegerConstantTracedValue) value1).bitwiseAnd(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IOR:
                    result = ((IntegerConstantTracedValue) value1).bitwiseOr(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
                case IXOR:
                    result = ((IntegerConstantTracedValue) value1).bitwiseXor(size, insn, (IntegerConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof LongConstantTracedValue && value2 instanceof LongConstantTracedValue) {
            switch(insn.getOpcode()) {
                case LADD:
                    result = ((LongConstantTracedValue) value1).add(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSUB:
                    result = ((LongConstantTracedValue) value1).subtract(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LMUL:
                    result = ((LongConstantTracedValue) value1).multiply(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LDIV:
                    result = ((LongConstantTracedValue) value1).divide(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LREM:
                    result = ((LongConstantTracedValue) value1).remainder(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSHL:
                    result = ((LongConstantTracedValue) value1).shiftLeft(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LSHR:
                    result = ((LongConstantTracedValue) value1).shiftRight(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LUSHR:
                    result = ((LongConstantTracedValue) value1).shiftRightUnsigned(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LAND:
                    result = ((LongConstantTracedValue) value1).bitwiseAnd(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LOR:
                    result = ((LongConstantTracedValue) value1).bitwiseOr(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LXOR:
                    result = ((LongConstantTracedValue) value1).bitwiseXor(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
                case LCMP:
                    result = ((LongConstantTracedValue) value1).compare(size, insn, (LongConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof FloatConstantTracedValue && value2 instanceof FloatConstantTracedValue) {
            switch(insn.getOpcode()) {
                case FADD:
                    result = ((FloatConstantTracedValue) value1).add(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FSUB:
                    result = ((FloatConstantTracedValue) value1).subtract(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FMUL:
                    result = ((FloatConstantTracedValue) value1).multiply(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FDIV:
                    result = ((FloatConstantTracedValue) value1).divide(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FREM:
                    result = ((FloatConstantTracedValue) value1).remainder(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FCMPL:
                    result = ((FloatConstantTracedValue) value1).compareL(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
                case FCMPG:
                    result = ((FloatConstantTracedValue) value1).compareG(size, insn, (FloatConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof DoubleConstantTracedValue && value2 instanceof DoubleConstantTracedValue) {
            switch(insn.getOpcode()) {
                case DADD:
                    result = ((DoubleConstantTracedValue) value1).add(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DSUB:
                    result = ((DoubleConstantTracedValue) value1).subtract(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DMUL:
                    result = ((DoubleConstantTracedValue) value1).multiply(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DDIV:
                    result = ((DoubleConstantTracedValue) value1).divide(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DREM:
                    result = ((DoubleConstantTracedValue) value1).remainder(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DCMPL:
                    result = ((DoubleConstantTracedValue) value1).compareL(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
                case DCMPG:
                    result = ((DoubleConstantTracedValue) value1).compareG(size, insn, (DoubleConstantTracedValue) value2);
                    finished = true;
                    break;
            }
        } else if(value1 instanceof DependentTracedValue && value2 instanceof DependentTracedValue) {
            result = new DependentTracedValue(size, insn, (DependentTracedValue) value1, (DependentTracedValue) value2);
            finished = true;
        } else if(value1 instanceof DependentTracedValue && value2 instanceof ConstantTracedValue) {
            result = value1.newInstance(size, insn);
            finished = true;
        } else if(value1 instanceof ConstantTracedValue && value2 instanceof DependentTracedValue) {
            result = value2.newInstance(size, insn);
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
            result = new VariantTracedValueImpl(size, insn, containingLoops);
        }
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2}, result));
        return result;
    }

    @Override
    public TracedValue ternaryOperation(AbstractInsnNode insn, TracedValue value1, TracedValue value2, TracedValue value3) {
        TracedValue result = new VariantTracedValueImpl(1, insn, new HashSet<>(containingLoopMap.get(insn)));
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value1, value2, value3}, result));
        return result;
    }

    @Override
    public TracedValue naryOperation(AbstractInsnNode insn, List<? extends TracedValue> values) {
        TracedValue result = new VariantTracedValueImpl(getSize(insn), insn, new HashSet<>(containingLoopMap.get(insn)));
        effectMap.put(insn, new InstructionEffect(values.toArray(new TracedValue[0]), result));
        return result;
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, TracedValue value, TracedValue expected) {
        effectMap.put(insn, new InstructionEffect(new TracedValue[]{value}, null));
    }

    public void preFrameMerge(int insnIndex) {
        currentInsnIndex = insnIndex;
    }

    public void preVarMerge(int varIndex) {
        this.varIndex = varIndex;
    }

    @Override
    public TracedValue merge(TracedValue value1, TracedValue value2) {
        if(value1 == value2 || value1.equals(value2)) {
            return value1;
        } else if(value1 instanceof DependentTracedValue && value1.equals(value2)) {
            return value1;
        }
        int size = Math.min(value1.getSize(), value2.getSize());
        if(value1 instanceof ConstantTracedValue && value2 instanceof ConstantTracedValue) {
            if(((ConstantTracedValue) value1).canMerge((ConstantTracedValue) value2)) {
                return value1.newInstance(size, null);
            } else if(((ConstantTracedValue) value2).canMerge((ConstantTracedValue) value1)) {
                return value2.newInstance(size, null);
            }
        }
        AbstractInsnNode currentInsn = instructions.get(currentInsnIndex);
        if(value1 instanceof MergePointTracedValue && ((MergePointTracedValue) value1).contains(value2)) {
            return value1;
        } else if(value2 instanceof MergePointTracedValue && ((MergePointTracedValue) value2).contains(value1)) {
            return value2;
        } else if(value1 instanceof MergePointTracedValue && ((MergePointTracedValue) value1).isValueForMergePoint(currentInsn, varIndex)) {
            ((MergePointTracedValue) value1).add(value2);
            return value1;
        } else if(value2 instanceof MergePointTracedValue && ((MergePointTracedValue) value2).isValueForMergePoint(currentInsn, varIndex)) {
            ((MergePointTracedValue) value2).add(value1);
            return value2;
        }
        Set<NaturalLoop<BasicBlock>> containingLoops = new HashSet<>(containingLoopMap.get(currentInsn));
        if(value1.getInsnSource() == value2.getInsnSource() && value1.getInsnSource() != null) {
            return new VariantTracedValueImpl(size, value1.getInsnSource(), containingLoops);
        }
        MergePointTracedValue result = mergePointCache.getMergePointValue(size, containingLoops, currentInsn, varIndex);
        result.add(value1);
        result.add(value2);
        return result;
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
        Map<AbstractInsnNode, LoopLevel> levelMap = calculateLoopLevelMap();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(levelMap.containsKey(insn) && levelMap.get(insn) instanceof ConstantLoopLevel) {
                exclusions.add(insn);
            }
        }
        return exclusions;
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
            DefinitionChecker checker = new LocalVariableDefinitionChecker(effectMap, currentDefinition);
            gatherSources(checker, effect.sources[0], sources, new HashSet<>());
            return sources;
        } else if(isFieldStoreInsn(insn)) {
            DefinitionChecker checker = new FieldDefinitionChecker(effectMap, (FieldInsnNode) insn);
            Set<TracedValue> sources = new HashSet<>();
            if(insn.getOpcode() == PUTSTATIC) {
                gatherSources(checker, effect.sources[0], sources, new HashSet<>());
            } else {
                sources.add(effect.sources[0]); // Add the receiver as a source
                gatherSources(checker, effect.sources[1], sources, new HashSet<>(sources));
            }
            return sources;
        } else if(isArrayStoreInsn(insn)) {
            DefinitionChecker checker = new ArrayDefinitionChecker(effectMap, (InsnNode) insn);
            Set<TracedValue> sources = new HashSet<>();
            sources.add(effect.sources[0]); // Add the array reference as a source
            sources.add(effect.sources[1]); // Add the index as a source
            gatherSources(checker, effect.sources[2], sources, new HashSet<>(sources));
            return sources;
        }
        return new HashSet<>(Arrays.asList(effect.sources));
    }

    private void gatherSources(DefinitionChecker checker, TracedValue current, Set<TracedValue> sources, Set<TracedValue> visited) {
        if(visited.add(current)) {
            if(!checker.usesCurrentDefinition(current) && !sources.contains(current)) {
                AbstractInsnNode insn = current.getInsnSource();
                if(insn != null && isArithmeticOrLogicalInsn(insn) && effectMap.containsKey(insn)) {
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
        Set<NaturalLoop<BasicBlock>> containingVariantLoops = new HashSet<>(containingLoopMap.get(insn));
        containingVariantLoops.retainAll(variantLoops);
        return new VariantLoopLevel(containingVariantLoops.size());
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into a field
     */
    private static boolean isFieldStoreInsn(AbstractInsnNode insn) {
        return insn.getOpcode() == PUTFIELD || insn.getOpcode() == PUTSTATIC;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction loads a value from a field
     */
    private static boolean isFieldLoadInsn(AbstractInsnNode insn) {
        return insn.getOpcode() == GETFIELD || insn.getOpcode() == GETSTATIC;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into an array
     */
    private static boolean isArrayStoreInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= IASTORE && insn.getOpcode() <= SASTORE;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction loads a value from an array
     */
    private static boolean isArrayLoadInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= IALOAD && insn.getOpcode() <= SALOAD;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction stores a value into a local variable
     */
    private static boolean isLocalVariableStoreInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= ISTORE && insn.getOpcode() <= ASTORE;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction pushes a constant onto the runtime stack
     */
    private static boolean isPushConstantInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= ACONST_NULL && insn.getOpcode() <= LDC;
    }

    /**
     * @param insn the instruction being checked
     * @return true if the specified instruction performs an arithmetic or logical computation
     * runtime stack and pushes the result onto the runtime stack
     */
    private static boolean isArithmeticOrLogicalInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= IADD && insn.getOpcode() <= DCMPG;
    }

    private static boolean isSameValue(TracedValue value1, TracedValue value2, Map<AbstractInsnNode, InstructionEffect> effectMap) {
        if(value1.equals(value2)) {
            return true;
        } else if(value1 instanceof ConstantTracedValue && value2 instanceof ConstantTracedValue) {
            return ((ConstantTracedValue) value1).canMerge((ConstantTracedValue) value2);
        }
        AbstractInsnNode insn1 = value1.getInsnSource();
        AbstractInsnNode insn2 = value2.getInsnSource();
        if(insn1 != null && insn2 != null
                && insn1.getOpcode() == insn2.getOpcode()
                && effectMap.containsKey(insn1)
                && effectMap.containsKey(insn2)) {
            InstructionEffect effect1 = effectMap.get(insn1);
            InstructionEffect effect2 = effectMap.get(insn2);
            if(isArithmeticOrLogicalInsn(insn1)) {
                // Note: does not take into account commutative operations
                for(int i = 0; i < effect1.sources.length; i++) {
                    if(!isSameValue(effect1.sources[i], effect2.sources[i], effectMap)) {
                        return false;
                    }
                }
                return true;
            } else if(insn1.getOpcode() == ARRAYLENGTH) {
                // TODO: check for method calls and redefinitions along all paths between accesses
                return isSameValue(effect1.sources[0], effect2.sources[0], effectMap);
            } else if(isArrayLoadInsn(insn1)) {
                // TODO: check for method calls and redefinitions along all paths between accesses
                return isSameValue(effect1.sources[0], effect2.sources[0], effectMap)
                        && isSameValue(effect1.sources[1], effect2.sources[1], effectMap);
            } else if(isFieldLoadInsn(insn1)) {
                // TODO: check for method calls and redefinitions along all paths between accesses
                FieldInsnNode fieldInsn1 = (FieldInsnNode) insn1;
                FieldInsnNode fieldInsn2 = (FieldInsnNode) insn2;
                return fieldInsn1.owner.equals(fieldInsn2.owner) && fieldInsn1.name.equals(fieldInsn2.name)
                        && (insn1.getOpcode() == GETSTATIC || isSameValue(effect1.sources[0], effect2.sources[0], effectMap));

            }
        }
        return false;
    }

    private abstract static class DefinitionChecker {
        final Map<AbstractInsnNode, InstructionEffect> effectMap;

        DefinitionChecker(Map<AbstractInsnNode, InstructionEffect> effectMap) {
            this.effectMap = effectMap;
        }

        abstract boolean usesCurrentDefinition(TracedValue tracedValue);
    }

    private static class LocalVariableDefinitionChecker extends DefinitionChecker {

        private final TracedValue currentDefinition;

        LocalVariableDefinitionChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, TracedValue currentDefinition) {
            super(effectMap);
            this.currentDefinition = currentDefinition;
        }

        @Override
        boolean usesCurrentDefinition(TracedValue tracedValue) {
            return currentDefinition.equals(tracedValue);
        }
    }

    private static class FieldDefinitionChecker extends DefinitionChecker {

        private final FieldInsnNode fieldInsn;
        private final TracedValue receiver;

        FieldDefinitionChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, FieldInsnNode fieldInsn) {
            super(effectMap);
            this.fieldInsn = fieldInsn;
            if(fieldInsn.getOpcode() == PUTSTATIC) {
                receiver = null;
            } else {
                receiver = effectMap.get(fieldInsn).sources[0];
            }
        }

        @Override
        boolean usesCurrentDefinition(TracedValue tracedValue) {
            AbstractInsnNode insn = tracedValue.getInsnSource();
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

    private static class ArrayDefinitionChecker extends DefinitionChecker {

        private final TracedValue arrayReference;
        private final TracedValue index;

        ArrayDefinitionChecker(Map<AbstractInsnNode, InstructionEffect> effectMap, InsnNode insn) {
            super(effectMap);
            InstructionEffect effect = effectMap.get(insn);
            this.arrayReference = effect.sources[0];
            this.index = effect.sources[1];
        }

        @Override
        boolean usesCurrentDefinition(TracedValue tracedValue) {
            AbstractInsnNode insn = tracedValue.getInsnSource();
            if(insn != null && isArrayLoadInsn(insn) && effectMap.containsKey(insn)) {
                InstructionEffect effect = effectMap.get(insn);
                TracedValue otherArrayReference = effect.sources[0];
                TracedValue otherIndex = effect.sources[1];
                return isSameValue(arrayReference, otherArrayReference, effectMap) && isSameValue(index, otherIndex, effectMap);
            }
            return false;
        }
    }
}
