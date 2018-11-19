package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class TaintedIntWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7831608795570286818L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readInt();
		taint = (Taint) stream.readObject();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public int val;
	static final int cache_low = -127;
	static final int cache_high = 128;

	static final TaintedIntWithObjTag[] cache;
	static {
		cache = new TaintedIntWithObjTag[1 + cache_high - cache_low];
		int j = cache_low;
		for (int k = 0; k < cache.length; k++)
			cache[k] = new TaintedIntWithObjTag(null, j++);
	}

	public static final TaintedIntWithObjTag valueOf(Taint taint, int val) {
		if (taint == null && val >= cache_low && val <= cache_high)
			return cache[val + (-cache_low)];
		return new TaintedIntWithObjTag(taint, val);
	}

	public TaintedIntWithObjTag(Taint taint, int val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedIntWithObjTag() {
		// TODO Auto-generated constructor stub
	}
	public Object toPrimitiveType()
	{
		Object ret = getValue();
		try{
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			ret.getClass().getDeclaredField("valuePHOSPHOR_TAG").setInt(ret, 0);
		}catch(Exception ex)
		{

		}
		return ret;
	}
}
