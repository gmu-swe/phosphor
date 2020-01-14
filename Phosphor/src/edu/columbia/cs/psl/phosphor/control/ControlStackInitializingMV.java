package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.control.binding.LoopLevel;
import edu.columbia.cs.psl.phosphor.control.binding.trace.LoopAwareConstancyInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager.CONTROL_STACK_INTERNAL_NAME;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static org.objectweb.asm.Opcodes.*;

public class ControlStackInitializingMV extends MethodVisitor {

    private LocalVariableManager localVariableManager;
    private PrimitiveArrayAnalyzer arrayAnalyzer;
    private LoopAwareConstancyInfo nextMethodFrameInfo = null;

    public ControlStackInitializingMV(MethodVisitor methodVisitor) {
        super(Configuration.ASM_VERSION, methodVisitor);
    }

    public void setArrayAnalyzer(PrimitiveArrayAnalyzer arrayAnalyzer) {
        this.arrayAnalyzer = arrayAnalyzer;
    }

    public void setLocalVariableManager(LocalVariableManager localVariableManager) {
        this.localVariableManager = localVariableManager;
    }

    /**
     * Creates a local variable for a {@link ControlFlowStack ControlFlowStack} instance.
     * Adds code to ensure that the ControlFlowStack instance is initialized and cast to the proper type.
     * Calls {@link ControlFlowPropagationPolicy#visitingCode()} visitingCode} to allow the policy to create any
     * local variables that it needs.
     * Adds a call to {@link ControlFlowStack#pushFrame() pushFrame} on the ControlFlowStack instance.
     */
    @Override
    public void visitCode() {
        super.visitCode();
        if(localVariableManager.getIndexOfMasterControlLV() < 0) {
            int tmpLV = localVariableManager.createMasterControlTaintLV();
            super.visitTypeInsn(NEW, Type.getInternalName(ControlTaintTagStack.class));
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "()V", false);
            super.visitVarInsn(ASTORE, tmpLV);
        } else {
            LocalVariableNode phosphorJumpControlTagIndex = new LocalVariableNode("phosphorJumpControlTag",
                    Type.getDescriptor(ControlTaintTagStack.class), null,
                    new LabelNode(localVariableManager.newStartLabel),
                    new LabelNode(localVariableManager.end),
                    localVariableManager.getIndexOfMasterControlLV()
            );
            localVariableManager.createdLVs.add(phosphorJumpControlTagIndex);
        }
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && arrayAnalyzer.getNumberOfTryCatch() > 0) {
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitVarInsn(ASTORE, localVariableManager.createControlExceptionTaintLV());
        }
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && arrayAnalyzer.getNumberOfThrows() > 0) {
            // Create a local variable for the exception data
            int exceptionTaintIndex = localVariableManager.createMasterExceptionTaintLV();
            super.visitTypeInsn(NEW, Type.getInternalName(ExceptionalTaintData.class));
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ExceptionalTaintData.class), "<init>", "()V", false);
            super.visitVarInsn(ASTORE, exceptionTaintIndex);
        }
        int numberOfBranchIDs = (arrayAnalyzer.getNumberOfJumps() + arrayAnalyzer.getNumberOfTryCatch() == 0) ? 0 : arrayAnalyzer.getNumberOfJumps() + arrayAnalyzer.getNumberOfTryCatch() + 2;
        if(!Configuration.IMPLICIT_HEADERS_NO_TRACKING && !Configuration.WITHOUT_PROPAGATION && numberOfBranchIDs > 0) {
            // Create a local variable for the array used to track tags pushed for each "branch" location
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitVarInsn(Opcodes.ASTORE, localVariableManager.createBranchesLV());
            // Push a frame for the method invocation
        }
        super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        CONTROL_STACK_PUSH_FRAME.delegateVisit(mv);
    }

    /**
     * If the method being called is not ignored by Phosphor and is passed a ControlFlowStack, adds code to prepares the
     * ControlFlowStack being passed to the call for the call.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        boolean copyStack = "<init>".equals(name);
        int controlStackDistance = methodNotIgnoredAndPassedControlStack(owner, name, descriptor);
        setUpStackForCall(controlStackDistance, copyStack, nextMethodFrameInfo != null);
        nextMethodFrameInfo = null;
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        String owner = bootstrapMethodHandle.getOwner();
        int controlStackDistance = methodNotIgnoredAndPassedControlStack(owner, name, descriptor);
        setUpStackForCall(controlStackDistance, false, nextMethodFrameInfo != null);
        nextMethodFrameInfo = null;
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    /**
     * @return distance of the control stack from the top or -1 if the specified method is ignored or is not passed
     * a control stack
     */
    private int methodNotIgnoredAndPassedControlStack(String owner, String name, String descriptor) {
        if(!Instrumenter.isIgnoredClass(owner) && !Instrumenter.isIgnoredMethod(owner, name, descriptor)) {
            Type[] args = Type.getArgumentTypes(descriptor);
            for(int dist = 0; dist < args.length; dist++) {
                Type arg = args[args.length - 1 - dist];
                if(arg.getInternalName().equals((CONTROL_STACK_INTERNAL_NAME))) {
                    return dist;
                }
            }
        }
        return -1;
    }

    private void setUpStackForCall(int controlStackDistance, boolean copy, boolean setInfo) {
        if(controlStackDistance == -1 || (!copy && !setInfo)) {
            return;
        }
        int[] temp = new int[controlStackDistance];
        for(int i = 0; i < temp.length; i++) {
            temp[i] = localVariableManager.getTmpLV();
            super.visitVarInsn(ASTORE, temp[i]); // Only reference types should be after the ControlTaintTagStack
        }
        if(copy) {
            CONTROL_STACK_COPY_TOP.delegateVisit(mv);
        }
        if(setInfo) {
            setFrameInfo();
        }
        for(int i = temp.length - 1; i >= 0; i--) {
            super.visitVarInsn(ALOAD, temp[i]);
            localVariableManager.freeTmpLV(temp[i]);
        }
    }

    // stack_pre: ControlTaintTagStack
    // stack_post: ControlTaintTagStack
    private void setFrameInfo() {
        // Start the frame and set the argument levels
        PropagatingControlFlowDelegator.push(mv, nextMethodFrameInfo.getInvocationLevel());
        PropagatingControlFlowDelegator.push(mv, nextMethodFrameInfo.getNumArguments());
        CONTROL_STACK_START_FRAME.delegateVisit(mv);
        Iterator<LoopLevel> argLevels = nextMethodFrameInfo.getLevelIterator();
        while(argLevels.hasNext()) {
            argLevels.next().setArgument(mv);
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof LoopAwareConstancyInfo) {
            nextMethodFrameInfo = (LoopAwareConstancyInfo) cst;
        } else {
            super.visitLdcInsn(cst);
        }
    }

    public static boolean isApplicable(boolean isImplicitLightTrackingMethod) {
        return Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING || isImplicitLightTrackingMethod;
    }
}
