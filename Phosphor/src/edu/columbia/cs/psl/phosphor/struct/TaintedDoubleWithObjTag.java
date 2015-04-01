package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedDoubleWithObjTag extends TaintedPrimitiveWithObjTag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1292260724904675172L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeDouble(val);
		stream.writeObject(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readDouble();
		taint = stream.readObject();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public double val;

	public static final TaintedDoubleWithObjTag valueOf(Object taint, double val) {
		return new TaintedDoubleWithObjTag(taint, val);
	}

	public TaintedDoubleWithObjTag(Object taint, double val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedDoubleWithObjTag() {

	}
}
