package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.WeakHashMap;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
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

public class RuntimeReflectionPropogator {

	public static void setUNINST(Field f, Object obj, Object val)
		throws IllegalArgumentException, IllegalAccessException {
		if (MultiDTaintedArray.class.isAssignableFrom(f.getType())) {
			throw new UnsupportedOperationException();
		}
		f.setAccessible(true);
		f.set(obj, val);
	}

	public static Object getUNINST(Field f, Object obj)
		throws IllegalArgumentException, IllegalAccessException {
		if (MultiDTaintedArray.class.isAssignableFrom(f.getType())) {
			throw new UnsupportedOperationException();
		}
		f.setAccessible(true);
		return f.get(obj);
	}

	public static Class<?> getType$$PHOSPHORTAGGED(Field f,ControlTaintTagStack ctrl) {
		return getType(f);
	}

	public static Class<?> getType(Field f) {
		Class<?> ret = f.getType();
		Class<?> component = ret;
		while(component.isArray())
			component = component.getComponentType();
		if (MultiDTaintedArrayWithIntTag.class.isAssignableFrom(component)) {
			Type t = Type.getType(ret);
			String newType = "[";
			for(int i = 0; i < t.getDimensions(); i++)
				newType += "[";
			newType += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(component);
			try {
				ret = Class.forName(newType);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else if(MultiDTaintedArrayWithObjTag.class.isAssignableFrom(component)) {
			Type t = Type.getType(ret);
			String newType = "[";
			for(int i = 0; i < t.getDimensions(); i++)
				newType += "[";
			newType += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(component);
			try {
				ret = Class.forName(newType);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public static Object get$$PHOSPHORTAGGED(Field f, Object obj,
			ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		return get(f, obj, true);
	}

	public static Object get(Field f, Object obj, boolean isObjTags)
		throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		Object ret;
		if (f.getType().isPrimitive()) {
			if (f.getType() == Boolean.TYPE)
				if (isObjTags)
					ret = getBoolean$$PHOSPHORTAGGED(f, obj, new TaintedBooleanWithObjTag());
				else
					ret = getBoolean$$PHOSPHORTAGGED(f, obj, new TaintedBooleanWithIntTag());
			else if (f.getType() == Byte.TYPE)
				if (isObjTags)
					ret = getByte$$PHOSPHORTAGGED(f, obj, new TaintedByteWithObjTag());
				else
					ret = getByte$$PHOSPHORTAGGED(f, obj, new TaintedByteWithIntTag());
			else if (f.getType() == Character.TYPE)
				if (isObjTags)
					ret = getChar$$PHOSPHORTAGGED(f, obj, new TaintedCharWithObjTag());
				else
					ret = getChar$$PHOSPHORTAGGED(f, obj, new TaintedCharWithIntTag());
			else if (f.getType() == Double.TYPE)
				if (isObjTags)
					ret = getDouble$$PHOSPHORTAGGED(f, obj, new TaintedDoubleWithObjTag());
				else
					ret = getDouble$$PHOSPHORTAGGED(f, obj, new TaintedDoubleWithIntTag());
			else if (f.getType() == Float.TYPE)
				if (isObjTags)
					ret = getFloat$$PHOSPHORTAGGED(f, obj, new TaintedFloatWithObjTag());
				else
					ret = getFloat$$PHOSPHORTAGGED(f, obj, new TaintedFloatWithIntTag());
			else if (f.getType() == Long.TYPE)
				if (isObjTags)
					ret = getLong$$PHOSPHORTAGGED(f, obj, new TaintedLongWithObjTag());
				else
					ret = getLong$$PHOSPHORTAGGED(f, obj, new TaintedLongWithIntTag());
			else if (f.getType() == Integer.TYPE)
				if (isObjTags)
					ret = getInt$$PHOSPHORTAGGED(f, obj, new TaintedIntWithObjTag());
				else
					ret = getInt$$PHOSPHORTAGGED(f, obj, new TaintedIntWithIntTag());
			else if (f.getType() == Short.TYPE)
				if (isObjTags)
					ret = getShort$$PHOSPHORTAGGED(f, obj, new TaintedShortWithObjTag());
				else
					ret = getShort$$PHOSPHORTAGGED(f, obj, new TaintedShortWithIntTag());
			else
				throw new IllegalArgumentException();
		} else 
			ret = f.get(obj);

		if (f.getType().isArray() && f.getType().getComponentType().isPrimitive()) {
			Object taint = null;

			try {
				try {
					if (ret instanceof MultiDTaintedArrayWithIntTag) {
						return ret;
					} else if (ret instanceof MultiDTaintedArrayWithObjTag) {
						return ret;
					} else {
						Field taintField;
						if (fieldToField.containsKey(f))
							taintField = fieldToField.get(f);
						else {
							taintField = f.getDeclaringClass()
											.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
							taintField.setAccessible(true);
							fieldToField.put(f, taintField);
						}
						taint = taintField.get(obj);
					}
				} catch (NoSuchFieldException t) {
					if (isObjTags) {
						if (ret != null)
							taint = new int[Array.getLength(ret)];
					} else {
						if (ret != null)
							taint = new Taint[Array.getLength(ret)];
					}
				}
				if (ret == null)
					return null;
				if (!isObjTags) {
					if (f.getType().getComponentType() == Boolean.TYPE) {
						return new MultiDTaintedBooleanArrayWithIntTag((int[]) taint, (boolean[]) ret);
					} else if (f.getType().getComponentType() == Byte.TYPE) {
						return new MultiDTaintedByteArrayWithIntTag((int[]) taint, (byte[]) ret);
		 			} else if (f.getType().getComponentType() == Character.TYPE) {
						return new MultiDTaintedCharArrayWithIntTag((int[]) taint, (char[]) ret);
					} else if (f.getType().getComponentType() == Double.TYPE) {
						return new MultiDTaintedDoubleArrayWithIntTag((int[]) taint, (double[]) ret);
					} else if (f.getType().getComponentType() == Float.TYPE) {
						return new MultiDTaintedFloatArrayWithIntTag((int[]) taint, (float[]) ret);
					} else if (f.getType().getComponentType() == Integer.TYPE) {
						return new MultiDTaintedIntArrayWithIntTag((int[]) taint, (int[]) ret);
					} else if (f.getType().getComponentType() == Long.TYPE) {
						return new MultiDTaintedLongArrayWithIntTag((int[]) taint, (long[]) ret);
					} else if (f.getType().getComponentType() == Short.TYPE) {
						return new MultiDTaintedShortArrayWithIntTag((int[]) taint, (short[]) ret);
					}
				} else {
					if (f.getType().getComponentType() == Boolean.TYPE) {
						return new MultiDTaintedBooleanArrayWithObjTag((Object[]) taint, (boolean[]) ret);
					} else if (f.getType().getComponentType() == Byte.TYPE) {
						return new MultiDTaintedByteArrayWithObjTag((Object[]) taint, (byte[]) ret);
					} else if (f.getType().getComponentType() == Character.TYPE) {
						return new MultiDTaintedCharArrayWithObjTag((Object[]) taint, (char[]) ret);
					} else if (f.getType().getComponentType() == Double.TYPE) {
						return new MultiDTaintedDoubleArrayWithObjTag((Object[]) taint, (double[]) ret);
					} else if (f.getType().getComponentType() == Float.TYPE) {
						return new MultiDTaintedFloatArrayWithObjTag((Object[]) taint, (float[]) ret);
					} else if (f.getType().getComponentType() == Integer.TYPE) {
						return new MultiDTaintedIntArrayWithObjTag((Object[]) taint, (int[]) ret);
					} else if (f.getType().getComponentType() == Long.TYPE) {
						return new MultiDTaintedLongArrayWithObjTag((Object[]) taint, (long[]) ret);
					} else if (f.getType().getComponentType() == Short.TYPE) {
						return new MultiDTaintedShortArrayWithObjTag((Object[]) taint, (short[]) ret);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (ret instanceof TaintedPrimitiveWithIntTag) {
			return ((TaintedPrimitiveWithIntTag) ret).toPrimitiveType();
		}

		if (ret instanceof TaintedPrimitiveWithObjTag) {
			return ((TaintedPrimitiveWithObjTag) ret).toPrimitiveType();
		}
		return ret;
	}

	public static TaintedBooleanWithIntTag getBoolean$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedBooleanWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getBoolean$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedByteWithIntTag getByte$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedByteWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getByte$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedCharWithIntTag getChar$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedCharWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getChar$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedDoubleWithIntTag getDouble$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedDoubleWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getDouble$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedFloatWithIntTag getFloat$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedFloatWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getFloat$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedIntWithIntTag getInt$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedIntWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getInt$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedLongWithIntTag getLong$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedLongWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getLong$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedShortWithIntTag getShort$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedShortWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getShort$$PHOSPHORTAGGED(f, obj, ret);
	}
	
	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getBoolean$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedByteWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getByte$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedCharWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getChar$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedDoubleWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getDouble$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedFloatWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getFloat$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedIntWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getInt$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedLongWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getLong$$PHOSPHORTAGGED(f, obj, ret);
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Field f,
			Object obj,ControlTaintTagStack ctrl, TaintedShortWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		return getShort$$PHOSPHORTAGGED(f, obj, ret);
	}
	
	public static TaintedBooleanWithIntTag getBoolean$$PHOSPHORTAGGED(Field f,
			Object obj,TaintedBooleanWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedByteWithIntTag getByte$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedByteWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedCharWithIntTag getChar$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedCharWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedDoubleWithIntTag getDouble$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedDoubleWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getDouble(obj);
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedFloatWithIntTag getFloat$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedFloatWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	static WeakHashMap<Field, Field> fieldToField = new WeakHashMap<Field, Field>();

	public static TaintedIntWithIntTag getInt$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedIntWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getInt(obj);
		try {
			//			Class c = f.getDeclaringClass();
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.getInt(obj);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			System.err.println("Couldn't find taint field: " + f.getDeclaringClass()
					+ ", taint field for: " + f.getName()+TaintUtils.TAINT_FIELD);
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedLongWithIntTag getLong$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedLongWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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

	public static TaintedShortWithIntTag getShort$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedShortWithIntTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Field f,
			Object obj,TaintedBooleanWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedByteWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedCharWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedDoubleWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		ret.val = f.getDouble(obj);
		try {
			Field taintField;
			if (fieldToField.containsKey(f))
				taintField = fieldToField.get(f);
			else {
				taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
				taintField.setAccessible(true);
				fieldToField.put(f, taintField);
			}
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedFloatWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedIntWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		}
		return ret;
	}

	public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedLongWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		}
		return ret;
	}

	public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Field f,
			Object obj, TaintedShortWithObjTag ret)
		throws IllegalArgumentException, IllegalAccessException {
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
			ret.taint = taintField.get(obj);
			if (ret.taint instanceof Integer)
				ret.taint = HardcodedBypassStore.get(((Integer) ret.taint).intValue());
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static void setAccessible$$PHOSPHORTAGGED(Field f, int tag, boolean flag) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass()
					.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}
	
	public static void setAccessible$$PHOSPHORTAGGED(Field f, int tag, boolean flag,
			ControlTaintTagStack ctrl) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass()
					.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}
	
	public static void setAccessible$$PHOSPHORTAGGED(Field f, Taint tag, boolean flag) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass()
					.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}
	
	public static void setAccessible$$PHOSPHORTAGGED(Field f, Taint tag, boolean flag,
			ControlTaintTagStack ctrl) {
		f.setAccessible(flag);
		if (isPrimitiveOrPrimitiveArrayType(f.getType())) {
			try {
				f.getDeclaringClass()
					.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
		}
	}

	public static void setBoolean$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			boolean val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setBoolean$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setByte$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			byte val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setByte$$PHOSPHORTAGGED(f, obj, tag, val);
	}

	public static void setChar$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			char val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setChar$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setDouble$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			double val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setDouble$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setFloat$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			float val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setFloat$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setInt$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			int val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setInt$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setLong$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			long val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setLong$$PHOSPHORTAGGED(f, obj, tag, val);
	}
	
	public static void setShort$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			short val, ControlTaintTagStack ctrl)
		throws IllegalArgumentException, IllegalAccessException {
		tag = Taint.combineTags(tag, ctrl);
		setShort$$PHOSPHORTAGGED(f, obj, tag, val);
	}

