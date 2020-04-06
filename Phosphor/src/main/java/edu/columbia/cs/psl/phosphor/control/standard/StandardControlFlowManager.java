package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static edu.columbia.cs.psl.phosphor.control.standard.ControlMethodRecord.STANDARD_CONTROL_STACK_FACTORY;

public class StandardControlFlowManager implements ControlFlowManager {

    @Override
    public Class<? extends ControlFlowStack> getControlStackClass() {
        return StandardControlFlowStack.class;
    }

    @Override
    public void visitCreateStack(MethodVisitor mv, boolean disabled) {
        mv.visitInsn(disabled ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        STANDARD_CONTROL_STACK_FACTORY.delegateVisit(mv);
    }

    @Override
    public ControlFlowStack getStack(boolean disabled) {
        return StandardControlFlowStack.factory(disabled);
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

    @Override
    public boolean isIgnoredFromControlTrack(String className, String methodName) {
        return (className.equals("java/nio/charset/Charset")
                || className.equals("java/lang/StringCoding")
                || className.equals("java/nio/charset/CharsetEncoder")
                || className.equals("java/nio/charset/CharsetDecoder"))
                && !methodName.equals("<clinit>") && !methodName.equals("<init>");
    }
}
