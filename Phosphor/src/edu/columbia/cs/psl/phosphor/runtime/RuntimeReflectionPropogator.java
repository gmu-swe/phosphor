package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedByte;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedChar;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedDouble;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloat;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedInt;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedLong;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitive;
import edu.columbia.cs.psl.phosphor.struct.TaintedShort;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class RuntimeReflectionPropogator {

	public static Class<?> getType$$INVIVO_PC(Field f,ControlTaintTagStack ctrl)
	{
		return getType(f);
	}
	public static Class<?> getType(Field f)
	{
		Class<?> ret = f.getType();
		Class<?> component = ret;
		while(component.isArray())
			component = component.getComponentType();
		if(MultiDTaintedArray.class.isAssignableFrom(component))
		{
			Type t = Type.getType(ret);
			String newType = "[";
			for(int i = 0; i < t.getDimensions(); i++)
				newType += "[";
			newType += MultiDTaintedArray.getPrimitiveTypeForWrapper(component);
			try {
				ret = Class.forName(newType);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}
	public static Object get$$INVIVO_PC(Field f, Object obj, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		return get(f, obj);
	}
	public static Object get(Field f, Object obj) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		Object ret = f.get(obj);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				Field taintField;
				if (fieldToField.containsKey(f))
					taintField = fieldToField.get(f);
				else {
					taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
					taintField.setAccessible(true);
					fieldToField.put(f, taintField);
				}
				Object taint = taintField.get(obj);
				if (f.getType().getComponentType() == Boolean.TYPE) {
					return new TaintedBooleanArray((int[]) taint, (boolean[]) ret);
				} else if (f.getType().getComponentType() == Byte.TYPE) {
					return new TaintedByteArray((int[]) taint, (byte[]) ret);
				} else if (f.getType().getComponentType() == Character.TYPE) {
					return new TaintedCharArray((int[]) taint, (char[]) ret);
				} else if (f.getType().getComponentType() == Double.TYPE) {
					return new TaintedDoubleArray((int[]) taint, (double[]) ret);
				} else if (f.getType().getComponentType() == Float.TYPE) {
					return new TaintedFloatArray((int[]) taint, (float[]) ret);
				} else if (f.getType().getComponentType() == Integer.TYPE) {
					return new TaintedIntArray((int[]) taint, (int[]) ret);
				} else if (f.getType().getComponentType() == Long.TYPE) {
					return new TaintedLongArray((int[]) taint, (long[]) ret);
				} else if (f.getType().getComponentType() == Short.TYPE) {
					return new TaintedShortArray((int[]) taint, (short[]) ret);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (ret instanceof TaintedPrimitive) {
			return ((TaintedPrimitive) ret).toPrimitiveType();
		}
		return ret;
	}
	public static TaintedBoolean getBoolean$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedBoolean ret) throws IllegalArgumentException, IllegalAccessException {
		return getBoolean$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedByte getByte$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedByte ret) throws IllegalArgumentException, IllegalAccessException {
		return getByte$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedChar getChar$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedChar ret) throws IllegalArgumentException, IllegalAccessException {
		return getChar$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedDouble getDouble$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedDouble ret) throws IllegalArgumentException, IllegalAccessException {
		return getDouble$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedFloat getFloat$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedFloat ret) throws IllegalArgumentException, IllegalAccessException {
		return getFloat$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedInt getInt$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedInt ret) throws IllegalArgumentException, IllegalAccessException {
		return getInt$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedLong getLong$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedLong ret) throws IllegalArgumentException, IllegalAccessException {
		return getLong$$INVIVO_PC(f, obj, ret);
	}
	public static TaintedShort getShort$$INVIVO_PC(Field f, Object obj,ControlTaintTagStack ctrl, TaintedShort ret) throws IllegalArgumentException, IllegalAccessException {
		return getShort$$INVIVO_PC(f, obj, ret);
	}
	
	public static TaintedBoolean getBoolean$$INVIVO_PC(Field f, Object obj,TaintedBoolean ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getBoolean(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedByte getByte$$INVIVO_PC(Field f, Object obj, TaintedByte ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getByte(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedChar getChar$$INVIVO_PC(Field f, Object obj, TaintedChar ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getChar(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedDouble getDouble$$INVIVO_PC(Field f, Object obj, TaintedDouble ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getInt(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedFloat getFloat$$INVIVO_PC(Field f, Object obj, TaintedFloat ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getFloat(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	static WeakHashMap<Field, Field> fieldToField = new WeakHashMap<Field, Field>();

	public static TaintedInt getInt$$INVIVO_PC(Field f, Object obj, TaintedInt ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getInt(obj);
		try {
			//			Class c = f.getDeclaringClass();
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedLong getLong$$INVIVO_PC(Field f, Object obj, TaintedLong ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getLong(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		}
		return ret;
	}

	public static TaintedShort getShort$$INVIVO_PC(Field f, Object obj, TaintedShort ret) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getShort(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static void setAccessible$$INVIVO_PC(Field f, int tag, boolean flag) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}
	
	public static void setAccessible$$INVIVO_PC(Field f, int tag, boolean flag,ControlTaintTagStack ctrl) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}

	public static void setBoolean$$INVIVO_PC(Field f, Object obj, int tag, boolean val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setBoolean$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setByte$$INVIVO_PC(Field f, Object obj, int tag, byte val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setByte$$INVIVO_PC(f, obj, tag, val);
	}

	public static void setChar$$INVIVO_PC(Field f, Object obj, int tag, char val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setChar$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setDouble$$INVIVO_PC(Field f, Object obj, int tag, double val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setDouble$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setFloat$$INVIVO_PC(Field f, Object obj, int tag, float val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setFloat$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setInt$$INVIVO_PC(Field f, Object obj, int tag, int val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setInt$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setLong$$INVIVO_PC(Field f, Object obj, int tag, long val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setLong$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setShort$$INVIVO_PC(Field f, Object obj, int tag, short val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		tag = SimpleMultiTaintHandler.combineTags(tag, ctrl);
		setShort$$INVIVO_PC(f, obj, tag, val);
	}
	
	public static void setBoolean$$INVIVO_PC(Field f, Object obj, int tag, boolean val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setBoolean(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setByte$$INVIVO_PC(Field f, Object obj, int tag, byte val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setByte(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setChar$$INVIVO_PC(Field f, Object obj, int tag, char val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setChar(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setDouble$$INVIVO_PC(Field f, Object obj, int tag, double val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setDouble(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setFloat$$INVIVO_PC(Field f, Object obj, int tag, float val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setFloat(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setInt$$INVIVO_PC(Field f, Object obj, int tag, int val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setInt(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setLong$$INVIVO_PC(Field f, Object obj, int tag, long val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setLong(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setShort$$INVIVO_PC(Field f, Object obj, int tag, short val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setShort(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private static int getTag(Object val) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		return val.getClass().getField("value" + TaintUtils.TAINT_FIELD).getInt(val);
	}
	public static void set$$INVIVO_PC(Field f, Object obj, Object val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().isPrimitive()) {
			if (val instanceof Integer && f.getType().equals(Integer.TYPE)) {
				Integer i = (Integer) val;
				f.setAccessible(true);
				f.setInt(obj, i.intValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Boolean && f.getType().equals(Boolean.TYPE)) {
				Boolean i = (Boolean) val;
				f.setAccessible(true);
				f.setBoolean(obj, i.booleanValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Byte && f.getType().equals(Byte.TYPE)) {
				Byte i = (Byte) val;
				f.setAccessible(true);
				f.setByte(obj, i.byteValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Character && f.getType().equals(Character.TYPE)) {
				Character i = (Character) val;
				f.setAccessible(true);
				f.setChar(obj, i.charValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Double && f.getType().equals(Double.TYPE)) {
				Double i = (Double) val;
				f.setAccessible(true);
				f.setDouble(obj, i.doubleValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Float && f.getType().equals(Float.TYPE)) {
				Float i = (Float) val;
				f.setAccessible(true);
				f.setFloat(obj, i.floatValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Long && f.getType().equals(Long.TYPE)) {
				Long i = (Long) val;
				f.setAccessible(true);
				f.setLong(obj, i.longValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Short && f.getType().equals(Short.TYPE)) {
				Short i = (Short) val;
				f.setAccessible(true);
				f.setShort(obj, i.shortValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, SimpleMultiTaintHandler.combineTags(getTag(val), ctrl));				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		SimpleMultiTaintHandler.combineTags(obj, ctrl);
		f.setAccessible(true);
		f.set(obj, val);
	}
	public static void set(Field f, Object obj, Object val) throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().isPrimitive()) {
			if (val instanceof Integer && f.getType().equals(Integer.TYPE)) {
				Integer i = (Integer) val;
				f.setAccessible(true);
				f.setInt(obj, i.intValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Boolean && f.getType().equals(Boolean.TYPE)) {
				Boolean i = (Boolean) val;
				f.setAccessible(true);
				f.setBoolean(obj, i.booleanValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Byte && f.getType().equals(Byte.TYPE)) {
				Byte i = (Byte) val;
				f.setAccessible(true);
				f.setByte(obj, i.byteValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Character && f.getType().equals(Character.TYPE)) {
				Character i = (Character) val;
				f.setAccessible(true);
				f.setChar(obj, i.charValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Double && f.getType().equals(Double.TYPE)) {
				Double i = (Double) val;
				f.setAccessible(true);
				f.setDouble(obj, i.doubleValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Float && f.getType().equals(Float.TYPE)) {
				Float i = (Float) val;
				f.setAccessible(true);
				f.setFloat(obj, i.floatValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Long && f.getType().equals(Long.TYPE)) {
				Long i = (Long) val;
				f.setAccessible(true);
				f.setLong(obj, i.longValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Short && f.getType().equals(Short.TYPE)) {
				Short i = (Short) val;
				f.setAccessible(true);
				f.setShort(obj, i.shortValue());
				try {
					f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setInt(obj, getTag(val));
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		f.setAccessible(true);
		f.set(obj, val);
		//		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
		//			try {
		//				f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).set(obj, ArrayObjectStore.arrayStore.get(val));
		//			} catch (NoSuchFieldException e) {
		//				e.printStackTrace();
		//			} catch (SecurityException e) {
		//				e.printStackTrace();
		//			}
		//		}
	}

	static int getNumberOfDimensions(Class<?> clazz) {
		String name = clazz.getName();
		return name.lastIndexOf('[') + 1;
	}

	static boolean isPrimitiveOrPrimitiveArrayType(Class<?> clazz) {
		if (clazz.isPrimitive())
			return true;
		if (clazz.isArray() && getNumberOfDimensions(clazz) == 1)
			return isPrimitiveOrPrimitiveArrayType(clazz.getComponentType());
		return false;
	}
}
