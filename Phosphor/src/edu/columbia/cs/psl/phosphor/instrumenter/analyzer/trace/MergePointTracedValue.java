package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MergePointTracedValue extends TracedValue {

    private final AbstractInsnNode mergePoint;

    public MergePointTracedValue(int size, Set<AbstractInsnNode> sources, AbstractInsnNode mergePoint) {
        super(size, sources);
        this.mergePoint = mergePoint;
    }

    @Override
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return null;
    }
}
