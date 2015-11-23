package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public final class MultiTainter {
	public static ControlTaintTagStack getControlFlow()
	{
		throw new IllegalStateException();
	}
	public static ControlTaintTagStack getControlFlow$$PHOSPHORTAGGED(ControlTaintTagStack ctrl)
	{
		return ctrl;
	}
	public static boolean taintedBoolean(boolean in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static byte taintedByte(byte in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static char taintedChar(char in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static double taintedDouble(double in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static float taintedFloat(float in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static long taintedLong(long in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static int taintedInt(int in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static short taintedShort(short in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static boolean[] taintedBooleanArray(boolean[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static byte[] taintedByteArray(byte[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static char[] taintedCharArray(char[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static double[] taintedDoubleArray(double[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static float[] taintedFloatArray(float[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static long[] taintedLongArray(long[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static int[] taintedIntArray(int[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static short[] taintedShortArray(short[] in, Object lbl)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(boolean in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(byte in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(char in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(double in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(float in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(int in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(long in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	public static Taint getTaint(short in)
	{
		throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
	}
	
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, boolean b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, byte b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, char b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, double b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, float b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, int b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, long b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, short b)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, boolean b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, byte b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, char b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, double b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, float b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, int b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, long b, Object[] prealloc)
	{
		return t;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Taint t, short b, Object[] prealloc)
	{
		return t;
	}
	
	public static TaintedBooleanWithObjTag taintedBoolean$$PHOSPHORTAGGED(Taint oldTag, boolean in, Object lbl, TaintedBooleanWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedByteWithObjTag taintedByte$$PHOSPHORTAGGED(Taint oldTag, byte in, Object lbl, TaintedByteWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedCharWithObjTag taintedChar$$PHOSPHORTAGGED(Taint oldTag, char in, Object lbl, TaintedCharWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedDoubleWithObjTag taintedDouble$$PHOSPHORTAGGED(Taint oldTag, double in, Object lbl, TaintedDoubleWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedFloatWithObjTag taintedFloat$$PHOSPHORTAGGED(Taint oldTag, float in, Object lbl, TaintedFloatWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedIntWithObjTag taintedInt$$PHOSPHORTAGGED(Taint oldTag, int in, Object lbl, TaintedIntWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedLongWithObjTag taintedLong$$PHOSPHORTAGGED(Taint oldTag, long in, Object lbl, TaintedLongWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedShortWithObjTag taintedShort$$PHOSPHORTAGGED(Taint oldTag, short in, Object lbl, TaintedShortWithObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedBooleanArrayWithObjTag taintedBooleanArray$$PHOSPHORTAGGED(Taint[] oldTag, boolean[] in, Object lbl, TaintedBooleanArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedByteArrayWithObjTag taintedByteArray$$PHOSPHORTAGGED(Taint[] oldTag, byte[] in, Object lbl, TaintedByteArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedCharArrayWithObjTag taintedCharArray$$PHOSPHORTAGGED(Taint[] oldTag, char[] in, Object lbl, TaintedCharArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedDoubleArrayWithObjTag taintedDoubleArray$$PHOSPHORTAGGED(Taint[] oldTag, double[] in, Object lbl, TaintedDoubleArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedFloatArrayWithObjTag taintedFloatArray$$PHOSPHORTAGGED(Taint[] oldTag, float[] in, Object lbl, TaintedFloatArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedIntArrayWithObjTag taintedIntArray$$PHOSPHORTAGGED(Taint[] oldTag, int[] in, Object lbl, TaintedIntArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedIntArrayWithSingleObjTag taintedIntArray$$PHOSPHORTAGGED(Taint oldTag, int[] in, Object lbl, TaintedIntArrayWithSingleObjTag ret)
	{
		ret.taint = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedShortArrayWithObjTag taintedShortArray$$PHOSPHORTAGGED(Taint[] oldTag, short[] in, Object lbl, TaintedShortArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	public static TaintedLongArrayWithObjTag taintedLongArray$$PHOSPHORTAGGED(Taint[] oldTag, long[] in, Object lbl, TaintedLongArrayWithObjTag ret)
	{
		ret.taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taint[i] = new Taint(lbl);
		ret.val = in;
		return ret;
	}
	
	public static TaintedBooleanWithObjTag taintedBoolean$$PHOSPHORTAGGED(Taint oldTag, boolean in, Object lbl, Object[] ret)
	{
		((TaintedBooleanWithObjTag) ret[TaintUtils.PREALLOC_BOOLEAN]).taint = new Taint(lbl);
		((TaintedBooleanWithObjTag) ret[TaintUtils.PREALLOC_BOOLEAN]).val = in;
		return ((TaintedBooleanWithObjTag) ret[TaintUtils.PREALLOC_BOOLEAN]);
	}
	public static TaintedByteWithObjTag taintedByte$$PHOSPHORTAGGED(Taint oldTag, byte in, Object lbl, Object[] ret)
	{
		((TaintedByteWithObjTag) ret[TaintUtils.PREALLOC_BYTE]).taint = new Taint(lbl);
		((TaintedByteWithObjTag) ret[TaintUtils.PREALLOC_BYTE]).val = in;
		return ((TaintedByteWithObjTag) ret[TaintUtils.PREALLOC_BYTE]);
	}
	public static TaintedCharWithObjTag taintedChar$$PHOSPHORTAGGED(Taint oldTag, char in, Object lbl, Object[] ret)
	{
		((TaintedCharWithObjTag) ret[TaintUtils.PREALLOC_CHAR]).taint = new Taint(lbl);
		((TaintedCharWithObjTag) ret[TaintUtils.PREALLOC_CHAR]).val = in;
		return ((TaintedCharWithObjTag) ret[TaintUtils.PREALLOC_CHAR]);
	}
	public static TaintedDoubleWithObjTag taintedDouble$$PHOSPHORTAGGED(Taint oldTag, double in, Object lbl, Object[] ret)
	{
		((TaintedDoubleWithObjTag) ret[TaintUtils.PREALLOC_DOUBLE]).taint = new Taint(lbl);
		((TaintedDoubleWithObjTag) ret[TaintUtils.PREALLOC_DOUBLE]).val = in;
		return ((TaintedDoubleWithObjTag) ret[TaintUtils.PREALLOC_DOUBLE]);
	}
	public static TaintedFloatWithObjTag taintedFloat$$PHOSPHORTAGGED(Taint oldTag, float in, Object lbl, Object[] ret)
	{
		((TaintedFloatWithObjTag) ret[TaintUtils.PREALLOC_FLOAT]).taint = new Taint(lbl);
		((TaintedFloatWithObjTag) ret[TaintUtils.PREALLOC_FLOAT]).val = in;
		return ((TaintedFloatWithObjTag) ret[TaintUtils.PREALLOC_FLOAT]);
	}
	public static TaintedIntWithObjTag taintedInt$$PHOSPHORTAGGED(Taint oldTag, int in, Object lbl, Object[] ret)
	{
		((TaintedIntWithObjTag) ret[TaintUtils.PREALLOC_INT]).taint = new Taint(lbl);
		((TaintedIntWithObjTag) ret[TaintUtils.PREALLOC_INT]).val = in;
		return ((TaintedIntWithObjTag) ret[TaintUtils.PREALLOC_INT]);
	}
	public static TaintedLongWithObjTag taintedLong$$PHOSPHORTAGGED(Taint oldTag, long in, Object lbl, Object[] ret)
	{
		((TaintedLongWithObjTag) ret[TaintUtils.PREALLOC_LONG]).taint = new Taint(lbl);
		((TaintedLongWithObjTag) ret[TaintUtils.PREALLOC_LONG]).val = in;
		return ((TaintedLongWithObjTag) ret[TaintUtils.PREALLOC_LONG]);
	}
	public static TaintedShortWithObjTag taintedShort$$PHOSPHORTAGGED(Taint oldTag, short in, Object lbl, Object[] ret)
	{
		((TaintedShortWithObjTag) ret[TaintUtils.PREALLOC_SHORT]).taint = new Taint(lbl);
		((TaintedShortWithObjTag) ret[TaintUtils.PREALLOC_SHORT]).val = in;
		return ((TaintedShortWithObjTag) ret[TaintUtils.PREALLOC_SHORT]);
	}
	public static TaintedBooleanArrayWithObjTag taintedBooleanArray$$PHOSPHORTAGGED(Taint[] oldTag, boolean[] in, Object lbl, Object[] ret)
	{
		((TaintedBooleanArrayWithObjTag)ret[TaintUtils.PREALLOC_BOOLEANARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedBooleanArrayWithObjTag)ret[TaintUtils.PREALLOC_BOOLEANARRAY]).taint[i] = new Taint(lbl);
		((TaintedBooleanArrayWithObjTag)ret[TaintUtils.PREALLOC_BOOLEANARRAY]).val = in;
		return ((TaintedBooleanArrayWithObjTag)ret[TaintUtils.PREALLOC_BOOLEANARRAY]);
	}
	public static TaintedByteArrayWithObjTag taintedByteArray$$PHOSPHORTAGGED(Taint[] oldTag, byte[] in, Object lbl, Object[] ret)
	{
		((TaintedByteArrayWithObjTag)ret[TaintUtils.PREALLOC_BYTEARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedByteArrayWithObjTag)ret[TaintUtils.PREALLOC_BYTEARRAY]).taint[i] = new Taint(lbl);
		((TaintedByteArrayWithObjTag)ret[TaintUtils.PREALLOC_BYTEARRAY]).val = in;
		return ((TaintedByteArrayWithObjTag)ret[TaintUtils.PREALLOC_BYTEARRAY]);
	}
	public static TaintedCharArrayWithObjTag taintedCharArray$$PHOSPHORTAGGED(Taint[] oldTag, char[] in, Object lbl, Object[] ret)
	{
		((TaintedCharArrayWithObjTag)ret[TaintUtils.PREALLOC_CHARARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedCharArrayWithObjTag)ret[TaintUtils.PREALLOC_CHARARRAY]).taint[i] = new Taint(lbl);
		((TaintedCharArrayWithObjTag)ret[TaintUtils.PREALLOC_CHARARRAY]).val = in;
		return ((TaintedCharArrayWithObjTag)ret[TaintUtils.PREALLOC_CHARARRAY]);
	}
	public static TaintedDoubleArrayWithObjTag taintedDoubleArray$$PHOSPHORTAGGED(Taint[] oldTag, double[] in, Object lbl, Object[] ret)
	{
		((TaintedDoubleArrayWithObjTag)ret[TaintUtils.PREALLOC_DOUBLEARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedDoubleArrayWithObjTag)ret[TaintUtils.PREALLOC_DOUBLEARRAY]).taint[i] = new Taint(lbl);
		((TaintedDoubleArrayWithObjTag)ret[TaintUtils.PREALLOC_DOUBLEARRAY]).val = in;
		return ((TaintedDoubleArrayWithObjTag)ret[TaintUtils.PREALLOC_DOUBLEARRAY]);
	}
	public static TaintedFloatArrayWithObjTag taintedFloatArray$$PHOSPHORTAGGED(Taint[] oldTag, float[] in, Object lbl, Object[] ret)
	{
		((TaintedFloatArrayWithObjTag)ret[TaintUtils.PREALLOC_FLOATARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedFloatArrayWithObjTag)ret[TaintUtils.PREALLOC_FLOATARRAY]).taint[i] = new Taint(lbl);
		((TaintedFloatArrayWithObjTag)ret[TaintUtils.PREALLOC_FLOATARRAY]).val = in;
		return ((TaintedFloatArrayWithObjTag)ret[TaintUtils.PREALLOC_FLOATARRAY]);
	}
	public static TaintedIntArrayWithObjTag taintedIntArray$$PHOSPHORTAGGED(Taint[] oldTag, int[] in, Object lbl, Object[] ret)
	{
		((TaintedIntArrayWithObjTag)ret[TaintUtils.PREALLOC_INTARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedIntArrayWithObjTag)ret[TaintUtils.PREALLOC_INTARRAY]).taint[i] = new Taint(lbl);
		((TaintedIntArrayWithObjTag)ret[TaintUtils.PREALLOC_INTARRAY]).val = in;
		return ((TaintedIntArrayWithObjTag)ret[TaintUtils.PREALLOC_INTARRAY]);
	}
	public static TaintedShortArrayWithObjTag taintedShortArray$$PHOSPHORTAGGED(Taint[] oldTag, short[] in, Object lbl, Object[] ret)
	{
		((TaintedShortArrayWithObjTag)ret[TaintUtils.PREALLOC_SHORTARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedShortArrayWithObjTag)ret[TaintUtils.PREALLOC_SHORTARRAY]).taint[i] = new Taint(lbl);
		((TaintedShortArrayWithObjTag)ret[TaintUtils.PREALLOC_SHORTARRAY]).val = in;
		return ((TaintedShortArrayWithObjTag)ret[TaintUtils.PREALLOC_SHORTARRAY]);
	}
	public static TaintedLongArrayWithObjTag taintedLongArray$$PHOSPHORTAGGED(Taint[] oldTag, long[] in, Object lbl, Object[] ret)
	{
		((TaintedLongArrayWithObjTag)ret[TaintUtils.PREALLOC_LONGARRAY]).taint = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			((TaintedLongArrayWithObjTag)ret[TaintUtils.PREALLOC_LONGARRAY]).taint[i] = new Taint(lbl);
		((TaintedLongArrayWithObjTag)ret[TaintUtils.PREALLOC_LONGARRAY]).val = in;
		return ((TaintedLongArrayWithObjTag)ret[TaintUtils.PREALLOC_LONGARRAY]);
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Object obj)
	{
		return getTaint(obj);
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Object obj,Object[] prealloc)
	{
		return getTaint(obj);
	}

	public static final Taint getTaint(Object obj)
	{
		if(obj instanceof MultiDTaintedArrayWithObjTag)
			obj = ((MultiDTaintedArrayWithObjTag) obj).getVal();
		if(Configuration.taintTagFactory == null)
			return null;
		if(obj instanceof TaintedWithObjTag)
		{
			Taint ret = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
			if(ret == null)
			{
				ret = Configuration.taintTagFactory.dynamicallyGenerateEmptyTaint();
				taintedObject(obj, ret);
			}
			return ret;
		}
		else if(obj != null && ArrayHelper.engaged == 1)
		{
			Taint ret = ArrayHelper.getTag(obj);
			if(ret == null)
			{
				ret = Configuration.taintTagFactory.dynamicallyGenerateEmptyTaint();
				taintedObject(obj, ret);
			}
			return ret;
		}
		else
			return null;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Object obj, ControlTaintTagStack ctrl)
	{
		return getTaint(obj);
	}
	public static void taintedObject(Object obj, Taint tag)
	{
		if(obj instanceof MultiDTaintedArrayWithObjTag)
			obj = ((MultiDTaintedArrayWithObjTag) obj).getVal();
		if(obj instanceof TaintedWithObjTag)
			((TaintedWithObjTag) obj).setPHOSPHOR_TAG(tag);
		else if(obj != null && ArrayHelper.engaged == 1)
			ArrayHelper.setTag(obj, tag);
	}
	public static void taintedObject$$PHOSPHORTAGGED(Object obj, Taint tag, ControlTaintTagStack ctrl)
	{
		taintedObject(obj, tag);
	}
	public static void taintedObject$$PHOSPHORTAGGED(Object obj, Taint tag)
	{
		taintedObject(obj, tag);
	}
	public static void taintedObject$$PHOSPHORTAGGED(Object obj, Taint tag, Object[] prealloc)
	{
		taintedObject(obj, tag);
	}
}
