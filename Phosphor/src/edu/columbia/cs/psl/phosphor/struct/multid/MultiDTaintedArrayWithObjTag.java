package edu.columbia.cs.psl.phosphor.struct.multid;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;

public abstract class MultiDTaintedArrayWithObjTag {
	public static final long serialVersionUID = 40523489234L;
	public int sort;

	
	public void setTaints(Object t)
	{
		throw new UnsupportedOperationException();
	}
	public MultiDTaintedArrayWithObjTag() {

	}

	public abstract Object getVal();

	public final int getSort() {
		return sort;
	}



	public abstract Object clone();

	public final int hashCode() {
		return getVal().hashCode();
	}

	public final TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret) {
		ret.taint = null;
		ret.val = hashCode();
		return ret;
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
		if (c == LazyDoubleArrayObjTags.class)
			return "D";
		if (c == LazyFloatArrayObjTags.class)
			return "F";
		if (c == LazyIntArrayObjTags.class)
			return "I";
		if (c == LazyLongArrayObjTags.class)
			return "J";
		if (c == LazyShortArrayObjTags.class)
			return "S";
		if (c == LazyBooleanArrayObjTags.class)
			return "Z";
		if (c == LazyByteArrayObjTags.class)
			return "B";
		if (c == LazyCharArrayObjTags.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == LazyDoubleArrayObjTags.class)
			return "D";
		if (c == LazyFloatArrayObjTags.class)
			return "F";
		if (c == LazyIntArrayObjTags.class)
			return "I";
		if (c == LazyLongArrayObjTags.class)
			return "J";
		if (c == LazyShortArrayObjTags.class)
			return "S";
		if (c == LazyBooleanArrayObjTags.class)
			return "Z";
		if (c == LazyByteArrayObjTags.class)
			return "B";
		if (c == LazyCharArrayObjTags.class)
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
				return LazyDoubleArrayObjTags.class;
			if (c == Float.TYPE)
				return LazyFloatArrayObjTags.class;
			if (c == Integer.TYPE)
				return LazyIntArrayObjTags.class;
			if (c == Long.TYPE)
				return LazyLongArrayObjTags.class;
			if (c == Short.TYPE)
				return LazyShortArrayObjTags.class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayObjTags.class;
			if (c == Byte.TYPE)
				return LazyByteArrayObjTags.class;
			if (c == Character.TYPE)
				return LazyCharArrayObjTags.class;
		}
		else 
			if(dims == 2)
		{
			if (c == Double.TYPE)
				return LazyDoubleArrayObjTags[].class;
			if (c == Float.TYPE)
				return LazyFloatArrayObjTags[].class;
			if (c == Integer.TYPE)
				return LazyIntArrayObjTags[].class;
			if (c == Long.TYPE)
				return LazyLongArrayObjTags[].class;
			if (c == Short.TYPE)
				return LazyShortArrayObjTags[].class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayObjTags[].class;
			if (c == Byte.TYPE)
				return LazyByteArrayObjTags[].class;
			if (c == Character.TYPE)
				return LazyCharArrayObjTags[].class;
		}
		else if(dims == 3)
		{
			if (c == Double.TYPE)
				return LazyDoubleArrayObjTags[][].class;
			if (c == Float.TYPE)
				return LazyFloatArrayObjTags[][].class;
			if (c == Integer.TYPE)
				return LazyIntArrayObjTags[][].class;
			if (c == Long.TYPE)
				return LazyLongArrayObjTags[][].class;
			if (c == Short.TYPE)
				return LazyShortArrayObjTags[][].class;
			if (c == Boolean.TYPE)
				return LazyBooleanArrayObjTags[][].class;
			if (c == Byte.TYPE)
				return LazyByteArrayObjTags[][].class;
			if (c == Character.TYPE)
				return LazyCharArrayObjTags[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: "+dims);
	}
	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return LazyBooleanArrayObjTags.class;
		case Type.BYTE:
			return LazyByteArrayObjTags.class;
		case Type.CHAR:
			return LazyCharArrayObjTags.class;
		case Type.DOUBLE:
			return LazyDoubleArrayObjTags.class;
		case Type.FLOAT:
			return LazyFloatArrayObjTags.class;
		case Type.INT:
			return LazyIntArrayObjTags.class;
		case Type.LONG:
			return LazyLongArrayObjTags.class;
		case Type.SHORT:
			return LazyShortArrayObjTags.class;
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
				return ((LazyArrayObjTags) _in).getVal();
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
						retz[i] = ((LazyBooleanArrayObjTags) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((LazyByteArrayObjTags) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((LazyCharArrayObjTags) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((LazyDoubleArrayObjTags) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((LazyFloatArrayObjTags) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((LazyIntArrayObjTags) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((LazyLongArrayObjTags) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((LazyShortArrayObjTags) in[i]).val;
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
						retz[i][j] = ((LazyBooleanArrayObjTags) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((LazyByteArrayObjTags) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((LazyCharArrayObjTags) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((LazyDoubleArrayObjTags) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((LazyFloatArrayObjTags) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((LazyIntArrayObjTags) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((LazyLongArrayObjTags) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((LazyShortArrayObjTags) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}
	public static int getSortForBoxClass(Class c)
	{
		if(c == LazyIntArrayObjTags.class)
			return Type.INT;
		if(c == LazyBooleanArrayObjTags.class)
			return Type.BOOLEAN;
		if(c == LazyByteArrayObjTags.class)
			return Type.BYTE;
		if(c == LazyFloatArrayObjTags.class)
			return Type.FLOAT;
		if(c == LazyCharArrayObjTags.class)
			return Type.CHAR;
		if(c == LazyDoubleArrayObjTags.class)
			return Type.DOUBLE;
		if(c == LazyLongArrayObjTags.class)
			return Type.LONG;
		if(c == LazyShortArrayObjTags.class)
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
						return new LazyBooleanArrayObjTags((boolean[]) in);
					if(tmp == Byte.TYPE)
						return new LazyByteArrayObjTags(((byte[]) in));
					if(tmp == Character.TYPE)
						return new LazyCharArrayObjTags(((char[]) in));
					if(tmp == Double.TYPE)
						return new LazyDoubleArrayObjTags(((double[]) in));
					if(tmp == Float.TYPE)
						return new LazyFloatArrayObjTags(((float[]) in));
					if(tmp == Integer.TYPE)
						return new LazyIntArrayObjTags(((int[]) in));
					if(tmp == Long.TYPE)
						return new LazyLongArrayObjTags(((long[]) in));
					if(tmp == Short.TYPE)
						return new LazyShortArrayObjTags(((short[]) in));
					throw new IllegalArgumentException();
				}
			}
			else if(in.getClass().getComponentType().isArray() && in.getClass().getComponentType().getComponentType().isPrimitive())
			{
				//THIS array is an prim[][] array
				Object[] _in = (Object[]) in;
				
				Class tmp = in.getClass();
				while(tmp.isArray())
				{
					tmp = tmp.getComponentType();
				}
				if(tmp == Boolean.TYPE)
				{
					LazyBooleanArrayObjTags[] ret = new LazyBooleanArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyBooleanArrayObjTags((boolean[]) _in[i]);
					return ret;
				}
				if(tmp == Byte.TYPE)
				{
					LazyByteArrayObjTags[] ret = new LazyByteArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyByteArrayObjTags((byte[]) _in[i]);
					return ret;
				}
				if(tmp == Character.TYPE)
				{
					LazyCharArrayObjTags[] ret = new LazyCharArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyCharArrayObjTags((char[]) _in[i]);
					return ret;
				}
				if(tmp == Double.TYPE)
				{
					LazyDoubleArrayObjTags[] ret = new LazyDoubleArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyDoubleArrayObjTags((double[]) _in[i]);
					return ret;
				}
				if(tmp == Float.TYPE)
				{
					LazyFloatArrayObjTags[] ret = new LazyFloatArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyFloatArrayObjTags((float[]) _in[i]);
					return ret;
				}
				if(tmp == Integer.TYPE)
				{
					LazyIntArrayObjTags[] ret = new LazyIntArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyIntArrayObjTags((int[]) _in[i]);
					return ret;
				}
				if(tmp == Short.TYPE)
				{
					LazyShortArrayObjTags[] ret = new LazyShortArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyShortArrayObjTags((short[]) _in[i]);
					return ret;
				}
				if(tmp == Long.TYPE)
				{
					LazyLongArrayObjTags[] ret = new LazyLongArrayObjTags[_in.length];
					for (int i = 0; i < _in.length; i++)
						ret[i] = new LazyLongArrayObjTags((long[]) _in[i]);
					return ret;
				}
				throw new UnsupportedOperationException();
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
				ret = new LazyBooleanArrayObjTags[ar.length];
				break;
			case Type.BYTE:
				ret = new LazyByteArrayObjTags[ar.length];
				break;
			case Type.CHAR:
				ret = new LazyCharArrayObjTags[ar.length];
				break;
			case Type.DOUBLE:
				ret = new LazyDoubleArrayObjTags[ar.length];
				break;
			case Type.FLOAT:
				ret = new LazyFloatArrayObjTags[ar.length];
				break;
			case Type.INT:
				ret = new LazyIntArrayObjTags[ar.length];
				break;
			case Type.LONG:
				ret = new LazyLongArrayObjTags[ar.length];
				break;
			case Type.SHORT:
				ret = new LazyShortArrayObjTags[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new LazyBooleanArrayObjTags(((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new LazyByteArrayObjTags(((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new LazyCharArrayObjTags(((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new LazyDoubleArrayObjTags(((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new LazyFloatArrayObjTags(((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new LazyIntArrayObjTags(((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new LazyLongArrayObjTags(((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new LazyShortArrayObjTags(((short[]) entry));
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
				ret = new LazyBooleanArrayObjTags[ar.length][];
				break;
			case Type.BYTE:
				ret = new LazyByteArrayObjTags[ar.length][];
				break;
			case Type.CHAR:
				ret = new LazyCharArrayObjTags[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new LazyDoubleArrayObjTags[ar.length][];
				break;
			case Type.FLOAT:
				ret = new LazyFloatArrayObjTags[ar.length][];
				break;
			case Type.INT:
				ret = new LazyIntArrayObjTags[ar.length][];
				break;
			case Type.LONG:
				ret = new LazyLongArrayObjTags[ar.length][];
				break;
			case Type.SHORT:
				ret = new LazyShortArrayObjTags[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new LazyBooleanArrayObjTags[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new LazyByteArrayObjTags[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new LazyCharArrayObjTags[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new LazyDoubleArrayObjTags[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new LazyFloatArrayObjTags[entry1.length];
						break;
					case Type.INT:
						ret[i] = new LazyIntArrayObjTags[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new LazyLongArrayObjTags[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new LazyShortArrayObjTags[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new LazyBooleanArrayObjTags(((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new LazyByteArrayObjTags(((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new LazyCharArrayObjTags(((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new LazyDoubleArrayObjTags(((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new LazyFloatArrayObjTags(((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new LazyIntArrayObjTags(((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new LazyLongArrayObjTags(((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new LazyShortArrayObjTags((short[]) entry);
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
					ar[i] = new LazyBooleanArrayObjTags(new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new LazyByteArrayObjTags(new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new LazyCharArrayObjTags(new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new LazyDoubleArrayObjTags(new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new LazyFloatArrayObjTags(new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new LazyIntArrayObjTags(new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new LazyLongArrayObjTags(new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new LazyShortArrayObjTags(new short[lastDimSize]);
					break;
				default:
					throw new IllegalArgumentException();
				}
			} else {
				initLastDim((Object[]) ar[i], lastDimSize, componentType);
			}
		}
	}

	public static final void initLastDim(final Object[] ar, final Taint<?> dimTaint, final int lastDimSize, final int componentType) {
		for (int i = 0; i < ar.length; i++) {
			if (ar[i] == null) {
				switch (componentType) {
				case Type.BOOLEAN:
					ar[i] = new LazyBooleanArrayObjTags(dimTaint, new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new LazyByteArrayObjTags(dimTaint, new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new LazyCharArrayObjTags(dimTaint, new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new LazyDoubleArrayObjTags(dimTaint, new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new LazyFloatArrayObjTags(dimTaint, new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new LazyIntArrayObjTags(dimTaint, new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new LazyLongArrayObjTags(dimTaint, new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new LazyShortArrayObjTags(dimTaint, new short[lastDimSize]);
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
