package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.SWAP;

/**
 * Specifies that control flow should not propagate.
 */
public class NoFlowControlFlowDelegator implements ControlFlowDelegator {

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    public NoFlowControlFlowDelegator(MethodVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void visitedCode() {

    }

    @Override
    public void visitedTryCatch() {

    }

    @Override
    public void visitingIncrement(int var) {

    }

    @Override
    public void visitingBranchStart(int branchID, boolean revisable) {

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
    public void generateEmptyTaint() {
        NEW_EMPTY_TAINT.delegateVisit(delegate);
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
    public void visitingTrackedInstanceOf() {
        NEW_EMPTY_TAINT.delegateVisit(delegate);
        delegate.visitInsn(SWAP);
    }

    @Override
    public void visitingMaxs(int maxStack, int maxLocals) {

    }

    @Override
    public void onMethodExit(int opcode) {

    }

    @Override
    public void visitingForceControlStore(Type stackTop) {

    }

    @Override
    public void visitingJump(int opcode) {

    }

    @Override
    public void visitingSwitch() {
        if(Configuration.IMPLICIT_TRACKING) {
            // Remove the taint tag
            delegate.visitInsn(SWAP);
            delegate.visitInsn(POP);
        }
    }

    @Override
    public void storingReferenceInArray() {

    }

    @Override
    public void visitingExcludeRevisableBranches() {

    }
}
