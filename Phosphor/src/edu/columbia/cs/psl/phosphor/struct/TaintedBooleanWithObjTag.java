package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;


public final class TaintedBooleanWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable{
	private static final long serialVersionUID = 2665598564631615110L;
	public boolean val;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeBoolean(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readBoolean();
		taint = stream.readObject();
	}

	@Override
	public Object getValue() {
		return val;
	}

	static TaintedBooleanWithObjTag[] cache = new TaintedBooleanWithObjTag[2];
	static {
		cache[0] = new TaintedBooleanWithObjTag(null, false);
		cache[1] = new TaintedBooleanWithObjTag(null, true);
	}

	public static final TaintedBooleanWithObjTag valueOf(Object taint, boolean val) {
		if (taint == null)
			return cache[val ? 1 : 0];
		return new TaintedBooleanWithObjTag(taint, val);
	}

	public TaintedBooleanWithObjTag(Object taint, boolean val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedBooleanWithObjTag() {}
}
