package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class LazyCharArrayObjTags extends LazyArrayObjTags {

	/**
	 *
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public char[] val;

	public LazyCharArrayObjTags(int len) {
		this.val = new char[len];
	}

	public LazyCharArrayObjTags(char[] array, Taint[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyCharArrayObjTags(char[] array) {
		this.val = array;
	}

	public LazyCharArrayObjTags(Taint lenTaint, char[] array) {
		this.val = array;
		this.lengthTaint = lenTaint;
	}

	@Override
	public Object clone() {
		LazyCharArrayObjTags ret = new LazyCharArrayObjTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(char[] b, Taint idxTag, int idx, char val) {
		set(b, idxTag, idx, null, val);
	}

	public void set(char[] l, Taint idxTag, int idx, Taint tag, char ival) {
		if (Configuration.derivedTaintListener != null)
			set(l, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, ival, null), ival);
		else if (idxTag == null)
			set(l, idx, tag, ival);
		else if (tag == null)
			set(l, idx, idxTag, ival);
		else
			set(l, idx, new Taint(tag, idxTag), ival);
	}

	public void set(char[] c, int idx, Taint tag, char val) {
		this.val[idx] = val;
		if (taints == null && tag != null)
			taints = new Taint[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedCharWithObjTag get(char[] b, Taint idxTaint, int idx, TaintedCharWithObjTag ret) {
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, null);
	}

	public TaintedCharWithObjTag get(char[] b, Taint idxTaint, int idx, TaintedCharWithObjTag ret, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTaint,idx,ctrl);
		return Configuration.derivedTaintListener.arrayGet(this, idxTaint, idx, ret, ctrl);
	}

	public TaintedCharWithObjTag get(char[] c, int idx, TaintedCharWithObjTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public void set(char[] b, Taint idxTag, int idx, Taint tag, char val, ControlTaintTagStack ctrl) {
		checkAIOOB(idxTag,idx,ctrl);
		set(b, idx, Configuration.derivedTaintListener.arraySet(this, idxTag, idx, tag, val, ctrl), val, ctrl);
	}

	public void set(char[] c, int idx, Taint tag, char val, ControlTaintTagStack tags) {
		checkAIOOB(null, idx, tags);
		this.val[idx] = val;
		tag = Taint.combineTags(tag, tags);
		if (tag != null) {
			if (taints == null)
				taints = new Taint[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedCharWithObjTag get(char[] c, int idx, TaintedCharWithObjTag ret, ControlTaintTagStack tags) {
		checkAIOOB(null, idx, tags);
		ret.val = val[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(ret.taint, tags);
		return ret;
	}

	public int getLength() {
		return val.length;
	}

	@Override
	public Object getVal() {
		return val;
	}

	public void ensureVal(char[] v) {
		if (v != val)
			val = v;
	}
}
