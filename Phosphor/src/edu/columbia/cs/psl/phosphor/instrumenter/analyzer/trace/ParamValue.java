package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import org.objectweb.asm.Type;

public class ParamValue {
    private final boolean isInstanceMethod;
    private final int local;
    private final Type type;

    public ParamValue(boolean isInstanceMethod, int local, Type type) {
        this.isInstanceMethod = isInstanceMethod;
        this.local = local;
        this.type = type;
    }

    public boolean isInstanceMethod() {
        return isInstanceMethod;
    }

    public int getLocal() {
        return local;
    }

    public Type getType() {
        return type;
    }
}
