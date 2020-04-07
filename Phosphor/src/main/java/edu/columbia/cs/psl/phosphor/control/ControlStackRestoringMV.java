package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import jdk.internal.org.objectweb.asm.Type;
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
     * True if the ControlFlowStack should be disabled in this method.
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

    private final ControlFlowPropagationPolicy controlFlowPolicy;

    /**
     * Analyzer used to determine the number of exception handlers on the method being visited
     */
    private PrimitiveArrayAnalyzer arrayAnalyzer;

    /**
     * The number of exception handlers for the method being visited that have not yet been visited.
     */
    private int numberOfExceptionHandlersRemaining;

    public ControlStackRestoringMV(MethodVisitor methodVisitor, MethodVisitor passThroughMV, String className,
                                   String methodName, ControlFlowPropagationPolicy controlFlowPolicy) {
        super(Configuration.ASM_VERSION, methodVisitor);
        this.passThroughMV = passThroughMV;
        this.excludedFromControlTrack = Configuration.controlFlowManager.isIgnoredFromControlTrack(className, methodName);
        this.handlerScopeStart = new Label();
        this.handlerScopeEnd = new Label();
        this.controlFlowPolicy = controlFlowPolicy;
    }

    public void setArrayAnalyzer(PrimitiveArrayAnalyzer arrayAnalyzer) {
        this.arrayAnalyzer = arrayAnalyzer;
    }

    public void setLocalVariableManager(LocalVariableManager localVariableManager) {
        this.localVariableManager = localVariableManager;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        this.numberOfExceptionHandlersRemaining = arrayAnalyzer.getNumberOfTryCatch();
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
        if(OpcodesUtil.isReturnOpcode(opcode)) {
            // Note: ATHROWs are handled by added exception handler
            restoreControlStack();
        }
        super.visitInsn(opcode);
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
        int max = indexOfMasterControl;
        LocalVariable[] createdLocalVariables = controlFlowPolicy.createdLocalVariables();
        for(LocalVariable lv : createdLocalVariables) {
            if(lv.getIndex() > max) {
                max = lv.getIndex();
            }
        }
        Object[] baseLvs = new Object[max + 1];
        Arrays.fill(baseLvs, TOP);
        for(LocalVariable lv : createdLocalVariables) {
            baseLvs[lv.getIndex()] = lv.getTypeInternalName();
        }
        baseLvs[indexOfMasterControl] = Type.getInternalName(Configuration.controlFlowManager.getControlStackClass());
        super.visitFrame(F_NEW, baseLvs.length, baseLvs, 1, new Object[]{"java/lang/Throwable"});
        restoreControlStack();
        super.visitInsn(ATHROW);
        super.visitMaxs(maxStack, maxLocals);
    }

    private void restoreControlStack() {
        controlFlowPolicy.poppingFrame(passThroughMV);
        // Add call to pop frame
        passThroughMV.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        CONTROL_STACK_POP_FRAME.delegateVisit(passThroughMV);
        if(excludedFromControlTrack) {
            super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            CONTROL_STACK_ENABLE.delegateVisit(mv);
        }
    }

    public static boolean isApplicable(String methodName) {
        return Configuration.IMPLICIT_TRACKING && !"<init>".equals(methodName);
    }
}
