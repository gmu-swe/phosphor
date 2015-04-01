package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedByteWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5518767062729812883L;
	public byte val;
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeByte(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readByte();
		taint = stream.readInt();
	}
	@Override
	public Object getValue() {
		return val;
	}

	static final TaintedByteWithIntTag cache[] = new TaintedByteWithIntTag[-(-128) + 127 + 1];

	static {
		for (int i = 0; i < cache.length; i++)
			cache[i] = new TaintedByteWithIntTag(0, (byte) (i - 128));
	}

	public static final TaintedByteWithIntTag valueOf(int taint, byte val) {
		final int offset = 128;
		if (taint == 0)
			return cache[(int) val + offset];
		return new TaintedByteWithIntTag(taint, val);
	}

	public TaintedByteWithIntTag(int taint, byte val) {
		this.val = val;
		this.taint = taint;
	}

	public TaintedByteWithIntTag() {
		// TODO Auto-generated constructor stub
	}
}
