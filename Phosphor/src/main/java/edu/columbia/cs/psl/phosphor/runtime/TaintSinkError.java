package edu.columbia.cs.psl.phosphor.runtime;

public class TaintSinkError extends IllegalAccessError {

    private static final long serialVersionUID = 1162066978522069763L;

    private Taint<?> taint;
    private Object obj;

    public TaintSinkError(Taint<?> taint, Object val) {
        super(taint + " flowed to sink! Value: " + val);
        this.taint = taint;
        this.obj = val;
    }
}
