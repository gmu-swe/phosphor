package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import org.objectweb.asm.Type;

public final class MultiDTaintedBooleanArrayWithObjTag extends MultiDTaintedArrayWithObjTag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8992129368854396408L;
	public boolean[] val;

	public MultiDTaintedBooleanArrayWithObjTag(LazyArrayObjTags taint, boolean[] val) {
		super(taint, Type.BOOLEAN);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedBooleanArrayWithObjTag((LazyArrayObjTags) taint.clone(), val.clone());
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (val == null) {
			stream.writeObject(null);
			return;
		}
		stream.writeInt(val.length);
		for (int i = 0; i < val.length; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				stream.writeObject(taint.taints == null ? null : taint.taints[i]);
			stream.writeBoolean(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new boolean[len];
		taint = new LazyArrayObjTags();
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint.taints[i] = (Taint) stream.readObject();
			val[i] = stream.readBoolean();
		}
	}
}
