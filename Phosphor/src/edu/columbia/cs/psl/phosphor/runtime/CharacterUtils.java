package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;

import javax.naming.ldap.Control;

public class CharacterUtils {

	public static TaintedCharWithObjTag reverseBytes$$PHOSPHORTAGGED(Taint t, char c, TaintedCharWithObjTag ret) {
		ret.val = Character.reverseBytes(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toLowerCase$$PHOSPHORTAGGED(Taint t, char c, TaintedCharWithObjTag ret) {
		ret.val = Character.toLowerCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toLowerCase$$PHOSPHORTAGGED(Taint t, int c, TaintedIntWithObjTag ret) {
		ret.val = Character.toLowerCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toTitleCase$$PHOSPHORTAGGED(Taint t, char c, TaintedCharWithObjTag ret) {
		ret.val = Character.toTitleCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toTitleCase$$PHOSPHORTAGGED(Taint t, int c, TaintedIntWithObjTag ret) {
		ret.val = Character.toTitleCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toUpperCase$$PHOSPHORTAGGED(Taint t, char c, TaintedCharWithObjTag ret) {
		ret.val = Character.toUpperCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toUpperCase$$PHOSPHORTAGGED(Taint t, int c, TaintedIntWithObjTag ret) {
		ret.val = Character.toUpperCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(ar, i);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(CharSequence seq, Taint t, int i, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(seq, i);
			if (seq instanceof String && ((String) seq).valuePHOSPHOR_TAG != null && ((String) seq).valuePHOSPHOR_TAG.taints != null)
				ret.taint = ((String) seq).valuePHOSPHOR_TAG.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			_t.addDependency((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, Taint t2, int i2, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(ar, i, i2);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}


	public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointBefore(ar, i);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(CharSequence seq, Taint t, int i, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointBefore(seq, i);
			if (seq instanceof String && ((String) seq).valuePHOSPHOR_TAG != null && ((String) seq).valuePHOSPHOR_TAG.taints != null)
				ret.taint = ((String) seq).valuePHOSPHOR_TAG.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			_t.addDependency((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, Taint t2, int i2, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointBefore(ar, i, i2);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static LazyCharArrayObjTags toChars$$PHOSPHORTAGGED(Taint idxTaint, int idx) {

		char[] v = Character.toChars(idx);
		LazyCharArrayObjTags ret = new LazyCharArrayObjTags(v);

		if (idxTaint != null) {
			ret.taints = new Taint[v.length];
			for (int i = 0; i < v.length; i++) {
				ret.taints[i] = idxTaint.copy();
			}
		}
		return ret;
	}

	public static TaintedIntWithObjTag toChars$$PHOSPHORTAGGED(Taint idxTaint, int idx, LazyCharArrayObjTags[] artags, char[] ar, Taint t, int dstIdx, TaintedIntWithObjTag ret) {

		ret.val = Character.toChars(idx, ar, dstIdx);

		if (idxTaint != null) {
			ret.taint = idxTaint.copy();
		}
		return ret;
	}


	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(ar, i);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			_t.addDependency(ctrl.taint);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(CharSequence seq, Taint t, int i, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(seq, i);
			if (seq instanceof String && ((String) seq).valuePHOSPHOR_TAG != null)
				ret.taint = seq.toString().valuePHOSPHOR_TAG.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			_t.addDependency((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
			_t.addDependency(ctrl.taint);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, Taint t2, int i2, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		try {
			ret.val = Character.codePointAt(ar, i, i2);
			ret.taint = null;
			if (tags.taints != null)
				ret.taint = tags.taints[i];
			return ret;
		} catch (StringIndexOutOfBoundsException ex) {
			Taint _t = new Taint(t);
			_t.addDependency(ctrl.taint);
			((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
			throw ex;
		}
	}

	public static LazyCharArrayObjTags toChars$$PHOSPHORTAGGED(Taint idxTaint, int idx, ControlTaintTagStack ctrl) {
		char[] v = Character.toChars(idx);
		LazyCharArrayObjTags ret = new LazyCharArrayObjTags(v);
		if (idxTaint != null) {
			ret.taints = new Taint[v.length];
			for (int i = 0; i < v.length; i++) {
				ret.taints[i] = idxTaint.copy();
			}
		}
		return ret;
	}

	public static TaintedIntWithObjTag toChars$$PHOSPHORTAGGED(Taint idxTaint, int idx, LazyCharArrayObjTags[] artags, char[] ar, Taint t, int dstIdx, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.toChars(idx, ar, dstIdx);
		if (idxTaint != null) {
			ret.taint = idxTaint.copy();
		}
		return ret;
	}

	public static TaintedCharWithObjTag reverseBytes$$PHOSPHORTAGGED(Taint t, char c, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		ret.val = Character.reverseBytes(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toLowerCase$$PHOSPHORTAGGED(Taint t, char c, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		ret.val = Character.toLowerCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toLowerCase$$PHOSPHORTAGGED(Taint t, int c, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.toLowerCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toTitleCase$$PHOSPHORTAGGED(Taint t, char c, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		ret.val = Character.toTitleCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toTitleCase$$PHOSPHORTAGGED(Taint t, int c, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.toTitleCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedCharWithObjTag toUpperCase$$PHOSPHORTAGGED(Taint t, char c, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		ret.val = Character.toUpperCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}

	public static TaintedIntWithObjTag toUpperCase$$PHOSPHORTAGGED(Taint t, int c, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.toUpperCase(c);
		if (t == null)
			ret.taint = null;
		else
			ret.taint = t.copy();
		return ret;
	}


	public static TaintedCharWithIntTag reverseBytes$$PHOSPHORTAGGED(int t, char c, TaintedCharWithIntTag ret) {
		ret.val = Character.reverseBytes(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedCharWithIntTag toLowerCase$$PHOSPHORTAGGED(int t, char c, TaintedCharWithIntTag ret) {
		ret.val = Character.toLowerCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedIntWithIntTag toLowerCase$$PHOSPHORTAGGED(int t, int c, TaintedIntWithIntTag ret) {
		ret.val = Character.toLowerCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedCharWithIntTag toTitleCase$$PHOSPHORTAGGED(int t, char c, TaintedCharWithIntTag ret) {
		ret.val = Character.toTitleCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedIntWithIntTag toTitleCase$$PHOSPHORTAGGED(int t, int c, TaintedIntWithIntTag ret) {
		ret.val = Character.toTitleCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedCharWithIntTag toUpperCase$$PHOSPHORTAGGED(int t, char c, TaintedCharWithIntTag ret) {
		ret.val = Character.toUpperCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedIntWithIntTag toUpperCase$$PHOSPHORTAGGED(int t, int c, TaintedIntWithIntTag ret) {
		ret.val = Character.toUpperCase(c);
		ret.taint = t;
		return ret;
	}

	public static TaintedIntWithIntTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointAt(ar, i);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static TaintedIntWithIntTag codePointAt$$PHOSPHORTAGGED(CharSequence seq, int t, int i, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointAt(seq, i);
		if (seq instanceof TaintedWithIntTag)
			ret.taint = ((TaintedWithIntTag) seq).getPHOSPHOR_TAG();
		return ret;
	}

	public static TaintedIntWithIntTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, int t2, int i2, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointAt(ar, i, i2);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}


	public static TaintedIntWithIntTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointBefore(ar, i);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static TaintedIntWithIntTag codePointBefore$$PHOSPHORTAGGED(CharSequence seq, int t, int i, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointBefore(seq, i);
		if (seq instanceof TaintedWithIntTag)
			ret.taint = ((TaintedWithIntTag) seq).getPHOSPHOR_TAG();
		return ret;
	}

	public static TaintedIntWithIntTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, int t2, int i2, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointBefore(ar, i, i2);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	//...



	public static TaintedIntWithIntTag codePointAtImpl$$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, int t2, int i2, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointAtImpl(ar, i, i2);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static TaintedIntWithIntTag codePointBeforeImpl$$PHOSPHORTAGGED(LazyCharArrayIntTags tags, char[] ar, int t, int i, int t2, int i2, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointBeforeImpl(ar, i, i2);
		ret.taint = 0;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static TaintedIntWithObjTag codePointBeforeImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, Taint t2, int i2, TaintedIntWithObjTag ret) {
		ret.val = Character.codePointBeforeImpl(ar, i, i2);
		ret.taint = null;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static TaintedIntWithObjTag codePointBeforeImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, char[] ar, Taint t, int i, Taint t2, int i2, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.codePointBeforeImpl(ar, i, i2);
		ret.taint = null;
		if (tags.taints != null)
			ret.taint = tags.taints[i];
		return ret;
	}

	public static LazyCharArrayIntTags toChars$$PHOSPHORTAGGED(int idxTaint, int idx) {

		char[] v = Character.toChars(idx);
		LazyCharArrayIntTags ret = new LazyCharArrayIntTags(v);

		if (idxTaint != 0) {
			ret.taints = new int[v.length];
			for (int i = 0; i < v.length; i++) {
				ret.taints[i] = idxTaint;
			}
		} else
			ret.taints = null;
		return ret;
	}

	public static TaintedIntWithIntTag toChars$$PHOSPHORTAGGED(int idxTaint, int idx, LazyCharArrayIntTags[] artags, char[] ar, int t, int dstIdx, TaintedIntWithIntTag ret) {
		ret.val = Character.toChars(idx, ar, dstIdx);
		ret.taint = idxTaint;
		return ret;
	}

	public static TaintedIntWithIntTag toChars$$PHOSPHORTAGGED(int idxTaint, int idx, LazyCharArrayIntTags artags, char[] ar, int t, int dstIdx, TaintedIntWithIntTag ret) {
		ret.val = Character.toChars(idx, ar, dstIdx);
		ret.taint = idxTaint;
		return ret;
	}

	public static TaintedIntWithObjTag toUpperCaseEx$$PHOSPHORTAGGED(Taint t, int cp, TaintedIntWithObjTag ret) {
		ret.val = Character.toUpperCaseEx(cp);
		if (t != null)
			ret.taint = t.copy();
		else
			ret.taint = null;
		return ret;
	}

	public static LazyCharArrayObjTags toUpperCaseCharArray$$PHOSPHORTAGGED(Taint t, int cp) {
		LazyCharArrayObjTags ret = new LazyCharArrayObjTags(Character.toUpperCaseCharArray(cp));
		if (t != null) {
			ret.taints = new Taint[ret.val.length];
			for (int i = 0; i < ret.taints.length; i++) {
				if (t != null)
					ret.taints[i] = t.copy();
			}
		} else
			ret.taints = null;
		return ret;
	}

	public static TaintedIntWithObjTag toUpperCaseEx$$PHOSPHORTAGGED(Taint t, int cp, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.toUpperCaseEx(cp);
		if (t != null)
			ret.taint = t.copy();
		else
			ret.taint = null;
		return ret;
	}

	public static LazyCharArrayObjTags toUpperCaseCharArray$$PHOSPHORTAGGED(Taint t, ControlTaintTagStack ctrl, int cp) {
		LazyCharArrayObjTags ret = new LazyCharArrayObjTags(Character.toUpperCaseCharArray(cp));
		if (t != null) {
			ret.taints = new Taint[ret.val.length];
			for (int i = 0; i < ret.taints.length; i++) {
				if (t != null)
					ret.taints[i] = t.copy();
			}
		} else
			ret.taints = null;
		return ret;
	}

	public static TaintedIntWithIntTag toUpperCaseEx$$PHOSPHORTAGGED(int t, int cp, TaintedIntWithIntTag ret) {
		ret.val = Character.toUpperCaseEx(cp);
		if (t != 0)
			ret.taint = t;
		else
			ret.taint = 0;
		return ret;
	}

	public static LazyCharArrayIntTags toUpperCaseCharArray$$PHOSPHORTAGGED(int t, int cp) {
		LazyCharArrayIntTags ret = new LazyCharArrayIntTags(Character.toUpperCaseCharArray(cp));
		if (t != 0) {
			ret.taints = new int[ret.val.length];
			for (int i = 0; i < ret.taints.length; i++) {
				ret.taints[i] = t;
			}
		} else
			ret.taints = null;
		return ret;
	}

	public static TaintedIntWithObjTag codePointAtImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags t, char[] a, Taint ti, int index, Taint tl, int limit, TaintedIntWithObjTag ret) {
		ret.val = Character.codePointAtImpl(a, index, limit);
		ret.taint = null;
		if (t.taints != null && t.taints[index] != null) {
			ret.taint = t.taints[index].copy();
		}
		return ret;
	}

	public static TaintedIntWithObjTag codePointAtImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags t, char[] a, Taint ti, int index, Taint tl, int limit, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Character.codePointAtImpl(a, index, limit);
		ret.taint = null;
		if (t.taints != null && t.taints[index] != null) {
			ret.taint = t.taints[index].copy();
		}
		return ret;
	}

	public static TaintedIntWithIntTag codePointAtImpl$$PHOSPHORTAGGED(LazyCharArrayIntTags t, char[] a, int ti, int index, int tl, int limit, TaintedIntWithIntTag ret) {
		ret.val = Character.codePointAtImpl(a, index, limit);
		ret.taint = 0;
		if (t.taints != null) {
			ret.taint = t.taints[index];
		}
		return ret;
	}
}
