package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;

public final class MultiDTaintedDoubleArrayWithSingleObjTag extends MultiDTaintedArrayWithSingleObjTag implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2983786880082730018L;
	public double[] val;

	public MultiDTaintedDoubleArrayWithSingleObjTag(Object taint, double[] val) {
		super(taint, Type.DOUBLE);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedDoubleArrayWithSingleObjTag(taint, val.clone());
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
			stream.writeDouble(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new double[len];
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint = stream.readObject();
			val[i] = stream.readDouble();
		}
	}
}
