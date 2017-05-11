package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyBooleanArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyDoubleArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyFloatArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyLongArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyShortArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class ArrayReflectionMasker {
	public static TaintedIntWithIntTag getLength$$PHOSPHORTAGGED(Object obj, TaintedIntWithIntTag ret) {
		if (obj.getClass().isArray()) {
			ret.taint = 0;
			ret.val = Array.getLength(obj);
			return ret;
		} else if (obj instanceof LazyArrayIntTags) {
			ret.taint = 0;
			ret.val = ((LazyArrayIntTags) obj).getLength();
			return ret;
		}
		throw new ArrayStoreException("Uknown array type: " + obj.getClass());
	}
	public static int getLength(Object obj) {
		if (obj.getClass().isArray()) {
			return Array.getLength(obj);
		} else if (obj instanceof LazyArrayIntTags) {
			return ((LazyArrayIntTags) obj).getLength();
		}
		else if (obj instanceof LazyArrayObjTags) {
			return Array.getLength(((LazyArrayObjTags) obj).getVal());
		}
		throw new ArrayStoreException("Uknown array type: " + obj.getClass());
	}

	public static Object newInstance(Class clazz, int len) {
		Class tmp = clazz;
		int dims = 0;
		while (tmp.isArray()) {
			tmp = tmp.getComponentType();
			dims++;
		}
		if (tmp.isPrimitive()) {
			if (dims == 0) {
				if (tmp == Double.TYPE)
					return new LazyDoubleArrayIntTags(new double[len]);
				if (tmp == Float.TYPE)
					return new LazyFloatArrayIntTags(new float[len]);
				if (tmp == Integer.TYPE)
					return new LazyIntArrayIntTags(new int[len]);
				if (tmp == Long.TYPE)
					return new LazyLongArrayIntTags(new long[len]);
				if (tmp == Short.TYPE)
					return new LazyShortArrayIntTags(new short[len]);
				if (tmp == Boolean.TYPE)
					return new LazyBooleanArrayIntTags(new boolean[len]);
				if (tmp == Byte.TYPE)
					return new LazyByteArrayIntTags(new byte[len]);
				if (tmp == Character.TYPE)
					return new LazyCharArrayIntTags(new char[len]);
			} else
				clazz = MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static TaintedIntWithIntTag getLength$$PHOSPHORTAGGED(Object obj, ControlTaintTagStack ctlr, TaintedIntWithIntTag ret) {
		return getLength$$PHOSPHORTAGGED(obj, ret);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int lenTaint, int len, ControlTaintTagStack zz) {
		return newInstance$$PHOSPHORTAGGED(clazz, lenTaint, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int lenTaint, int len) {
		Class tmp = clazz;
		int dims = 0;
		while (tmp.isArray()) {
			tmp = tmp.getComponentType();
			dims++;
		}
		if (tmp.isPrimitive()) {
			//			if(dims == 0)
			//			{
			//				
			//			}
			//			else
			if (dims == 0) {
				if (tmp == Double.TYPE)
					return new LazyDoubleArrayIntTags(new double[len]);
				if (tmp == Float.TYPE)
					return new LazyFloatArrayIntTags(new float[len]);
				if (tmp == Integer.TYPE)
					return new LazyIntArrayIntTags(new int[len]);
				if (tmp == Long.TYPE)
					return new LazyLongArrayIntTags(new long[len]);
				if (tmp == Short.TYPE)
					return new LazyShortArrayIntTags(new short[len]);
				if (tmp == Boolean.TYPE)
					return new LazyBooleanArrayIntTags(new boolean[len]);
				if (tmp == Byte.TYPE)
					return new LazyByteArrayIntTags(new byte[len]);
				if (tmp == Character.TYPE)
					return new LazyCharArrayIntTags(new char[len]);
			} else
				clazz = MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, LazyIntArrayIntTags dimsTaint, int[] dims, ControlTaintTagStack ctrl) {
		return newInstance$$PHOSPHORTAGGED(clazz, dimsTaint, dims);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, LazyIntArrayIntTags dimsTaint, int[] dims) {
		//		System.out.println("22Creating array instance of type " + clazz);
		Type t = Type.getType(clazz);
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
			try {
				clazz = Class.forName(MultiDTaintedArrayWithIntTag.getTypeForType(t).getInternalName().replace("/", "."));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return Array.newInstance(clazz, dims);
		} else if (t.getSort() != Type.OBJECT) {
			clazz = MultiDTaintedArrayWithIntTag.getClassForComponentType(t.getSort());
			if (clazz.isArray()) {
				int lastDim = dims[dims.length - 1];
				int[] newDims = new int[dims.length - 1];
				System.arraycopy(dims, 0, newDims, 0, dims.length - 1);
				Object[] ret = (Object[]) Array.newInstance(clazz, newDims);
				MultiDTaintedArrayWithIntTag.initLastDim(ret, lastDim, t.getSort());
				return ret;

			} else {
				int lastDimSize = dims[dims.length - 1];
				switch (t.getSort()) {
				case Type.BOOLEAN:
					return new LazyBooleanArrayIntTags(new boolean[lastDimSize]);
				case Type.BYTE:
					return new LazyByteArrayIntTags(new byte[lastDimSize]);
				case Type.CHAR:
					return new LazyCharArrayIntTags(new char[lastDimSize]);
				case Type.DOUBLE:
					return new LazyDoubleArrayIntTags(new double[lastDimSize]);
				case Type.FLOAT:
					return new LazyFloatArrayIntTags(new float[lastDimSize]);
				case Type.INT:
					return new LazyIntArrayIntTags(new int[lastDimSize]);
				case Type.LONG:
					return new LazyLongArrayIntTags(new long[lastDimSize]);
				case Type.SHORT:
					return new LazyShortArrayIntTags(new short[lastDimSize]);
				default:
					throw new IllegalArgumentException();
				}
			}
		}
		return Array.newInstance(clazz, dims);
	}

	public static TaintedIntWithObjTag getLength$$PHOSPHORTAGGED(Object obj, TaintedIntWithObjTag ret) {
		if (obj.getClass().isArray()) {
			ret.taint = null;
			ret.val = Array.getLength(obj);
			return ret;
		} else if (obj instanceof LazyArrayObjTags) {
			ret.taint = null;
			ret.val = Array.getLength(((LazyArrayObjTags) obj).getVal());
			return ret;
		}
		throw new ArrayStoreException("Uknown array type: " + obj.getClass());
	}

	public static TaintedIntWithObjTag getLength$$PHOSPHORTAGGED(Object obj, ControlTaintTagStack ctlr, TaintedIntWithObjTag ret) {
		return getLength$$PHOSPHORTAGGED(obj, ret);
	}
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Taint lenTaint, int len, ControlTaintTagStack zz) {
		return newInstance$$PHOSPHORTAGGED(clazz, lenTaint, len);
	}
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Object lenTaint, int len, ControlTaintTagStack zz) {
		return newInstance$$PHOSPHORTAGGED(clazz, (Taint) lenTaint, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Taint lenTaint, int len) {
		Class tmp = clazz;
		int dims = 0;
		while (tmp.isArray()) {
			tmp = tmp.getComponentType();
			dims++;
		}
		if (tmp.isPrimitive()) {
			//			if(dims == 0)
			//			{
			//				
			//			}
			//			else
			if (dims == 0) {
				if (tmp == Double.TYPE)
					return new LazyDoubleArrayObjTags(new double[len]);
				if (tmp == Float.TYPE)
					return new LazyFloatArrayObjTags(new float[len]);
				if (tmp == Integer.TYPE)
					return new LazyIntArrayObjTags(new int[len]);
				if (tmp == Long.TYPE)
					return new LazyLongArrayObjTags(new long[len]);
				if (tmp == Short.TYPE)
					return new LazyShortArrayObjTags(new short[len]);
				if (tmp == Boolean.TYPE)
					return new LazyBooleanArrayObjTags(new boolean[len]);
				if (tmp == Byte.TYPE)
					return new LazyByteArrayObjTags(new byte[len]);
				if (tmp == Character.TYPE)
					return new LazyCharArrayObjTags(new char[len]);
			} else
				clazz = MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, LazyIntArrayObjTags dimsTaint, int[] dims, ControlTaintTagStack ctrl) {
		return newInstance$$PHOSPHORTAGGED(clazz, dimsTaint, dims);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, LazyIntArrayObjTags dimsTaint, int[] dims) {
		//		System.out.println("22Creating array instance of type " + clazz);
		Type t = Type.getType(clazz);
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
			try {
				clazz = Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(t).getInternalName().replace("/", "."));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return Array.newInstance(clazz, dims);
		} else if (t.getSort() != Type.OBJECT) {
			clazz = MultiDTaintedArrayWithObjTag.getClassForComponentType(t.getSort());
			if (clazz.isArray()) {
				int lastDim = dims[dims.length - 1];
				int[] newDims = new int[dims.length - 1];
				System.arraycopy(dims, 0, newDims, 0, dims.length - 1);
				Object[] ret = (Object[]) Array.newInstance(clazz, newDims);
				MultiDTaintedArrayWithObjTag.initLastDim(ret, lastDim, t.getSort());
				return ret;

			} else {
				int lastDimSize = dims[dims.length - 1];

				switch (t.getSort()) {
				case Type.BOOLEAN:
					return new LazyBooleanArrayObjTags(new boolean[lastDimSize]);
				case Type.BYTE:
					return new LazyByteArrayObjTags(new byte[lastDimSize]);
				case Type.CHAR:
					return new LazyCharArrayObjTags(new char[lastDimSize]);
				case Type.DOUBLE:
					return new LazyDoubleArrayObjTags(new double[lastDimSize]);
				case Type.FLOAT:
					return new LazyFloatArrayObjTags(new float[lastDimSize]);
				case Type.INT:
					return new LazyIntArrayObjTags(new int[lastDimSize]);
				case Type.LONG:
					return new LazyLongArrayObjTags(new long[lastDimSize]);
				case Type.SHORT:
					return new LazyShortArrayObjTags(new short[lastDimSize]);
				default:
					throw new IllegalArgumentException();
				}
			}
		}
		return Array.newInstance(clazz, dims);
	}

	public static TaintedByteWithIntTag getByte$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedByteWithIntTag ret) {
		if (obj instanceof LazyByteArrayIntTags) {
			LazyByteArrayIntTags ar = (LazyByteArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedBooleanWithIntTag getBoolean$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedBooleanWithIntTag ret) {
		return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedIntWithIntTag getInt$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedIntWithIntTag ret) {
		return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedCharWithIntTag getChar$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedCharWithIntTag ret) {
		return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedDoubleWithIntTag getDouble$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedDoubleWithIntTag ret) {
		return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedFloatWithIntTag getFloat$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedFloatWithIntTag ret) {
		return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedShortWithIntTag getShort$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedShortWithIntTag ret) {
		return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedLongWithIntTag getLong$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedLongWithIntTag ret) {
		return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedByteWithIntTag getByte$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl, TaintedByteWithIntTag ret) {
		return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedBooleanWithIntTag getBoolean$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedBooleanWithIntTag ret) {
		if (obj instanceof LazyBooleanArrayIntTags) {
			LazyBooleanArrayIntTags ar = (LazyBooleanArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedCharWithIntTag getChar$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedCharWithIntTag ret) {
		if (obj instanceof LazyCharArrayIntTags) {
			LazyCharArrayIntTags ar = (LazyCharArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDoubleWithIntTag getDouble$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedDoubleWithIntTag ret) {
		if (obj instanceof LazyDoubleArrayIntTags) {
			LazyDoubleArrayIntTags ar = (LazyDoubleArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedIntWithIntTag getInt$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedIntWithIntTag ret) {
		if (obj instanceof LazyIntArrayIntTags) {
			LazyIntArrayIntTags ar = (LazyIntArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLongWithIntTag getLong$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedLongWithIntTag ret) {
		if (obj instanceof LazyLongArrayIntTags) {
			LazyLongArrayIntTags ar = (LazyLongArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShortWithIntTag getShort$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedShortWithIntTag ret) {
		if (obj instanceof LazyShortArrayIntTags) {
			LazyShortArrayIntTags ar = (LazyShortArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloatWithIntTag getFloat$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedFloatWithIntTag ret) {
		if (obj instanceof LazyFloatArrayIntTags) {
			LazyFloatArrayIntTags ar = (LazyFloatArrayIntTags) obj;
			return ar.get(null, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx) {
		if (obj instanceof LazyBooleanArrayIntTags)
			return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedBooleanWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyByteArrayIntTags)
			return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedByteWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyCharArrayIntTags)
			return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedCharWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyDoubleArrayIntTags)
			return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedDoubleWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyFloatArrayIntTags)
			return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedFloatWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyIntArrayIntTags)
			return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedIntWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyLongArrayIntTags)
			return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedLongWithIntTag()).toPrimitiveType();
		else if (obj instanceof LazyShortArrayIntTags)
			return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedShortWithIntTag()).toPrimitiveType();
		return Array.get(obj, idx);
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl) {
		return get$$PHOSPHORTAGGED(obj, idxTaint, idx);
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx) {
		if (obj instanceof LazyBooleanArrayObjTags)
			return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedBooleanWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyByteArrayObjTags)
			return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedByteWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyCharArrayObjTags)
			return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedCharWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyDoubleArrayObjTags)
			return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedDoubleWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyFloatArrayObjTags)
			return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedFloatWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyIntArrayObjTags)
			return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedIntWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyLongArrayObjTags)
			return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedLongWithObjTag()).toPrimitiveType();
		else if (obj instanceof LazyShortArrayObjTags)
			return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedShortWithObjTag()).toPrimitiveType();
		return Array.get(obj, idx);
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, ControlTaintTagStack ctrl) {
		return get$$PHOSPHORTAGGED(obj, idxTaint, idx);
	}

	public static int tryToGetTaint(Object val) {
		try {
			val.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			return val.getClass().getDeclaredField("valuePHOSPHOR_TAG").getInt(val);
		} catch (Exception ex) {
			return 0;
		}
	}

	public static Taint tryToGetTaintObj(Object val) {
		try {
			val.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			return (Taint) val.getClass().getDeclaredField("valuePHOSPHOR_TAG").get(val);
		} catch (Exception ex) {
			return null;
		}
	}

	public static void set$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof LazyBooleanArrayIntTags)
				setBooleanInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Boolean) val);
			else if (obj instanceof LazyByteArrayIntTags)
				setByteInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Byte) val);
			else if (obj instanceof LazyCharArrayIntTags)
				setCharInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Character) val);
			else if (obj instanceof LazyDoubleArrayIntTags)
				setDoubleInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Double) val);
			else if (obj instanceof LazyFloatArrayIntTags)
				setFloatInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Float) val);
			else if (obj instanceof LazyIntArrayIntTags)
				setIntInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Integer) val);
			else if (obj instanceof LazyLongArrayIntTags)
				setLongInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Long) val);
			else if (obj instanceof LazyShortArrayIntTags)
				setShortInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Short) val);
			else
				throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}
	
	public static void setBooleanInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, boolean val) {
		setBoolean$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setByteInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, byte val) {
		setByte$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setCharInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, char val) {
		setChar$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setDoubleInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, double val) {
		setDouble$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setFloatInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, float val) {
		setFloat$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setIntInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, int val) {
		setInt$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setLongInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, long val) {
		setLong$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setShortInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, short val) {
		setShort$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, boolean val) {
		if (obj instanceof LazyBooleanArrayIntTags) {
			LazyBooleanArrayIntTags a = (LazyBooleanArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, byte val) {
		if (obj instanceof LazyByteArrayIntTags) {
			LazyByteArrayIntTags a = (LazyByteArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, char val) {
		if (obj instanceof LazyCharArrayIntTags) {
			LazyCharArrayIntTags a = (LazyCharArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, double val) {
		if (obj instanceof LazyDoubleArrayIntTags) {
			LazyDoubleArrayIntTags a = (LazyDoubleArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, float val) {
		if (obj instanceof LazyFloatArrayIntTags) {
			LazyFloatArrayIntTags a = (LazyFloatArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, int val) {
		if (obj instanceof LazyIntArrayIntTags) {
			LazyIntArrayIntTags a = (LazyIntArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, long val) {
		if (obj instanceof LazyLongArrayIntTags) {
			LazyLongArrayIntTags a = (LazyLongArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, short val) {
		if (obj instanceof LazyShortArrayIntTags) {
			LazyShortArrayIntTags a = (LazyShortArrayIntTags) obj;
			a.set(null, idx,taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedByteWithObjTag ret) {
		if (obj instanceof LazyByteArrayObjTags) {
			return ((LazyByteArrayObjTags)obj).get(((LazyByteArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedDoubleWithObjTag ret) {
		return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedFloatWithObjTag ret) {
		return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedShortWithObjTag ret) {
		return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, ControlTaintTagStack ctrl, TaintedByteWithObjTag ret) {
		return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedBooleanWithObjTag ret) {
		if (obj instanceof LazyByteArrayObjTags) {
			return ((LazyBooleanArrayObjTags)obj).get(((LazyBooleanArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedCharWithObjTag ret) {
		if (obj instanceof LazyCharArrayObjTags) {
			return ((LazyCharArrayObjTags)obj).get(((LazyCharArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedDoubleWithObjTag ret) {
		if (obj instanceof LazyDoubleArrayObjTags) {
			return ((LazyDoubleArrayObjTags)obj).get(((LazyDoubleArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedIntWithObjTag ret) {
		if (obj instanceof LazyIntArrayObjTags) {
			return ((LazyIntArrayObjTags)obj).get(((LazyIntArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedLongWithObjTag ret) {
		if (obj instanceof LazyLongArrayObjTags) {
			return ((LazyLongArrayObjTags)obj).get(((LazyLongArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedShortWithObjTag ret) {
		if (obj instanceof LazyShortArrayObjTags) {
			return ((LazyShortArrayObjTags)obj).get(((LazyShortArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, TaintedFloatWithObjTag ret) {
		if (obj instanceof LazyFloatArrayObjTags) {
			return ((LazyFloatArrayObjTags)obj).get(((LazyFloatArrayObjTags) obj).val, idx, ret);
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static void set$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof LazyBooleanArrayObjTags)
				setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Boolean) val);
			else if (obj instanceof LazyByteArrayObjTags)
				setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Byte) val);
			else if (obj instanceof LazyCharArrayObjTags)
				setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Character) val);
			else if (obj instanceof LazyDoubleArrayObjTags)
				setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Double) val);
			else if (obj instanceof LazyFloatArrayObjTags)
				setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Float) val);
			else if (obj instanceof LazyIntArrayObjTags)
				setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Integer) val);
			else if (obj instanceof LazyLongArrayObjTags)
				setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Long) val);
			else if (obj instanceof LazyShortArrayObjTags)
				setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Short) val);
			else
				throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void set$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Object val, ControlTaintTagStack ctrl) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof LazyBooleanArrayObjTags)
				setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Boolean) val, ctrl);
			else if (obj instanceof LazyByteArrayObjTags)
				setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Byte) val, ctrl);
			else if (obj instanceof LazyCharArrayObjTags)
				setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Character) val, ctrl);
			else if (obj instanceof LazyDoubleArrayObjTags)
				setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Double) val, ctrl);
			else if (obj instanceof LazyFloatArrayObjTags)
				setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Float) val, ctrl);
			else if (obj instanceof LazyIntArrayObjTags)
				setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Integer) val, ctrl);
			else if (obj instanceof LazyLongArrayObjTags)
				setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Long) val, ctrl);
			else if (obj instanceof LazyShortArrayObjTags)
				setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Short) val, ctrl);
			else
				throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, boolean val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, byte val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, char val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, double val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, float val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, int val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, long val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, short val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, boolean val) {
		if (obj instanceof LazyBooleanArrayObjTags) {
			LazyBooleanArrayObjTags a = (LazyBooleanArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, byte val) {
		if (obj instanceof LazyByteArrayObjTags) {
			LazyByteArrayObjTags a = (LazyByteArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, char val) {
		if (obj instanceof LazyCharArrayObjTags) {
			LazyCharArrayObjTags a = (LazyCharArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, double val) {
		if (obj instanceof LazyDoubleArrayObjTags) {
			LazyDoubleArrayObjTags a = (LazyDoubleArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, float val) {
		if (obj instanceof LazyFloatArrayObjTags) {
			LazyFloatArrayObjTags a = (LazyFloatArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, int val) {
		if (obj instanceof LazyIntArrayObjTags) {
			LazyIntArrayObjTags a = (LazyIntArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, long val) {
		if (obj instanceof LazyLongArrayObjTags) {
			LazyLongArrayObjTags a = (LazyLongArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, Taint idxTaint, int idx, Taint taint, short val) {
		if (obj instanceof LazyShortArrayObjTags) {
			LazyShortArrayObjTags a = (LazyShortArrayObjTags) obj;
			a.set(a.val, idx, taint, val);
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}
}
