package edu.columbia.cs.psl.phosphor.struct;


public interface TaintedObjectWithObjCtrlTag extends TaintedWithObjTag {
	public TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedIntWithObjTag ret);
	public TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret);
}
