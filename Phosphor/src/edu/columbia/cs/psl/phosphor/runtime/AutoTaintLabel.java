package edu.columbia.cs.psl.phosphor.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class AutoTaintLabel {
	private String source;
	private StackTraceElement[] trace;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return source.hashCode();
	}

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
