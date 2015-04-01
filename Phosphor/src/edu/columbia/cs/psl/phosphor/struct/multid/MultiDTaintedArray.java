package edu.columbia.cs.psl.phosphor.struct.multid;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;

public abstract class MultiDTaintedArray {
	public static final long serialVersionUID = 40523489234L;
	public int sort;
	public int length;

	public boolean hasTaints()
	{
		for(int i : taint)
			if(i != 0)
				return true;
		return false;
	}
	public void setTaints(int t)
	{
		for(int i = 0; i < taint.length; i++)
		{
			taint[i] = t;
		}
	}
	public MultiDTaintedArray() {

	}

	public abstract Object getVal();

	public final int getSort() {
		return sort;
	}

	protected MultiDTaintedArray(int[] taint, int sort) {
		this.taint = taint;
		this.sort = sort;
		this.length = taint.length;
	}

	public int[] taint;

	public abstract Object clone();

	public final int hashCode() {
		return getVal().hashCode();
	}

	public final TaintedInt hashCode$$PHOSPHORTAGGED(TaintedInt ret) {
		ret.taint = 0;
		ret.val = hashCode();
		return ret;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof MultiDTaintedArray)
			return getVal().equals(((MultiDTaintedArray) obj).getVal());
		return getVal().equals(obj);
	}

	public static final Type getTypeForType(final Type originalElementType) {
		if (originalElementType.getSort() != Type.ARRAY)
			throw new IllegalArgumentException("passed " + originalElementType);
		Class clazz = getClassForComponentType(originalElementType.getElementType().getSort());
		return Type.getType(originalElementType.getDescriptor().substring(0, originalElementType.getDescriptor().length() - 2) + Type.getDescriptor(clazz));
	}

	@Override
	public String toString() {
		switch (sort) {
		case Type.BOOLEAN:
			return "[Z";
		case Type.BYTE:
			return "[B";
		case Type.CHAR:
			return "[C";
		case Type.DOUBLE:
			return "[D";
		case Type.INT:
			return "[I";
		case Type.FLOAT:
			return "[F";
		case Type.LONG:
			return "[J";
		case Type.SHORT:
			return "[S";
		}
		return super.toString();
	}

	public static final String isPrimitiveBoxClass(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == MultiDTaintedDoubleArray.class)
			return "D";
		if (c == MultiDTaintedFloatArray.class)
			return "F";
		if (c == MultiDTaintedIntArray.class)
			return "I";
		if (c == MultiDTaintedLongArray.class)
			return "J";
		if (c == MultiDTaintedShortArray.class)
			return "S";
		if (c == MultiDTaintedBooleanArray.class)
			return "Z";
		if (c == MultiDTaintedByteArray.class)
			return "B";
		if (c == MultiDTaintedCharArray.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == MultiDTaintedDoubleArray.class)
			return "D";
		if (c == MultiDTaintedFloatArray.class)
			return "F";
		if (c == MultiDTaintedIntArray.class)
			return "I";
		if (c == MultiDTaintedLongArray.class)
			return "J";
		if (c == MultiDTaintedShortArray.class)
			return "S";
		if (c == MultiDTaintedBooleanArray.class)
			return "Z";
		if (c == MultiDTaintedByteArray.class)
			return "B";
		if (c == MultiDTaintedCharArray.class)
			return "C";
		throw new IllegalStateException("Got passed class: " + c);

	}

	public static final Class getUnderlyingBoxClassForUnderlyingClass(Class c)
	{
		int dims = 0;
		if(c.isArray())
		{
			while(c.isArray())
			{
				c = c.getComponentType();
				dims++;
			}
		}
		
		if(dims == 1)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArray.class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArray.class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArray.class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArray.class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArray.class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArray.class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArray.class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArray.class;
		}
		else 
			if(dims == 2)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArray[].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArray[].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArray[].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArray[].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArray[].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArray[].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArray[].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArray[].class;
		}
		else if(dims == 3)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArray[][].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArray[][].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArray[][].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArray[][].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArray[][].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArray[][].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArray[][].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArray[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: "+dims);
	}
	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return MultiDTaintedBooleanArray.class;
		case Type.BYTE:
			return MultiDTaintedByteArray.class;
		case Type.CHAR:
			return MultiDTaintedCharArray.class;
		case Type.DOUBLE:
			return MultiDTaintedDoubleArray.class;
		case Type.FLOAT:
			return MultiDTaintedFloatArray.class;
		case Type.INT:
			return MultiDTaintedIntArray.class;
		case Type.LONG:
			return MultiDTaintedLongArray.class;
		case Type.SHORT:
			return MultiDTaintedShortArray.class;
		default:
			throw new IllegalArgumentException("invalid sort: " + componentSort);
		}
	}

	public static final Object unboxRaw(final Object in) {
		if(in == null)
			return null;
		if (!in.getClass().isArray()) {
			return unboxVal(in, getSortForBoxClass(in.getClass()), 0);
		}
		Class tmp = in.getClass();
		int dims = 0;
		while(tmp.isArray())
		{
			tmp = tmp.getComponentType();
			dims++;
		}
		return unboxVal((Object[]) in, getSortForBoxClass(tmp), dims);
	}

	public static final Object unboxVal(final Object _in, final int componentType, final int dims) {

		if (dims == 0) {
			switch (componentType) {
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.DOUBLE:
			case Type.FLOAT:
			case Type.INT:
			case Type.LONG:
			case Type.SHORT:
				return ((MultiDTaintedArray) _in).getVal();
			default:
				throw new IllegalArgumentException();
			}
		} else if (dims == 1) {
			final Object[] in = (Object[]) _in;
			switch (componentType) {
			case Type.BOOLEAN:
				boolean[][] retz = new boolean[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retz[i] = ((MultiDTaintedBooleanArray) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((MultiDTaintedByteArray) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((MultiDTaintedCharArray) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((MultiDTaintedDoubleArray) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((MultiDTaintedFloatArray) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((MultiDTaintedIntArray) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((MultiDTaintedLongArray) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((MultiDTaintedShortArray) in[i]).val;
				return rets;
			}
		} else if (dims == 2) {
			final Object[][] ina = (Object[][]) _in;
			final Object[] in = (Object[]) _in;
			switch (componentType) {
			case Type.BOOLEAN:
				boolean[][][] retz = new boolean[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retz[i] = new boolean[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retz[i][j] = ((MultiDTaintedBooleanArray) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((MultiDTaintedByteArray) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((MultiDTaintedCharArray) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((MultiDTaintedDoubleArray) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((MultiDTaintedFloatArray) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((MultiDTaintedIntArray) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((MultiDTaintedLongArray) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((MultiDTaintedShortArray) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}
	public static int getSortForBoxClass(Class c)
	{
		if(c == MultiDTaintedIntArray.class)
			return Type.INT;
		if(c == MultiDTaintedBooleanArray.class)
			return Type.BOOLEAN;
		if(c == MultiDTaintedByteArray.class)
			return Type.BYTE;
		if(c == MultiDTaintedFloatArray.class)
			return Type.FLOAT;
		if(c == MultiDTaintedCharArray.class)
			return Type.CHAR;
		if(c == MultiDTaintedDoubleArray.class)
			return Type.DOUBLE;
		if(c == MultiDTaintedLongArray.class)
			return Type.LONG;
		if(c == MultiDTaintedShortArray.class)
			return Type.SHORT;
		throw new IllegalArgumentException();
	}
	public static int getSort(Class c)
	{
		if(c == Integer.TYPE)
			return Type.INT;
		if(c == Boolean.TYPE)
			return Type.BOOLEAN;
		if(c == Byte.TYPE)
			return Type.BYTE;
		if(c == Float.TYPE)
			return Type.FLOAT;
		if(c == Character.TYPE)
			return Type.CHAR;
		if(c == Double.TYPE)
			return Type.DOUBLE;
		if(c == Long.TYPE)
			return Type.LONG;
		if(c == Short.TYPE)
			return Type.SHORT;
		throw new IllegalArgumentException();
	}
	public static final Object boxIfNecessary(final Object in) {
		if (in != null && in.getClass().isArray()) {
			if (in.getClass().getComponentType().isPrimitive()) {
				//Is prim arraytype
				Class tmp = in.getClass();
				int dims = 0;
				while(tmp.isArray())
				{
					tmp = tmp.getComponentType();
					dims++;
				}
				if (dims > 1) { //this should never be possible.
					Type t = Type.getType(in.getClass());
					initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
				} else {
					if(tmp == Boolean.TYPE)
						return new MultiDTaintedBooleanArray(new int[(((boolean[]) in)).length], ((boolean[]) in));
					if(tmp == Byte.TYPE)
						return new MultiDTaintedByteArray(new int[(((byte[]) in)).length], ((byte[]) in));
					if(tmp == Character.TYPE)
						return new MultiDTaintedCharArray(new int[(((char[]) in)).length], ((char[]) in));
					if(tmp == Double.TYPE)
						return new MultiDTaintedDoubleArray(new int[(((double[]) in)).length], ((double[]) in));
					if(tmp == Float.TYPE)
						return new MultiDTaintedFloatArray(new int[(((float[]) in)).length], ((float[]) in));
					if(tmp == Integer.TYPE)
						return new MultiDTaintedIntArray(new int[(((int[]) in)).length], ((int[]) in));
					if(tmp == Long.TYPE)
						return new MultiDTaintedLongArray(new int[(((long[]) in)).length], ((long[]) in));
					if(tmp == Short.TYPE)
						return new MultiDTaintedShortArray(new int[(((short[]) in)).length], ((short[]) in));
						throw new IllegalArgumentException();
				}
			}
		}
		return in;
	}

	public static final Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
		if (dims == 2) {
			Object[] ret;
			switch (componentType) {
			case Type.BOOLEAN:
				ret = new MultiDTaintedBooleanArray[ar.length];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArray[ar.length];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArray[ar.length];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArray[ar.length];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArray[ar.length];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArray[ar.length];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArray[ar.length];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArray[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArray(new int[(((boolean[]) entry)).length], ((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArray(new int[(((byte[]) entry)).length], ((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArray(new int[(((char[]) entry)).length], ((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArray(new int[(((double[]) entry)).length], ((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArray(new int[(((float[]) entry)).length], ((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArray(new int[(((int[]) entry)).length], ((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArray(new int[(((long[]) entry)).length], ((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArray(new int[(((short[]) entry)).length], ((short[]) entry));
						break;
					default:
						throw new IllegalArgumentException();
					}
				}
			}
			return ret;
		} else if (dims == 3) {
			Object[][] ret;
			switch (componentType) {
			case Type.BOOLEAN:
				ret = new MultiDTaintedBooleanArray[ar.length][];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArray[ar.length][];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArray[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArray[ar.length][];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArray[ar.length][];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArray[ar.length][];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArray[ar.length][];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArray[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArray[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArray[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArray[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArray[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArray[entry1.length];
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArray[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArray[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArray[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new MultiDTaintedBooleanArray(new int[(((boolean[]) entry)).length], ((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new MultiDTaintedByteArray(new int[(((byte[]) entry)).length], ((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new MultiDTaintedCharArray(new int[(((char[]) entry)).length], ((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new MultiDTaintedDoubleArray(new int[(((double[]) entry)).length], ((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new MultiDTaintedFloatArray(new int[(((float[]) entry)).length], ((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new MultiDTaintedIntArray(new int[(((int[]) entry)).length], ((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new MultiDTaintedLongArray(new int[(((long[]) entry)).length], ((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new MultiDTaintedShortArray(new int[(((short[]) entry)).length], ((short[]) entry));
							break;
						default:
							throw new IllegalArgumentException();
						}
					}
				}
			}
			return ret;
		}
		throw new IllegalArgumentException();
	}

	public static final void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
		for (int i = 0; i < ar.length; i++) {
			if (ar[i] == null) {
				switch (componentType) {
				case Type.BOOLEAN:
					ar[i] = new MultiDTaintedBooleanArray(new int[lastDimSize], new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new MultiDTaintedByteArray(new int[lastDimSize], new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new MultiDTaintedCharArray(new int[lastDimSize], new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new MultiDTaintedDoubleArray(new int[lastDimSize], new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new MultiDTaintedFloatArray(new int[lastDimSize], new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new MultiDTaintedIntArray(new int[lastDimSize], new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new MultiDTaintedLongArray(new int[lastDimSize], new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new MultiDTaintedShortArray(new int[lastDimSize], new short[lastDimSize]);
					break;
				default:
					throw new IllegalArgumentException();
				}
			} else {
				initLastDim((Object[]) ar[i], lastDimSize, componentType);
			}
		}
	}

	public static Type getPrimitiveTypeForWrapper(String internalName) {
		try {
			return Type.getType(getPrimitiveTypeForWrapper(Class.forName(internalName.replace("/", "."))));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
