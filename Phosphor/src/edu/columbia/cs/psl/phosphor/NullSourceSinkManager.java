package edu.columbia.cs.psl.phosphor;

public class NullSourceSinkManager extends SourceSinkManager {

    @Override
    public boolean isSourceOrSinkOrTaintThrough(Class<?> clazz) {
        return false;
    }

    @Override
    public boolean isSource(String str) {
        return false;
    }

    @Override
    public boolean isSink(String str) {
        return false;
    }

    @Override
    public Object getLabel(String str) {
        return null;
    }

    @Override
    public boolean isTaintThrough(String str) {
        return false;
    }

    @Override
    public String getBaseSink(String str) {
        return null;
    }
}
