package edu.columbia.cs.psl.phosphor.struct.multid;

import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;

public abstract class MultiDTaintedArrayWithIntTag {
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
	public MultiDTaintedArrayWithIntTag() {

	}

	public abstract Object getVal();

	public final int getSort() {
		return sort;
	}

	protected MultiDTaintedArrayWithIntTag(int[] taint, int sort) {
		this.taint = taint;
		this.sort = sort;
		this.length = taint.length;
	}

	public int[] taint;

	public abstract Object clone();

	public final int hashCode() {
		return getVal().hashCode();
	}

	public final TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(TaintedIntWithIntTag ret) {
		ret.taint = 0;
		ret.val = hashCode();
		return ret;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof MultiDTaintedArrayWithIntTag)
			return getVal().equals(((MultiDTaintedArrayWithIntTag) obj).getVal());
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
		if (c == MultiDTaintedDoubleArrayWithIntTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithIntTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithIntTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithIntTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithIntTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithIntTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithIntTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithIntTag.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == MultiDTaintedDoubleArrayWithIntTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithIntTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithIntTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithIntTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithIntTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithIntTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithIntTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithIntTag.class)
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
				return MultiDTaintedDoubleArrayWithIntTag.class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithIntTag.class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithIntTag.class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithIntTag.class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithIntTag.class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithIntTag.class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithIntTag.class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithIntTag.class;
		}
		else 
			if(dims == 2)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithIntTag[].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithIntTag[].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithIntTag[].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithIntTag[].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithIntTag[].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithIntTag[].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithIntTag[].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithIntTag[].class;
		}
		else if(dims == 3)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithIntTag[][].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithIntTag[][].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithIntTag[][].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithIntTag[][].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithIntTag[][].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithIntTag[][].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithIntTag[][].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithIntTag[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: "+dims);
	}
	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return MultiDTaintedBooleanArrayWithIntTag.class;
		case Type.BYTE:
			return MultiDTaintedByteArrayWithIntTag.class;
		case Type.CHAR:
			return MultiDTaintedCharArrayWithIntTag.class;
		case Type.DOUBLE:
			return MultiDTaintedDoubleArrayWithIntTag.class;
		case Type.FLOAT:
			return MultiDTaintedFloatArrayWithIntTag.class;
		case Type.INT:
			return MultiDTaintedIntArrayWithIntTag.class;
		case Type.LONG:
			return MultiDTaintedLongArrayWithIntTag.class;
		case Type.SHORT:
			return MultiDTaintedShortArrayWithIntTag.class;
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
				return ((MultiDTaintedArrayWithIntTag) _in).getVal();
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
						retz[i] = ((MultiDTaintedBooleanArrayWithIntTag) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((MultiDTaintedByteArrayWithIntTag) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((MultiDTaintedCharArrayWithIntTag) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((MultiDTaintedDoubleArrayWithIntTag) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((MultiDTaintedFloatArrayWithIntTag) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((MultiDTaintedIntArrayWithIntTag) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((MultiDTaintedLongArrayWithIntTag) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((MultiDTaintedShortArrayWithIntTag) in[i]).val;
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
						retz[i][j] = ((MultiDTaintedBooleanArrayWithIntTag) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((MultiDTaintedByteArrayWithIntTag) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((MultiDTaintedCharArrayWithIntTag) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((MultiDTaintedDoubleArrayWithIntTag) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((MultiDTaintedFloatArrayWithIntTag) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((MultiDTaintedIntArrayWithIntTag) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((MultiDTaintedLongArrayWithIntTag) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((MultiDTaintedShortArrayWithIntTag) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}
	public static int getSortForBoxClass(Class c)
	{
		if(c == MultiDTaintedIntArrayWithIntTag.class)
			return Type.INT;
		if(c == MultiDTaintedBooleanArrayWithIntTag.class)
			return Type.BOOLEAN;
		if(c == MultiDTaintedByteArrayWithIntTag.class)
			return Type.BYTE;
		if(c == MultiDTaintedFloatArrayWithIntTag.class)
			return Type.FLOAT;
		if(c == MultiDTaintedCharArrayWithIntTag.class)
			return Type.CHAR;
		if(c == MultiDTaintedDoubleArrayWithIntTag.class)
			return Type.DOUBLE;
		if(c == MultiDTaintedLongArrayWithIntTag.class)
			return Type.LONG;
		if(c == MultiDTaintedShortArrayWithIntTag.class)
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
						return new MultiDTaintedBooleanArrayWithIntTag(new int[(((boolean[]) in)).length], ((boolean[]) in));
					if(tmp == Byte.TYPE)
						return new MultiDTaintedByteArrayWithIntTag(new int[(((byte[]) in)).length], ((byte[]) in));
					if(tmp == Character.TYPE)
						return new MultiDTaintedCharArrayWithIntTag(new int[(((char[]) in)).length], ((char[]) in));
					if(tmp == Double.TYPE)
						return new MultiDTaintedDoubleArrayWithIntTag(new int[(((double[]) in)).length], ((double[]) in));
					if(tmp == Float.TYPE)
						return new MultiDTaintedFloatArrayWithIntTag(new int[(((float[]) in)).length], ((float[]) in));
					if(tmp == Integer.TYPE)
						return new MultiDTaintedIntArrayWithIntTag(new int[(((int[]) in)).length], ((int[]) in));
					if(tmp == Long.TYPE)
						return new MultiDTaintedLongArrayWithIntTag(new int[(((long[]) in)).length], ((long[]) in));
					if(tmp == Short.TYPE)
						return new MultiDTaintedShortArrayWithIntTag(new int[(((short[]) in)).length], ((short[]) in));
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
				ret = new MultiDTaintedBooleanArrayWithIntTag[ar.length];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithIntTag[ar.length];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithIntTag[ar.length];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithIntTag[ar.length];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithIntTag[ar.length];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithIntTag[ar.length];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithIntTag[ar.length];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithIntTag[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithIntTag(new int[(((boolean[]) entry)).length], ((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithIntTag(new int[(((byte[]) entry)).length], ((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithIntTag(new int[(((char[]) entry)).length], ((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithIntTag(new int[(((double[]) entry)).length], ((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithIntTag(new int[(((float[]) entry)).length], ((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithIntTag(new int[(((int[]) entry)).length], ((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithIntTag(new int[(((long[]) entry)).length], ((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithIntTag(new int[(((short[]) entry)).length], ((short[]) entry));
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
				ret = new MultiDTaintedBooleanArrayWithIntTag[ar.length][];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithIntTag[ar.length][];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithIntTag[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithIntTag[ar.length][];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithIntTag[ar.length][];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithIntTag[ar.length][];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithIntTag[ar.length][];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithIntTag[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithIntTag[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithIntTag[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithIntTag[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithIntTag[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithIntTag[entry1.length];
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithIntTag[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithIntTag[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithIntTag[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new MultiDTaintedBooleanArrayWithIntTag(new int[(((boolean[]) entry)).length], ((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new MultiDTaintedByteArrayWithIntTag(new int[(((byte[]) entry)).length], ((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new MultiDTaintedCharArrayWithIntTag(new int[(((char[]) entry)).length], ((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new MultiDTaintedDoubleArrayWithIntTag(new int[(((double[]) entry)).length], ((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new MultiDTaintedFloatArrayWithIntTag(new int[(((float[]) entry)).length], ((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new MultiDTaintedIntArrayWithIntTag(new int[(((int[]) entry)).length], ((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new MultiDTaintedLongArrayWithIntTag(new int[(((long[]) entry)).length], ((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new MultiDTaintedShortArrayWithIntTag(new int[(((short[]) entry)).length], ((short[]) entry));
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
					ar[i] = new MultiDTaintedBooleanArrayWithIntTag(new int[lastDimSize], new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new MultiDTaintedByteArrayWithIntTag(new int[lastDimSize], new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new MultiDTaintedCharArrayWithIntTag(new int[lastDimSize], new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new MultiDTaintedDoubleArrayWithIntTag(new int[lastDimSize], new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new MultiDTaintedFloatArrayWithIntTag(new int[lastDimSize], new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new MultiDTaintedIntArrayWithIntTag(new int[lastDimSize], new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new MultiDTaintedLongArrayWithIntTag(new int[lastDimSize], new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new MultiDTaintedShortArrayWithIntTag(new int[lastDimSize], new short[lastDimSize]);
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
