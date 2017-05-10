package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyLongArrayObjTags extends LazyArrayObjTags {
	
	public long[] val;

	public LazyLongArrayObjTags(int len)
	{
		val = new long[len];
	}
	public LazyLongArrayObjTags(long[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyLongArrayObjTags(long[] array) {
		this.val = array;
	}

	public LazyLongArrayObjTags(Taint lenTaint, long[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}
	
	@Override
	public Object clone() {
		LazyLongArrayObjTags ret = new LazyLongArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(long[] l, Taint idxTag, int idx, Taint tag, long ival) {
		set(l, idx, new Taint(tag, idxTag), ival);
	}
	
	public void set(long[] b, int idx, Taint tag, long lval) {
		val[idx] = lval;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedLongWithObjTag get(long[] b, int idx, TaintedLongWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void setImplicit(long[] b, Taint idxTag, int idx, Taint tag, long val, ControlTaintTagStack ctrl) {
		setImplicit(b, idx, new Taint(tag, idxTag), val, ctrl);
	}
	
	public void setImplicit(long[] b, int idx, Taint tag, long lval, ControlTaintTagStack tags) {
		val[idx] = lval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedLongWithObjTag getImplicit(long[] b, int idx, TaintedLongWithObjTag ret, ControlTaintTagStack tags) {
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
	public void ensureVal(long[] v)
	{
		if(v != val)
			val = v;
	}
}