	public static void setBoolean$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			boolean val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setBoolean(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setByte$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			byte val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setByte(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setChar$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			char val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setChar(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setDouble$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			double val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setDouble(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setFloat$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			float val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setFloat(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setInt$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			int val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setInt(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setLong$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			long val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setLong(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setShort$$PHOSPHORTAGGED(Field f, Object obj, int tag,
			short val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setShort(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.setInt(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public static void setBoolean$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			boolean val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setBoolean(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setByte$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			byte val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setByte(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setChar$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			char val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setChar(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setDouble$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			double val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setDouble(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setFloat$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			float val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setFloat(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setInt$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			int val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setInt(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setLong$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			long val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setLong(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public static void setShort$$PHOSPHORTAGGED(Field f, Object obj, Taint tag,
			short val) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		f.setShort(obj, val);
		try {
			Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
			taintField.setAccessible(true);
			taintField.set(obj, tag);
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private static Taint getTagObj(Object val)
		throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Object ret = val.getClass().getField("value" + TaintUtils.TAINT_FIELD).get(val);
		if(ret instanceof Integer)
			ret = HardcodedBypassStore.get(((Integer) ret).intValue());
		return (Taint) ret;
	}

	private static int getTag(Object val)
		throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		return val.getClass().getField("value" + TaintUtils.TAINT_FIELD).getInt(val);
	}

	public static void set$$PHOSPHORTAGGED(Field f, Object obj,
			Object val, ControlTaintTagStack ctrl) throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().isPrimitive()) {
			if (val instanceof Integer && f.getType().equals(Integer.TYPE)) {
				Integer i = (Integer) val;
				f.setAccessible(true);
				f.setInt(obj, i.intValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
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
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Byte && f.getType().equals(Byte.TYPE)) {
				Byte i = (Byte) val;
				f.setAccessible(true);
				f.setByte(obj, i.byteValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Character && f.getType().equals(Character.TYPE)) {
				Character i = (Character) val;
				f.setAccessible(true);
				f.setChar(obj, i.charValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Double && f.getType().equals(Double.TYPE)) {
				Double i = (Double) val;
				f.setAccessible(true);
				f.setDouble(obj, i.doubleValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Float && f.getType().equals(Float.TYPE)) {
				Float i = (Float) val;
				f.setAccessible(true);
				f.setFloat(obj, i.floatValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Long && f.getType().equals(Long.TYPE)) {
				Long i = (Long) val;
				f.setAccessible(true);
				f.setLong(obj, i.longValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			} else if (val instanceof Short && f.getType().equals(Short.TYPE)) {
				Short i = (Short) val;
				f.setAccessible(true);
				f.setShort(obj, i.shortValue());
				try {
					f.getDeclaringClass()
						.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
						.set(obj, Taint.combineTags(getTagObj(val), ctrl));
				} catch (SecurityException e) {
					e.printStackTrace();
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		Taint.combineTagsOnObject(obj, ctrl);
		f.setAccessible(true);
		f.set(obj, val);
	}

	public static void set(Field f, Object obj, Object val, boolean isObjTags)
		throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().isPrimitive()) {
			if (val instanceof Integer && f.getType().equals(Integer.TYPE)) {
				Integer i = (Integer) val;
				f.setAccessible(true);
				f.setInt(obj, i.intValue());
				try {
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
					if (!isObjTags)
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.setInt(obj, getTag(val));
					else
						f.getDeclaringClass()
							.getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD)
							.set(obj, getTagObj(val));
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
