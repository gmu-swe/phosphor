package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

/**
 * This class handles dynamically doing source-based tainting.
 * 
 * If you want to replace the *value* dynamically, then:
 * 	1. extend this class
 *  2. Set Configuration.autoTainter to be an instance of your new TaintSourceWrapper
 *  3. Override autoTaint..., change the value, then call super.autoTaint... in order to set the taint correctly
 *  
 *  Example:
 *  
 *  
	public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String source, int argIdx) {
		ret.val = 100; //Change value to be 100 instead of whatever it was normally
		ret = super.autoTaint(ret, source, argIdx); //will set the taint
		return ret;
	}
 * @author jon
 *
 */
public class TaintSourceWrapper {
	
	
	public Taint<? extends AutoTaintLabel> generateTaint(String source) {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement[] s = new StackTraceElement[st.length - 3];
		System.arraycopy(st, 3, s, 0, s.length);
		return new Taint<AutoTaintLabel>(new AutoTaintLabel(source, s));
	}

	public Object autoTaint(Object obj, String source, int argIdx) {
		if (obj instanceof TaintedWithObjTag)
			((TaintedWithObjTag) obj).setPHOSPHOR_TAG(generateTaint(source));
		return obj;
	}

	public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedShortWithObjTag autoTaint(TaintedShortWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedLongWithObjTag autoTaint(TaintedLongWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedFloatWithObjTag autoTaint(TaintedFloatWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedDoubleWithObjTag autoTaint(TaintedDoubleWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedBooleanWithObjTag autoTaint(TaintedBooleanWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedByteWithObjTag autoTaint(TaintedByteWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	public TaintedCharWithObjTag autoTaint(TaintedCharWithObjTag ret, String source, int argIdx) {
		if (ret.taint != null)
			ret.taint.addDependency(generateTaint(source));
		else
			ret.taint = generateTaint(source);
		return ret;
	}

	private void setTaints(LazyArrayObjTags ret, String source) {
		Taint retTaint = generateTaint(source);
		if (ret.taints != null) {
			for (Taint t : ret.taints)
				t.addDependency(retTaint);
		} else
			ret.setTaints(retTaint);
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
}
