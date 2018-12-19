package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyByteArrayObjTags extends LazyArrayObjTags {
	public byte[] val;
	
	public LazyByteArrayObjTags(int len)
	{
		val = new byte[len];
	}
	
	public LazyByteArrayObjTags(byte[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyByteArrayObjTags(byte[] array) {
		this.val = array;
	}
	
	public LazyByteArrayObjTags(Taint lenTaint, byte[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}


	@Override
	public Object clone() {
		LazyByteArrayObjTags ret = new LazyByteArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(byte[] b, Taint idxTag, int idx, byte val){
		set(b,idxTag,idx,null,val);
	}

	public void set(byte[] l, Taint idxTag, int idx, Taint tag, byte ival) {
		if(Configuration.derivedTaintListener != null)
			set(l,idx, Configuration.derivedTaintListener.arraySet(this,idxTag,idx,tag, ival, null), ival);
		else if(idxTag == null)
			set(l, idx, tag, ival);
		else if(tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(byte[] b, int idx, Taint tag, byte val) {
		this.val[idx] = val;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedByteWithObjTag get(byte[] b, Taint idxTaint, int idx, TaintedByteWithObjTag ret){
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}

	public TaintedByteWithObjTag get(byte[] b, Taint idxTaint, int idx, TaintedByteWithObjTag ret, ControlTaintTagStack ctrl){
		checkAIOOB(idxTaint,idx,ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}

	public TaintedByteWithObjTag get(byte[] b, int idx, TaintedByteWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public void set(byte[] b, Taint idxTag, int idx, Taint tag, byte val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag,idx,ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}
	
	public void set(byte[] b, int idx, Taint tag, byte val, ControlTaintTagStack ctrl) {
		checkAIOOB(null,idx,ctrl);
		this.val[idx] = val;
		tag = Taint.combineTags(tag, ctrl);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedByteWithObjTag get(byte[] b, int idx, TaintedByteWithObjTag ret, ControlTaintTagStack ctrl) {
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
	public void ensureVal(byte[] v)
	{
		if(v != val)
			val = v;
	}
}
