package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BranchStartInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.CopyTagInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ExitLoopLevelInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopAwarePopInfo;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

public abstract class AbstractControlFlowDelegator implements ControlFlowDelegator {

    @Override
    public void visitingIncrement(int var, int shadowVar) {

    }

    @Override
    public void visitingBranchStart(int branchID) {

    }

    @Override
    public void visitingBranchEnd(int branchID) {

    }

    @Override
    public void storingTaintedValue(int opcode, int var) {

    }

    @Override
    public void visitingForceControlStoreField(Field field) {

    }

    @Override
    public void visitingPutField(boolean isStatic, Type type, boolean topCarriesTaint) {

    }

    @Override
    public void visitingExceptionHandlerStart(String type) {

    }

    @Override
    public void visitingExceptionHandlerEnd(String type) {

    }

    @Override
    public void visitingUnthrownException(String type) {

    }

    @Override
    public void visitingUnthrownExceptionCheck(String type) {

    }

    @Override
    public void onMethodExit(int opcode) {

    }

    @Override
    public void visitingForceControlStore(Type stackTop) {

    }

    @Override
    public void visitingArrayStore() {

    }

    @Override
    public void visitingExitLoopLevelInfo(ExitLoopLevelInfo info) {

    }

    @Override
    public void visitingLoopAwarePop(LoopAwarePopInfo info) {

    }

    @Override
    public void visitingBranchStart(BranchStartInfo info) {

    }

    @Override
    public void visitingCopyTagInfo(CopyTagInfo info) {

    }

    @Override
    public void visitingMaxs() {

    }

    @Override
    public void visitingLabel(Label label) {

    }

    @Override
    public void visitingFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {

    }
}
