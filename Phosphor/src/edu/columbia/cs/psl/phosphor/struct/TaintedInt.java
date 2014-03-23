package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedInt extends TaintedPrimitive implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7831608795570286818L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readInt();
		taint = stream.readInt();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public int val;
	static final int cache_low = -127;
	static final int cache_high = 128;

	static final TaintedInt[] cache;
	static {
		cache = new TaintedInt[1 + cache_high - cache_low];
		int j = cache_low;
		for (int k = 0; k < cache.length; k++)
			cache[k] = new TaintedInt(0, j++);
	}

	public static final TaintedInt valueOf(int taint, int val) {
		if (taint == 0 && val >= cache_low && val <= cache_high)
			return cache[val + (-cache_low)];
		return new TaintedInt(taint, val);
	}

	public TaintedInt(int taint, int val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedInt() {
		// TODO Auto-generated constructor stub
	}
}
