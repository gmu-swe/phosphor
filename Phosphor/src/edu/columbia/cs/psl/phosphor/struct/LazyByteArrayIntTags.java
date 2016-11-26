package edu.columbia.cs.psl.phosphor.struct;

public final class LazyByteArrayIntTags extends LazyArrayIntTags {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2765977210252782974L;
	public byte[] val;
	
	public LazyByteArrayIntTags(int len)
	{
		val = new byte[len];
	}
	
	public LazyByteArrayIntTags(byte[] array, int[] taints) {
		this.taints = taints;
		this.val = array;
	}

	public LazyByteArrayIntTags(byte[] array) {
		this.val = array;
	}


	@Override
	public Object clone() {
		LazyByteArrayIntTags ret = new LazyByteArrayIntTags(val.clone());
		if (taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(byte[] b, int idx, int tag, byte val) {
		this.val[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[this.val.length];
			taints[idx] = tag;
		}
	}

	public TaintedByteWithIntTag get(byte[] b, int idx, TaintedByteWithIntTag ret) {
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
	public void ensureVal(byte[] v)
	{
		if(v != val)
			val = v;
	}
}
