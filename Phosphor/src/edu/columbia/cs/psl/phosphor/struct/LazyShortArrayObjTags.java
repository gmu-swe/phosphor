package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyShortArrayObjTags extends LazyArrayObjTags {
	
	public short[] val;

	public LazyShortArrayObjTags(int len){
		val = new short[len];
	}
	public LazyShortArrayObjTags(short[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyShortArrayObjTags(short[] array) {
		this.val = array;
	}
	
	public LazyShortArrayObjTags(Taint lenTaint, short[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}
	
	@Override
	public Object clone() {
		LazyShortArrayObjTags ret = new LazyShortArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}
	
	public void set(short[] l, Taint idxTag, int idx, Taint tag, short ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(short[] g, int idx, Taint tag, short sval) {
		val[idx] = sval;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedShortWithObjTag get(short[] g, int idx, TaintedShortWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void setImplicit(short[] g, int idx, Taint tag, short sval, ControlTaintTagStack tags) {
		val[idx] = sval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public void setImplicit(short[] b, Taint idxTag, int idx, Taint tag, short val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public TaintedShortWithObjTag getImplicit(short[] g, int idx, TaintedShortWithObjTag ret, ControlTaintTagStack tags) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(ret.taint, tags);
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
	public void ensureVal(short[] v)
	{
		if(v != val)
			val = v;
	}
}
