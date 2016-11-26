package edu.columbia.cs.psl.phosphor.struct;

public final class LazyIntArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public int[] val;
	
	public LazyIntArrayIntTags(int len)
	{
		this.val = new int[len];
	}

	public LazyIntArrayIntTags(int[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyIntArrayIntTags(int[] array) {
		this.val = array;
	}
	private LazyIntArrayIntTags()
	{
		
	}

	@Override
	public Object clone() {
		LazyIntArrayIntTags ret = new LazyIntArrayIntTags();
		if (taints != null)
			ret.taints = taints.clone();
		ret.val = val.clone();
		return ret;
	}

	public void set(int[] l, int idx, int tag, int ival) {
		val[idx] = ival;
		if (tag != 0) {
			if (taints == null)
				taints = new int[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedIntWithIntTag get(int[] l, int idx, TaintedIntWithIntTag ret) {
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
	public void ensureVal(int[] v)
	{
		if(v != val)
			val = v;
	}
}
