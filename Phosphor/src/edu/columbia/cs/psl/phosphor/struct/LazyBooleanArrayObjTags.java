package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyBooleanArrayObjTags extends LazyArrayObjTags {
	
	public boolean[] val;

	public LazyBooleanArrayObjTags(int len)
	{
		val = new boolean[len];
	}
	
	public LazyBooleanArrayObjTags(boolean[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyBooleanArrayObjTags(boolean[] array) {
		this.val = array;
	}
	public LazyBooleanArrayObjTags(Taint lenTaint, boolean[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}

	@Override
	public Object clone() {
		LazyBooleanArrayObjTags ret = new LazyBooleanArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}
	
	public void set(boolean[] l, Taint idxTag, int idx, Taint tag, boolean ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(boolean[] b, int idx, Taint tag, boolean val) {
		this.val[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public void setImplicit(boolean[] b, Taint idxTag, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public void setImplicit(boolean[] b, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
		this.val[idx] = val;
		tag = Taint.combineTags(tag, ctrl);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedBooleanWithObjTag get(boolean[] b, int idx, TaintedBooleanWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	public TaintedBooleanWithObjTag getImplicit(boolean[] b, int idx, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
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
	
	public void ensureVal(boolean[] v)
	{
		if(v != val)
			val = v;
	}
}
