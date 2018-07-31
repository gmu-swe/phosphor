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
		if (o == null || getClass() != o.getClass()) return false;

		AutoTaintLabel that = (AutoTaintLabel) o;

		if (source != null ? !source.equals(that.source) : that.source != null) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		return Arrays.equals(trace, that.trace);
	}

	@Override
	public int hashCode() {
		int result = source != null ? source.hashCode() : 0;
		result = 31 * result + Arrays.hashCode(trace);
		return result;
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
