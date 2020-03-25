package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;


import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.analysis.Analyzer;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.analysis.Subroutine;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

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

    @Override
    public Frame<V>[] analyze(String owner, MethodNode method) throws AnalyzerException {
        return super.analyze(owner, method);
    }

    @Override
    protected void merge(int insnIndex, Frame<V> frame, Subroutine subroutine) throws AnalyzerException {
        if(interpreter instanceof MergeAwareInterpreter) {
            ((MergeAwareInterpreter<V>) interpreter).mergingFrame(insnIndex);
        }
        super.merge(insnIndex, frame, subroutine);
    }

    @Override
    protected void merge(int insnIndex, Frame<V> frameBeforeJsr, Frame<V> frameAfterRet, Subroutine subroutineBeforeJsr, boolean[] localsUsed) throws AnalyzerException {
        if(interpreter instanceof MergeAwareInterpreter) {
            ((MergeAwareInterpreter<V>) interpreter).mergingFrame(insnIndex);
        }
        super.merge(insnIndex, frameBeforeJsr, frameAfterRet, subroutineBeforeJsr, localsUsed);
    }

    public static class PhosphorOpcodeIgnoringFrame<V extends Value> extends Frame<V> {

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

        @Override
        public boolean merge(final Frame<? extends V> frame, final Interpreter<V> interpreter)
                throws AnalyzerException {
            if(getStackSize() != frame.getStackSize()) {
                throw new AnalyzerException(null, "Incompatible stack heights");
            }
            boolean changed = false;
            for(int i = 0; i < getLocals(); i++) {
                if(interpreter instanceof MergeAwareInterpreter) {
                    ((MergeAwareInterpreter<V>) interpreter).mergingLocalVariable(i, getLocals(), getStackSize());
                }
                V v = interpreter.merge(getLocal(i), frame.getLocal(i));
                if(!v.equals(getLocal(i))) {
                    setLocal(i, v);
                    changed = true;
                }
            }
            for(int i = 0; i < getStackSize(); i++) {
                if(interpreter instanceof MergeAwareInterpreter) {
                    ((MergeAwareInterpreter<V>) interpreter).mergingStackElement(i, getLocals(), getStackSize());
                }
                V v = interpreter.merge(getStack(i), frame.getStack(i));
                if(!v.equals(getStack(i))) {
                    setStack(i, v);
                    changed = true;
                }
            }
            return changed;
        }
    }
}

