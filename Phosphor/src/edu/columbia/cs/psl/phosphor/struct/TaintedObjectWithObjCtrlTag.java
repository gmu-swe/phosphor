package edu.columbia.cs.psl.phosphor.struct;


public interface TaintedObjectWithObjCtrlTag extends TaintedWithObjTag {
    TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedIntWithObjTag ret);

    TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret);
}
