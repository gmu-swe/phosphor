package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;


public final class TaintedBooleanWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2665598564631615110L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeBoolean(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readBoolean();
		taint = stream.readInt();
	}
	@Override
	public Object getValue() {
		return val;
	}
	public boolean val;

	static TaintedBooleanWithIntTag[] cache = new TaintedBooleanWithIntTag[2];
	static {
		cache[0] = new TaintedBooleanWithIntTag(0, false);
		cache[1] = new TaintedBooleanWithIntTag(0, true);
	}

	public static final TaintedBooleanWithIntTag valueOf(int taint, boolean val) {
		if (taint == 0)
			return cache[val ? 1 : 0];
		return new TaintedBooleanWithIntTag(taint, val);
	}

	public TaintedBooleanWithIntTag(int taint, boolean val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedBooleanWithIntTag() {

	}
}
