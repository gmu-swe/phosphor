package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
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

	public void set(boolean[] b, Taint idxTag, int idx, boolean val){
		set(b,idxTag,idx,null,val);
	}
	
	public void set(boolean[] l, Taint idxTag, int idx, Taint tag, boolean ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival, null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(boolean[] b, int idx, Taint tag, boolean val) {
		this.val[idx] = val;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public void set(boolean[] b, Taint idxTag, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag,idx,ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}

	public void set(boolean[] b, int idx, Taint tag, boolean val, ControlTaintTagStack ctrl) {
		checkAIOOB(null,idx,ctrl);
		this.val[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}


	public TaintedBooleanWithObjTag get(boolean[] b, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret) {
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}

	public TaintedBooleanWithObjTag get(boolean[] b, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTaint,idx,ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}

	public TaintedBooleanWithObjTag get(boolean[] b, int idx, TaintedBooleanWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	public TaintedBooleanWithObjTag get(boolean[] b, int idx, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		checkAIOOB(null,idx,ctrl);
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
