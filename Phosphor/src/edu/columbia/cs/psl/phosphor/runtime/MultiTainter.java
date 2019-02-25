package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.*;
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
	public static LazyBooleanArrayObjTags taintedBooleanArray$$PHOSPHORTAGGED(LazyBooleanArrayObjTags ret, boolean[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyByteArrayObjTags taintedByteArray$$PHOSPHORTAGGED(LazyByteArrayObjTags ret, byte[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyCharArrayObjTags taintedCharArray$$PHOSPHORTAGGED(LazyCharArrayObjTags ret, char[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyDoubleArrayObjTags taintedDoubleArray$$PHOSPHORTAGGED(LazyDoubleArrayObjTags ret, double[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyFloatArrayObjTags taintedFloatArray$$PHOSPHORTAGGED(LazyFloatArrayObjTags ret, float[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyIntArrayObjTags taintedIntArray$$PHOSPHORTAGGED(LazyIntArrayObjTags ret, int[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyShortArrayObjTags taintedShortArray$$PHOSPHORTAGGED(LazyShortArrayObjTags ret, short[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static LazyLongArrayObjTags taintedLongArray$$PHOSPHORTAGGED(LazyLongArrayObjTags ret, long[] in, Object lbl)
	{
		if(ret.taints == null)
			ret.taints = new Taint[in.length];
		for(int i =0 ; i < in.length; i++)
			ret.taints[i] = new Taint(lbl);
		return ret;
	}
	public static Taint getTaint$$PHOSPHORTAGGED(Object obj)
	{
		return getTaint(obj);
	}

	public static Taint[] getStringCharTaints(String str)
	{
		if(str == null)
			return null;
		return str.valuePHOSPHOR_TAG.taints;
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
			return ret;
		}
		else if(obj != null && ArrayHelper.engaged == 1)
		{
			Taint ret = ArrayHelper.getTag(obj);
			return ret;
		}
		else if(obj instanceof Boolean)
			return BoxedPrimitiveStoreWithObjTags.booleanValue((Boolean) obj).taint;
		else if(obj instanceof Byte)
			return BoxedPrimitiveStoreWithObjTags.byteValue((Byte)obj).taint;
		else if(obj instanceof Short)
			return BoxedPrimitiveStoreWithObjTags.shortValue((Short) obj).taint;
		else if(obj instanceof Character)
			return BoxedPrimitiveStoreWithObjTags.charValue((Character) obj).taint;
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
		else if(obj instanceof TaintedPrimitiveWithObjTag)
			((TaintedPrimitiveWithObjTag) obj).taint = tag;
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
}
