package edu.columbia.cs.psl.phosphor;

public class NullSourceSinkManager extends SourceSinkManager{

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

}
