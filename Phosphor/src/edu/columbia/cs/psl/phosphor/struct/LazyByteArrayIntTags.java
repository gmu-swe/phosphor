package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyByteArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public byte[] val;
	
	public LazyByteArrayIntTags(int len) {
		val = new byte[len];
	}
	
	public LazyByteArrayIntTags(byte[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyByteArrayIntTags(byte[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyByteArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(byte[] b, int idx, int tag, byte val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedByteWithIntTag get(byte[] b, int idx, TaintedByteWithIntTag ret) {
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

	public void ensureVal(byte[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(byte el : val) {
				stream.writeByte(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new byte[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readByte();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
