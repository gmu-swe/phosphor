package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedFloat extends TaintedPrimitive implements Serializable {

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

	public static final TaintedFloat valueOf(int taint, float val) {
		return new TaintedFloat(taint, val);
	}

	public TaintedFloat(int taint, float val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedFloat() {
		// TODO Auto-generated constructor stub
	}
}
