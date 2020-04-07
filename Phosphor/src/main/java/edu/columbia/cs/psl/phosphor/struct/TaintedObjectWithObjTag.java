package edu.columbia.cs.psl.phosphor.struct;


import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public interface TaintedObjectWithObjTag extends TaintedWithObjTag {
    default TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Taint<?> refTaint, TaintedIntWithObjTag ret) {
        ret.val = this.hashCode();
        ret.taint = refTaint;
        return ret;
    }

    default TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Taint<?> refTaint, Object o, Taint<?> t, TaintedBooleanWithObjTag ret) {
        ret.val = this.equals(o);
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    default TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Taint<?> refTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = this.hashCode();
        ret.taint = refTaint;
        return ret;
    }

    default TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Taint<?> refTaint, Object o, Taint<?> t, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        ret.val = this.equals(o);
        ret.taint = Taint.emptyTaint();
        return ret;
    }
}