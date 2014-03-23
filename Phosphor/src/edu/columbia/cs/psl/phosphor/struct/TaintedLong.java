package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedLong extends TaintedPrimitive implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2036116913949916760L;

	@Override
	public Object getValue() {
		return val;
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeLong(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readLong();
		taint = stream.readInt();
	}

	public long val;

	public static final TaintedLong valueOf(int taint, long val) {
		return new TaintedLong(taint, val);
	}

	public TaintedLong(int taint, long val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedLong() {

	}
}
