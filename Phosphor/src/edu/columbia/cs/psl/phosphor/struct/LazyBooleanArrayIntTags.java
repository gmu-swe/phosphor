package edu.columbia.cs.psl.phosphor.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class LazyBooleanArrayIntTags extends LazyArrayIntTags {

	private static final long serialVersionUID = 2765977210252782974L;

	public boolean[] val;

	public LazyBooleanArrayIntTags(int len) {
		val = new boolean[len];
	}
	
	public LazyBooleanArrayIntTags(boolean[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyBooleanArrayIntTags(boolean[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		return new LazyBooleanArrayIntTags(val.clone(), (taints != null) ? taints.clone() : null);
	}

	public void set(boolean[] b, int idx, int tag, boolean val) {
		this.val[idx] = val;
		if(taints == null && tag != 0) {
			taints = new int[this.val.length];
		}
		if(taints != null) {
			taints[idx] = tag;
		}
	}

	public TaintedBooleanWithIntTag get(boolean[] b, int idx, TaintedBooleanWithIntTag ret) {
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
	
	public void ensureVal(boolean[] v) {
		if(v != val) {
			val = v;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		if(val == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(val.length);
			for(boolean el : val) {
				stream.writeBoolean(el);
			}
		}
		stream.writeObject(taints);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		if(len == -1) {
			val = null;
		} else {
			val = new boolean[len];
			for(int i = 0; i < len; i++) {
				val[i] = stream.readBoolean();
			}
		}
		taints = (int[]) stream.readObject();
	}
}
