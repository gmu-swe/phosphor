package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedDoubleWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1292260724904675172L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeDouble(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readDouble();
		taint = stream.readInt();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public double val;

	public static final TaintedDoubleWithIntTag valueOf(int taint, double val) {
		return new TaintedDoubleWithIntTag(taint, val);
	}

	public TaintedDoubleWithIntTag(int taint, double val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedDoubleWithIntTag() {

	}
}
