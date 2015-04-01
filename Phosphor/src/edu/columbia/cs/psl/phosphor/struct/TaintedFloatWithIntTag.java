package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedFloatWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4950080138821634577L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeFloat(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readFloat();
		taint = stream.readInt();
	}

	@Override
	public Object getValue() {
		return val;
	}

	public float val;

	public static final TaintedFloatWithIntTag valueOf(int taint, float val) {
		return new TaintedFloatWithIntTag(taint, val);
	}

	public TaintedFloatWithIntTag(int taint, float val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedFloatWithIntTag() {
		// TODO Auto-generated constructor stub
	}
}
