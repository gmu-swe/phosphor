package edu.columbia.cs.psl.phosphor.struct;


import edu.columbia.cs.psl.phosphor.runtime.Taint;

public interface TaintedObjectWithObjTag extends TaintedWithObjTag {
    TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Taint t, TaintedIntWithObjTag ret);

    TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Taint t1, Object o, Taint ot, TaintedBooleanWithObjTag ret);
}