package edu.columbia.cs.psl.phosphor.control.binding.trace;

import edu.columbia.cs.psl.phosphor.control.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.control.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MergePointTracedValue extends VariantTracedValue {

    private final AbstractInsnNode mergePoint;
    private final int varIndex;
    private final Set<TracedValue> mergedValues;

    private MergePointTracedValue(int size, Set<FlowGraph.NaturalLoop<BasicBlock>> variantLoops, AbstractInsnNode mergePoint,
                                  int varIndex) {
        super(size, null, variantLoops);
        this.mergePoint = mergePoint;
        this.varIndex = varIndex;
        this.mergedValues = new HashSet<>();
    }

    boolean contains(TracedValue value) {
        return mergedValues.contains(value);
    }

    void add(TracedValue value) {
        mergedValues.add(value);
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode sourceInsn) {
        return new VariantTracedValueImpl(size, sourceInsn, getVariantLoops());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof MergePointTracedValue)) {
            return false;
        }
        MergePointTracedValue that = (MergePointTracedValue) o;
        if(varIndex != that.varIndex) {
            return false;
        }
        return mergePoint.equals(that.mergePoint);
    }

    @Override
    public int hashCode() {
        int result = mergePoint.hashCode();
        result = 31 * result + varIndex;
        return result;
    }

    public AbstractInsnNode getMergePoint() {
        return mergePoint;
    }

    public int getVarIndex() {
        return varIndex;
    }

    public boolean isValueForMergePoint(AbstractInsnNode mergePoint, int varIndex) {
        return this.varIndex == varIndex && this.mergePoint == mergePoint;
    }

    static class MergePointValueCache {

        private final Map<MergePointTracedValue, MergePointTracedValue> cache = new HashMap<>();

        MergePointTracedValue getMergePointValue(int size, Set<FlowGraph.NaturalLoop<BasicBlock>> variantLoops,
                                                 AbstractInsnNode mergePoint, int varIndex) {
            if(mergePoint == null) {
                throw new NullPointerException();
            }
            MergePointTracedValue value = new MergePointTracedValue(size, variantLoops, mergePoint, varIndex);
            if(!cache.containsKey(value)) {
                cache.put(value, value);
            }
            return cache.get(value);
        }
    }
}
