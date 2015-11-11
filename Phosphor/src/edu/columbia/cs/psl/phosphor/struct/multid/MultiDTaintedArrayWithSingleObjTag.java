package edu.columbia.cs.psl.phosphor.struct.multid;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;

public abstract class MultiDTaintedArrayWithSingleObjTag {
	public static final long serialVersionUID = 40523489234L;
	public int sort;

	public boolean hasTaints() {
		if (taint != null)
			return true;
		return false;
	}

	public void setTaints(Object t) {
		taint = t;
	}

	public MultiDTaintedArrayWithSingleObjTag() {

	}

	public abstract Object getVal();

	public final int getSort() {
		return sort;
	}

	protected MultiDTaintedArrayWithSingleObjTag(Object taint, int sort) {
		this.taint = taint;
		this.sort = sort;
	}

	public Object taint;

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
		if (obj instanceof MultiDTaintedArrayWithSingleObjTag)
			return getVal().equals(((MultiDTaintedArrayWithSingleObjTag) obj).getVal());
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
		if (c == MultiDTaintedDoubleArrayWithSingleObjTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithSingleObjTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithSingleObjTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithSingleObjTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithSingleObjTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithSingleObjTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithSingleObjTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithSingleObjTag.class)
			return "C";
		return null;
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		while (c.isArray())
			c = c.getComponentType();
		if (c == MultiDTaintedDoubleArrayWithSingleObjTag.class)
			return "D";
		if (c == MultiDTaintedFloatArrayWithSingleObjTag.class)
			return "F";
		if (c == MultiDTaintedIntArrayWithSingleObjTag.class)
			return "I";
		if (c == MultiDTaintedLongArrayWithSingleObjTag.class)
			return "J";
		if (c == MultiDTaintedShortArrayWithSingleObjTag.class)
			return "S";
		if (c == MultiDTaintedBooleanArrayWithSingleObjTag.class)
			return "Z";
		if (c == MultiDTaintedByteArrayWithSingleObjTag.class)
			return "B";
		if (c == MultiDTaintedCharArrayWithSingleObjTag.class)
			return "C";
		throw new IllegalStateException("Got passed class: " + c);

	}

	public static final Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
		int dims = 0;
		if (c.isArray()) {
			while (c.isArray()) {
				c = c.getComponentType();
				dims++;
			}
		}

		if (dims == 1) {
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithSingleObjTag.class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithSingleObjTag.class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithSingleObjTag.class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithSingleObjTag.class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithSingleObjTag.class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithSingleObjTag.class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithSingleObjTag.class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithSingleObjTag.class;
		} else if (dims == 2) {
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithSingleObjTag[].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithSingleObjTag[].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithSingleObjTag[].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithSingleObjTag[].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithSingleObjTag[].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithSingleObjTag[].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithSingleObjTag[].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithSingleObjTag[].class;
		} else if (dims == 3) {
			if (c == Double.TYPE)
				return MultiDTaintedDoubleArrayWithSingleObjTag[][].class;
			if (c == Float.TYPE)
				return MultiDTaintedFloatArrayWithSingleObjTag[][].class;
			if (c == Integer.TYPE)
				return MultiDTaintedIntArrayWithSingleObjTag[][].class;
			if (c == Long.TYPE)
				return MultiDTaintedLongArrayWithSingleObjTag[][].class;
			if (c == Short.TYPE)
				return MultiDTaintedShortArrayWithSingleObjTag[][].class;
			if (c == Boolean.TYPE)
				return MultiDTaintedBooleanArrayWithSingleObjTag[][].class;
			if (c == Byte.TYPE)
				return MultiDTaintedByteArrayWithSingleObjTag[][].class;
			if (c == Character.TYPE)
				return MultiDTaintedCharArrayWithSingleObjTag[][].class;
		}
		throw new IllegalArgumentException("Can't handle that many dims yet: " + dims);
	}

	public static final Class getClassForComponentType(final int componentSort) {
		switch (componentSort) {
		case Type.BOOLEAN:
			return MultiDTaintedBooleanArrayWithSingleObjTag.class;
		case Type.BYTE:
			return MultiDTaintedByteArrayWithSingleObjTag.class;
		case Type.CHAR:
			return MultiDTaintedCharArrayWithSingleObjTag.class;
		case Type.DOUBLE:
			return MultiDTaintedDoubleArrayWithSingleObjTag.class;
		case Type.FLOAT:
			return MultiDTaintedFloatArrayWithSingleObjTag.class;
		case Type.INT:
			return MultiDTaintedIntArrayWithSingleObjTag.class;
		case Type.LONG:
			return MultiDTaintedLongArrayWithSingleObjTag.class;
		case Type.SHORT:
			return MultiDTaintedShortArrayWithSingleObjTag.class;
		default:
			throw new IllegalArgumentException("invalid sort: " + componentSort);
		}
	}

	public static final Object unboxRaw(final Object in) {
		if (in == null)
			return null;
		if (!in.getClass().isArray()) {
			return unboxVal(in, getSortForBoxClass(in.getClass()), 0);
		}
		Class tmp = in.getClass();
		int dims = 0;
		while (tmp.isArray()) {
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
				return ((MultiDTaintedArrayWithSingleObjTag) _in).getVal();
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
						retz[i] = ((MultiDTaintedBooleanArrayWithSingleObjTag) in[i]).val;
				return retz;
			case Type.BYTE:
				byte[][] retb = new byte[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retb[i] = ((MultiDTaintedByteArrayWithSingleObjTag) in[i]).val;
				return retb;
			case Type.CHAR:
				char[][] retc = new char[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retc[i] = ((MultiDTaintedCharArrayWithSingleObjTag) in[i]).val;
				return retc;
			case Type.DOUBLE:
				double[][] retd = new double[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retd[i] = ((MultiDTaintedDoubleArrayWithSingleObjTag) in[i]).val;
				return retd;
			case Type.FLOAT:
				float[][] retf = new float[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retf[i] = ((MultiDTaintedFloatArrayWithSingleObjTag) in[i]).val;
				return retf;
			case Type.INT:
				int[][] reti = new int[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						reti[i] = ((MultiDTaintedIntArrayWithSingleObjTag) in[i]).val;
				return reti;
			case Type.LONG:
				long[][] retl = new long[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						retl[i] = ((MultiDTaintedLongArrayWithSingleObjTag) in[i]).val;
				return retl;
			case Type.SHORT:
				short[][] rets = new short[in.length][];
				for (int i = 0; i < in.length; i++)
					if (in[i] != null)
						rets[i] = ((MultiDTaintedShortArrayWithSingleObjTag) in[i]).val;
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
						retz[i][j] = ((MultiDTaintedBooleanArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retz;
			case Type.BYTE:
				byte[][][] retb = new byte[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retb[i] = new byte[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retb[i][j] = ((MultiDTaintedByteArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retb;
			case Type.CHAR:
				char[][][] retc = new char[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retc[i] = new char[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retc[i][j] = ((MultiDTaintedCharArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retc;
			case Type.DOUBLE:
				double[][][] retd = new double[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retd[i] = new double[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retd[i][j] = ((MultiDTaintedDoubleArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retd;
			case Type.FLOAT:
				float[][][] retf = new float[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retf[i] = new float[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retf[i][j] = ((MultiDTaintedFloatArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retf;
			case Type.INT:
				int[][][] reti = new int[in.length][][];
				for (int i = 0; i < in.length; i++) {
					reti[i] = new int[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						reti[i][j] = ((MultiDTaintedIntArrayWithSingleObjTag) ina[i][j]).val;
				}
				return reti;
			case Type.LONG:
				long[][][] retl = new long[in.length][][];
				for (int i = 0; i < in.length; i++) {
					retl[i] = new long[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						retl[i][j] = ((MultiDTaintedLongArrayWithSingleObjTag) ina[i][j]).val;
				}
				return retl;
			case Type.SHORT:
				short[][][] rets = new short[in.length][][];
				for (int i = 0; i < in.length; i++) {
					rets[i] = new short[ina[i].length][];
					for (int j = 0; j < ina[i].length; j++)
						rets[i][j] = ((MultiDTaintedShortArrayWithSingleObjTag) ina[i][j]).val;
				}
				return rets;
			}
		}
		throw new IllegalArgumentException();
	}

	public static int getSortForBoxClass(Class c) {
		if (c == MultiDTaintedIntArrayWithSingleObjTag.class)
			return Type.INT;
		if (c == MultiDTaintedBooleanArrayWithSingleObjTag.class)
			return Type.BOOLEAN;
		if (c == MultiDTaintedByteArrayWithSingleObjTag.class)
			return Type.BYTE;
		if (c == MultiDTaintedFloatArrayWithSingleObjTag.class)
			return Type.FLOAT;
		if (c == MultiDTaintedCharArrayWithSingleObjTag.class)
			return Type.CHAR;
		if (c == MultiDTaintedDoubleArrayWithSingleObjTag.class)
			return Type.DOUBLE;
		if (c == MultiDTaintedLongArrayWithSingleObjTag.class)
			return Type.LONG;
		if (c == MultiDTaintedShortArrayWithSingleObjTag.class)
			return Type.SHORT;
		throw new IllegalArgumentException();
	}

	public static int getSort(Class c) {
		if (c == Integer.TYPE)
			return Type.INT;
		if (c == Boolean.TYPE)
			return Type.BOOLEAN;
		if (c == Byte.TYPE)
			return Type.BYTE;
		if (c == Float.TYPE)
			return Type.FLOAT;
		if (c == Character.TYPE)
			return Type.CHAR;
		if (c == Double.TYPE)
			return Type.DOUBLE;
		if (c == Long.TYPE)
			return Type.LONG;
		if (c == Short.TYPE)
			return Type.SHORT;
		throw new IllegalArgumentException();
	}

	public static final Object boxIfNecessary(final Object in) {
		if (in != null && in.getClass().isArray()) {
			if (in.getClass().getComponentType().isPrimitive()) {
				//Is prim arraytype
				Class tmp = in.getClass();
				int dims = 0;
				while (tmp.isArray()) {
					tmp = tmp.getComponentType();
					dims++;
				}
				if (dims > 1) { //this should never be possible.
					Type t = Type.getType(in.getClass());
					initWithEmptyTaints((Object[]) in, t.getElementType().getSort(), t.getDimensions());
				} else {

					if (tmp == Boolean.TYPE)
						return new MultiDTaintedBooleanArrayWithSingleObjTag(null, ((boolean[]) in));
					if (tmp == Byte.TYPE)
						return new MultiDTaintedByteArrayWithSingleObjTag(null, ((byte[]) in));
					if (tmp == Character.TYPE)
						return new MultiDTaintedCharArrayWithSingleObjTag(null, ((char[]) in));
					if (tmp == Double.TYPE)
						return new MultiDTaintedDoubleArrayWithSingleObjTag(null, ((double[]) in));
					if (tmp == Float.TYPE)
						return new MultiDTaintedFloatArrayWithSingleObjTag(null, ((float[]) in));
					if (tmp == Integer.TYPE)
						return new MultiDTaintedIntArrayWithSingleObjTag(null, ((int[]) in));
					if (tmp == Long.TYPE)
						return new MultiDTaintedLongArrayWithSingleObjTag(null, ((long[]) in));
					if (tmp == Short.TYPE)
						return new MultiDTaintedShortArrayWithSingleObjTag(null, ((short[]) in));
					throw new IllegalArgumentException();
				}
			} else if (in.getClass().getComponentType().getName().equals("java.lang.Object")) {
				Object[] _in = (Object[]) in;
				for (int i = 0; i < _in.length; i++) {
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
				ret = new MultiDTaintedBooleanArrayWithSingleObjTag[ar.length];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithSingleObjTag[ar.length];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithSingleObjTag[ar.length];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithSingleObjTag[ar.length];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithSingleObjTag[ar.length];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithSingleObjTag[ar.length];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithSingleObjTag[ar.length];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithSingleObjTag[ar.length];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object entry = (Object) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithSingleObjTag(null, ((boolean[]) entry));
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithSingleObjTag(null, ((byte[]) entry));
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithSingleObjTag(null, ((char[]) entry));
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithSingleObjTag(null, ((double[]) entry));
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithSingleObjTag(null, ((float[]) entry));
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithSingleObjTag(null, ((int[]) entry));
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithSingleObjTag(null, ((long[]) entry));
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithSingleObjTag(null, ((short[]) entry));
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
				ret = new MultiDTaintedBooleanArrayWithSingleObjTag[ar.length][];
				break;
			case Type.BYTE:
				ret = new MultiDTaintedByteArrayWithSingleObjTag[ar.length][];
				break;
			case Type.CHAR:
				ret = new MultiDTaintedCharArrayWithSingleObjTag[ar.length][];
				break;
			case Type.DOUBLE:
				ret = new MultiDTaintedDoubleArrayWithSingleObjTag[ar.length][];
				break;
			case Type.FLOAT:
				ret = new MultiDTaintedFloatArrayWithSingleObjTag[ar.length][];
				break;
			case Type.INT:
				ret = new MultiDTaintedIntArrayWithSingleObjTag[ar.length][];
				break;
			case Type.LONG:
				ret = new MultiDTaintedLongArrayWithSingleObjTag[ar.length][];
				break;
			case Type.SHORT:
				ret = new MultiDTaintedShortArrayWithSingleObjTag[ar.length][];
				break;
			default:
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					Object[] entry1 = (Object[]) ar[i];
					switch (componentType) {
					case Type.BOOLEAN:
						ret[i] = new MultiDTaintedBooleanArrayWithSingleObjTag[entry1.length];
						break;
					case Type.BYTE:
						ret[i] = new MultiDTaintedByteArrayWithSingleObjTag[entry1.length];
						break;
					case Type.CHAR:
						ret[i] = new MultiDTaintedCharArrayWithSingleObjTag[entry1.length];
						break;
					case Type.DOUBLE:
						ret[i] = new MultiDTaintedDoubleArrayWithSingleObjTag[entry1.length];
						break;
					case Type.FLOAT:
						ret[i] = new MultiDTaintedFloatArrayWithSingleObjTag[entry1.length];
						break;
					case Type.INT:
						ret[i] = new MultiDTaintedIntArrayWithSingleObjTag[entry1.length];
						break;
					case Type.LONG:
						ret[i] = new MultiDTaintedLongArrayWithSingleObjTag[entry1.length];
						break;
					case Type.SHORT:
						ret[i] = new MultiDTaintedShortArrayWithSingleObjTag[entry1.length];
						break;
					default:
						throw new IllegalArgumentException();
					}
					for (int j = 0; j < entry1.length; j++) {
						Object entry = (Object) entry1[j];
						switch (componentType) {
						case Type.BOOLEAN:
							ret[i][j] = new MultiDTaintedBooleanArrayWithSingleObjTag(null, ((boolean[]) entry));
							break;
						case Type.BYTE:
							ret[i][j] = new MultiDTaintedByteArrayWithSingleObjTag(null, ((byte[]) entry));
							break;
						case Type.CHAR:
							ret[i][j] = new MultiDTaintedCharArrayWithSingleObjTag(null, ((char[]) entry));
							break;
						case Type.DOUBLE:
							ret[i][j] = new MultiDTaintedDoubleArrayWithSingleObjTag(null, ((double[]) entry));
							break;
						case Type.FLOAT:
							ret[i][j] = new MultiDTaintedFloatArrayWithSingleObjTag(null, ((float[]) entry));
							break;
						case Type.INT:
							ret[i][j] = new MultiDTaintedIntArrayWithSingleObjTag(null, ((int[]) entry));
							break;
						case Type.LONG:
							ret[i][j] = new MultiDTaintedLongArrayWithSingleObjTag(null, ((long[]) entry));
							break;
						case Type.SHORT:
							ret[i][j] = new MultiDTaintedShortArrayWithSingleObjTag(null, ((short[]) entry));
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
					ar[i] = new MultiDTaintedBooleanArrayWithSingleObjTag(null, new boolean[lastDimSize]);
					break;
				case Type.BYTE:
					ar[i] = new MultiDTaintedByteArrayWithSingleObjTag(null, new byte[lastDimSize]);
					break;
				case Type.CHAR:
					ar[i] = new MultiDTaintedCharArrayWithSingleObjTag(null, new char[lastDimSize]);
					break;
				case Type.DOUBLE:
					ar[i] = new MultiDTaintedDoubleArrayWithSingleObjTag(null, new double[lastDimSize]);
					break;
				case Type.FLOAT:
					ar[i] = new MultiDTaintedFloatArrayWithSingleObjTag(null, new float[lastDimSize]);
					break;
				case Type.INT:
					ar[i] = new MultiDTaintedIntArrayWithSingleObjTag(null, new int[lastDimSize]);
					break;
				case Type.LONG:
					ar[i] = new MultiDTaintedLongArrayWithSingleObjTag(null, new long[lastDimSize]);
					break;
				case Type.SHORT:
					ar[i] = new MultiDTaintedShortArrayWithSingleObjTag(null, new short[lastDimSize]);
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
