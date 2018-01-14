package edu.columbia.cs.psl.phosphor.runtime;

import java.util.Arrays;

public class AutoTaintLabel {
	private String source;
	private StackTraceElement[] trace;

	public AutoTaintLabel(String source, StackTraceElement[] stackTrace) {
		this.source = source;
		this.trace = stackTrace;
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
	
}
