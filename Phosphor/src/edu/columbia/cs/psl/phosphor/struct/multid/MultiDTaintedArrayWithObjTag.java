package edu.columbia.cs.psl.phosphor.struct.multid;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;

public abstract class MultiDTaintedArrayWithObjTag {
	public static final long serialVersionUID = 40523489234L;
	public int sort;
	public int length;

	public boolean hasTaints()
	{
		for(Object o : taint)
			if(o != null)
				return true;
		return false;
	}
	public void setTaints(Object t)
	{
		for(int i = 0; i < taint.length; i++)
		{
			taint[i] = t;
		}
	}
	public MultiDTaintedArrayWithObjTag() {

	}

	public abstract Object getVal();

	public final int getSort() {
		return sort;
	}

	protected MultiDTaintedArrayWithObjTag(Object[] taint, int sort) {
		this.taint = taint;
		this.sort = sort;
		this.length = taint.length;
	}

	public Object[] taint;

	public abstract Object clone();

	public final int hashCode() {
		return getVal().hashCode();
	}

	public final TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(TaintedIntWithObjTag ret) {
		ret.taint = null;
		ret.val = hashCode();
		return ret;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof MultiDTaintedArrayWithObjTag)
			return getVal().equals(((MultiDTaintedArrayWithObjTag) obj).getVal());
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
		if (c == MultiDTaintedDoubleArrayWithObjTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithObjTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithObjTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithObjTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithObjTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithObjTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithObjTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithObjTag.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == MultiDTaintedDoubleArrayWithObjTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithObjTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithObjTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithObjTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithObjTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithObjTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithObjTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithObjTag.class)
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
				return MultiDTaintedDoubleArrayWithObjTag.class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithObjTag.class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithObjTag.class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithObjTag.class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithObjTag.class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithObjTag.class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithObjTag.class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithObjTag.class;
		}
		else 
			if(dims == 2)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithObjTag[].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithObjTag[].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithObjTag[].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithObjTag[].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithObjTag[].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithObjTag[].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithObjTag[].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithObjTag[].class;
		}
		else if(dims == 3)
		{
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithObjTag[][].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithObjTag[][].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithObjTag[][].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithObjTag[][].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithObjTag[][].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithObjTag[][].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithObjTag[][].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithObjTag[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: "+dims);
	}
	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return MultiDTaintedBooleanArrayWithObjTag.class;
		case Type.BYTE:
			return MultiDTaintedByteArrayWithObjTag.class;
		case Type.CHAR:
			return MultiDTaintedCharArrayWithObjTag.class;
		case Type.DOUBLE:
			return MultiDTaintedDoubleArrayWithObjTag.class;
		case Type.FLOAT:
			return MultiDTaintedFloatArrayWithObjTag.class;
		case Type.INT:
			return MultiDTaintedIntArrayWithObjTag.class;
		case Type.LONG:
			return MultiDTaintedLongArrayWithObjTag.class;
		case Type.SHORT:
			return MultiDTaintedShortArrayWithObjTag.class;
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
				return ((MultiDTaintedArrayWithObjTag) _in).getVal();
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
						retz[i] = ((MultiDTaintedBooleanArrayWithObjTag) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((MultiDTaintedByteArrayWithObjTag) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((MultiDTaintedCharArrayWithObjTag) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((MultiDTaintedDoubleArrayWithObjTag) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((MultiDTaintedFloatArrayWithObjTag) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((MultiDTaintedIntArrayWithObjTag) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((MultiDTaintedLongArrayWithObjTag) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((MultiDTaintedShortArrayWithObjTag) in[i]).val;
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
						retz[i][j] = ((MultiDTaintedBooleanArrayWithObjTag) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((MultiDTaintedByteArrayWithObjTag) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((MultiDTaintedCharArrayWithObjTag) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((MultiDTaintedDoubleArrayWithObjTag) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((MultiDTaintedFloatArrayWithObjTag) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((MultiDTaintedIntArrayWithObjTag) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((MultiDTaintedLongArrayWithObjTag) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((MultiDTaintedShortArrayWithObjTag) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}
	public static int getSortForBoxClass(Class c)
	{
		if(c == MultiDTaintedIntArrayWithObjTag.class)
			return Type.INT;
		if(c == MultiDTaintedBooleanArrayWithObjTag.class)
			return Type.BOOLEAN;
		if(c == MultiDTaintedByteArrayWithObjTag.class)
			return Type.BYTE;
		if(c == MultiDTaintedFloatArrayWithObjTag.class)
			return Type.FLOAT;
		if(c == MultiDTaintedCharArrayWithObjTag.class)
			return Type.CHAR;
		if(c == MultiDTaintedDoubleArrayWithObjTag.class)
			return Type.DOUBLE;
		if(c == MultiDTaintedLongArrayWithObjTag.class)
			return Type.LONG;
		if(c == MultiDTaintedShortArrayWithObjTag.class)
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
						return new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray(((boolean[]) in).length), ((boolean[]) in));
					if(tmp == Byte.TYPE)
						return new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray(((byte[]) in).length), ((byte[]) in));
					if(tmp == Character.TYPE)
						return new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray(((char[]) in).length), ((char[]) in));
					if(tmp == Double.TYPE)
						return new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray(((double[]) in).length), ((double[]) in));
					if(tmp == Float.TYPE)
						return new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray(((float[]) in).length), ((float[]) in));
					if(tmp == Integer.TYPE)
						return new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray(((int[]) in).length), ((int[]) in));
					if(tmp == Long.TYPE)
						return new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray(((long[]) in).length), ((long[]) in));
					if(tmp == Short.TYPE)
						return new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray(((short[]) in).length), ((short[]) in));
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
				ret = new MultiDTaintedBooleanArrayWithObjTag[ar.length];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithObjTag[ar.length];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithObjTag[ar.length];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithObjTag[ar.length];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithObjTag[ar.length];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithObjTag[ar.length];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithObjTag[ar.length];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithObjTag[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray((((boolean[]) entry)).length), ((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray((((byte[]) entry)).length), ((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray((((char[]) entry)).length), ((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray((((double[]) entry)).length), ((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray((((float[]) entry)).length), ((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray((((int[]) entry)).length), ((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray((((long[]) entry)).length), ((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray((((short[]) entry)).length), ((short[]) entry));
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
				ret = new MultiDTaintedBooleanArrayWithObjTag[ar.length][];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithObjTag[ar.length][];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithObjTag[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithObjTag[ar.length][];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithObjTag[ar.length][];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithObjTag[ar.length][];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithObjTag[ar.length][];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithObjTag[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithObjTag[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithObjTag[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithObjTag[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithObjTag[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithObjTag[entry1.length];
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithObjTag[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithObjTag[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithObjTag[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray((((boolean[]) entry)).length), ((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray((((byte[]) entry)).length), ((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray((((char[]) entry)).length), ((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray((((double[]) entry)).length), ((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray((((float[]) entry)).length), ((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray((((int[]) entry)).length), ((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray((((long[]) entry)).length), ((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray((((short[]) entry)).length), ((short[]) entry));
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
					ar[i] = new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new short[lastDimSize]);
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
