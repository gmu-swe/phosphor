package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * Specifies that control flow should not propagate.
 */
public class NoFlowControlFlowDelegator implements ControlFlowDelegator {

    /**
     * Visitor to which instruction visiting is delegated.
     */
    private final MethodVisitor delegate;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private final LocalVariableManager localVariableManager;


    /**
     * True if the method being visited is a class initialization method (i.e.,  {@code <clinit>}).
     */
    private final boolean isClassInitializer;

    public NoFlowControlFlowDelegator(MethodVisitor delegate, LocalVariableManager localVariableManager, String methodName) {
        this.delegate = delegate;
        this.localVariableManager = localVariableManager;
        this.isClassInitializer = "<clinit>".equals(methodName);
    }

    @Override
    public void visitedCode() {
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            if(localVariableManager.idxOfMasterControlLV < 0) {
                int tmpLV = localVariableManager.createMasterControlTaintLV();
                delegate.visitTypeInsn(NEW, Type.getInternalName(ControlTaintTagStack.class));
                delegate.visitInsn(DUP);
                if(isClassInitializer) {
                    delegate.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "()V", false);
                }
                delegate.visitVarInsn(ASTORE, tmpLV);
            } else {
                LocalVariableNode phosphorJumpControlTagIndex = new LocalVariableNode("phosphorJumpControlTag",
                        Type.getDescriptor(ControlTaintTagStack.class), null,
                        new LabelNode(localVariableManager.start),
                        new LabelNode(localVariableManager.end),
                        localVariableManager.idxOfMasterControlLV
                );
                localVariableManager.createdLVs.add(phosphorJumpControlTagIndex);
            }
        }
    }

    @Override
    public void visitedTryCatch() {

    }

    @Override
    public void visitingIncrement(int var) {

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
    public void generateEmptyTaint() {
        delegate.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
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
        delegate.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
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
        if(Configuration.IMPLICIT_TRACKING ) {
            // Remove the taint tag
            delegate.visitInsn(SWAP);
            delegate.visitInsn(POP);
        }
    }
}
