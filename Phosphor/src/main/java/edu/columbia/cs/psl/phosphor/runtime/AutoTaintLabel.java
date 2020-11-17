package edu.columbia.cs.psl.phosphor.runtime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

public class AutoTaintLabel implements Serializable {
    private String source;
    private StackTraceElement[] trace;

    public AutoTaintLabel(String source, StackTraceElement[] stackTrace) {
        this.source = source;
        this.trace = stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        AutoTaintLabel that = (AutoTaintLabel) o;
        if(source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(trace, that.trace);
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(trace);
        return result;
    }

    public String getSource() {
        return source;
    }

    public StackTraceElement[] getTrace() {
        return trace;
    }

    @Override
    public String toString() {
        return "AutoTaintLabel [source=" + source + ", trace=" + Arrays.toString(trace) + "]";
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(source);
        stream.writeInt(trace == null ? -1 : trace.length);
        if(trace != null) {
            for(StackTraceElement element : trace) {
                stream.writeObject(element);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        source = (String) stream.readObject();
        int len = stream.readInt();
        if(len != -1) {
            trace = new StackTraceElement[len];
            for(int i = 0; i < len; i++) {
                trace[i] = (StackTraceElement) stream.readObject();
            }
        } else {
            trace = null;
        }
    }
}
