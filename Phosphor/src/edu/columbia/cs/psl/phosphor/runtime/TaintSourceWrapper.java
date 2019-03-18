package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

import java.lang.reflect.Array;

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

	public void combineTaintsOnArray(Object inputArray, Taint<T> tag){
		if(tag == null) {
			return;
		}
		if(inputArray instanceof LazyArrayObjTags)
		{
			LazyArrayObjTags array = ((LazyArrayObjTags) inputArray);
			if(array.taints == null)
				array.taints = new Taint[array.getLength()];
			for(int i=0; i < array.getLength();i++)
			{
				if(array.taints[i] == null)
					array.taints[i] = tag.copy();
				else
					array.taints[i].addDependency(tag);
			}

		}else if (inputArray instanceof Object[])
		{
			//Object[]
			for(int i = 0; i < ((Object[]) inputArray).length; i++){
				Object o = ((Object[])inputArray)[i];
				if(o instanceof TaintedWithObjTag)
				{

					Taint existing = (Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG();
					if(existing != null)
						existing.addDependency(tag);
					else
						((TaintedWithObjTag) o).setPHOSPHOR_TAG(tag.copy());
				}
			}
		}
	}

	public Taint<AutoTaintLabel> generateTaint(String source) {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement[] s = new StackTraceElement[st.length - 3];
		System.arraycopy(st, 3, s, 0, s.length);
		return new Taint<>(new AutoTaintLabel(source, s));
	}

	/* Called by sources for the arguments and return value. Adds a new taint tag to the specified object. */
	public Object autoTaint(Object obj, String source, int argIdx) {
		return autoTaint(obj, source, argIdx, generateTaint(source));
	}

	/* Called by sources for the arguments and return value. Adds the specified tag to the specified object. */
	public Object autoTaint(Object obj, String source, int argIdx, Taint<? extends AutoTaintLabel> tag) {
	    if(obj == null) {
	        return null;
        } else if(obj instanceof LazyArrayObjTags) {
			return autoTaint((LazyArrayObjTags) obj, source, argIdx, tag);
		} else if(obj instanceof TaintedWithObjTag) {
			return autoTaint((TaintedWithObjTag) obj, source, argIdx, tag);
		} else if(obj instanceof TaintedPrimitiveWithObjTag) {
			return autoTaint((TaintedPrimitiveWithObjTag) obj, source, argIdx, tag);
		} else if(obj.getClass().isArray()) {
			for(int i = 0; i < Array.getLength(obj); i++) {
				Array.set(obj, i, autoTaint(Array.get(obj, i), source, argIdx, tag));
			}
			return obj;
		}
		return obj;
	}

    @SuppressWarnings("unchecked")
	public TaintedWithObjTag autoTaint(TaintedWithObjTag ret, String source, int argIdx, Taint<? extends AutoTaintLabel> tag) {
        Taint prevTag = (Taint)ret.getPHOSPHOR_TAG();
        if(prevTag != null) {
            prevTag.addDependency(tag);
        } else {
            ret.setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

	@SuppressWarnings("unchecked")
	public LazyArrayObjTags autoTaint(LazyArrayObjTags ret, String source, int argIdx, Taint<? extends AutoTaintLabel> tag) {
		Taint[] taintArray = ret.taints;
		if (taintArray != null) {
			for (int i = 0; i < taintArray.length; i++) {
				if(taintArray[i] == null)
					taintArray[i] = tag.copy();
				else
					taintArray[i].addDependency(tag);
			}
		} else {
			ret.setTaints(tag);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public TaintedPrimitiveWithObjTag autoTaint(TaintedPrimitiveWithObjTag ret, String source, int argIdx, Taint<? extends AutoTaintLabel> tag) {
		if (ret.taint != null)
			ret.taint.addDependency(tag);
		else
			ret.taint = tag;
		return ret;
	}

	public void checkTaint(int tag, Object obj, String baseSink, String actualSink) {
		if (tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag + " at " + actualSink);
	}

	public static void checkTaint(int tag) {
		if (tag != 0)
			throw new IllegalAccessError("Argument carries taint " + tag);
	}

	public Taint[] getStringValueTaints(String str) {
		if(str == null) {
			return null;
		} else {
			return((String)str).valuePHOSPHOR_TAG.taints;

		}
	}

	@SuppressWarnings("unchecked")
	public void checkTaint(Object obj, String baseSink, String actualSink) {
		if (obj == null)
			return;
		if (obj instanceof String) {
			if (obj instanceof TaintedWithObjTag) {
				Taint[] taints = getStringValueTaints((String) obj);
				if (taints != null) {
					SimpleHashSet<String> reported = new SimpleHashSet<>();
					for (Taint t : taints) {
						if (t != null) {
							String _t = new String(t.toString().getBytes());
							if (reported.add(_t))
								taintViolation(t, obj, baseSink, actualSink);
						}
					}
				}
			}
		} else if (obj instanceof TaintedWithIntTag) {
			if (((TaintedWithIntTag) obj).getPHOSPHOR_TAG() != 0)
				throw new IllegalAccessError("Argument carries taint " + ((TaintedWithIntTag) obj).getPHOSPHOR_TAG());
		} else if (obj instanceof TaintedWithObjTag) {
			if (((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null)
				taintViolation((Taint<T>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), obj, baseSink, actualSink);
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
						taintViolation((Taint<T>) i, obj, baseSink, actualSink);
				}
		} else if (obj instanceof Object[]) {
			for (Object o : ((Object[]) obj))
				checkTaint(o, baseSink, actualSink);
		} else if (obj instanceof ControlTaintTagStack) {
			ControlTaintTagStack ctrl = (ControlTaintTagStack) obj;
			if (ctrl.taint != null && !ctrl.isEmpty()) {
				taintViolation((Taint<T>) ctrl.taint, obj, baseSink, actualSink);
			}
		} else if (obj instanceof Taint) {
			taintViolation((Taint<T>) obj, null, baseSink, actualSink);
		} else if (obj instanceof TaintedPrimitiveWithObjTag) {
			if(((TaintedPrimitiveWithObjTag)obj).taint != null) {
				taintViolation(((TaintedPrimitiveWithObjTag)obj).taint, ((TaintedPrimitiveWithObjTag)obj).getValue(), baseSink, actualSink);
			}
		} else if (obj instanceof TaintedPrimitiveWithIntTag) {
			if(((TaintedPrimitiveWithIntTag)obj).taint != 0) {
				throw new IllegalAccessError("Argument carries taint " + ((TaintedPrimitiveWithIntTag)obj).taint);
			}
		}
	}

    public void taintViolation(Taint<T> tag, Object obj, String baseSink, String actualSink) {
        throw new TaintSinkError(tag, obj);
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

	/* Called just before a sink method returns. */
	public void exitingSink(String sink) {
		return;
	}

	/* Called after a sink method makes its calls to checkTaint but before the rest of the method body executes. */
	public void enteringSink(String sink) {
		return;
	}
}
