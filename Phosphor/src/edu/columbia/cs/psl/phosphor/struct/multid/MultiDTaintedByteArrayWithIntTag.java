package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

public final class MultiDTaintedByteArrayWithIntTag extends MultiDTaintedArrayWithIntTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3521425835220214125L;
	public byte[] val;

	public MultiDTaintedByteArrayWithIntTag(int[] taint, byte[] val) {
		super(taint, Type.BYTE);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedByteArrayWithIntTag(taint.clone(), val.clone());
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (val == null) {
			stream.writeObject(null);
			return;
		}
		stream.writeInt(val.length);
		for (int i = 0; i < val.length; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				stream.writeInt(taint[i]);
			stream.writeByte(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new byte[len];
		taint = new int[len];
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint[i] = stream.readInt();
			val[i] = stream.readByte();
		}
	}
}
