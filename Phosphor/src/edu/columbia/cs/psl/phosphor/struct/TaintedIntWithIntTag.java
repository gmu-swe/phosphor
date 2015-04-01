package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedIntWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable {

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

	static final TaintedIntWithIntTag[] cache;
	static {
		cache = new TaintedIntWithIntTag[1 + cache_high - cache_low];
		int j = cache_low;
		for (int k = 0; k < cache.length; k++)
			cache[k] = new TaintedIntWithIntTag(0, j++);
	}

	public static final TaintedIntWithIntTag valueOf(int taint, int val) {
		if (taint == 0 && val >= cache_low && val <= cache_high)
			return cache[val + (-cache_low)];
		return new TaintedIntWithIntTag(taint, val);
	}

	public TaintedIntWithIntTag(int taint, int val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedIntWithIntTag() {
		// TODO Auto-generated constructor stub
	}
}
