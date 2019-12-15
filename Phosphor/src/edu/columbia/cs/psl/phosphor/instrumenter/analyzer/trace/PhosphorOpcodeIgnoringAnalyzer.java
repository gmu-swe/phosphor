package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.*;

public class PhosphorOpcodeIgnoringAnalyzer<V extends Value> extends Analyzer<V> {

    public PhosphorOpcodeIgnoringAnalyzer(Interpreter<V> interpreter) {
        super(interpreter);
    }

    @Override
    protected Frame<V> newFrame(final int numLocals, final int numStack) {
        return new PhosphorOpcodeIgnoringFrame<>(numLocals, numStack);
    }

    @Override
    protected Frame<V> newFrame(final Frame<? extends V> frame) {
        return new PhosphorOpcodeIgnoringFrame<>(frame);
    }

    private static class PhosphorOpcodeIgnoringFrame<V extends Value> extends Frame<V> {

        public PhosphorOpcodeIgnoringFrame(int numLocals, int numStack) {
            super(numLocals, numStack);
        }

        public PhosphorOpcodeIgnoringFrame(Frame<? extends V> frame) {
            super(frame);
        }

        @Override
        public void execute(final AbstractInsnNode insn, final Interpreter<V> interpreter) throws AnalyzerException {
            if(insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof PhosphorInstructionInfo) {
                return;
            }
            int opcode = insn.getOpcode();
            if(opcode < TaintUtils.RAW_INSN) {
                super.execute(insn, interpreter);
            }
        }
    }
}

