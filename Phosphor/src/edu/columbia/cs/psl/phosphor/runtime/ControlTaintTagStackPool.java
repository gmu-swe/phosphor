package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;

public class ControlTaintTagStackPool {

    @InvokedViaInstrumentation(record = TaintMethodRecord.CONTROL_STACK_POOL_INSTANCE)
    public static ControlTaintTagStack instance() {
        return new ControlTaintTagStack();
    }
}
