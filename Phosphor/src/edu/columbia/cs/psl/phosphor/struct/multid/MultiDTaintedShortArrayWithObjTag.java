package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.IOException;
import java.io.Serializable;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;

public final class MultiDTaintedShortArrayWithObjTag extends MultiDTaintedArrayWithObjTag implements Serializable{
	private static final long serialVersionUID = 4176487299864850587L;
	public short[] val;

	public MultiDTaintedShortArrayWithObjTag(Object[] taint, short[] val) {
		super(taint,Type.SHORT);
		this.val=val;
	}
	
	@Override
	public Object getVal() {
		return val;
	}
	
	@Override
	public Object clone() {
		return new MultiDTaintedShortArrayWithObjTag(taint.clone(), val.clone());
	}
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (val == null) {
			stream.writeObject(null);
			return;
		}
		stream.writeInt(val.length);
		for (int i = 0; i < val.length; i++) {
			if(TaintUtils.TAINT_THROUGH_SERIALIZATION)
				stream.writeObject(taint[i]);
			stream.writeShort(val[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int len = stream.readInt();
		val = new short[len];
		taint = TaintUtils.newTaintArray(len);
		for(int i=0;i<len;i++) {
			if(TaintUtils.TAINT_THROUGH_SERIALIZATION)
			taint[i] = stream.readObject();
			val[i] = stream.readShort();
		}
	}
}
