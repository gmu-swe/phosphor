package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

/**
 * This class handles dynamically doing source-based tainting.
 * 
 * If you want to replace the *value* dynamically, then: 1. extend this class 2.
 * Set Configuration.autoTainter to be an instance of your new
 * TaintSourceWrapper 3. Override autoTaint..., change the value, then call
 * super.autoTaint... in order to set the taint correctly
 * 
 * Example:
 * 
 * 
 * public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String
 * source, int argIdx) { ret.val = 100; //Change value to be 100 instead of
 * whatever it was normally ret = super.autoTaint(ret, source, argIdx); //will
 * set the taint return ret; }
 * 
 * @author jon
 *
 */
public class TaintSourceWrapper<T extends AutoTaintLabel> {

	public void taintViolation(Taint<T> tag, Object obj, String sink) {
		throw new TaintSinkError(tag, obj);
	}

	public Taint<? extends AutoTaintLabel> generateTaint(String source) {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement[] s = new StackTraceElement[st.length - 3];
		System.arraycopy(st, 3, s, 0, s.length);
		return new Taint<AutoTaintLabel>(new AutoTaintLabel(source, s));
	}

	public Object autoTaint(Object obj, String source, int argIdx) {
		if (obj instanceof TaintedWithObjTag)
			((TaintedWithObjTag) obj).setPHOSPHOR_TAG(generateTaint(source));
		else if(obj instanceof TaintedPrimitiveWithObjTag)
			((TaintedPrimitiveWithObjTag)obj).taint = generateTaint(source);
		return obj;
	}

	@SuppressWarnings("unchecked")
	public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedShortWithObjTag autoTaint(TaintedShortWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedLongWithObjTag autoTaint(TaintedLongWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedFloatWithObjTag autoTaint(TaintedFloatWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedDoubleWithObjTag autoTaint(TaintedDoubleWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedBooleanWithObjTag autoTaint(TaintedBooleanWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedByteWithObjTag autoTaint(TaintedByteWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedCharWithObjTag autoTaint(TaintedCharWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setTaints(LazyArrayObjTags ret, String source) {
		Taint retTaint = generateTaint(source);
		Taint[] taintArray = ret.taints;
		if (taintArray != null) {
			for (int i = 0; i < taintArray.length; i++) {
				if(taintArray[i] == null)
					taintArray[i] = retTaint.copy();
				else
					taintArray[i].addDependency(retTaint);
			}
		} else {
			ret.setTaints(retTaint);
		}
	}

	public LazyByteArrayObjTags autoTaint(LazyByteArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyBooleanArrayObjTags autoTaint(LazyBooleanArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyCharArrayObjTags autoTaint(LazyCharArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyDoubleArrayObjTags autoTaint(LazyDoubleArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyFloatArrayObjTags autoTaint(LazyFloatArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyIntArrayObjTags autoTaint(LazyIntArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyShortArrayObjTags autoTaint(LazyShortArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public LazyLongArrayObjTags autoTaint(LazyLongArrayObjTags ret, String source, int argIdx) {
		setTaints(ret, source);
		return ret;
	}

	public void checkTaint(int tag, String sink) {
		if (tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag + " at " + sink);
	}

	public static void checkTaint(int tag) {
		if (tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}

	public void checkTaint(Taint<T> tag, String sink) {
		if (tag != null)
			taintViolation(tag, null, sink);
	}

	@SuppressWarnings("unchecked")
	public void checkTaint(Object obj, String sink) {
		if (obj == null)
			return;
		if (obj instanceof String) {
			if (obj instanceof TaintedWithObjTag) {
				if (obj != null && ((String) obj).valuePHOSPHOR_TAG != null && ((String) obj).valuePHOSPHOR_TAG.taints != null) {
					SimpleHashSet<String> reported = new SimpleHashSet<>();
					for (Taint t : ((String) obj).valuePHOSPHOR_TAG.taints) {
						if (t != null) {
							String _t = new String(t.toString().getBytes());
							if (reported.add(_t))
								taintViolation(t, obj, sink);
						}
					}
				}
			}
		} else if (obj instanceof TaintedWithIntTag) {
			if (((TaintedWithIntTag) obj).getPHOSPHOR_TAG() != 0)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithIntTag) obj).getPHOSPHOR_TAG());
		} else if (obj instanceof TaintedWithObjTag) {
			if (((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null)
				taintViolation((Taint<T>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), obj, sink);
		} else if (obj instanceof int[]) {
			for (int i : ((int[]) obj)) {
				if (i > 0)
					throw new IllegalAccessError("Argument carries taints - example: " + i);
			}
		} else if (obj instanceof LazyArrayIntTags) {
			LazyArrayIntTags tags = ((LazyArrayIntTags) obj);
			if (tags.taints != null)
				for (int i : tags.taints) {
					if (i > 0)
						throw new IllegalAccessError("Argument carries taints - example: " + i);
				}
		} else if (obj instanceof LazyArrayObjTags) {
			LazyArrayObjTags tags = ((LazyArrayObjTags) obj);
			if (tags.taints != null)
				for (Object i : tags.taints) {
					if (i != null)
						taintViolation((Taint<T>) i, obj, sink);
				}
		} else if (obj instanceof Object[]) {
			for (Object o : ((Object[]) obj))
				checkTaint(o, sink);
		} else if (obj instanceof ControlTaintTagStack) {
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if (ctrl.taint != null && !ctrl.isEmpty()) {
				taintViolation((Taint<T>) ctrl.taint, obj, sink);
			}
		} else if (obj instanceof Taint) {
			taintViolation((Taint<T>) obj, null, sink);
		}
	}

	public boolean hasTaints(int[] tags) {
		if (tags == null)
			return false;
		for (int i : tags) {
			if (i != 0)
				return true;
		}
		return false;
	}
}
