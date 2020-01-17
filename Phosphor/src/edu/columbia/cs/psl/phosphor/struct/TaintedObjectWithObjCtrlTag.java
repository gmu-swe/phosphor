package edu.columbia.cs.psl.phosphor.struct;


import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;

public interface TaintedObjectWithObjCtrlTag extends TaintedWithObjTag {
    TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(ControlFlowStack ctrl, TaintedIntWithObjTag ret);

    TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret);
}
