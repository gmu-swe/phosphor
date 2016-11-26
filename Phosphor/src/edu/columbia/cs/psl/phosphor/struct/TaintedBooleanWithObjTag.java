package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.runtime.Taint;


public final class TaintedBooleanWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2665598564631615110L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeBoolean(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readBoolean();
		taint = (Taint) stream.readObject();
	}
	@Override
	public Object getValue() {
		return val;
	}
	public boolean val;

	static TaintedBooleanWithObjTag[] cache = new TaintedBooleanWithObjTag[2];
	static {
		cache[0] = new TaintedBooleanWithObjTag(null, false);
		cache[1] = new TaintedBooleanWithObjTag(null, true);
	}

	public static final TaintedBooleanWithObjTag valueOf(Taint taint, boolean val) {
		if (taint == null)
			return cache[val ? 1 : 0];
		return new TaintedBooleanWithObjTag(taint, val);
	}

	public TaintedBooleanWithObjTag(Taint taint, boolean val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedBooleanWithObjTag() {

	}
}
