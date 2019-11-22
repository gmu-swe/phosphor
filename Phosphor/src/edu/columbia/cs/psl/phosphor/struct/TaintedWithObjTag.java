package edu.columbia.cs.psl.phosphor.struct;


public interface TaintedWithObjTag extends Tainted {
    Object getPHOSPHOR_TAG();

    void setPHOSPHOR_TAG(Object t);
}
