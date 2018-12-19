package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
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

	public TaintedLongWithObjTag get(long[] b, Taint idxTaint, int idx, TaintedLongWithObjTag ret){
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}

	public TaintedLongWithObjTag get(long[] b, Taint idxTaint, int idx, TaintedLongWithObjTag ret, ControlTaintTagStack ctrl){
		checkAIOOB(idxTaint, idx, ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}


	@Override
	public Object clone() {
		LazyLongArrayObjTags ret = new LazyLongArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(long[] l, Taint idxTag, int idx, Taint tag, long ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival, null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}
	
	public void set(long[] b, int idx, Taint tag, long lval) {
		this.val[idx] = lval;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedLongWithObjTag get(long[] b, int idx, TaintedLongWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void set(long[] b, Taint idxTag, int idx, Taint tag, long val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag, idx, ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}
	
	public void set(long[] b, int idx, Taint tag, long lval, ControlTaintTagStack tags) {
		checkAIOOB(null, idx, tags);
		val[idx] = lval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public void set(long[] b, Taint idxTag, int idx, long val){
		set(b,idxTag,idx,null,val);
	}

	public TaintedLongWithObjTag get(long[] b, int idx, TaintedLongWithObjTag ret, ControlTaintTagStack tags) {
		checkAIOOB(null, idx, tags);
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
