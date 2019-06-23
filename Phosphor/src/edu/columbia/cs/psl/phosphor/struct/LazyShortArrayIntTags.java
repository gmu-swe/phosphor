package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyShortArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public short[] val;

	public LazyShortArrayIntTags(int len){
		val = new short[len];
	}

	public LazyShortArrayIntTags(short[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyShortArrayIntTags(short[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyShortArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(short[] g, int idx, int tag, short val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedShortWithIntTag get(short[] g, int idx, TaintedShortWithIntTag ret) {
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

	public void ensureVal(short[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(short el : val) {
				stream.writeShort(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new short[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readShort();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
