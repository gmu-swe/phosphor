package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyFloatArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public float[] val;

	public LazyFloatArrayIntTags(int len) {
		val = new float[len];
	}

	public LazyFloatArrayIntTags(float[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyFloatArrayIntTags(float[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyFloatArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(float[] f, int idx, int tag, float val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedFloatWithIntTag get(float[] f, int idx, TaintedFloatWithIntTag ret) {
		ret.val = val[idx];
		ret.taint = (taints == null) ? 0 : taints[idx];
		return ret;
	}

	public int getLength() {
		return val.length;
	}

	@Override
	public Object getVal() {
		return val;
	}

	public void ensureVal(float[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(float el : val) {
				stream.writeFloat(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new float[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readFloat();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
