package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

public class TaggedValue {
    public Object v;

    public TaggedValue(Object v) {
        if(v instanceof TaggedValue) {
            this.v = ((TaggedValue) v).v;
        } else {
            this.v = v;
        }
    }

    @Override
    public String toString() {
        return "T:" + v;
    }
}
