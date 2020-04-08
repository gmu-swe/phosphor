package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public abstract class ControlFlowStack {

    /**
     * 0 if copying and pushing tags is enabled for this stack, otherwise > 0
     */
    private int disabled;

    public ControlFlowStack(boolean disabled) {
        this.disabled = disabled ? 1 : 0;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.CONTROL_STACK_ENABLE)
    public void enable() {
        disabled--;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.CONTROL_STACK_DISABLE)
    public void disable() {
        disabled++;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TOP)
    public abstract ControlFlowStack copyTop();

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TOP)
    public abstract void reset();

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_FRAME)
    public abstract void pushFrame();

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_FRAME)
    public abstract void popFrame();

    @InvokedViaInstrumentation(record = CONTROL_STACK_UNINSTRUMENTED_WRAPPER)
    public void enteringUninstrumentedWrapper() {

    }

    public boolean isDisabled() {
        return disabled != 0;
    }

    /**
     * Used by Phosphor masking classes to retrieve the control taint tag
     *
     * @return the current control taint tag
     */
    public abstract Taint<?> copyTag();
}
