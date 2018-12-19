package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyIntArrayObjTags extends LazyArrayObjTags {
	
	private static final long serialVersionUID = 6001767066132212417L;
	public int[] val;
	
	public LazyIntArrayObjTags(int len)
	{
		this.val = new int[len];
	}

	public LazyIntArrayObjTags(int[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyIntArrayObjTags(int[] array) {
		this.val = array;
	}

	public LazyIntArrayObjTags(Taint lenTaint, int[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}
	
	private LazyIntArrayObjTags()
	{
		
	}

	public TaintedIntWithObjTag get(int[] b, Taint idxTaint, int idx, TaintedIntWithObjTag ret){
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}
	public TaintedIntWithObjTag get(int[] b, Taint idxTaint, int idx, TaintedIntWithObjTag ret, ControlTaintTagStack ctrl){
		checkAIOOB(idxTaint, idx, ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}
	@Override
	public Object clone() {
		LazyIntArrayObjTags ret = new LazyIntArrayObjTags();
		if (taints != null)
			ret.taints = taints.clone();
		ret.val = val.clone();
		return ret;
	}

	public void set(int[] l, Taint idxTag, int idx, Taint tag, int ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival,null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(int[] l, int idx, Taint tag, int ival) {
		this.val[idx] = ival;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedIntWithObjTag get(int[] l, int idx, TaintedIntWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void set(int[] b, Taint idxTag, int idx, Taint tag, int val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag, idx, ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}
	
	public void set(int[] l, int idx, Taint tag, int ival, ControlTaintTagStack tags) {
		checkAIOOB(null, idx, tags);
		val[idx] = ival;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}
	public void set(int[] b, Taint idxTag, int idx, int val){
		set(b,idxTag,idx,null,val);
	}

	public TaintedIntWithObjTag get(int[] l, int idx, TaintedIntWithObjTag ret, ControlTaintTagStack tags) {
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
	public void ensureVal(int[] v)
	{
		if(v != val)
			val = v;
	}
}
