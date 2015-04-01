package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedFloatWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4950080138821634577L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeFloat(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readFloat();
		taint = stream.readObject();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public float val;

	public static final TaintedFloatWithObjTag valueOf(Object taint, float val) {
		return new TaintedFloatWithObjTag(taint, val);
	}

	public TaintedFloatWithObjTag(Object taint, float val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedFloatWithObjTag() {

	}
}
