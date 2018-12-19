package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
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

	public TaintedShortWithObjTag get(short[] b, Taint idxTaint, int idx, TaintedShortWithObjTag ret){
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}
	public TaintedShortWithObjTag get(short[] b, Taint idxTaint, int idx, TaintedShortWithObjTag ret, ControlTaintTagStack ctrl){
		checkAIOOB(null,idx,ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}
	
	@Override
	public Object clone() {
		LazyShortArrayObjTags ret = new LazyShortArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(short[] b, Taint idxTag, int idx, short val){
		set(b,idxTag,idx,null,val);
	}

	public void set(short[] l, Taint idxTag, int idx, Taint tag, short ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival, null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(short[] g, int idx, Taint tag, short sval) {
		this.val[idx] = sval;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedShortWithObjTag get(short[] g, int idx, TaintedShortWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void set(short[] g, int idx, Taint tag, short sval, ControlTaintTagStack tags) {
		checkAIOOB(null,idx,tags);
		val[idx] = sval;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[val.length];
			taints[idx] = tag;
		}
	}

	public void set(short[] b, Taint idxTag, int idx, Taint tag, short val, ControlTaintTagStack ctrl) {
		checkAIOOB(null,idx,ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}
	
	public TaintedShortWithObjTag get(short[] g, int idx, TaintedShortWithObjTag ret, ControlTaintTagStack tags) {
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
	public void ensureVal(short[] v)
	{
		if(v != val)
			val = v;
	}
}
