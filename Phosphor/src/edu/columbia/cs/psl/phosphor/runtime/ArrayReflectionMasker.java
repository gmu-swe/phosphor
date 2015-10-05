package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
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
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArrayWithObjTag;

public class ArrayReflectionMasker {
	public static TaintedIntWithIntTag getLength$$PHOSPHORTAGGED(Object obj, TaintedIntWithIntTag ret) {
		if (obj.getClass().isArray()) {
			ret.taint = 0;
			ret.val = Array.getLength(obj);
			return ret;
		} else if (obj instanceof MultiDTaintedArrayWithIntTag) {
			ret.taint = 0;
			ret.val = Array.getLength(((MultiDTaintedArrayWithIntTag) obj).getVal());
			return ret;
		}
		throw new ArrayStoreException("Uknown array type: " + obj.getClass());
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
					return new MultiDTaintedDoubleArrayWithIntTag(new int[len], new double[len]);
				if (tmp == Float.TYPE)
					return new MultiDTaintedFloatArrayWithIntTag(new int[len], new float[len]);
				if (tmp == Integer.TYPE)
					return new MultiDTaintedIntArrayWithIntTag(new int[len], new int[len]);
				if (tmp == Long.TYPE)
					return new MultiDTaintedLongArrayWithIntTag(new int[len], new long[len]);
				if (tmp == Short.TYPE)
					return new MultiDTaintedShortArrayWithIntTag(new int[len], new short[len]);
				if (tmp == Boolean.TYPE)
					return new MultiDTaintedBooleanArrayWithIntTag(new int[len], new boolean[len]);
				if (tmp == Byte.TYPE)
					return new MultiDTaintedByteArrayWithIntTag(new int[len], new byte[len]);
				if (tmp == Character.TYPE)
					return new MultiDTaintedCharArrayWithIntTag(new int[len], new char[len]);
			} else
				clazz = MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int[] dimsTaint, int[] dims, ControlTaintTagStack ctrl) {
		return newInstance$$PHOSPHORTAGGED(clazz, dimsTaint, dims);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int[] dimsTaint, int[] dims) {
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
					return new MultiDTaintedBooleanArrayWithIntTag(new int[lastDimSize], new boolean[lastDimSize]);
				case Type.BYTE:
					return new MultiDTaintedByteArrayWithIntTag(new int[lastDimSize], new byte[lastDimSize]);
				case Type.CHAR:
					return new MultiDTaintedCharArrayWithIntTag(new int[lastDimSize], new char[lastDimSize]);
				case Type.DOUBLE:
					return new MultiDTaintedDoubleArrayWithIntTag(new int[lastDimSize], new double[lastDimSize]);
				case Type.FLOAT:
					return new MultiDTaintedFloatArrayWithIntTag(new int[lastDimSize], new float[lastDimSize]);
				case Type.INT:
					return new MultiDTaintedIntArrayWithIntTag(new int[lastDimSize], new int[lastDimSize]);
				case Type.LONG:
					return new MultiDTaintedLongArrayWithIntTag(new int[lastDimSize], new long[lastDimSize]);
				case Type.SHORT:
					return new MultiDTaintedShortArrayWithIntTag(new int[lastDimSize], new short[lastDimSize]);
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
		} else if (obj instanceof MultiDTaintedArrayWithObjTag) {
			ret.taint = null;
			ret.val = Array.getLength(((MultiDTaintedArrayWithObjTag) obj).getVal());
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
		return newInstance$$PHOSPHORTAGGED(clazz, lenTaint, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Object lenTaint, int len) {
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
					return new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray(len), new double[len]);
				if (tmp == Float.TYPE)
					return new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray(len), new float[len]);
				if (tmp == Integer.TYPE)
					return new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray(len), new int[len]);
				if (tmp == Long.TYPE)
					return new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray(len), new long[len]);
				if (tmp == Short.TYPE)
					return new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray(len), new short[len]);
				if (tmp == Boolean.TYPE)
					return new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray(len), new boolean[len]);
				if (tmp == Byte.TYPE)
					return new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray(len), new byte[len]);
				if (tmp == Character.TYPE)
					return new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray(len), new char[len]);
			} else
				clazz = MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Object[] dimsTaint, int[] dims, ControlTaintTagStack ctrl) {
		return newInstance$$PHOSPHORTAGGED(clazz, dimsTaint, dims);
	}

	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, Object[] dimsTaint, int[] dims) {
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
					return new MultiDTaintedBooleanArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new boolean[lastDimSize]);
				case Type.BYTE:
					return new MultiDTaintedByteArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new byte[lastDimSize]);
				case Type.CHAR:
					return new MultiDTaintedCharArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new char[lastDimSize]);
				case Type.DOUBLE:
					return new MultiDTaintedDoubleArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new double[lastDimSize]);
				case Type.FLOAT:
					return new MultiDTaintedFloatArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new float[lastDimSize]);
				case Type.INT:
					return new MultiDTaintedIntArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new int[lastDimSize]);
				case Type.LONG:
					return new MultiDTaintedLongArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new long[lastDimSize]);
				case Type.SHORT:
					return new MultiDTaintedShortArrayWithObjTag(TaintUtils.newTaintArray(lastDimSize), new short[lastDimSize]);
				default:
					throw new IllegalArgumentException();
				}
			}
		}
		return Array.newInstance(clazz, dims);
	}

	public static TaintedByteWithIntTag getByte$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedByteWithIntTag ret) {
		if (obj instanceof MultiDTaintedByteArrayWithIntTag) {
			MultiDTaintedByteArrayWithIntTag ar = (MultiDTaintedByteArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
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
		if (obj instanceof MultiDTaintedBooleanArrayWithIntTag) {
			MultiDTaintedBooleanArrayWithIntTag ar = (MultiDTaintedBooleanArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedCharWithIntTag getChar$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedCharWithIntTag ret) {
		if (obj instanceof MultiDTaintedCharArrayWithIntTag) {
			MultiDTaintedCharArrayWithIntTag ar = (MultiDTaintedCharArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDoubleWithIntTag getDouble$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedDoubleWithIntTag ret) {
		if (obj instanceof MultiDTaintedDoubleArrayWithIntTag) {
			MultiDTaintedDoubleArrayWithIntTag ar = (MultiDTaintedDoubleArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedIntWithIntTag getInt$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedIntWithIntTag ret) {
		if (obj instanceof MultiDTaintedIntArrayWithIntTag) {
			MultiDTaintedIntArrayWithIntTag ar = (MultiDTaintedIntArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLongWithIntTag getLong$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedLongWithIntTag ret) {
		if (obj instanceof MultiDTaintedLongArrayWithIntTag) {
			MultiDTaintedLongArrayWithIntTag ar = (MultiDTaintedLongArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShortWithIntTag getShort$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedShortWithIntTag ret) {
		if (obj instanceof MultiDTaintedShortArrayWithIntTag) {
			MultiDTaintedShortArrayWithIntTag ar = (MultiDTaintedShortArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloatWithIntTag getFloat$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedFloatWithIntTag ret) {
		if (obj instanceof MultiDTaintedFloatArrayWithIntTag) {
			MultiDTaintedFloatArrayWithIntTag ar = (MultiDTaintedFloatArrayWithIntTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx) {
		if (obj instanceof MultiDTaintedBooleanArrayWithIntTag)
			return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedBooleanWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedByteArrayWithIntTag)
			return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedByteWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedCharArrayWithIntTag)
			return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedCharWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedDoubleArrayWithIntTag)
			return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedDoubleWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedFloatArrayWithIntTag)
			return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedFloatWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedIntArrayWithIntTag)
			return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedIntWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedLongArrayWithIntTag)
			return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedLongWithIntTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedShortArrayWithIntTag)
			return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedShortWithIntTag()).toPrimitiveType();
		return Array.get(obj, idx);
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl) {
		return get$$PHOSPHORTAGGED(obj, idxTaint, idx);
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx) {
		if (obj instanceof MultiDTaintedBooleanArrayWithObjTag)
			return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedBooleanWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedByteArrayWithObjTag)
			return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedByteWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedCharArrayWithObjTag)
			return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedCharWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedDoubleArrayWithObjTag)
			return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedDoubleWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedFloatArrayWithObjTag)
			return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedFloatWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedIntArrayWithObjTag)
			return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedIntWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedLongArrayWithObjTag)
			return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedLongWithObjTag()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedShortArrayWithObjTag)
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

	public static Object tryToGetTaintObj(Object val) {
		try {
			val.getClass().getDeclaredField("valuePHOSPHOR_TAG").setAccessible(true);
			return val.getClass().getDeclaredField("valuePHOSPHOR_TAG").get(val);
		} catch (Exception ex) {
			return null;
		}
	}

	public static void set$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArrayWithIntTag)
				setBooleanInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Boolean) val);
			else if (obj instanceof MultiDTaintedByteArrayWithIntTag)
				setByteInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Byte) val);
			else if (obj instanceof MultiDTaintedCharArrayWithIntTag)
				setCharInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Character) val);
			else if (obj instanceof MultiDTaintedDoubleArrayWithIntTag)
				setDoubleInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Double) val);
			else if (obj instanceof MultiDTaintedFloatArrayWithIntTag)
				setFloatInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Float) val);
			else if (obj instanceof MultiDTaintedIntArrayWithIntTag)
				setIntInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Integer) val);
			else if (obj instanceof MultiDTaintedLongArrayWithIntTag)
				setLongInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Long) val);
			else if (obj instanceof MultiDTaintedShortArrayWithIntTag)
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
		if (obj instanceof MultiDTaintedBooleanArrayWithIntTag) {
			MultiDTaintedBooleanArrayWithIntTag a = (MultiDTaintedBooleanArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, byte val) {
		if (obj instanceof MultiDTaintedByteArrayWithIntTag) {
			MultiDTaintedByteArrayWithIntTag a = (MultiDTaintedByteArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, char val) {
		if (obj instanceof MultiDTaintedCharArrayWithIntTag) {
			MultiDTaintedCharArrayWithIntTag a = (MultiDTaintedCharArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, double val) {
		if (obj instanceof MultiDTaintedDoubleArrayWithIntTag) {
			MultiDTaintedDoubleArrayWithIntTag a = (MultiDTaintedDoubleArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, float val) {
		if (obj instanceof MultiDTaintedFloatArrayWithIntTag) {
			MultiDTaintedFloatArrayWithIntTag a = (MultiDTaintedFloatArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, int val) {
		if (obj instanceof MultiDTaintedIntArrayWithIntTag) {
			MultiDTaintedIntArrayWithIntTag a = (MultiDTaintedIntArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, long val) {
		if (obj instanceof MultiDTaintedLongArrayWithIntTag) {
			MultiDTaintedLongArrayWithIntTag a = (MultiDTaintedLongArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, short val) {
		if (obj instanceof MultiDTaintedShortArrayWithIntTag) {
			MultiDTaintedShortArrayWithIntTag a = (MultiDTaintedShortArrayWithIntTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedByteWithObjTag ret) {
		if (obj instanceof MultiDTaintedByteArrayWithObjTag) {
			MultiDTaintedByteArrayWithObjTag ar = (MultiDTaintedByteArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedCharWithObjTag ret) {
		return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedDoubleWithObjTag ret) {
		return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedFloatWithObjTag ret) {
		return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedShortWithObjTag ret) {
		return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedLongWithObjTag ret) {
		return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, ControlTaintTagStack ctrl, TaintedByteWithObjTag ret) {
		return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}

	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedBooleanWithObjTag ret) {
		if (obj instanceof MultiDTaintedBooleanArrayWithObjTag) {
			MultiDTaintedBooleanArrayWithObjTag ar = (MultiDTaintedBooleanArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedCharWithObjTag ret) {
		if (obj instanceof MultiDTaintedCharArrayWithObjTag) {
			MultiDTaintedCharArrayWithObjTag ar = (MultiDTaintedCharArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedDoubleWithObjTag ret) {
		if (obj instanceof MultiDTaintedDoubleArrayWithObjTag) {
			MultiDTaintedDoubleArrayWithObjTag ar = (MultiDTaintedDoubleArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedIntWithObjTag ret) {
		if (obj instanceof MultiDTaintedIntArrayWithObjTag) {
			MultiDTaintedIntArrayWithObjTag ar = (MultiDTaintedIntArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedLongWithObjTag ret) {
		if (obj instanceof MultiDTaintedLongArrayWithObjTag) {
			MultiDTaintedLongArrayWithObjTag ar = (MultiDTaintedLongArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedShortWithObjTag ret) {
		if (obj instanceof MultiDTaintedShortArrayWithObjTag) {
			MultiDTaintedShortArrayWithObjTag ar = (MultiDTaintedShortArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, TaintedFloatWithObjTag ret) {
		if (obj instanceof MultiDTaintedFloatArrayWithObjTag) {
			MultiDTaintedFloatArrayWithObjTag ar = (MultiDTaintedFloatArrayWithObjTag) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static void set$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArrayWithObjTag)
				setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Boolean) val);
			else if (obj instanceof MultiDTaintedByteArrayWithObjTag)
				setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Byte) val);
			else if (obj instanceof MultiDTaintedCharArrayWithObjTag)
				setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Character) val);
			else if (obj instanceof MultiDTaintedDoubleArrayWithObjTag)
				setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Double) val);
			else if (obj instanceof MultiDTaintedFloatArrayWithObjTag)
				setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Float) val);
			else if (obj instanceof MultiDTaintedIntArrayWithObjTag)
				setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Integer) val);
			else if (obj instanceof MultiDTaintedLongArrayWithObjTag)
				setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Long) val);
			else if (obj instanceof MultiDTaintedShortArrayWithObjTag)
				setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Short) val);
			else
				throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void set$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object val, ControlTaintTagStack ctrl) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArrayWithObjTag)
				setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Boolean) val, ctrl);
			else if (obj instanceof MultiDTaintedByteArrayWithObjTag)
				setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Byte) val, ctrl);
			else if (obj instanceof MultiDTaintedCharArrayWithObjTag)
				setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Character) val, ctrl);
			else if (obj instanceof MultiDTaintedDoubleArrayWithObjTag)
				setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Double) val, ctrl);
			else if (obj instanceof MultiDTaintedFloatArrayWithObjTag)
				setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Float) val, ctrl);
			else if (obj instanceof MultiDTaintedIntArrayWithObjTag)
				setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Integer) val, ctrl);
			else if (obj instanceof MultiDTaintedLongArrayWithObjTag)
				setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Long) val, ctrl);
			else if (obj instanceof MultiDTaintedShortArrayWithObjTag)
				setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, tryToGetTaintObj(val), (Short) val, ctrl);
			else
				throw new ArrayStoreException("Got passed an obj of type " + obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, boolean val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, byte val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setByte$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, char val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setChar$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, double val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, float val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, int val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setInt$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, long val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setLong$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, short val, ControlTaintTagStack ctrl) {
		taint = Taint.combineTags((Taint) taint, ctrl);
		setShort$$PHOSPHORTAGGED(obj, idxTaint, idx, taint, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, boolean val) {
		if (obj instanceof MultiDTaintedBooleanArrayWithObjTag) {
			MultiDTaintedBooleanArrayWithObjTag a = (MultiDTaintedBooleanArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, byte val) {
		if (obj instanceof MultiDTaintedByteArrayWithObjTag) {
			MultiDTaintedByteArrayWithObjTag a = (MultiDTaintedByteArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, char val) {
		if (obj instanceof MultiDTaintedCharArrayWithObjTag) {
			MultiDTaintedCharArrayWithObjTag a = (MultiDTaintedCharArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, double val) {
		if (obj instanceof MultiDTaintedDoubleArrayWithObjTag) {
			MultiDTaintedDoubleArrayWithObjTag a = (MultiDTaintedDoubleArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, float val) {
		if (obj instanceof MultiDTaintedFloatArrayWithObjTag) {
			MultiDTaintedFloatArrayWithObjTag a = (MultiDTaintedFloatArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, int val) {
		if (obj instanceof MultiDTaintedIntArrayWithObjTag) {
			MultiDTaintedIntArrayWithObjTag a = (MultiDTaintedIntArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, long val) {
		if (obj instanceof MultiDTaintedLongArrayWithObjTag) {
			MultiDTaintedLongArrayWithObjTag a = (MultiDTaintedLongArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, Object idxTaint, int idx, Object taint, short val) {
		if (obj instanceof MultiDTaintedShortArrayWithObjTag) {
			MultiDTaintedShortArrayWithObjTag a = (MultiDTaintedShortArrayWithObjTag) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		} else
			throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}
}
