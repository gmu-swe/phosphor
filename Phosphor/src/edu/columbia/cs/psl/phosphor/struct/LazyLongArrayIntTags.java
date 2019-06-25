package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyLongArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public long[] val;

	public LazyLongArrayIntTags(int len) {
		val = new long[len];
	}

	public LazyLongArrayIntTags(long[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyLongArrayIntTags(long[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyLongArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(long[] b, int idx, int tag, long val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedLongWithIntTag get(long[] b, int idx, TaintedLongWithIntTag ret) {
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

	public void ensureVal(long[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(long el : val) {
				stream.writeLong(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new long[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readLong();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
