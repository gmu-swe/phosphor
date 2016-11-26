package edu.columbia.cs.psl.phosphor.struct;

public final class LazyShortArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public short[] val;

	public LazyShortArrayIntTags(int len){
		val = new short[len];
	}
	public LazyShortArrayIntTags(short[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyShortArrayIntTags(short[] array) {
		this.val = array;
	}
	@Override
	public Object clone() {
		LazyShortArrayIntTags ret = new LazyShortArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(short[] g, int idx, int tag, short sval) {
		val[idx] = sval;
		if (tag != 0) {
			if (taints == null)
				taints = new int[val.length];
			taints[idx] = tag;
		}
	}

	public TaintedShortWithIntTag get(short[] g, int idx, TaintedShortWithIntTag ret) {
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
	public void ensureVal(short[] v)
	{
		if(v != val)
			val = v;
	}
}
