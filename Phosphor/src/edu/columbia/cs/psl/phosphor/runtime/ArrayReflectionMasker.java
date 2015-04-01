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
	public static TaintedInt getLength$$PHOSPHORTAGGED(Object obj, TaintedInt ret)
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
	public static TaintedInt getLength$$PHOSPHORTAGGED(Object obj, ControlTaintTagStack ctlr, TaintedInt ret)
	{
		return getLength$$PHOSPHORTAGGED(obj, ret);
	}
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int lenTaint, int len,ControlTaintTagStack zz) {
		return newInstance$$PHOSPHORTAGGED(clazz, lenTaint, len);
	}
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int lenTaint, int len) {
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
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int[] dimsTaint, int[] dims, ControlTaintTagStack ctrl) {
		return newInstance$$PHOSPHORTAGGED(clazz, dimsTaint, dims);
	}
	public static Object newInstance$$PHOSPHORTAGGED(Class clazz, int[] dimsTaint, int[] dims) {
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

	public static TaintedByte getByte$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedByte ret) {
		if (obj instanceof MultiDTaintedByteArray) {
			MultiDTaintedByteArray ar = (MultiDTaintedByteArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedBoolean getBoolean$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedBoolean ret) {
		return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedInt getInt$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedInt ret) {
		return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedChar getChar$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedChar ret) {
		return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedDouble getDouble$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedDouble ret) {
		return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedFloat getFloat$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedFloat ret) {
		return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedShort getShort$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedShort ret) {
		return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedLong getLong$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedLong ret) {
		return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	public static TaintedByte getByte$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl, TaintedByte ret) {
		return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, ret);
	}
	
	public static TaintedBoolean getBoolean$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,TaintedBoolean ret) {
		if (obj instanceof MultiDTaintedBooleanArray) {
			MultiDTaintedBooleanArray ar = (MultiDTaintedBooleanArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedChar getChar$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,TaintedChar ret) {
		if (obj instanceof MultiDTaintedCharArray) {
			MultiDTaintedCharArray ar = (MultiDTaintedCharArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedDouble getDouble$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,TaintedDouble ret) {
		if (obj instanceof MultiDTaintedDoubleArray) {
			MultiDTaintedDoubleArray ar = (MultiDTaintedDoubleArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedInt getInt$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedInt ret) {
		if (obj instanceof MultiDTaintedIntArray) {
			MultiDTaintedIntArray ar = (MultiDTaintedIntArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedLong getLong$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedLong ret) {
		if (obj instanceof MultiDTaintedLongArray) {
			MultiDTaintedLongArray ar = (MultiDTaintedLongArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedShort getShort$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedShort ret) {
		if (obj instanceof MultiDTaintedShortArray) {
			MultiDTaintedShortArray ar = (MultiDTaintedShortArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static TaintedFloat getFloat$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx, TaintedFloat ret) {
		if (obj instanceof MultiDTaintedFloatArray) {
			MultiDTaintedFloatArray ar = (MultiDTaintedFloatArray) obj;
			ret.val = ar.val[idx];
			ret.taint = ar.taint[idx];
			return ret;
		}
		throw new ArrayStoreException("Called getX, but don't have tainted X array!");
	}

	public static Object get$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx) {
		if (obj instanceof MultiDTaintedBooleanArray)
			return getBoolean$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedBoolean()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedByteArray)
			return getByte$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedByte()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedCharArray)
			return getChar$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedChar()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedDoubleArray)
			return getDouble$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedDouble()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedFloatArray)
			return getFloat$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedFloat()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedIntArray)
			return getInt$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedInt()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedLongArray)
			return getLong$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedLong()).toPrimitiveType();
		else if (obj instanceof MultiDTaintedShortArray)
			return getShort$$PHOSPHORTAGGED(obj, idxTaint, idx, new TaintedShort()).toPrimitiveType();
		return Array.get(obj, idx);
	}
	
	public static Object get$$PHOSPHORTAGGED(Object obj, int idxTaint, int idx,ControlTaintTagStack ctrl) {
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

	public static void set$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, Object val) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArray)
				setBoolean$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Boolean) val);
			else if (obj instanceof MultiDTaintedByteArray)
				setByte$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Byte) val);
			else if (obj instanceof MultiDTaintedCharArray)
				setChar$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Character) val);
			else if (obj instanceof MultiDTaintedDoubleArray)
				setDouble$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Double) val);
			else if (obj instanceof MultiDTaintedFloatArray)
				setFloat$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Float) val);
			else if (obj instanceof MultiDTaintedIntArray)
				setInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Integer) val);
			else if (obj instanceof MultiDTaintedLongArray)
				setLong$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Long) val);
			else if (obj instanceof MultiDTaintedShortArray)
				setShort$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Short) val);
			else
				throw new ArrayStoreException("Got passed an obj of type "+obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}

	public static void set$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, Object val,ControlTaintTagStack ctrl) {
		if (obj != null && !obj.getClass().isArray()) {
			//in this case obj will be boxed, and we need to pull the taint out of val when we unbox it
			if (obj instanceof MultiDTaintedBooleanArray)
				setBoolean$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Boolean) val, ctrl);
			else if (obj instanceof MultiDTaintedByteArray)
				setByte$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Byte) val, ctrl);
			else if (obj instanceof MultiDTaintedCharArray)
				setChar$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Character) val, ctrl);
			else if (obj instanceof MultiDTaintedDoubleArray)
				setDouble$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Double) val, ctrl);
			else if (obj instanceof MultiDTaintedFloatArray)
				setFloat$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Float) val, ctrl);
			else if (obj instanceof MultiDTaintedIntArray)
				setInt$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Integer) val, ctrl);
			else if (obj instanceof MultiDTaintedLongArray)
				setLong$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Long) val, ctrl);
			else if (obj instanceof MultiDTaintedShortArray)
				setShort$$PHOSPHORTAGGED(obj, idxtaint, idx, tryToGetTaint(val), (Short) val, ctrl);
			else
				throw new ArrayStoreException("Got passed an obj of type "+obj + " to store to");
		} else
			Array.set(obj, idx, val);
	}
	public static void setBoolean$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, boolean val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setBoolean$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setByte$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, byte val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setByte$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setChar$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, char val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setChar$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setDouble$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, double val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setDouble$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setFloat$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, float val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setFloat$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, int val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setInt$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setLong$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, long val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setLong$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	public static void setShort$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, short val, ControlTaintTagStack ctrl) {
		taint = SimpleMultiTaintHandler.combineTags(taint, ctrl);
		setShort$$PHOSPHORTAGGED(obj, idxtaint, idx, taint, val);
	}
	
	
	public static void setBoolean$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, boolean val) {
		if (obj instanceof MultiDTaintedBooleanArray) {
			MultiDTaintedBooleanArray a = (MultiDTaintedBooleanArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setByte$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, byte val) {
		if (obj instanceof MultiDTaintedByteArray) {
			MultiDTaintedByteArray a = (MultiDTaintedByteArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!, got " + obj.getClass());
	}

	public static void setChar$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, char val) {
		if (obj instanceof MultiDTaintedCharArray) {
			MultiDTaintedCharArray a = (MultiDTaintedCharArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setDouble$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, double val) {
		if (obj instanceof MultiDTaintedDoubleArray) {
			MultiDTaintedDoubleArray a = (MultiDTaintedDoubleArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setFloat$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, float val) {
		if (obj instanceof MultiDTaintedFloatArray) {
			MultiDTaintedFloatArray a = (MultiDTaintedFloatArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setInt$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, int val) {
		if (obj instanceof MultiDTaintedIntArray) {
			MultiDTaintedIntArray a = (MultiDTaintedIntArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setLong$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, long val) {
		if (obj instanceof MultiDTaintedLongArray) {
			MultiDTaintedLongArray a = (MultiDTaintedLongArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}

	public static void setShort$$PHOSPHORTAGGED(Object obj, int idxtaint, int idx, int taint, short val) {
		if (obj instanceof MultiDTaintedShortArray) {
			MultiDTaintedShortArray a = (MultiDTaintedShortArray) obj;
			a.val[idx] = val;
			a.taint[idx] = taint;
		}
		else
		throw new ArrayStoreException("Called setX, but don't have tainted X array!");
	}
}
