package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyCharArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public char[] val;

	public LazyCharArrayIntTags(int len) {
		this.val = new char[len];
	}

	public LazyCharArrayIntTags(char[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyCharArrayIntTags(char[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyCharArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(char[] c, int idx, int tag, char val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedCharWithIntTag get(char[] c, int idx, TaintedCharWithIntTag ret) {
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

	public void ensureVal(char[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(char el : val) {
				stream.writeChar(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new char[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readChar();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
