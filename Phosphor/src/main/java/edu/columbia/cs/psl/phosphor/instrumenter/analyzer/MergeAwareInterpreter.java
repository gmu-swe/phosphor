package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public abstract class MergeAwareInterpreter<V extends Value> extends Interpreter<V> {

    public MergeAwareInterpreter() {
        super(Configuration.ASM_VERSION);
    }

    public abstract void mergingFrame(int instructionIndexOfNextMerge);

    public abstract void mergingLocalVariable(int localIndexOfNextMerge, int numLocals, int stackSize);

    public abstract void mergingStackElement(int stackIndexOfNextMerge, int numLocals, int stackSize);
}
