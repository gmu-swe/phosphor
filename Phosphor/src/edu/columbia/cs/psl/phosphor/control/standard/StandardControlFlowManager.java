package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.CONTROL_STACK_FACTORY;

public class StandardControlFlowManager implements ControlFlowManager {

    @Override
    public void visitCreateStack(MethodVisitor mv, boolean disabled) {
        mv.visitInsn(disabled ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        CONTROL_STACK_FACTORY.delegateVisit(mv);
    }

    @Override
    public ControlTaintTagStack getStack(boolean disabled) {
        return ControlTaintTagStack.factory(disabled);
    }

    @Override
    public ControlFlowPropagationPolicy createPropagationPolicy(int access, String owner, String name, String descriptor) {
        boolean isImplicitLightTrackingMethod = TaintTrackingClassVisitor.isImplicitLightMethod(owner, name, descriptor);
        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTrackingMethod) && !Configuration.WITHOUT_PROPAGATION) {
            StandardControlFlowAnalyzer flowAnalyzer = new StandardControlFlowAnalyzer(isImplicitLightTrackingMethod);
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            return new StandardControlFlowPropagationPolicy(flowAnalyzer, isStatic, descriptor);
        } else {
            return new NoControlFlowPropagationPolicy(new NoControlFlowAnalyzer());
        }
    }
}
