package edu.columbia.cs.psl.phosphor.struct;

public final class LazyCharArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public char[] val;

	public LazyCharArrayIntTags(int len)
	{
		this.val = new char[len];
	}
	public LazyCharArrayIntTags(char[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyCharArrayIntTags(char[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		LazyCharArrayIntTags ret = new LazyCharArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(char[] c, int idx, int tag, char val) {
		this.val[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedCharWithIntTag get(char[] c, int idx, TaintedCharWithIntTag ret) {
		ret.val = val[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
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
	public void ensureVal(char[] v)
	{
		if(v != val)
			val = v;
	}
}
