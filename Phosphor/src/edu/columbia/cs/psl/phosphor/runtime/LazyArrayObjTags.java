package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;

public class LazyArrayObjTags implements Cloneable {
//	public static final String INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/LazyArrayIntTags";
	public Taint[] taints;
	
	public Taint arTaint;

	public LazyArrayObjTags(Taint[] taints) {
		this.taints = taints;
	}

	public LazyArrayObjTags() {
	}

	@Override
	public Object clone() {
		LazyArrayObjTags ret = new LazyArrayObjTags();
		if(taints != null)
			ret.taints = taints.clone();
		return ret;
	}

	public void set(int[] ar, Taint idxtag, int idx, Taint tag, int val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(short[] ar, Taint idxtag, int idx, Taint tag, short val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(long[] ar, Taint idxtag, int idx, Taint tag, long val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(byte[] ar, Taint idxtag, int idx, Taint tag, byte val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(boolean[] ar, int idx, Taint tag, boolean val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(byte[] ar, int idx, Taint tag, byte val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(char[] ar, int idx, Taint tag, char val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(double[] ar, int idx, Taint tag, double val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(float[] ar, int idx, Taint tag, float val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(int[] ar, int idx, Taint tag, int val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(long[] ar, int idx, Taint tag, long val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(short[] ar, int idx, Taint tag, short val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(boolean[] ar, Taint idxtag, int idx, Taint tag, boolean val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(char[] ar, Taint idxtag, int idx, Taint tag, char val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(float[] ar, Taint idxtag, int idx, Taint tag, float val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public void set(double[] ar, Taint idxtag, int idx, Taint tag, double val) {
		ar[idx] = val;
		if (tag != null) {
			if (taints == null)
				taints = new Taint[ar.length];
			taints[idx] = tag;
		}
	}

	public TaintedIntWithObjTag get(int[] ar, Taint idxtag, int idx, TaintedIntWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedShortWithObjTag get(short[] ar, Taint idxtag, int idx, TaintedShortWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedLongWithObjTag get(long[] ar, Taint idxtag, int idx, TaintedLongWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedCharWithObjTag get(char[] ar, Taint idxtag, int idx, TaintedCharWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedByteWithObjTag get(byte[] ar, Taint idxtag, int idx, TaintedByteWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	

	public TaintedFloatWithObjTag get(float[] ar, Taint idxtag, int idx, TaintedFloatWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedDoubleWithObjTag get(double[] ar, Taint idxtag, int idx, TaintedDoubleWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}

	public TaintedBooleanWithObjTag get(boolean[] ar, Taint idxtag, int idx, TaintedBooleanWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		return ret;
	}
	
	public TaintedIntWithObjTag getImplicit(int[] ar, Taint idxtag, int idx, TaintedIntWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedShortWithObjTag getImplicit(short[] ar, Taint idxtag, int idx, TaintedShortWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedLongWithObjTag getImplicit(long[] ar, Taint idxtag, int idx, TaintedLongWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedCharWithObjTag getImplicit(char[] ar, Taint idxtag, int idx, TaintedCharWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedByteWithObjTag getImplicit(byte[] ar, Taint idxtag, int idx, TaintedByteWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}
	

	public TaintedFloatWithObjTag getImplicit(float[] ar, Taint idxtag, int idx, TaintedFloatWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedDoubleWithObjTag getImplicit(double[] ar, Taint idxtag, int idx, TaintedDoubleWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}

	public TaintedBooleanWithObjTag getImplicit(boolean[] ar, Taint idxtag, int idx, TaintedBooleanWithObjTag ret) {
		ret.val = ar[idx];
		if (taints == null)
			ret.taint = null;
		else
			ret.taint = taints[idx];
		ret.taint = Taint.combineTags(idxtag, (Taint) ret.taint);
		return ret;
	}
}
