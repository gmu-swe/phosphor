package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyFloatArrayObjTags extends LazyArrayObjTags {
	
	public float[] val;

	public LazyFloatArrayObjTags(int len){
		val = new float[len];
	}
	public LazyFloatArrayObjTags(float[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyFloatArrayObjTags(float[] array) {
		this.val = array;
	}
	
	public LazyFloatArrayObjTags(Taint lenTaint, float[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}

	@Override
	public Object clone() {
		LazyFloatArrayObjTags ret = new LazyFloatArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(float[] l, Taint idxTag, int idx, Taint tag, float ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}
	
	public void set(float[] f, int idx, Taint tag, float fval) {
		val[idx] = fval;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedFloatWithObjTag get(float[] f, int idx, TaintedFloatWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public void setImplicit(float[] b, Taint idxTag, int idx, Taint tag, float val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public void setImplicit(float[] f, int idx, Taint tag, float fval, ControlTaintTagStack tags) {
		val[idx] = fval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedFloatWithObjTag getImplicit(float[] f, int idx, TaintedFloatWithObjTag ret, ControlTaintTagStack tags) {
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
	public void ensureVal(float[] v)
	{
		if(v != val)
			val = v;
	}
}
