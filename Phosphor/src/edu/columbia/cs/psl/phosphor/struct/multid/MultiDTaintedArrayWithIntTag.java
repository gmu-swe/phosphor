package edu.columbia.cs.psl.phosphor.struct.multid;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;

public abstract class MultiDTaintedArrayWithIntTag {
	public static final long serialVersionUID = 40523489234L;

	public final TaintedIntWithIntTag hashCode$$PHOSPHORTAGGED(TaintedIntWithIntTag ret) {
		ret.taint = 0;
		ret.val = hashCode();
		return ret;
	}


	public static final Type getTypeForType(final Type originalElementType) {
		if (originalElementType.getSort() != Type.ARRAY)
			throw new IllegalArgumentException("passed " + originalElementType);
		String desc = "";
		for(int i = 0; i < originalElementType.getDimensions() - 1; i++)
			desc += "[";
		switch (originalElementType.getElementType().getSort()) {
		case Type.BOOLEAN:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyBooleanArrayIntTags;";
			break;
		case Type.BYTE:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyByteArrayIntTags;";
			break;
		case Type.CHAR:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyCharArrayIntTags;";
			break;
		case Type.DOUBLE:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyDoubleArrayIntTags;";
			break;
		case Type.FLOAT:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyFloatArrayIntTags;";
			break;
		case Type.INT:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyIntArrayIntTags;";
			break;
		case Type.LONG:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyLongArrayIntTags;";
			break;
		case Type.SHORT:
			desc += "Ledu/columbia/cs/psl/phosphor/struct/LazyShortArrayIntTags;";
			break;
		default:
			throw new IllegalArgumentException("invalid sort: " + originalElementType);

		}
		return Type.getType(desc);
	}

