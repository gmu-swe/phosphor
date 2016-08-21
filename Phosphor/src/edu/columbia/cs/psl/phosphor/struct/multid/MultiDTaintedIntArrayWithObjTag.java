package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import org.objectweb.asm.Type;

public final class MultiDTaintedIntArrayWithObjTag extends MultiDTaintedArrayWithObjTag implements Serializable {

	public int[] val;

	public MultiDTaintedIntArrayWithObjTag(LazyArrayObjTags taint, int[] val) {
		super(taint, Type.INT);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedIntArrayWithObjTag((LazyArrayObjTags) taint.clone(), val.clone());
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
			stream.writeInt(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new int[len];
		taint = new LazyArrayObjTags(new Taint[len]);
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint.taints[i] = (Taint) stream.readObject();
			val[i] = stream.readInt();
		}
	}

}
