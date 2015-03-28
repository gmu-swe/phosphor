package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.TaintedByte;
import edu.columbia.cs.psl.phosphor.struct.TaintedChar;
import edu.columbia.cs.psl.phosphor.struct.TaintedDouble;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloat;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedLong;
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedBooleanArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedByteArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedCharArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedDoubleArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedFloatArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedIntArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedLongArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedShortArray;

public class ArrayReflectionMasker {
	public static TaintedInt getLength$$INVIVO_PC(Object obj, TaintedInt ret)
	{
		if(obj.getClass().isArray())
		{
			ret.taint = 0;
			ret.val = Array.getLength(obj);
			return ret;
		}
		else if(obj instanceof MultiDTaintedArray)
		{
			ret.taint = 0; 
			ret.val = Array.getLength(((MultiDTaintedArray)obj).getVal());
			return ret;
		}
		throw new ArrayStoreException("Uknown array type: " + obj.getClass());
	}
	public static Object newInstance$$INVIVO_PC(Class clazz, int lenTaint, int len,ControlTaintTagStack zz) {
		return newInstance$$INVIVO_PC(clazz, lenTaint, len);
	}
	public static Object newInstance$$INVIVO_PC(Class clazz, int lenTaint, int len) {
		Class tmp = clazz;
		int dims =0;
		while(tmp.isArray())
		{
			tmp = tmp.getComponentType();
			dims++;
		}
		if(tmp.isPrimitive())
		{
//			if(dims == 0)
//			{
//				
//			}
//			else
			if(dims ==0)
			{
				if (tmp == Double.TYPE)
					return new MultiDTaintedDoubleArray(new int[len],new double[len]);
				if (tmp == Float.TYPE)
					return new MultiDTaintedFloatArray(new int[len],new float[len]);
				if (tmp == Integer.TYPE)
					return new MultiDTaintedIntArray(new int[len],new int[len]);
				if (tmp == Long.TYPE)
					return new MultiDTaintedLongArray(new int[len],new long[len]);
				if (tmp == Short.TYPE)
					return new MultiDTaintedShortArray(new int[len],new short[len]);
				if (tmp == Boolean.TYPE)
					return new MultiDTaintedBooleanArray(new int[len],new boolean[len]);
				if (tmp == Byte.TYPE)
					return new MultiDTaintedByteArray(new int[len],new byte[len]);
				if (tmp == Character.TYPE)
					return new MultiDTaintedCharArray(new int[len],new char[len]);
			}
			else
				clazz = MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(clazz);
		}
		return Array.newInstance(clazz, len);
	}

	public static Object newInstance$$INVIVO_PC(Class clazz, int[] dimsTaint, int[] dims) {
//		System.out.println("22Creating array instance of type " + clazz);
		Type t = Type.getType(clazz);
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
			try {
				clazz = Class.forName(MultiDTaintedArray.getTypeForType(t).getInternalName().replace("/", "."));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return Array.newInstance(clazz, dims);
		} else if (t.getSort() != Type.OBJECT) {
			clazz = MultiDTaintedArray.getClassForComponentType(t.getSort());
			if(clazz.isArray())
			{
				int lastDim = dims[dims.length - 1];
				int[] newDims = new int[dims.length - 1];
				System.arraycopy(dims, 0, newDims, 0, dims.length - 1);
				Object[] ret = (Object[]) Array.newInstance(clazz, newDims);
				MultiDTaintedArray.initLastDim(ret, lastDim, t.getSort());
				return ret;

			}
			else
			{
				int lastDimSize = dims[dims.length - 1];

				switch (t.getSort()) {
				case Type.BOOLEAN:
					return new MultiDTaintedBooleanArray(new int[lastDimSize], new boolean[lastDimSize]);
				case Type.BYTE:
					return new MultiDTaintedByteArray(new int[lastDimSize], new byte[lastDimSize]);
				case Type.CHAR:
					return new MultiDTaintedCharArray(new int[lastDimSize], new char[lastDimSize]);
				case Type.DOUBLE:
					return new MultiDTaintedDoubleArray(new int[lastDimSize], new double[lastDimSize]);
				case Type.FLOAT:
					return new MultiDTaintedFloatArray(new int[lastDimSize], new float[lastDimSize]);
				case Type.INT:
					return new MultiDTaintedIntArray(new int[lastDimSize], new int[lastDimSize]);
				case Type.LONG:
					return new MultiDTaintedLongArray(new int[lastDimSize], new long[lastDimSize]);
				case Type.SHORT:
					return new MultiDTaintedShortArray(new int[lastDimSize], new short[lastDimSize]);
				default:
					throw new IllegalArgumentException();
				}
			}
		}
		return Array.newInstance(clazz, dims);
	}

