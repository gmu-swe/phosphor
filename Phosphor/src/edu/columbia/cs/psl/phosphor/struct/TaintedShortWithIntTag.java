package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.Serializable;

public final class TaintedShortWithIntTag extends TaintedPrimitiveWithIntTag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 719385575251672639L;

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeShort(val);
		stream.writeInt(taint);
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		val = stream.readShort();
		taint = stream.readInt();
	}
	@Override
	public Object getValue() {
		return val;
	}

	public short val;

	public static final TaintedShortWithIntTag valueOf(int taint, short val) {
		return new TaintedShortWithIntTag(taint, val);
	}

	public TaintedShortWithIntTag(int taint, short val) {
		this.taint = taint;
		this.val = val;
	}

	public TaintedShortWithIntTag() {
		// TODO Auto-generated constructor stub
	}
}
