package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.instrumenter.LocalVariableManager;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.CONTROL_STACK_COPY_TOP;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.CONTROL_STACK_PUSH_FRAME;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

public class ControlStackInitializingMV extends MethodVisitor {

    private LocalVariableManager localVariableManager;
    private final ControlFlowPropagationPolicy controlFlowPolicy;

    public ControlStackInitializingMV(MethodVisitor methodVisitor, ControlFlowPropagationPolicy controlFlowPolicy) {
        super(Configuration.ASM_VERSION, methodVisitor);
        this.controlFlowPolicy = controlFlowPolicy;
    }

    public void setLocalVariableManager(LocalVariableManager localVariableManager) {
        this.localVariableManager = localVariableManager;
    }

    /**
     * Creates a local variable for a {@link ControlFlowStack ControlFlowStack} instance.
     * Adds code to ensure that the ControlFlowStack instance is initialized and cast to the proper type.
     * Calls {@link ControlFlowPropagationPolicy#initializeLocalVariables(MethodVisitor)} visitingCode} to allow the policy to create any
     * local variables that it needs.
     * Adds a call to {@link ControlFlowStack#pushFrame() pushFrame} on the ControlFlowStack instance.
     */
    @Override
    public void visitCode() {
        super.visitCode();
        int currentIndex = localVariableManager.getIndexOfMasterControlLV();
        localVariableManager.createMasterControlStackLV();
        if(currentIndex == -1) {
            Configuration.controlFlowManager.visitCreateStack(mv, false);
        } else {
            super.visitVarInsn(ALOAD, currentIndex);
            super.visitTypeInsn(CHECKCAST, Type.getInternalName(Configuration.controlFlowManager.getControlStackClass()));
        }
        super.visitVarInsn(ASTORE, localVariableManager.getIndexOfMasterControlLV());
        super.visitVarInsn(ALOAD, localVariableManager.getIndexOfMasterControlLV());
        CONTROL_STACK_PUSH_FRAME.delegateVisit(mv);
        controlFlowPolicy.initializeLocalVariables(mv);
    }

    /**
     * If the method being called is not ignored by Phosphor and is passed a ControlFlowStack, adds code to prepares the
     * ControlFlowStack being passed to the call for the call.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        boolean copyStack = "<init>".equals(name);
        int controlStackDistance = methodNotIgnoredAndPassedControlStack(owner, name, descriptor);
        setUpStackForCall(controlStackDistance, copyStack);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        String owner = bootstrapMethodHandle.getOwner();
        int controlStackDistance = methodNotIgnoredAndPassedControlStack(owner, name, descriptor);
        setUpStackForCall(controlStackDistance, false);
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

    private void setUpStackForCall(int controlStackDistance, boolean copy) {
        if(controlStackDistance == -1) {
            return;
        }
        int[] temp = new int[controlStackDistance];
        for(int i = 0; i < temp.length; i++) {
            temp[i] = localVariableManager.getTmpLV();
            super.visitVarInsn(ASTORE, temp[i]); // Only reference types should be after the ControlFlowStack
        }
        if(copy) {
            CONTROL_STACK_COPY_TOP.delegateVisit(mv);
            super.visitTypeInsn(CHECKCAST, Type.getInternalName(Configuration.controlFlowManager.getControlStackClass()));
        }
        controlFlowPolicy.preparingFrame();
        for(int i = temp.length - 1; i >= 0; i--) {
            super.visitVarInsn(ALOAD, temp[i]);
            localVariableManager.freeTmpLV(temp[i]);
        }
    }

    public static boolean isApplicable(boolean isImplicitLightTrackingMethod) {
        return Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING || isImplicitLightTrackingMethod;
    }
}