	public static TaintedByte getByte$$INVIVO_PC(Object obj, int idxTaint, int idx, TaintedByte ret) {
		if (obj instanceof MultiDTaintedByteArray) {
			MultiDTaintedByteArray ar = (MultiDTaintedByteArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedBoolean getBoolean$$INVIVO_PC(Object obj, int idxTaint, int idx,TaintedBoolean ret) {
		if (obj instanceof MultiDTaintedBooleanArray) {
			MultiDTaintedBooleanArray ar = (MultiDTaintedBooleanArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedChar getChar$$INVIVO_PC(Object obj, int idxTaint, int idx,TaintedChar ret) {
		if (obj instanceof MultiDTaintedCharArray) {
			MultiDTaintedCharArray ar = (MultiDTaintedCharArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDouble getDouble$$INVIVO_PC(Object obj, int idxTaint, int idx,TaintedDouble ret) {
		if (obj instanceof MultiDTaintedDoubleArray) {
			MultiDTaintedDoubleArray ar = (MultiDTaintedDoubleArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedInt getInt$$INVIVO_PC(Object obj, int idxTaint, int idx, TaintedInt ret) {
		if (obj instanceof MultiDTaintedIntArray) {
			MultiDTaintedIntArray ar = (MultiDTaintedIntArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLong getLong$$INVIVO_PC(Object obj, int idxTaint, int idx, TaintedLong ret) {
		if (obj instanceof MultiDTaintedLongArray) {
			MultiDTaintedLongArray ar = (MultiDTaintedLongArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShort getShort$$INVIVO_PC(Object obj, int idxTaint, int idx, TaintedShort ret) {
		if (obj instanceof MultiDTaintedShortArray) {
			MultiDTaintedShortArray ar = (MultiDTaintedShortArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloat getFloat$$INVIVO_PC(Object obj, int idxTaint, int idx, TaintedFloat ret) {
		if (obj instanceof MultiDTaintedFloatArray) {
			MultiDTaintedFloatArray ar = (MultiDTaintedFloatArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static Object get$$INVIVO_PC(Object obj, int idxTaint, int idx) {
		if (obj instanceof MultiDTaintedBooleanArray)
			return getBoolean$$INVIVO_PC(obj, idxTaint, idx, new TaintedBoolean()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedByteArray)
			return getByte$$INVIVO_PC(obj, idxTaint, idx, new TaintedByte()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedCharArray)
			return getChar$$INVIVO_PC(obj, idxTaint, idx, new TaintedChar()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedDoubleArray)
			return getDouble$$INVIVO_PC(obj, idxTaint, idx, new TaintedDouble()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedFloatArray)
			return getFloat$$INVIVO_PC(obj, idxTaint, idx, new TaintedFloat()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedIntArray)
			return getInt$$INVIVO_PC(obj, idxTaint, idx, new TaintedInt()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedLongArray)
			return getLong$$INVIVO_PC(obj, idxTaint, idx, new TaintedLong()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedShortArray)
			return getShort$$INVIVO_PC(obj, idxTaint, idx, new TaintedShort()).toPrimitiveType();
		return Array.get(obj, idx);
	}

	public static int tryToGetTaint(Object val) {
		try {
			val.getClass().getDeclaredField("valueINVIVO_PC_TAINT").setAccessible(true);
			return val.getClass().getDeclaredField("valueINVIVO_PC_TAINT").getInt(val);
		} catch (Exception ex) {
			return 0;
		}
	}

	public static void set$$INVIVO_PC(Object obj, int idxtaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArray)
				setBoolean$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Boolean) val);
			else if (obj instanceof MultiDTaintedByteArray)
				setByte$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Byte) val);
			else if (obj instanceof MultiDTaintedCharArray)
				setChar$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Character) val);
			else if (obj instanceof MultiDTaintedDoubleArray)
				setDouble$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Double) val);
			else if (obj instanceof MultiDTaintedFloatArray)
				setFloat$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Float) val);
			else if (obj instanceof MultiDTaintedIntArray)
				setInt$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Integer) val);
			else if (obj instanceof MultiDTaintedLongArray)
				setLong$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Long) val);
			else if (obj instanceof MultiDTaintedShortArray)
				setShort$$INVIVO_PC(obj, idxtaint, idx, tryToGetTaint(val), (Short) val);
			else
				throw new ArrayStoreException("Got passed an obj of type "+obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void setBoolean$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, boolean val) {
		if (obj instanceof MultiDTaintedBooleanArray) {
			MultiDTaintedBooleanArray a = (MultiDTaintedBooleanArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, byte val) {
		if (obj instanceof MultiDTaintedByteArray) {
			MultiDTaintedByteArray a = (MultiDTaintedByteArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, char val) {
		if (obj instanceof MultiDTaintedCharArray) {
			MultiDTaintedCharArray a = (MultiDTaintedCharArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, double val) {
		if (obj instanceof MultiDTaintedDoubleArray) {
			MultiDTaintedDoubleArray a = (MultiDTaintedDoubleArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, float val) {
		if (obj instanceof MultiDTaintedFloatArray) {
			MultiDTaintedFloatArray a = (MultiDTaintedFloatArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, int val) {
		if (obj instanceof MultiDTaintedIntArray) {
			MultiDTaintedIntArray a = (MultiDTaintedIntArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, long val) {
		if (obj instanceof MultiDTaintedLongArray) {
			MultiDTaintedLongArray a = (MultiDTaintedLongArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$INVIVO_PC(Object obj, int idxtaint, int idx, int taint, short val) {
		if (obj instanceof MultiDTaintedShortArray) {
			MultiDTaintedShortArray a = (MultiDTaintedShortArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}
}
