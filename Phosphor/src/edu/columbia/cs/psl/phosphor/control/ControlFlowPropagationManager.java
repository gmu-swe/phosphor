package edu.columbia.cs.psl.phosphor.control;

import edu.columbia.cs.psl.phosphor.instrumenter.MethodRecord;

public interface ControlFlowPropagationManager {

    MethodRecord getRecordForCreateEnabledStack();

    MethodRecord getRecordForGetSharedDisabledStack();

    ControlFlowStack createEnabledStack();

    ControlFlowStack getSharedDisabledStack();

    /**
     * @param access       the access flags of the method
     * @param owner        the internal name of the owner class of the method
     * @param name         the name of the method
     * @param descriptor   the descriptor of the method
     * @return a new ControlFlowPropagationPolicy for the specified method
     */
    ControlFlowPropagationPolicy createPropagationPolicy(int access, String owner, String name,
                                                         String descriptor);
}
