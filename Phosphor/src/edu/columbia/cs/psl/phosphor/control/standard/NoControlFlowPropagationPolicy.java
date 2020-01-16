package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.control.AbstractControlFlowPropagationPolicy;
import org.objectweb.asm.Label;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;
import static org.objectweb.asm.Opcodes.*;

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
        switch(opcode) {
            case IFNULL:
            case IFNONNULL:
            case IFEQ:
            case IFNE:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFLT:
                delegate.visitInsn(POP);
                break;
            case IF_ICMPEQ:
            case IF_ICMPLE:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGT:
            case IF_ICMPGE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
                delegate.visitInsn(POP);
                delegate.visitInsn(SWAP);
                delegate.visitInsn(POP);
                break;
        }
    }

    @Override
    public void visitTableSwitch(int min, int max, Label defaultLabel, Label[] labels) {
        delegate.visitInsn(POP); // Remove the taint tag
    }

    @Override
    public void visitLookupSwitch(Label defaultLabel, int[] keys, Label[] labels) {
        delegate.visitInsn(POP); // Remove the taint tag
    }
}