	public static final String isPrimitiveBoxClass(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == LazyDoubleArrayIntTags.class)
			return "D";
		if (c == LazyFloatArrayIntTags.class)
			return "F";
		if (c == LazyIntArrayIntTags.class)
			return "I";
		if (c == LazyLongArrayIntTags.class)
			return "J";
		if (c == LazyShortArrayIntTags.class)
			return "S";
		if (c == LazyBooleanArrayIntTags.class)
			return "Z";
		if (c == LazyByteArrayIntTags.class)
			return "B";
		if (c == LazyCharArrayIntTags.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == LazyDoubleArrayIntTags.class)
			return "D";
		if (c == LazyFloatArrayIntTags.class)
			return "F";
		if (c == LazyIntArrayIntTags.class)
			return "I";
		if (c == LazyLongArrayIntTags.class)
			return "J";
		if (c == LazyShortArrayIntTags.class)
			return "S";
		if (c == LazyBooleanArrayIntTags.class)
			return "Z";
		if (c == LazyByteArrayIntTags.class)
			return "B";
		if (c == LazyCharArrayIntTags.class)
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
				return LazyDoubleArrayIntTags.class;
			if (c == Float.TYPE)
				return LazyFloatArrayIntTags.class;
			if (c == Integer.TYPE)
				return LazyIntArrayIntTags.class;
			if (c == Long.TYPE)
				return LazyLongArrayIntTags.class;
			if (c == Short.TYPE)
				return LazyShortArrayIntTags.class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayIntTags.class;
			if (c == Byte.TYPE)
				return LazyByteArrayIntTags.class;
			if (c == Character.TYPE)
				return LazyCharArrayIntTags.class;
		}
		else 
			if(dims == 2)
		{
			if (c == Double.TYPE)
				return LazyDoubleArrayIntTags[].class;
			if (c == Float.TYPE)
				return LazyFloatArrayIntTags[].class;
			if (c == Integer.TYPE)
				return LazyIntArrayIntTags[].class;
			if (c == Long.TYPE)
				return LazyLongArrayIntTags[].class;
			if (c == Short.TYPE)
				return LazyShortArrayIntTags[].class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayIntTags[].class;
			if (c == Byte.TYPE)
				return LazyByteArrayIntTags[].class;
			if (c == Character.TYPE)
				return LazyCharArrayIntTags[].class;
		}
		else if(dims == 3)
		{
			if (c == Double.TYPE)
				return LazyDoubleArrayIntTags[][].class;
			if (c == Float.TYPE)
				return LazyFloatArrayIntTags[][].class;
			if (c == Integer.TYPE)
				return LazyIntArrayIntTags[][].class;
			if (c == Long.TYPE)
				return LazyLongArrayIntTags[][].class;
			if (c == Short.TYPE)
				return LazyShortArrayIntTags[][].class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayIntTags[][].class;
			if (c == Byte.TYPE)
				return LazyByteArrayIntTags[][].class;
			if (c == Character.TYPE)
				return LazyCharArrayIntTags[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: "+dims);
	}
	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return LazyBooleanArrayIntTags.class;
		case Type.BYTE:
			return LazyByteArrayIntTags.class;
		case Type.CHAR:
			return LazyCharArrayIntTags.class;
		case Type.DOUBLE:
			return LazyDoubleArrayIntTags.class;
		case Type.FLOAT:
			return LazyFloatArrayIntTags.class;
		case Type.INT:
			return LazyIntArrayIntTags.class;
		case Type.LONG:
			return LazyLongArrayIntTags.class;
		case Type.SHORT:
			return LazyShortArrayIntTags.class;
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
				return ((LazyArrayIntTags) _in).getVal();
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
						retz[i] = ((LazyBooleanArrayIntTags) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((LazyByteArrayIntTags) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((LazyCharArrayIntTags) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((LazyDoubleArrayIntTags) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((LazyFloatArrayIntTags) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((LazyIntArrayIntTags) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((LazyLongArrayIntTags) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((LazyShortArrayIntTags) in[i]).val;
				return rets;
			}
		} else if (dims == 2) {
			final Object[][] ina = (Object[][]) _in;
			final Object[] in = (Object[]) _in;
			switch (componentType) {
			case Type.BOOLEAN:
				boolean[][][] retz = new boolean[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retz[i] = new boolean[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retz[i][j] = ((LazyBooleanArrayIntTags) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((LazyByteArrayIntTags) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((LazyCharArrayIntTags) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((LazyDoubleArrayIntTags) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((LazyFloatArrayIntTags) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((LazyIntArrayIntTags) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((LazyLongArrayIntTags) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					if(ina[i] == null)
						continue;
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((LazyShortArrayIntTags) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}
	public static int getSortForBoxClass(Class c)
	{
		if(c == LazyIntArrayIntTags.class)
			return Type.INT;
		if(c == LazyBooleanArrayIntTags.class)
			return Type.BOOLEAN;
		if(c == LazyByteArrayIntTags.class)
			return Type.BYTE;
		if(c == LazyFloatArrayIntTags.class)
			return Type.FLOAT;
		if(c == LazyCharArrayIntTags.class)
			return Type.CHAR;
		if(c == LazyDoubleArrayIntTags.class)
			return Type.DOUBLE;
		if(c == LazyLongArrayIntTags.class)
			return Type.LONG;
		if(c == LazyShortArrayIntTags.class)
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
			Class tmp = in.getClass();
			int dims = 0;
			while(tmp.isArray())
			{
				tmp = tmp.getComponentType();
				dims++;
			}
			if (tmp.isPrimitive()) {
				//Is prim arraytype
				if (dims > 1) { //this should never be possible.
					Type t = Type.getType(in.getClass());
					return initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
				} else {
					if(tmp == Boolean.TYPE)
						return new LazyBooleanArrayIntTags((boolean[]) in);
					if(tmp == Byte.TYPE)
						return new LazyByteArrayIntTags(((byte[]) in));
					if(tmp == Character.TYPE)
						return new LazyCharArrayIntTags(((char[]) in));
					if(tmp == Double.TYPE)
						return new LazyDoubleArrayIntTags(((double[]) in));
					if(tmp == Float.TYPE)
						return new LazyFloatArrayIntTags(((float[]) in));
					if(tmp == Integer.TYPE)
						return new LazyIntArrayIntTags(((int[]) in));
					if(tmp == Long.TYPE)
						return new LazyLongArrayIntTags(((long[]) in));
					if(tmp == Short.TYPE)
						return new LazyShortArrayIntTags(((short[]) in));
						throw new IllegalArgumentException();
				}
			}
			else if(in.getClass().getComponentType().getName().equals("java.lang.Object"))
			{
				Object[] _in = (Object[]) in;
				for(int i = 0; i < _in.length;i++)
				{
					_in[i] = boxIfNecessary(_in[i]);
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
				ret = new LazyBooleanArrayIntTags[ar.length];
				break;
			case Type.BYTE:
				ret = new LazyByteArrayIntTags[ar.length];
				break;
			case Type.CHAR:
				ret = new LazyCharArrayIntTags[ar.length];
				break;
			case Type.DOUBLE:
				ret = new LazyDoubleArrayIntTags[ar.length];
				break;
			case Type.FLOAT:
				ret = new LazyFloatArrayIntTags[ar.length];
				break;
			case Type.INT:
				ret = new LazyIntArrayIntTags[ar.length];
				break;
			case Type.LONG:
				ret = new LazyLongArrayIntTags[ar.length];
				break;
			case Type.SHORT:
				ret = new LazyShortArrayIntTags[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new LazyBooleanArrayIntTags(((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new LazyByteArrayIntTags(((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new LazyCharArrayIntTags(((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new LazyDoubleArrayIntTags(((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new LazyFloatArrayIntTags(((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new LazyIntArrayIntTags(((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new LazyLongArrayIntTags(((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new LazyShortArrayIntTags(((short[]) entry));
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
				ret = new LazyBooleanArrayIntTags[ar.length][];
				break;
			case Type.BYTE:
				ret = new LazyByteArrayIntTags[ar.length][];
				break;
			case Type.CHAR:
				ret = new LazyCharArrayIntTags[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new LazyDoubleArrayIntTags[ar.length][];
				break;
			case Type.FLOAT:
				ret = new LazyFloatArrayIntTags[ar.length][];
				break;
			case Type.INT:
				ret = new LazyIntArrayIntTags[ar.length][];
				break;
			case Type.LONG:
				ret = new LazyLongArrayIntTags[ar.length][];
				break;
			case Type.SHORT:
				ret = new LazyShortArrayIntTags[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new LazyBooleanArrayIntTags[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new LazyByteArrayIntTags[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new LazyCharArrayIntTags[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new LazyDoubleArrayIntTags[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new LazyFloatArrayIntTags[entry1.length];
						break;
					case Type.INT:
						ret[i] = new LazyIntArrayIntTags[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new LazyLongArrayIntTags[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new LazyShortArrayIntTags[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new LazyBooleanArrayIntTags(((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new LazyByteArrayIntTags(((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new LazyCharArrayIntTags(((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new LazyDoubleArrayIntTags(((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new LazyFloatArrayIntTags(((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new LazyIntArrayIntTags(((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new LazyLongArrayIntTags(((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new LazyShortArrayIntTags((short[]) entry);
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
					ar[i] = new LazyBooleanArrayIntTags(new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new LazyByteArrayIntTags(new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new LazyCharArrayIntTags(new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new LazyDoubleArrayIntTags(new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new LazyFloatArrayIntTags(new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new LazyIntArrayIntTags(new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new LazyLongArrayIntTags(new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new LazyShortArrayIntTags(new short[lastDimSize]);
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
	public abstract int getLength();
}
