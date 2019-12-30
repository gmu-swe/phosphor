package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MergePointTracedValue extends VariantTracedValue {

    private final AbstractInsnNode mergePoint;
    private final int varIndex;
    private final Set<TracedValue> mergedValues;

    public MergePointTracedValue(int size, Set<FlowGraph.NaturalLoop<BasicBlock>> variantLoops, AbstractInsnNode mergePoint,
                                 int varIndex, TracedValue mergedValue1, TracedValue mergedValue2) {
        super(size, null, variantLoops);
        if(mergePoint == null) {
            throw new NullPointerException();
        }
        this.mergePoint = mergePoint;
        this.varIndex = varIndex;
        this.mergedValues = new HashSet<>();
        if(hasSameMergePoint(mergedValue1)) {
            mergedValues.addAll(((MergePointTracedValue) mergedValue1).mergedValues);
        } else {
            mergedValues.add(mergedValue1);
        }
        if(hasSameMergePoint(mergedValue2)) {
            mergedValues.addAll(((MergePointTracedValue) mergedValue2).mergedValues);
        } else {
            mergedValues.add(mergedValue2);
        }
    }

    public MergePointTracedValue(MergePointTracedValue other) {
        super(other.getSize(), null, other.getVariantLoops());
        this.mergePoint = other.mergePoint;
        this.varIndex = other.varIndex;
        this.mergedValues = new HashSet<>(other.mergedValues);
    }

    boolean contains(TracedValue other) {
        if(mergedValues.contains(other)) {
            return true;
        } else if(hasSameMergePoint(other)) {
            MergePointTracedValue otherValue = (MergePointTracedValue) other;
            return mergedValues.containsAll(otherValue.mergedValues);
        } else {
            return false;
        }
    }

    boolean hasSameMergePoint(TracedValue other) {
        if(other instanceof MergePointTracedValue) {
            MergePointTracedValue otherValue = (MergePointTracedValue) other;
            return otherValue.mergePoint == mergePoint && otherValue.varIndex == varIndex;
        }
        return false;
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode sourceInsn) {
        return new VariantTracedValueImpl(size, sourceInsn, getVariantLoops());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof MergePointTracedValue) || !super.equals(o)) {
            return false;
        }
        MergePointTracedValue that = (MergePointTracedValue) o;
        if(varIndex != that.varIndex) {
            return false;
        }
        if(!mergePoint.equals(that.mergePoint)) {
            return false;
        }
        return mergedValues.equals(that.mergedValues);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mergePoint.hashCode();
        result = 31 * result + varIndex;
        result = 31 * result + mergedValues.hashCode();
        return result;
    }

    public AbstractInsnNode getMergePoint() {
        return mergePoint;
    }

    public int getVarIndex() {
        return varIndex;
    }
}
