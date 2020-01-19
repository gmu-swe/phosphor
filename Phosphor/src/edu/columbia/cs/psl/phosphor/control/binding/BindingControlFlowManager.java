package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static edu.columbia.cs.psl.phosphor.control.binding.BindingMethodRecord.BINDING_CONTROL_STACK_FACTORY;

public class BindingControlFlowManager implements ControlFlowManager {

    @Override
    public Class<? extends ControlFlowStack> getControlStackClass() {
        return BindingControlFlowStack.class;
    }

    @Override
    public void visitCreateStack(MethodVisitor mv, boolean disabled) {
        mv.visitInsn(disabled ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        BINDING_CONTROL_STACK_FACTORY.delegateVisit(mv);
    }

    @Override
    public ControlFlowStack getStack(boolean disabled) {
        return BindingControlFlowStack.factory(disabled);
    }

    @Override
    public ControlFlowPropagationPolicy createPropagationPolicy(int access, String owner, String name, String descriptor) {
        return new BindingControlFlowPropagationPolicy(new BindingControlFlowAnalyzer());
    }
}
