package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedCharWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 64664108579896882L;
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeChar(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readChar();
		taint = stream.readObject();
	}
	@Override
	public Object getValue() {
		return val;
	}
	public char val;

	static TaintedCharWithObjTag[] cache;
	static {
		cache = new TaintedCharWithObjTag[127 + 1];

		for (int i = 0; i < cache.length; i++)
			cache[i] = new TaintedCharWithObjTag(null, (char) i);
	}

	public static final TaintedCharWithObjTag valueOf(Object taint, char val) {
		if (taint == null && val <= 127) {
			return cache[(int) val];
		}
		return new TaintedCharWithObjTag(taint, val);
	}

	public TaintedCharWithObjTag(Object taint, char val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedCharWithObjTag() {
	}
}
