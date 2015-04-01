package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedByte;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedChar;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedDouble;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloat;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedLong;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArray;

public class MultiTainter {
	static class Entry
	{
		String method;
		int n;
	}
	public static void printMethod(Method m)
	{
		System.out.println(m);
		System.out.println(Arrays.toString(m.getAnnotations()));
	}
	public static void printClass(Object o)
	{
		System.out.println("Dump " + o + ": " + o.getClass());
	}

	public static TaintedShort taintedShort$$PHOSPHORTAGGED(int i, short s, int z, int tag, TaintedShort ret)
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
	public static TaintedDouble taintedDouble$$PHOSPHORTAGGED(int i, double s, int z, int tag, TaintedDouble ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static float taintedFloat(float f, int tag)
	{
		return f;
	}
	public static TaintedFloat taintedFloat$$PHOSPHORTAGGED(int i, float s, int z, int tag, TaintedFloat ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static boolean taintedBoolean(boolean i, int tag)
	{
		return i;
	}
	public static TaintedBoolean taintedBoolean$$PHOSPHORTAGGED(int i, boolean s, int z, int tag, TaintedBoolean ret)
	{
		ret.taint = tag;
		ret.val = s;
		return ret;
	}

	public static byte taintedByte(byte i, int tag)
	{
		return i;
	}

	public static int taintedInt(int i, String tag)
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
	public static TaintedCharArray taintedCharArray$$PHOSPHORTAGGED(int[] oldCA, char[] ca, int b, int tag, TaintedCharArray ret)
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
	public static TaintedBooleanArray taintedBooleanArray$$PHOSPHORTAGGED(int[] oldCA, boolean[] ca, int b, int tag, TaintedBooleanArray ret)
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
	public static TaintedByteArray taintedByteArray$$PHOSPHORTAGGED(int[] oldCA, byte[] ca, int b, int tag, TaintedByteArray ret)
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
	public static TaintedDoubleArray taintedDoubleArray$$PHOSPHORTAGGED(int[] oldCA, double[] ca, int b, int tag, TaintedDoubleArray ret)
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
	public static TaintedFloatArray taintedFloatArray$$PHOSPHORTAGGED(int[] oldCA, float[] ca, int b, int tag, TaintedFloatArray ret)
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
	public static TaintedIntArray taintedIntArray$$PHOSPHORTAGGED(int[] oldCA, int[] ca, int b, int tag, TaintedIntArray ret)
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
	public static TaintedLongArray taintedLongArray$$PHOSPHORTAGGED(int[] oldCA, long[] ca, int b, int tag, TaintedLongArray ret)
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
	public static TaintedShortArray taintedShortArray$$PHOSPHORTAGGED(int[] oldCA, short[] ca, int b, int tag, TaintedShortArray ret)
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
	
	public static TaintedByte taintedByte$$PHOSPHORTAGGED(int curTag, byte i, int tagTaint, int tag, TaintedByte ret)
	{
		ret.taint = tag;
		ret.val = i;
		return ret;
	}
	public static TaintedInt taintedInt$$PHOSPHORTAGGED(int curTag, int i, String tag, TaintedInt ret)
	{
		ret.taint =SimpleMultiTaintHandler.nextTag(tag);
		ret.val = i;
		return ret;
	}
	public static void dumpTaint(char c)
	{
		System.out.println("You called this without instrumentation? char is " + c);
	}
	public static TaintedChar taintedChar$$PHOSPHORTAGGED(int curTag, char c, int tagTaint, int tag,TaintedChar ret)
	{
		ret.taint = tag;
		ret.val =c;
		return ret;
	}
	public static int getTaint(char c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, char c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;	}
	public static int getTaint(byte c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, byte c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;	}
	public static int getTaint(boolean c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, boolean c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(int c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, int c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(short c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, short c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(long c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, long c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(float c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, float c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	public static int getTaint(double c)
	{
		return 0;
	}
	public static TaintedInt getTaint$$PHOSPHORTAGGED(int t, double c, TaintedInt ret)
	{
		ret.taint = t;
		ret.val = t;
		return ret;
	}
	
	
	public static TaintedLong taintedLong$$PHOSPHORTAGGED(int curTag, long c, int tagTaint, int tag, TaintedLong ret)
	{
		ret.taint = tag;
		ret.val =c;
		return ret;
	}
	public static void dumpTaint(char[][][] c)
	{
		System.out.println("char c:" + c);
	}
	public static void dumpTaint$$PHOSPHORTAGGED(MultiDTaintedCharArray[][] ar)
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
