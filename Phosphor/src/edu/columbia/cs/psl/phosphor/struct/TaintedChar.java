package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedChar extends TaintedPrimitive implements Serializable{
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

	static TaintedChar[] cache;
	static {
		cache = new TaintedChar[127 + 1];

		for (int i = 0; i < cache.length; i++)
			cache[i] = new TaintedChar(0, (char) i);
	}

	public static final TaintedChar valueOf(int taint, char val) {
		if (taint == 0 && val <= 127) {
			return cache[(int) val];
		}
		return new TaintedChar(taint, val);
	}

	public TaintedChar(int taint, char val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedChar() {
		// TODO Auto-generated constructor stub
	}
}
