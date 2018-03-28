package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

public abstract class DerivedTaintListener {
	public void singleDepCreated(Taint in, Taint out) {
	}


	public void doubleDepCreated(Taint in1, Taint in2, Taint out) {
	}


	public void controlApplied(Object o, ControlTaintTagStack tags) {
	}

	public Taint arrayGet(LazyArrayObjTags b, Taint idxTaint, int idx, TaintedPrimitiveWithObjTag ret) {
		return ret.taint;
	}

	public Taint arraySet(LazyShortArrayObjTags a, Taint idxTaint, int idx, Taint t, short v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyIntArrayObjTags a, Taint idxTaint, int idx, Taint t, int v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyByteArrayObjTags a, Taint idxTaint, int idx, Taint t, byte v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyBooleanArrayObjTags a, Taint idxTaint, int idx, Taint t, boolean v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyCharArrayObjTags a, Taint idxTaint, int idx, Taint t, char v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyFloatArrayObjTags a, Taint idxTaint, int idx, Taint t, float v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyDoubleArrayObjTags a, Taint idxTaint, int idx, Taint t, double v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}

	public Taint arraySet(LazyLongArrayObjTags a, Taint idxTaint, int idx, Taint t, long v) {
		if (t == null)
			return idxTaint;
		else if (idxTaint == null)
			return t;
		else
			return new Taint(t, idxTaint);
	}
}
