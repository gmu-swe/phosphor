package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.LazyArrayIntTags;

import org.objectweb.asm.Type;

public final class MultiDTaintedCharArrayWithIntTag extends MultiDTaintedArrayWithIntTag implements Serializable {

	private static final long serialVersionUID = -6808286317985074403L;
	public char[] val;

	public MultiDTaintedCharArrayWithIntTag() {
		super(null, Type.CHAR);
	}

	public MultiDTaintedCharArrayWithIntTag(LazyArrayIntTags taint, char[] val) {
		super(taint, Type.CHAR);
		this.val = val;
	}

	@Override
	public Object getVal() {
		return val;
	}

	@Override
	public Object clone() {
		return new MultiDTaintedCharArrayWithIntTag((LazyArrayIntTags) taint.clone(), val.clone());
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (val == null) {
			stream.writeObject(null);
			return;
		}
		if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
			System.out.println("Tainting into serialization");
		stream.writeInt(val.length);
		for (int i = 0; i < val.length; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				stream.writeInt(taint.taints == null ? 0 : taint.taints[i]);
			stream.writeChar(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new char[len];
		taint = new LazyArrayIntTags();
		taint.taints = new int[len];
		for (int i = 0; i < len; i++) {
			if (TaintUtils.TAINT_THROUGH_SERIALIZATION)
				taint.taints[i] = stream.readInt();
			val[i] = stream.readChar();
		}
	}

	@Override
	public int getLength() {
		return val.length;
	}
}
