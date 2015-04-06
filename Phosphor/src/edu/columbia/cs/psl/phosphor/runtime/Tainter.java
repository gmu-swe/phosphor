package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithIntTag;

public class Tainter {
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
	public static TaintedCharArrayWithIntTag taintedCharArray$$PHOSPHORTAGGED(int[] oldCA, char[] ca, int b, int tag, TaintedCharArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static boolean[] taintedBooleanArray(boolean[] ca, int tag)
	{
		return ca;
	}
	public static TaintedBooleanArrayWithIntTag taintedBooleanArray$$PHOSPHORTAGGED(int[] oldCA, boolean[] ca, int b, int tag, TaintedBooleanArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static byte[] taintedByteArray(byte[] ca, int tag)
	{
		return ca;
	}
	public static TaintedByteArrayWithIntTag taintedByteArray$$PHOSPHORTAGGED(int[] oldCA, byte[] ca, int b, int tag, TaintedByteArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static double[] taintedDoubleArray(double[] ca, int tag)
	{
		return ca;
	}
	public static TaintedDoubleArrayWithIntTag taintedDoubleArray$$PHOSPHORTAGGED(int[] oldCA, double[] ca, int b, int tag, TaintedDoubleArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static float[] taintedFloatArray(float[] ca, int tag)
	{
		return ca;
	}
	public static TaintedFloatArrayWithIntTag taintedFloatArray$$PHOSPHORTAGGED(int[] oldCA, float[] ca, int b, int tag, TaintedFloatArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static int[] taintedIntArray(int[] ca, int tag)
	{
		return ca;
	}
	public static TaintedIntArrayWithIntTag taintedIntArray$$PHOSPHORTAGGED(int[] oldCA, int[] ca, int b, int tag, TaintedIntArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static long[] taintedLongArray(long[] ca, int tag)
	{
		return ca;
	}
	public static TaintedLongArrayWithIntTag taintedLongArray$$PHOSPHORTAGGED(int[] oldCA, long[] ca, int b, int tag, TaintedLongArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
	}
	public static short[] taintedShortArray(short[] ca, int tag)
	{
		return ca;
	}
	public static TaintedShortArrayWithIntTag taintedShortArray$$PHOSPHORTAGGED(int[] oldCA, short[] ca, int b, int tag, TaintedShortArrayWithIntTag ret)
	{
		ret.val = ca;
		ret.taint = new int[ca.length];
		for(int i = 0; i < ca.length; i++)
			ret.taint[i] = tag;
		return ret;
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
	public static void dumpTaint$$PHOSPHORTAGGED(MultiDTaintedCharArrayWithIntTag[][] ar)
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
