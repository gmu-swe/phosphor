package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class RuntimeBoxUnboxPropogator {
	public static Long valueOf(int t, long l)
	{
		if(t == 0)
			return Long.valueOf$$PHOSPHORTAGGED(0,l);
		else
		{
			Long ret = new Long(t,l,null);
			((TaintedWithIntTag)((Object)ret)).setPHOSPHOR_TAG(t);
			return ret;
		}
	}
	public static Long valueOf(Taint t, long l)
	{
		if(t == null)
			return Long.valueOf$$PHOSPHORTAGGED(null,l);
		else
		{
			Long ret = new Long(t,l,null);
			((TaintedWithObjTag)((Object)ret)).setPHOSPHOR_TAG(t);
			return ret;
		}
	}
	public static Long valueOf(Taint t, long l, ControlTaintTagStack ctrl)
	{
		if(t == null)
			return Long.valueOf$$PHOSPHORTAGGED(null,l, ctrl);
		else
		{
			Long ret = new Long(t,l,ctrl, null);
			((TaintedWithObjTag)((Object)ret)).setPHOSPHOR_TAG(t);
			return ret;
		}
	}
	public static void getChars$$PHOSPHORTAGGED(int lt, long l, int idt, int idx, LazyCharArrayIntTags ta, char[] ar)
	{
		Long.getChars(l, idx, ar);
		if (lt != 0) {
			int nChars = 0;
			if (l < 0)
				nChars++;
			long q;
			for (;;) {
				q = (l * 52429) >>> (16 + 3);
				nChars++;
				l = q;
				if (l == 0)
					break;
			}
			if (ta.taints == null)
				ta.taints = new int[ar.length];
			for (int k = idx - nChars; k <= idx; k++)
				ta.taints[k] = lt;
		}
	}
	public static void getChars$$PHOSPHORTAGGED(int it, int i, int idt, int idx, LazyCharArrayIntTags ta, char[] ar)
	{
		Integer.getChars(i, idx, ar);
		if (it != 0) {
			int nChars = 0;
			if (i < 0)
				nChars++;
			int q;
			for (;;) {
				q = (i * 52429) >>> (16 + 3);
				nChars++;
				i = q;
				if (i == 0)
					break;
			}
			if (ta.taints == null)
				ta.taints = new int[ar.length];
			for (int k = idx - nChars; k <= idx; k++)
				ta.taints[k] |= it;
		}
	}

	public static void getChars$$PHOSPHORTAGGED(Taint lt, long l, Taint idt, int idx, LazyCharArrayObjTags ta, char[] ar)
	{
		Long.getChars(l, idx, ar);
		if (lt != null) {
			int nChars = 0;
			if (l < 0)
				nChars++;
			long q;
			for (;;) {
				q = (l * 52429) >>> (16 + 3);
				nChars++;
				l = q;
				if (l == 0)
					break;
			}
			if(ta.taints == null)
				ta.taints = new Taint[ar.length];
			for (int k = idx - nChars; k <= idx; k++)
				ta.taints[k] = lt;
		}
	}
	public static void getChars$$PHOSPHORTAGGED(Taint it, int i, Taint idt, int idx, LazyCharArrayObjTags ta, char[] ar)
	{
		Integer.getChars(i, idx, ar);
		if (it != null) {
			int nChars = 0;
			if(i < 0)
				nChars++;
			int q;
			for (;;) {
	            q = (i * 52429) >>> (16+3);
	            nChars++;
	            i = q;
	            if (i == 0) break;
	        }
			if(ta.taints == null)
				ta.taints = new Taint[ar.length];
			for (int k = idx - nChars; k < Math.min(idx, ta.taints.length); k++)
				ta.taints[k] = it;
		}
	}
	public static void getChars$$PHOSPHORTAGGED(Taint lt, long l, Taint idt, int idx, LazyCharArrayObjTags ta, char[] ar, ControlTaintTagStack ctrl)
	{
		getChars$$PHOSPHORTAGGED(lt, l, idt, idx, ta, ar);
	}
	public static void getChars$$PHOSPHORTAGGED(Taint it, int i, Taint idt, int idx, LazyCharArrayObjTags ta, char[] ar, ControlTaintTagStack ctrl)
	{
		getChars$$PHOSPHORTAGGED(it, i, idt, idx, ta, ar);
	}
	public static String toString$$PHOSPHORTAGGED(int t, byte i) {
		if (t == 0)
			return Byte.toString(i);
		String ret = new String(Byte.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, char i) {
		if (t == 0)
			return Character.toString(i);
		String ret = new String(Character.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(int t, int i, int t2, int r) {
		if (t == 0)
			return Integer.toString(i, r);
		String ret = new String(Integer.toString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toUnsignedString(i);
		String ret = new String(Integer.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, int i, int tr, int r) {
		if (t == 0)
			return Integer.toUnsignedString(i, r);
		String ret = new String(Integer.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	
	public static String toOctalString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toOctalString(i);
		String ret = new String(Integer.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, int i) {
		if (t == 0)
			return Integer.toHexString(i);
		String ret = new String(Integer.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, short i) {
		if (t == 0)
			return Short.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, boolean i) {
		if (t == 0)
			return Boolean.toString(i);
		String ret = new String(Boolean.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, float i) {
		if (t == 0)
			return Float.toString(i);
		String ret = new String(Float.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, float i) {
		if (t == 0)
			return Float.toHexString(i);
		String ret = new String(Float.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, double i) {
		if (t == 0)
			return Double.toString(i);
		String ret = new String(Double.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, double i) {
		if (t == 0)
			return Double.toHexString(i);
		String ret = new String(Double.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toString(i);
		String ret = new String(Long.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(int t, long i, int t2, int r) {
		if (t == 0)
			return Long.toString(i, r);
		String ret = new String(Long.toString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toBinaryString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toBinaryString(i);
		String ret = new String(Long.toBinaryString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toHexString(i);
		String ret = new String(Long.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toUnsignedString(i);
		String ret = new String(Long.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(int t, long i, int tr, int r) {
		if (t == 0)
			return Long.toUnsignedString(i, r);
		String ret = new String(Long.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toOctalString$$PHOSPHORTAGGED(int t, long i) {
		if (t == 0)
			return Long.toOctalString(i);
		String ret = new String(Long.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, byte i) {
		if (t == null)
			return Byte.toString(i);
		String ret = new String(Byte.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, char i) {
		if (t == null)
			return Character.toString(i);
		String ret = new String(Character.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(Taint t, int i, Taint t2, int r) {
		if (t == null)
			return Integer.toString(i, r);
		String ret = new String(Integer.toString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toUnsignedString(i);
		String ret = new String(Integer.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, int i, Taint tr, int r) {
		if (t == null)
			return Integer.toUnsignedString(i, r);
		String ret = new String(Integer.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toOctalString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toOctalString(i);
		String ret = new String(Integer.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, int i) {
		if (t == null)
			return Integer.toHexString(i);
		String ret = new String(Integer.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, short i) {
		if (t == null)
			return Short.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, boolean i) {
		if (t == null)
			return Boolean.toString(i);
		String ret = new String(Boolean.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, float i) {
		if (t == null)
			return Float.toString(i);
		String ret = new String(Float.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, float i) {
		if (t == null)
			return Float.toHexString(i);
		String ret = new String(Float.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, double i) {
		if (t == null)
			return Double.toString(i);
		String ret = new String(Double.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, double i) {
		if (t == null)
			return Double.toHexString(i);
		String ret = new String(Double.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toString(i);
		String ret = new String(Long.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(Taint t, long i, Taint t2, int r) {
		if (t == null)
			return Long.toString(i, r);
		String ret = new String(Long.toString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toBinaryString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toBinaryString(i);
		String ret = new String(Long.toBinaryString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toHexString(i);
		String ret = new String(Long.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toUnsignedString(i);
		String ret = new String(Long.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, long i, Taint tr, int r) {
		if (t == null)
			return Long.toUnsignedString(i, r);
		String ret = new String(Long.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, long i, Taint tr, int r, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toUnsignedString(i, r);
		String ret = new String(Long.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(Taint t, long i) {
		if (t == null)
			return Long.toOctalString(i);
		String ret = new String(Long.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, byte i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Byte.toString(i);
		String ret = new String(Byte.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, char i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Character.toString(i);
		String ret = new String(Character.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, int i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Integer.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, int i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Integer.toUnsignedString(i);
		String ret = new String(Integer.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, int i, Taint tr, int r, ControlTaintTagStack ctrl) {
		if (t == null)
			return Integer.toUnsignedString(i, r);
		String ret = new String(Integer.toUnsignedString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(Taint t, int i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Integer.toOctalString(i);
		String ret = new String(Integer.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, int i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Integer.toHexString(i);
		String ret = new String(Integer.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, short i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Short.toString(i);
		String ret = new String(Integer.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, boolean i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Boolean.toString(i);
		String ret = new String(Boolean.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, float i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Float.toString(i);
		String ret = new String(Float.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, float i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Float.toHexString(i);
		String ret = new String(Float.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, double i, ControlTaintTagStack ctrl) {
		char[] c = Double.toString$$PHOSPHORTAGGED(t, i, ctrl).value;
		String ret = new String(new LazyCharArrayObjTags(c), c, ctrl, null) ;
		if (t != null) {
			ret.setPHOSPHOR_TAG(t);
		}
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, double i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Double.toHexString(i);
		String ret = new String(Double.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toString$$PHOSPHORTAGGED(Taint t, long i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toString(i);
		String ret = new String(Long.toString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}
	
	public static String toString$$PHOSPHORTAGGED(Taint t, long i, Taint t2, int r, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toString(i, r);
		String ret = new String(Long.toString(i, r).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toBinaryString$$PHOSPHORTAGGED(Taint t, long i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toBinaryString(i);
		String ret = new String(Long.toBinaryString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toHexString$$PHOSPHORTAGGED(Taint t, long i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toHexString(i);
		String ret = new String(Long.toHexString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toUnsignedString$$PHOSPHORTAGGED(Taint t, long i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toUnsignedString(i);
		String ret = new String(Long.toUnsignedString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static String toOctalString$$PHOSPHORTAGGED(Taint t, long i, ControlTaintTagStack ctrl) {
		if (t == null)
			return Long.toOctalString(i);
		String ret = new String(Long.toOctalString(i).toCharArray());
		ret.setPHOSPHOR_TAG(t);
		return ret;
	}

	public static TaintedBooleanWithObjTag parseBoolean$$PHOSPHORTAGGED(String s, TaintedBooleanWithObjTag ret) {
		ret.val = Boolean.parseBoolean(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, TaintedByteWithObjTag ret) {
		ret.val = Byte.parseByte(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedByteWithObjTag ret) {
		try {
			ret.val = Byte.parseByte(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if (t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			} else if (t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		} catch (NumberFormatException ex) {
			Taint.combineTagsInPlace(ex, t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, TaintedIntWithObjTag ret) {
		ret.val = Integer.parseInt(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedIntWithObjTag ret) {
		try {
			ret.val = Integer.parseInt(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, TaintedIntWithObjTag ret) {
		ret.val = Integer.parseUnsignedInt(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedIntWithObjTag ret) {
		try {
			ret.val = Integer.parseUnsignedInt(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if (t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			} else if (t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		} catch (NumberFormatException ex) {
			Taint.combineTagsInPlace(ex, t);
			throw ex;
		}
	}

	public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, TaintedShortWithObjTag ret) {
		ret.val = Short.parseShort(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedShortWithObjTag ret) {
		try {
			ret.val = Short.parseShort(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if (t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			} else if (t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		} catch (NumberFormatException ex) {
			Taint.combineTagsInPlace(ex, t);
			throw ex;
		}
	}

	public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, TaintedLongWithObjTag ret) {
		ret.val = Long.parseLong(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedLongWithObjTag ret) {
		ret.val = Long.parseLong(s, radix);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, TaintedLongWithObjTag ret) {
		ret.val = Long.parseUnsignedLong(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint t, int radix, TaintedLongWithObjTag ret) {
		try {
			ret.val = Long.parseUnsignedLong(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if (t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			} else if (t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		} catch (NumberFormatException ex) {
			Taint.combineTagsInPlace(ex, t);
			throw ex;
		}
	}

	public static TaintedFloatWithObjTag parseFloat$$PHOSPHORTAGGED(String s, TaintedFloatWithObjTag ret) {
		ret.val = Float.parseFloat(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedDoubleWithObjTag parseDouble$$PHOSPHORTAGGED(String s, TaintedDoubleWithObjTag ret) {
		ret.val = Double.parseDouble(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedBooleanWithObjTag parseBoolean$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		ret.val = Boolean.parseBoolean(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedByteWithObjTag ret) {
		ret.val = Byte.parseByte(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedByteWithObjTag ret) {
		try{
		ret.val = Byte.parseByte(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Integer.parseInt(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		try{
		ret.val = Integer.parseInt(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = Integer.parseUnsignedInt(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		try{
		ret.val = Integer.parseUnsignedInt(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedShortWithObjTag ret) {
		ret.val = Short.parseShort(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedShortWithObjTag ret) {
		try{
		ret.val = Short.parseShort(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		ret.val = Long.parseLong(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		try{
		ret.val = Long.parseLong(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if(t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			}
			else if(t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		}catch(NumberFormatException ex){
			Taint.combineTagsInPlace(ex,t);
			throw ex;
		}
	}

	public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		ret.val = Long.parseUnsignedLong(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		try {
			ret.val = Long.parseUnsignedLong(s, radix);
			if (s != null) {
				ret.taint = (Taint) s.getPHOSPHOR_TAG();
				if (t != null)
					if(ret.taint == null)
						ret.taint = t.copy();
					else
						ret.taint.addDependency(t);
			} else if (t != null)
				ret.taint = t.copy();
			else
				ret.taint = null;
			return ret;
		} catch (NumberFormatException ex) {
			Taint.combineTagsInPlace(ex, t);
			throw ex;
		}
	}

	public static TaintedFloatWithObjTag parseFloat$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedFloatWithObjTag ret) {
		ret.val = Float.parseFloat(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	public static TaintedDoubleWithObjTag parseDouble$$PHOSPHORTAGGED(String s, ControlTaintTagStack ctrl, TaintedDoubleWithObjTag ret) {
		ret.val = Double.parseDouble(s);
		if(s != null)
			ret.taint = (Taint) s.getPHOSPHOR_TAG();
		else
			ret.taint = null;
		return ret;
	}

	private static int getTaint(Object o) {
		if(o == null)
			return 0;
		return ((TaintedWithIntTag) o).getPHOSPHOR_TAG();
	}

	public static TaintedBooleanWithIntTag parseBoolean$$PHOSPHORTAGGED(String s, TaintedBooleanWithIntTag ret) {
		ret.val = Boolean.parseBoolean(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedByteWithIntTag parseByte$$PHOSPHORTAGGED(String s, TaintedByteWithIntTag ret) {
		ret.val = Byte.parseByte(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedByteWithIntTag parseByte$$PHOSPHORTAGGED(String s, int t, int radix, TaintedByteWithIntTag ret) {
		ret.val = Byte.parseByte(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedIntWithIntTag parseInt$$PHOSPHORTAGGED(String s, TaintedIntWithIntTag ret) {
		ret.val = Integer.parseInt(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedIntWithIntTag parseInt$$PHOSPHORTAGGED(String s, int t, int radix, TaintedIntWithIntTag ret) {
		ret.val = Integer.parseInt(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedIntWithIntTag parseUnsignedInt$$PHOSPHORTAGGED(String s, TaintedIntWithIntTag ret) {
		ret.val = Integer.parseUnsignedInt(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedIntWithIntTag parseUnsignedInt$$PHOSPHORTAGGED(String s, int t, int radix, TaintedIntWithIntTag ret) {
		ret.val = Integer.parseUnsignedInt(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedShortWithIntTag parseShort$$PHOSPHORTAGGED(String s, TaintedShortWithIntTag ret) {
		ret.val = Short.parseShort(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedShortWithIntTag parseShort$$PHOSPHORTAGGED(String s, int t, int radix, TaintedShortWithIntTag ret) {
		ret.val = Short.parseShort(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedLongWithIntTag parseLong$$PHOSPHORTAGGED(String s, TaintedLongWithIntTag ret) {
		ret.val = Long.parseLong(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedLongWithIntTag parseLong$$PHOSPHORTAGGED(String s, int t, int radix, TaintedLongWithIntTag ret) {
		ret.val = Long.parseLong(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedLongWithIntTag parseUnsignedLong$$PHOSPHORTAGGED(String s, TaintedLongWithIntTag ret) {
		ret.val = Long.parseUnsignedLong(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedLongWithIntTag parseUnsignedLong$$PHOSPHORTAGGED(String s, int t, int radix, TaintedLongWithIntTag ret) {
		ret.val = Long.parseUnsignedLong(s, radix);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedFloatWithIntTag parseFloat$$PHOSPHORTAGGED(String s, TaintedFloatWithIntTag ret) {
		ret.val = Float.parseFloat(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static TaintedDoubleWithIntTag parseDouble$$PHOSPHORTAGGED(String s, TaintedDoubleWithIntTag ret) {
		ret.val = Double.parseDouble(s);
		ret.taint = getTaint(s);
		return ret;
	}

	public static Boolean valueOfZ$$PHOSPHORTAGGED(String s) {
		return valueOfZ(s);
	}

	public static Byte valueOfB$$PHOSPHORTAGGED(String s) {
		return valueOfB(s);
	}

	public static Short valueOfS$$PHOSPHORTAGGED(String s) {
		return valueOfS(s);
	}

	public static Boolean valueOfZ(String s) {
		if(s == null)
			return Boolean.FALSE;
		if (Configuration.MULTI_TAINTING)
			return BoxedPrimitiveStoreWithObjTags.valueOf((Taint) s.getPHOSPHOR_TAG(), Boolean.parseBoolean(s));
		else
			return BoxedPrimitiveStoreWithIntTags.valueOf(getTaint(s), Boolean.parseBoolean(s));
	}

	public static Byte valueOfB(String s) {
		if (Configuration.MULTI_TAINTING)
			return BoxedPrimitiveStoreWithObjTags.valueOf((Taint) s.getPHOSPHOR_TAG(), Byte.parseByte(s));
		else
			return BoxedPrimitiveStoreWithIntTags.valueOf(getTaint(s), Byte.parseByte(s));
	}

	public static Short valueOfS(String s) {
		if (Configuration.MULTI_TAINTING)
			return BoxedPrimitiveStoreWithObjTags.valueOf((Taint) s.getPHOSPHOR_TAG(), Short.parseShort(s));
		else
			return BoxedPrimitiveStoreWithIntTags.valueOf(getTaint(s), Short.parseShort(s));
	}

	public static Short valueOfS$$PHOSPHORTAGGED(String s, Taint t, int radix, ControlTaintTagStack ctrl) {
		return BoxedPrimitiveStoreWithObjTags.valueOf((Taint) s.getPHOSPHOR_TAG(), Short.parseShort(s, radix));
	}

	public static Short valueOfS$$PHOSPHORTAGGED(String s, Taint t, int radix) {
		return BoxedPrimitiveStoreWithObjTags.valueOf((Taint) s.getPHOSPHOR_TAG(), Short.parseShort(s, radix));
	}

	public static Short valueOfS$$PHOSPHORTAGGED(String s, int t, int radix) {
		return BoxedPrimitiveStoreWithIntTags.valueOf(getTaint(s), Short.parseShort(s, radix));
	}

	public static TaintedIntWithObjTag digit$$PHOSPHORTAGGED(Taint cTaint, char c, Taint rTaint, int radix, TaintedIntWithObjTag ret) {
		ret.val = Character.digit(c, radix);
		if (cTaint != null)
			ret.taint = cTaint;
		return ret;
	}

	public static TaintedIntWithObjTag digit$$PHOSPHORTAGGED(Taint cTaint, int codePoint, Taint rTaint, int radix, TaintedIntWithObjTag ret) {
		ret.val = Character.digit(codePoint, radix);
		if (cTaint != null)
			ret.taint = cTaint;
		return ret;
	}

	public static TaintedIntWithIntTag digit$$PHOSPHORTAGGED(int cTaint, char c, int rTaint, int radix, TaintedIntWithIntTag ret) {
		ret.val = Character.digit(c, radix);
		ret.taint = cTaint;
		return ret;
	}

	public static TaintedIntWithIntTag digit$$PHOSPHORTAGGED(int cTaint, int codePoint, int rTaint, int radix, TaintedIntWithIntTag ret) {
		ret.val = Character.digit(codePoint, radix);
		ret.taint = cTaint;
		return ret;
	}
}
