package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyIntArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public int[] val;
	
	public LazyIntArrayIntTags(int len) {
		this.val = new int[len];
	}

	public LazyIntArrayIntTags(int[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyIntArrayIntTags(int[] array) {
		this.val = array;
	}

	private LazyIntArrayIntTags() {
		
	}

	@Override
	public Object clone() {
		return new LazyIntArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(int[] l, int idx, int tag, int val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedIntWithIntTag get(int[] l, int idx, TaintedIntWithIntTag ret) {
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

	public void ensureVal(int[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(int el : val) {
				stream.writeInt(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new int[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readInt();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
