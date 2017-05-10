package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyDoubleArrayObjTags extends LazyArrayObjTags {
		public double[] val;

	public LazyDoubleArrayObjTags(int len)
	{
		this.val = new double[len];
	}
	public LazyDoubleArrayObjTags(double[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyDoubleArrayObjTags(double[] array) {
		this.val = array;
	}
	
	public LazyDoubleArrayObjTags(Taint lenTaint, double[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}
	
	@Override
	public Object clone() {
		LazyDoubleArrayObjTags ret = new LazyDoubleArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}
	
	public void setImplicit(double[] b, Taint idxTag, int idx, Taint tag, double val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public void set(double[] l, Taint idxTag, int idx, Taint tag, double ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(double[] d, int idx, Taint tag, double newval) {
		val[idx] = newval;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedDoubleWithObjTag get(double[] d, int idx, TaintedDoubleWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void setImplicit(double[] d, int idx, Taint tag, double newval, ControlTaintTagStack tags) {
		val[idx] = newval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedDoubleWithObjTag getImplicit(double[] d, int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack tags) {
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
	public void ensureVal(double[] v)
	{
		if(v != val)
			val = v;
	}
}
