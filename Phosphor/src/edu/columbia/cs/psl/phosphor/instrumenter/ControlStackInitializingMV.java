package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import static org.objectweb.asm.Opcodes.*;

public class ControlStackInitializingMV extends MethodVisitor {

    private final boolean isImplicitLightTrackingMethod;
    private LocalVariableManager localVariableManager;
    private PrimitiveArrayAnalyzer arrayAnalyzer;

    public ControlStackInitializingMV(MethodVisitor methodVisitor, boolean isImplicitLightTrackingMethod) {
        super(Configuration.ASM_VERSION, methodVisitor);
        this.isImplicitLightTrackingMethod = isImplicitLightTrackingMethod;
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
        }
    }

    public static boolean isApplicable(boolean isImplicitLightTrackingMethod) {
        return Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING || isImplicitLightTrackingMethod;
    }
}
