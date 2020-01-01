package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.NEW_EMPTY_TAINT;
import static org.objectweb.asm.Opcodes.*;

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
    public void visitingSwitch() {
        // Remove the taint tag
        delegate.visitInsn(POP);
    }

    @Override
    public void storingReferenceInArray() {

    }

    @Override
    public void visitingExcludeRevisableBranches() {

    }
}
