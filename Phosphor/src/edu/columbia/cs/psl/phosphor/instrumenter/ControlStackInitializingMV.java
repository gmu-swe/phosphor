package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ExitLoopLevelInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopAwareConstancyInfo;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.ConstantLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.DependentLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
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

    void setArrayAnalyzer(PrimitiveArrayAnalyzer arrayAnalyzer) {
        this.arrayAnalyzer = arrayAnalyzer;
    }

    void setLocalVariableManager(LocalVariableManager localVariableManager) {
        this.localVariableManager = localVariableManager;
    }

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
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && arrayAnalyzer.nTryCatch > 0) {
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitVarInsn(ASTORE, localVariableManager.createControlExceptionTaintLV());
        }
        if(Configuration.IMPLICIT_EXCEPTION_FLOW && arrayAnalyzer.nThrow > 0) {
            // Create a local variable for the exception data
            int exceptionTaintIndex = localVariableManager.createMasterExceptionTaintLV();
            super.visitTypeInsn(NEW, Type.getInternalName(ExceptionalTaintData.class));
            super.visitInsn(DUP);
            super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ExceptionalTaintData.class), "<init>", "()V", false);
            super.visitVarInsn(ASTORE, exceptionTaintIndex);
        }
        int numberOfBranchIDs = (arrayAnalyzer.nJumps + arrayAnalyzer.nTryCatch == 0) ? 0 : arrayAnalyzer.nJumps + arrayAnalyzer.nTryCatch + 2;
        if(!Configuration.IMPLICIT_HEADERS_NO_TRACKING && !Configuration.WITHOUT_PROPAGATION && numberOfBranchIDs > 0) {
            // Create a local variable for the array used to track tags pushed for each "branch" location
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitVarInsn(Opcodes.ASTORE, localVariableManager.createBranchesLV());
            // Push a frame for the method invocation
            super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
            CONTROL_STACK_PUSH_FRAME.delegateVisit(mv);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(descriptor);
        if(args.length >= 2 && args[args.length - 2].getInternalName().equals(CONTROL_STACK_INTERNAL_NAME)) {
            if(nextMethodFrameInfo != null) {
                setFrameInfo();
            }
            if("<init>".equals(name)) {
                // Stack = ControlTaintTagStack TaintSentinel
                super.visitInsn(SWAP);
                CONTROL_STACK_COPY_TOP.delegateVisit(mv);
                super.visitInsn(SWAP);
            }
        }
        nextMethodFrameInfo = null;
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        Type[] args = Type.getArgumentTypes(descriptor);
        if(nextMethodFrameInfo != null && args.length >= 2 && args[args.length - 2].getInternalName().equals(CONTROL_STACK_INTERNAL_NAME)) {
            setFrameInfo();
        }
        nextMethodFrameInfo = null;
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    private void setFrameInfo() {
        // Start the frame and set the argument levels
        super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        PropagatingControlFlowDelegator.push(mv, nextMethodFrameInfo.getInvocationLevel());
        PropagatingControlFlowDelegator.push(mv, nextMethodFrameInfo.getNumArguments());
        CONTROL_STACK_START_FRAME.delegateVisit(mv);
        Iterator<LoopLevel> argLevels = nextMethodFrameInfo.getLevelIterator();
        while(argLevels.hasNext()) {
            LoopLevel argLevel = argLevels.next();
            if(argLevel instanceof ConstantLoopLevel) {
                CONTROL_STACK_SET_ARG_CONSTANT.delegateVisit(mv);
            } else if(argLevel instanceof DependentLoopLevel) {
                int[] dependencies = ((DependentLoopLevel) argLevel).getDependencies();
                // Make the dependencies array
                PropagatingControlFlowDelegator.push(mv, dependencies.length);
                super.visitIntInsn(NEWARRAY, T_INT);
                for(int i = 0; i < dependencies.length; i++) {
                    super.visitInsn(DUP); // Duplicate the array
                    PropagatingControlFlowDelegator.push(mv, i); // Push the index
                    PropagatingControlFlowDelegator.push(mv, dependencies[i]); // Push the dependency value
                    super.visitInsn(IASTORE);
                }
                CONTROL_STACK_SET_ARG_DEPENDENT.delegateVisit(mv);
            } else if(argLevel instanceof VariantLoopLevel) {
                PropagatingControlFlowDelegator.push(mv, ((VariantLoopLevel) argLevel).getLevelOffset());
                CONTROL_STACK_SET_ARG_VARIANT.delegateVisit(mv);
            }
        }
        super.visitInsn(POP); // Pop the ControlTaintTagStack off the runtime stack
    }

    private void exitLoopLevel(ExitLoopLevelInfo insn) {
        super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        PropagatingControlFlowDelegator.push(mv, insn.getLevelOffset());
        CONTROL_STACK_EXIT_LOOP_LEVEL.delegateVisit(mv);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof ExitLoopLevelInfo) {
            exitLoopLevel((ExitLoopLevelInfo) cst);
        } else if(cst instanceof LoopAwareConstancyInfo) {
            nextMethodFrameInfo = (LoopAwareConstancyInfo) cst;
        } else {
            super.visitLdcInsn(cst);
        }
    }

    public static boolean isApplicable(boolean isImplicitLightTrackingMethod) {
        return Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING || isImplicitLightTrackingMethod;
    }
}
