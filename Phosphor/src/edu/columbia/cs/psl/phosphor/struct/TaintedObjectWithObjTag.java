package edu.columbia.cs.psl.phosphor.struct;


public interface TaintedObjectWithObjTag extends TaintedWithObjTag {
    TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret);

    TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o, TaintedBooleanWithObjTag ret);
}