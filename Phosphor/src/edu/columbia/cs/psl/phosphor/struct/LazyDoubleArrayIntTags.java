package edu.columbia.cs.psl.phosphor.struct;

public final class LazyDoubleArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public double[] val;

	public LazyDoubleArrayIntTags(int len)
	{
		this.val = new double[len];
	}
	public LazyDoubleArrayIntTags(double[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyDoubleArrayIntTags(double[] array) {
		this.val = array;
	}
	@Override
	public Object clone() {
		LazyDoubleArrayIntTags ret = new LazyDoubleArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(double[] d, int idx, int tag, double newval) {
		val[idx] = newval;
		if (tag != 0) {
			if (taints == null)
				taints = new int[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedDoubleWithIntTag get(double[] d, int idx, TaintedDoubleWithIntTag ret) {
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
	public void ensureVal(double[] v)
	{
		if(v != val)
			val = v;
	}
}
