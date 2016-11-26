package edu.columbia.cs.psl.phosphor.struct;

public final class LazyLongArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public long[] val;

	public LazyLongArrayIntTags(int len)
	{
		val = new long[len];
	}
	public LazyLongArrayIntTags(long[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyLongArrayIntTags(long[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		LazyLongArrayIntTags ret = new LazyLongArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(long[] b, int idx, int tag, long lval) {
		val[idx] = lval;
		if (tag != 0) {
			if (taints == null)
				taints = new int[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedLongWithIntTag get(long[] b, int idx, TaintedLongWithIntTag ret) {
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
	public void ensureVal(long[] v)
	{
		if(v != val)
			val = v;
	}
}
