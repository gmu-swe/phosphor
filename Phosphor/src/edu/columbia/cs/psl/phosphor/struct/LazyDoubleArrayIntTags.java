package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyDoubleArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public double[] val;

	public LazyDoubleArrayIntTags(int len) {
		this.val = new double[len];
	}

	public LazyDoubleArrayIntTags(double[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyDoubleArrayIntTags(double[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyDoubleArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(double[] d, int idx, int tag, double val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedDoubleWithIntTag get(double[] d, int idx, TaintedDoubleWithIntTag ret) {
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

	public void ensureVal(double[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(double el : val) {
				stream.writeDouble(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new double[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readDouble();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
