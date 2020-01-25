package edu.columbia.cs.psl.phosphor.control.binding.trace;

import edu.columbia.cs.psl.phosphor.control.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.control.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

public class VariantTracedValueImpl extends VariantTracedValue {

    VariantTracedValueImpl(int size, AbstractInsnNode sourceInsn, Set<FlowGraph.NaturalLoop<BasicBlock>> variantLoops) {
        super(size, sourceInsn, variantLoops);
    }

    @Override
    TracedValue newInstance(int size, AbstractInsnNode sourceInsn) {
        return new VariantTracedValueImpl(size, sourceInsn, getVariantLoops());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else {
            return o instanceof VariantTracedValueImpl && super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
