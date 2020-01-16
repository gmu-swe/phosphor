package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationManager;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.control.binding.BindingControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.control.binding.BindingControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecord;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import org.objectweb.asm.Opcodes;

public enum StandardControlFlowPropagationManager implements ControlFlowPropagationManager {

    INSTANCE;

    @Override
    public MethodRecord getRecordForCreateEnabledStack() {
        return null;
    }

    @Override
    public MethodRecord getRecordForGetSharedDisabledStack() {
        return null;
    }

    @Override
    public ControlFlowStack createEnabledStack() {
        return null;
    }

    @Override
    public ControlFlowStack getSharedDisabledStack() {
        return null;
    }

    @Override
    public ControlFlowPropagationPolicy createPropagationPolicy(int access, String owner, String name, String descriptor) {
        boolean isImplicitLightTrackingMethod = TaintTrackingClassVisitor.isImplicitLightMethod(owner, name, descriptor);
        if(Configuration.BINDING_CONTROL_FLOWS_ONLY) {
            return new BindingControlFlowPropagationPolicy(new BindingControlFlowAnalyzer());
        } else if((Configuration.IMPLICIT_TRACKING || isImplicitLightTrackingMethod) && !Configuration.WITHOUT_PROPAGATION) {
            StandardControlFlowAnalyzer flowAnalyzer = new StandardControlFlowAnalyzer(isImplicitLightTrackingMethod);
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            return new StandardControlFlowPropagationPolicy(flowAnalyzer, isStatic, descriptor);
        } else {
            return new NoControlFlowPropagationPolicy(new NoControlFlowAnalyzer());
        }
    }
}
