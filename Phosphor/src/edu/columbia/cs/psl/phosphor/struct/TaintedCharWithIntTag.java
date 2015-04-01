package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedCharWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 64664108579896882L;
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeChar(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readChar();
		taint = stream.readInt();
	}
	@Override
	public Object getValue() {
		return val;
	}
	public char val;

	static TaintedCharWithIntTag[] cache;
	static {
		cache = new TaintedCharWithIntTag[127 + 1];

		for (int i = 0; i < cache.length; i++)
			cache[i] = new TaintedCharWithIntTag(0, (char) i);
	}

	public static final TaintedCharWithIntTag valueOf(int taint, char val) {
		if (taint == 0 && val <= 127) {
			return cache[(int) val];
		}
		return new TaintedCharWithIntTag(taint, val);
	}

	public TaintedCharWithIntTag(int taint, char val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedCharWithIntTag() {
		// TODO Auto-generated constructor stub
	}
}
