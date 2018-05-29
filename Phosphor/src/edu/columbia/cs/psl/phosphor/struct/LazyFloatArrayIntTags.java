package edu.columbia.cs.psl.phosphor.struct;

public final class LazyFloatArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public float[] val;

	public LazyFloatArrayIntTags(int len){
		val = new float[len];
	}
	public LazyFloatArrayIntTags(float[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyFloatArrayIntTags(float[] array) {
		this.val = array;
	}

	@Override
	public Object clone() {
		LazyFloatArrayIntTags ret = new LazyFloatArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(float[] f, int idx, int tag, float fval) {
		val[idx] = fval;
		if (taints == null && tag != 0)
			taints = new int[this.val.length];
		if (taints != null)
			taints[idx] = tag;
	}

	public TaintedFloatWithIntTag get(float[] f, int idx, TaintedFloatWithIntTag ret) {
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
	public void ensureVal(float[] v)
	{
		if(v != val)
			val = v;
	}
}
