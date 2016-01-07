package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;

public class LazyArrayIntTags implements Cloneable {
//	public static final String INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/LazyArrayIntTags";
	public int[] taints;

	public LazyArrayIntTags(int[] taints) {
		this.taints = taints;
	}

	public LazyArrayIntTags() {
	}

	@Override
	public Object clone() {
		LazyArrayIntTags ret = new LazyArrayIntTags();
		if(taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(int[] ar, int idxtag, int idx, int tag, int val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(short[] ar, int idxtag, int idx, int tag, short val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(long[] ar, int idxtag, int idx, int tag, long val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(byte[] ar, int idxtag, int idx, int tag, byte val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(boolean[] ar, int idx, int tag, boolean val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(byte[] ar, int idx, int tag, byte val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(char[] ar, int idx, int tag, char val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(double[] ar, int idx, int tag, double val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(float[] ar, int idx, int tag, float val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(int[] ar, int idx, int tag, int val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(long[] ar, int idx, int tag, long val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(short[] ar, int idx, int tag, short val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(boolean[] ar, int idxtag, int idx, int tag, boolean val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(char[] ar, int idxtag, int idx, int tag, char val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(float[] ar, int idxtag, int idx, int tag, float val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(double[] ar, int idxtag, int idx, int tag, double val) {
		ar[idx] = val;
		if (tag != 0) {
			if (taints == null)
				taints = new int[ar.length];
			taints[idx] = tag;
		}
	}

	public TaintedIntWithIntTag get(int[] ar, int idxtag, int idx, TaintedIntWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedShortWithIntTag get(short[] ar, int idxtag, int idx, TaintedShortWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedLongWithIntTag get(long[] ar, int idxtag, int idx, TaintedLongWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedCharWithIntTag get(char[] ar, int idxtag, int idx, TaintedCharWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedByteWithIntTag get(byte[] ar, int idxtag, int idx, TaintedByteWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}
	

	public TaintedFloatWithIntTag get(float[] ar, int idxtag, int idx, TaintedFloatWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedDoubleWithIntTag get(double[] ar, int idxtag, int idx, TaintedDoubleWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedBooleanWithIntTag get(boolean[] ar, int idxtag, int idx, TaintedBooleanWithIntTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = 0;
		else
			ret.taint = taints[idx];
		return ret;
	}
}
