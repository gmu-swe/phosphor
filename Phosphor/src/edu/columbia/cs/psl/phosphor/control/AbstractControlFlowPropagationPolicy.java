package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import org.objectweb.asm.Type;

public abstract class AbstractControlFlowPropagationPolicy implements ControlFlowPropagationPolicy {

    @Override
    public void visitingCode() {

    }

    @Override
    public void visitingMaxs() {

    }

    @Override
    public void prepareFrame() {

    }

    @Override
    public void visitingIncrement(int var, int shadowVar) {

    }

    @Override
    public void visitingLocalVariableStore(int opcode, int var) {

    }

    @Override
    public void visitingArrayStore() {

    }

    @Override
    public void visitingFieldStore(boolean isStatic, Type type, boolean topCarriesTaint) {

    }

    @Override
    public void onMethodExit(int opcode) {

    }

    @Override
    public void visitingPhosphorInstructionInfo(PhosphorInstructionInfo info) {

    }
}
