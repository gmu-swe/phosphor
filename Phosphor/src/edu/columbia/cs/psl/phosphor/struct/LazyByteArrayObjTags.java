package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyByteArrayObjTags extends LazyArrayObjTags {
	public byte[] val;
	
	public LazyByteArrayObjTags(int len)
	{
		val = new byte[len];
	}
	
	public LazyByteArrayObjTags(byte[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyByteArrayObjTags(byte[] array) {
		this.val = array;
	}
	
	public LazyByteArrayObjTags(Taint lenTaint, byte[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}


	@Override
	public Object clone() {
		LazyByteArrayObjTags ret = new LazyByteArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(byte[] l, Taint idxTag, int idx, Taint tag, byte ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}
	
	public void set(byte[] b, int idx, Taint tag, byte val) {
		this.val[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedByteWithObjTag get(byte[] b, int idx, TaintedByteWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void setImplicit(byte[] b, Taint idxTag, int idx, Taint tag, byte val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public void setImplicit(byte[] b, int idx, Taint tag, byte val, ControlTaintTagStack ctrl) {
		this.val[idx] = val;
		tag = Taint.combineTags(tag, ctrl);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedByteWithObjTag getImplicit(byte[] b, int idx, TaintedByteWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(ret.taint, ctrl);
		return ret;
	}
	public int getLength()
	{
		return val.length;
	}
	@Override
	public Object getVal() {
		return val;
	}
	public void ensureVal(byte[] v)
	{
		if(v != val)
			val = v;
	}
}
