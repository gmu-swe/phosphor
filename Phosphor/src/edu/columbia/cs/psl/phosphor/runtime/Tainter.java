package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;

public class Tainter {
	public static void taintedObject(Object obj, int tag)
	{
		throw new IllegalStateException("Phosphor not engaged");
	}
	public static void taintedObject$$PHOSPHORTAGGED(Object obj, int tag_tag, int tag)
	{
		if(obj instanceof LazyArrayIntTags)
			obj = ((LazyArrayIntTags) obj).getVal();
		if(obj instanceof TaintedWithIntTag)
			((TaintedWithIntTag) obj).setPHOSPHOR_TAG(tag);
	}
	public static int getTaint(Object obj)
	{
		throw new IllegalStateException("Phosphor not engaged");
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(Object obj,TaintedIntWithIntTag ret)
	{
		if(obj instanceof TaintedWithIntTag)
			ret.val = ((TaintedWithIntTag) obj).getPHOSPHOR_TAG();
		return ret;
	}
	public static TaintedShortWithIntTag taintedShort$$PHOSPHORTAGGED(int i, short s, int z, int tag, TaintedShortWithIntTag ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}
	public static short taintedShort(short s, int tag)
	{
		return s;
	}
	public static double taintedDouble(double d, int tag){
		return d;
	}
	public static TaintedDoubleWithIntTag taintedDouble$$PHOSPHORTAGGED(int i, double s, int z, int tag, TaintedDoubleWithIntTag ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static float taintedFloat(float f, int tag)
	{
		return f;
	}
	public static TaintedFloatWithIntTag taintedFloat$$PHOSPHORTAGGED(int i, float s, int z, int tag, TaintedFloatWithIntTag ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static boolean taintedBoolean(boolean i, int tag)
	{
		return i;
	}
	public static TaintedBooleanWithIntTag taintedBoolean$$PHOSPHORTAGGED(int i, boolean s, int z, int tag, TaintedBooleanWithIntTag ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static byte taintedByte(byte i, int tag)
	{
		return i;
	}

	public static int taintedInt(int i, int tag)
	{
		return i;
	}
	public static char taintedChar(char c, int tag)
	{
		return c;
	}
	public static long taintedLong(long i, int tag) {
		return i;
	}
	
	public static char[] taintedCharArray(char[] ca, int tag)
	{
		return ca;
	}
	public static LazyCharArrayIntTags taintedCharArray$$PHOSPHORTAGGED(LazyCharArrayIntTags oldCA, char[] c, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static boolean[] taintedBooleanArray(boolean[] ca, int tag)
	{
		return ca;
	}
	public static LazyBooleanArrayIntTags taintedBooleanArray$$PHOSPHORTAGGED(LazyBooleanArrayIntTags oldCA, byte[] a, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static byte[] taintedByteArray(byte[] ca, int tag)
	{
		return ca;
	}
	public static LazyByteArrayIntTags taintedByteArray$$PHOSPHORTAGGED(LazyByteArrayIntTags oldCA, byte[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static double[] taintedDoubleArray(double[] ca, int tag)
	{
		return ca;
	}
	public static LazyDoubleArrayIntTags taintedDoubleArray$$PHOSPHORTAGGED(LazyDoubleArrayIntTags oldCA, double[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static float[] taintedFloatArray(float[] ca, int tag)
	{
		return ca;
	}
	public static LazyFloatArrayIntTags taintedFloatArray$$PHOSPHORTAGGED(LazyFloatArrayIntTags oldCA, float[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static int[] taintedIntArray(int[] ca, int tag)
	{
		return ca;
	}
	public static LazyIntArrayIntTags taintedIntArray$$PHOSPHORTAGGED(LazyIntArrayIntTags oldCA, int[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static long[] taintedLongArray(long[] ca, int tag)
	{
		return ca;
	}
	public static LazyLongArrayIntTags taintedLongArray$$PHOSPHORTAGGED(LazyLongArrayIntTags oldCA, long[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static short[] taintedShortArray(short[] ca, int tag)
	{
		return ca;
	}
	public static LazyShortArrayIntTags taintedShortArray$$PHOSPHORTAGGED(LazyShortArrayIntTags oldCA, short[] ca, int b, int tag)
	{
		oldCA.setTaints(tag);
		return oldCA;
	}
	public static void dumpTaint(byte i)
	{
		System.out.println("You called this without instrumentation? byte is " + i);
	}	
	public static void dumpTaint(int i)
	{
		System.out.println("You called this without instrumentation? int is " + i);
	}	
	
	public static TaintedByteWithIntTag taintedByte$$PHOSPHORTAGGED(int curTag, byte i, int tagTaint, int tag, TaintedByteWithIntTag ret)
	{
		ret.taint = tag;
		ret.val = i;
		return ret;
	}
	public static TaintedIntWithIntTag taintedInt$$PHOSPHORTAGGED(int curTag, int i, int tagTaint, int tag, TaintedIntWithIntTag ret)
	{
		ret.taint =tag;
		ret.val = i;
		return ret;
	}
	public static void dumpTaint(char c)
	{
		System.out.println("You called this without instrumentation? char is " + c);
	}
	public static TaintedCharWithIntTag taintedChar$$PHOSPHORTAGGED(int curTag, char c, int tagTaint, int tag,TaintedCharWithIntTag ret)
	{
		ret.taint = tag;
		ret.val =c;
		return ret;
	}
	public static int getTaint(char c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, char c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;	}
	public static int getTaint(byte c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, byte c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;	}
	public static int getTaint(boolean c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, boolean c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(int c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, int c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(short c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, short c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(long c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, long c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(float c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, float c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(double c)
	{
		return 0;
	}
	public static TaintedIntWithIntTag getTaint$$PHOSPHORTAGGED(int t, double c, TaintedIntWithIntTag ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	
	
	public static TaintedLongWithIntTag taintedLong$$PHOSPHORTAGGED(int curTag, long c, int tagTaint, int tag, TaintedLongWithIntTag ret)
	{
		ret.taint = tag;
		ret.val =c;
		return ret;
	}
	public static void dumpTaint(char[][][] c)
	{
		System.out.println("char c:" + c);
	}
	public static void dumpTaint$$PHOSPHORTAGGED(LazyCharArrayIntTags[][] ar)
	{
		System.out.println("its boxed");
	}
	public static void dumpTaint$$PHOSPHORTAGGED(int taint, char c)
	{
		System.out.println("Taint on int ("+c+"): " + taint);
	}
	public static void dumpTaint$$PHOSPHORTAGGED(int taint, byte c)
	{
		System.out.println("Taint on byte ("+c+"): " + taint);
	}
	public static void dumpTaint$$PHOSPHORTAGGED(int taint, int v)
	{
		System.out.println("Taint on int ("+v+"): " + taint);
	}
	public static void dumpTaint(Object[] res) {
//		System.out.println("Taint on " + Arrays.deepToString(res) +": " + Arrays.deepToString(((Object[]) ArrayObjectStore.get(res, 2,null))));
	}

	public static void dumpTaint(long longValue) {
		System.out.println("No taint/no instrument:" + longValue);
	}
	public static void dumpTaint$$PHOSPHORTAGGED(int taint, long longValue) {
		System.out.println("Taint on :" + longValue + " : " + taint);
	}
}
