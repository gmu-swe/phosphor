package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.control.AbstractControlFlowPropagationPolicy;
import org.objectweb.asm.Label;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;

/**
 * Specifies that control flow should not propagate.
 */
public class NoControlFlowPropagationPolicy extends AbstractControlFlowPropagationPolicy<NoControlFlowAnalyzer> {

    public NoControlFlowPropagationPolicy(NoControlFlowAnalyzer flowAnalyzer) {
        super(flowAnalyzer);
    }

    @Override
    public void generateEmptyTaint() {
        NEW_EMPTY_TAINT.delegateVisit(delegate);
    }

    @Override
    public void visitingJump(int opcode, Label label) {

    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
    }
}
