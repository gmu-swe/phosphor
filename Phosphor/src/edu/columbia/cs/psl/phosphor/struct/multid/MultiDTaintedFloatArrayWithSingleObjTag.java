package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;

public final class MultiDTaintedFloatArrayWithSingleObjTag extends MultiDTaintedArrayWithSingleObjTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4876975513767857832L;
	public float[] val;

	public MultiDTaintedFloatArrayWithSingleObjTag(Object taint, float[] val) {
		super(taint, Type.FLOAT);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedFloatArrayWithSingleObjTag(taint, val.clone());
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (val == null) {
			stream.writeObject(null);
			return;
		}
		stream.writeInt(val.length);
		for (int i = 0; i < val.length; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				stream.writeObject(taint);
			stream.writeFloat(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new float[len];
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint = stream.readInt();
			val[i] = stream.readFloat();
		}
	}
}
