package edu.columbia.cs.psl.phosphor.struct;

public final class LazyBooleanArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public boolean[] val;

	public LazyBooleanArrayIntTags(int len)
	{
		val = new boolean[len];
	}
	
	public LazyBooleanArrayIntTags(boolean[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyBooleanArrayIntTags(boolean[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		LazyBooleanArrayIntTags ret = new LazyBooleanArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(boolean[] b, int idx, int tag, boolean val) {
		this.val[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedBooleanWithIntTag get(boolean[] b, int idx, TaintedBooleanWithIntTag ret) {
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
	
	public void ensureVal(boolean[] v)
	{
		if(v != val)
			val = v;
	}
}
