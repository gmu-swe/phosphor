package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
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

	public void set(double[] b, Taint idxTag, int idx, double val){
		set(b,idxTag,idx,null,val);
	}

	@Override
	public Object clone() {
		LazyDoubleArrayObjTags ret = new LazyDoubleArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}
	
	public void set(double[] b, Taint idxTag, int idx, Taint tag, double val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag,idx,ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}
	public TaintedDoubleWithObjTag get(double[] b, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret){
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}
	public TaintedDoubleWithObjTag get(double[] b, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack ctrl){
		checkAIOOB(idxTaint,idx,ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}
	
	public void set(double[] l, Taint idxTag, int idx, Taint tag, double ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival,null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(double[] d, int idx, Taint tag, double newval) {
		this.val[idx] = newval;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedDoubleWithObjTag get(double[] d, int idx, TaintedDoubleWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void set(double[] d, int idx, Taint tag, double newval, ControlTaintTagStack tags) {
		checkAIOOB(null,idx,tags);
		val[idx] = newval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedDoubleWithObjTag get(double[] d, int idx, TaintedDoubleWithObjTag ret, ControlTaintTagStack tags) {
		checkAIOOB(null,idx,tags);
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
