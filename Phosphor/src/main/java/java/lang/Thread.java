package java.lang;

import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;

public class Thread {
    public PhosphorStackFrame phosphorStackFrame;
    public static native Thread currentThread();

    public StackTraceElement[] getStackTrace() {
        return null;
    }
}
