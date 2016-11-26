package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class TaintedByteWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {
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
		taint = (Taint) stream.readObject();
	}
	@Override
	public Object getValue() {
		return val;
	}

	static final TaintedByteWithObjTag cache[] = new TaintedByteWithObjTag[-(-128) + 127 + 1];

	static {
		for (int i = 0; i < cache.length; i++)
			cache[i] = new TaintedByteWithObjTag(null, (byte) (i - 128));
	}

	public static final TaintedByteWithObjTag valueOf(Taint taint, byte val) {
		final int offset = 128;
		if (taint == null)
			return cache[(int) val + offset];
		return new TaintedByteWithObjTag(taint, val);
	}

	public TaintedByteWithObjTag(Taint taint, byte val) {
		this.val = val;
		this.taint = taint;
	}

	public TaintedByteWithObjTag() {
		// TODO Auto-generated constructor stub
	}
}
