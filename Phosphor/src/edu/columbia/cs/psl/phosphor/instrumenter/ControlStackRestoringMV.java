package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class ControlStackRestoringMV extends MethodVisitor {

    /**
     * Visitor to which instruction visiting that needs to "pass through" the primary visitor is delegated.
     */
    private final MethodVisitor passThroughMV;

    /**
     * True if the ControlTaintTagStack should be disabled in this method.
     */
    private final boolean excludedFromControlTrack;

    /**
     * If an exception handler is to be added to the method being visited, the label marking the start of the exception
     * handler's scope. Otherwise, null.
     */
    private final Label handlerScopeStart;

    /**
     * If an exception handler is to be added to the method being visited, the label marking the end of the exception
     * handler's scope and the start of its code. Otherwise, null.
     */
    private final Label handlerScopeEnd;

    /**
     * Manager that handles freeing and allocating local variables.
     */
    private LocalVariableManager localVariableManager;

    /**
     * Analyzer used to determine the number of exception handlers on the method being visited
     */
    private PrimitiveArrayAnalyzer arrayAnalyzer;

    /**
     * The number of exception handlers for the method being visited that have not yet been visited.
     */
    private int numberOfExceptionHandlersRemaining;

    public ControlStackRestoringMV(MethodVisitor methodVisitor, MethodVisitor passThroughMV, String className, String methodName) {
        super(Configuration.ASM_VERSION, methodVisitor);
        this.passThroughMV = passThroughMV;
        this.excludedFromControlTrack = Instrumenter.isIgnoredFromControlTrack(className, methodName);
        this.handlerScopeStart = new Label();
        this.handlerScopeEnd = new Label();
    }

    void setArrayAnalyzer(PrimitiveArrayAnalyzer arrayAnalyzer) {
        this.arrayAnalyzer = arrayAnalyzer;
    }

    void setLocalVariableManager(LocalVariableManager localVariableManager) {
        this.localVariableManager = localVariableManager;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        this.numberOfExceptionHandlersRemaining = arrayAnalyzer.nTryCatch;
        if(excludedFromControlTrack) {
            super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            CONTROL_STACK_DISABLE.delegateVisit(mv);
        }
        if(numberOfExceptionHandlersRemaining == 0) {
            super.visitTryCatchBlock(handlerScopeStart, handlerScopeEnd, handlerScopeEnd, null);
            super.visitLabel(handlerScopeStart);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if(TaintUtils.isReturnOpcode(opcode)) {
            restoreControlStack();
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        numberOfExceptionHandlersRemaining--;
        if(numberOfExceptionHandlersRemaining == 0) {
            super.visitTryCatchBlock(handlerScopeStart, handlerScopeEnd, handlerScopeEnd, null);
            super.visitLabel(handlerScopeStart);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitLabel(handlerScopeEnd);
        int indexOfMasterControl = localVariableManager.getIndexOfMasterControlLV();
        int indexOfMasterException = localVariableManager.getIndexOfMasterExceptionLV();
        int indexOfBranches = localVariableManager.getIndexOfBranchesLV();
        int maxLV = TaintUtils.max(indexOfMasterControl, indexOfMasterException, indexOfBranches);
        Object[] baseLvs = new Object[maxLV + 1];
        Arrays.fill(baseLvs, TOP);
        baseLvs[localVariableManager.getIndexOfMasterControlLV()] = CONTROL_STACK_INTERNAL_NAME;
        if(indexOfMasterException >= 0) {
            baseLvs[indexOfMasterException] = MASTER_EXCEPTION_INTERNAL_NAME;
        }
        if(indexOfBranches > 0) {
            baseLvs[indexOfBranches] = BRANCHES_INTERNAL_NAME;
        }
        super.visitFrame(F_NEW, baseLvs.length, baseLvs, 1, new Object[]{"java/lang/Throwable"});
        restoreControlStack();
        super.visitInsn(ATHROW);
        super.visitMaxs(maxStack, maxLocals);
    }

    private void restoreControlStack() {
        if(localVariableManager.getIndexOfBranchesLV() >= 0) {
            callPopAll();
        }
        if(excludedFromControlTrack) {
            super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            CONTROL_STACK_ENABLE.delegateVisit(mv);
        }
    }

    /**
     * Pops all of the tags pushed onto the ControlTaintTagStack during the execution of the method
     * being visited off of the stack.
     */
    private void callPopAll() {
        if(localVariableManager.getIndexOfBranchesLV() >= 0) {
            passThroughMV.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            passThroughMV.visitVarInsn(ALOAD, localVariableManager.getIndexOfBranchesLV());
            if(localVariableManager.getIndexOfMasterExceptionLV() >= 0) {
                passThroughMV.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterExceptionLV());
                CONTROL_STACK_POP_ALL_EXCEPTION.delegateVisit(passThroughMV);
            } else {
                CONTROL_STACK_POP_ALL.delegateVisit(passThroughMV);
            }
        }
    }

    public static boolean isApplicable(String methodName) {
        return Configuration.IMPLICIT_TRACKING && !"<init>".equals(methodName);
    }
}
