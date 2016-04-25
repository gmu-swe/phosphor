package edu.columbia.cs.psl.phosphor.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.WeakHashMap;

import sun.reflect.ReflectionFactory;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.MethodDescriptor;
import edu.columbia.cs.psl.phosphor.SelectiveInstrumentationManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.struct.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.MethodInvoke;
import edu.columbia.cs.psl.phosphor.struct.Tainted;
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
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedPrimitiveWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class ReflectionMasker {

	static {
		System.setSecurityManager(null);
	}
	//	static WeakHashMap<Method, Method> methodCache = new WeakHashMap<Method, Method>();

	static WeakHashMap<Class, Class> cachedClasses = new WeakHashMap<Class, Class>();

	static final int SUFFIX_LEN = "$$PHOSPHORTAGGED".length();
	static final int GETSETLEN = "setPHOSPHOR_TAG".length();
	static final char[] GETSETCHARS = "setPHOSPHOR_TAG".toCharArray();
	static final char[] SUFFIXCHARS = "$$PHOSPHORTAGGED".toCharArray();
	static final char[] SUFFIX2CHARS = "$$PHOSPHORUNTAGGED".toCharArray();

	static final char[] FIELDSUFFIXCHARS = TaintUtils.TAINT_FIELD.toCharArray();
	static final int FIELDSUFFIXLEN = FIELDSUFFIXCHARS.length;

	public static final boolean IS_KAFFE = false;

	private static boolean isSelectiveInstManagerInit = false;

	private static ReflectionFactory reflectionFactory;


	public static Method getTaintMethodControlTrack(Method m) {
		if (m.getDeclaringClass().isAnnotation())
			return m;

		final char[] chars = m.getName().toCharArray();
		if (chars.length > SUFFIX_LEN) {
			boolean isEq = true;
			int x = 0;
			for (int i = chars.length - SUFFIX_LEN; i < chars.length; i++) {
				if (chars[i] != SUFFIXCHARS[x]) {
					isEq = false;
				}
				x++;
			}

			if (isEq) {
				if (!IS_KAFFE)
					m.PHOSPHOR_TAGmarked = true;
				return m;
			}

			x = 0;
			if (Configuration.GENERATE_UNINST_STUBS && chars.length > SUFFIX_LEN + 2)
				for (int i = chars.length - SUFFIX_LEN - 2; i < chars.length; i++) {
					if (chars[i] != SUFFIX2CHARS[x]) {
						isEq = false;
					}
					x++;
				}

			if (isEq) {
				if (!IS_KAFFE)
					m.PHOSPHOR_TAGmarked = true;
				return m;
			}
		}

		if(Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
			return m;
		}

		ArrayList<Class> newArgs = new ArrayList<Class>();
		boolean madeChange = false;
		for (final Class c : m.getParameterTypes()) {
			if (c.isArray()) {
				if (c.getComponentType().isPrimitive()) {
					// 1d primitive array
					madeChange = true;
					newArgs.add(Configuration.TAINT_TAG_OBJ_ARRAY_CLASS);
					newArgs.add(c);
					continue;
				} else {
					Class elementType = c.getComponentType();
					while (elementType.isArray())
						elementType = c.getComponentType();
					if (elementType.isPrimitive()) {
						// 2d primtiive array
						madeChange = true;
						try {
							newArgs.add(Class.forName(
										MultiDTaintedArrayWithObjTag
										.getTypeForType(Type.getType(c)).getInternalName()));
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						continue;
					} else {
						// reference array
						newArgs.add(c);
						continue;
					}
				}
			} else if (c.isPrimitive()) {
				madeChange = true;
				newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
				newArgs.add(c);
				continue;
			} else {
				// anything else
				newArgs.add(c);
				continue;
			}
		}

		madeChange = true;
		newArgs.add(ControlTaintTagStack.class);
		final Class returnType = m.getReturnType();

		if (returnType.isArray()) {
			if (returnType.getComponentType().isPrimitive()) {
				Class comp = returnType.getComponentType();
				if (comp == Integer.TYPE)
					newArgs.add(TaintedIntArrayWithObjTag.class);
				else if (comp == Short.TYPE)
					newArgs.add(TaintedShortArrayWithObjTag.class);
				else if (comp == Float.TYPE)
					newArgs.add(TaintedFloatArrayWithObjTag.class);
				else if (comp == Double.TYPE)
					newArgs.add(TaintedDoubleArrayWithObjTag.class);
				else if (comp == Long.TYPE)
					newArgs.add(TaintedLongArrayWithObjTag.class);
				else if (comp == Character.TYPE)
					newArgs.add(TaintedCharArrayWithObjTag.class);
				else if (comp == Byte.TYPE)
					newArgs.add(TaintedByteArrayWithObjTag.class);
				else if (comp == Boolean.TYPE)
					newArgs.add(TaintedBooleanArrayWithObjTag.class);
				madeChange = true;
			}
		} else if (returnType.isPrimitive() && returnType != Void.TYPE) {
			if (returnType == Integer.TYPE)
				newArgs.add(TaintedIntWithObjTag.class);
			else if (returnType == Short.TYPE)
				newArgs.add(TaintedShortWithObjTag.class);
			else if (returnType == Float.TYPE)
				newArgs.add(TaintedFloatWithObjTag.class);
			else if (returnType == Double.TYPE)
				newArgs.add(TaintedDoubleWithObjTag.class);
			else if (returnType == Long.TYPE)
				newArgs.add(TaintedLongWithObjTag.class);
			else if (returnType == Character.TYPE)
				newArgs.add(TaintedCharWithObjTag.class);
			else if (returnType == Byte.TYPE)
				newArgs.add(TaintedByteWithObjTag.class);
			else if (returnType == Boolean.TYPE)
				newArgs.add(TaintedBooleanWithObjTag.class);
			madeChange = true;
		}

		if (madeChange) {
			Class[] args = new Class[newArgs.size()];
			newArgs.toArray(args);
			Method ret = null;
			try {
				ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORTAGGED", args);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			return ret;
		} else {
			return m;
		}
	}

	@SuppressWarnings("rawtypes")
	public static Method getTaintMethod(Method m, boolean isObjTags) {
		//		if (m.INVIVO_PC_TAINTmarked)
		//			return m;
		if (m.getDeclaringClass().isAnnotation())
			return m;

		final char[] chars = m.getName().toCharArray();
		if (chars.length > SUFFIX_LEN) {
			boolean isEq = true;
			int x = 0;
			for (int i = chars.length - SUFFIX_LEN; i < chars.length; i++) {
				if (chars[i] != SUFFIXCHARS[x]) {
					isEq = false;
				}
				x++;
			}
			if (isEq) {
				if (!IS_KAFFE)
					m.PHOSPHOR_TAGmarked = true;
				return m;
			}

			x = 0;
			if (Configuration.GENERATE_UNINST_STUBS && chars.length > SUFFIX_LEN + 2)
				for (int i = chars.length - SUFFIX_LEN - 2; i < chars.length; i++) {
					if (chars[i] != SUFFIX2CHARS[x]) {
						isEq = false;
					}
					x++;
				}
			if (isEq) {
				if (!IS_KAFFE)
					m.PHOSPHOR_TAGmarked = true;
				return m;
			}
		}

		ArrayList<Class> newArgs = new ArrayList<Class>();
		boolean madeChange = false;
		for (final Class c : m.getParameterTypes()) {
			if (c.isArray()) {
				if (c.getComponentType().isPrimitive()) {
					// 1d primitive array
					madeChange = true;
					if (isObjTags)
						newArgs.add(Configuration.TAINT_TAG_OBJ_ARRAY_CLASS);
					else
						newArgs.add(int[].class);
					newArgs.add(c);
					continue;
				} else {
					Class elementType = c.getComponentType();
					while (elementType.isArray())
						elementType = elementType.getComponentType();
					if (elementType.isPrimitive()) {
						//2d primtiive array
						madeChange = true;
						try {
							if(isObjTags)
								newArgs.add(Class.forName(
											MultiDTaintedArrayWithObjTag
											.getTypeForType(Type.getType(c)).getInternalName()));
							else
								newArgs.add(Class.forName(
											MultiDTaintedArrayWithIntTag
											.getTypeForType(Type.getType(c)).getInternalName()));
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						continue;
					} else {
						// reference array
						newArgs.add(c);
						continue;
					}
				}
			} else if (c.isPrimitive()) {
				madeChange = true;
				if (isObjTags)
					newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
				else
					newArgs.add(Integer.TYPE);
				newArgs.add(c);
				continue;
			} else {
				// anything else
				newArgs.add(c);
				continue;
			}
		}

		final Class returnType = m.getReturnType();
		if (!isObjTags) {
			if (returnType.isArray()) {
				if (returnType.getComponentType().isPrimitive()) {
					Class comp = returnType.getComponentType();
					if (comp == Integer.TYPE)
						newArgs.add(TaintedIntArrayWithIntTag.class);
					else if (comp == Short.TYPE)
						newArgs.add(TaintedShortArrayWithIntTag.class);
					else if (comp == Float.TYPE)
						newArgs.add(TaintedFloatArrayWithIntTag.class);
					else if (comp == Double.TYPE)
						newArgs.add(TaintedDoubleArrayWithIntTag.class);
					else if (comp == Long.TYPE)
						newArgs.add(TaintedLongArrayWithIntTag.class);
					else if (comp == Character.TYPE)
						newArgs.add(TaintedCharArrayWithIntTag.class);
					else if (comp == Byte.TYPE)
						newArgs.add(TaintedByteArrayWithIntTag.class);
					else if (comp == Boolean.TYPE)
						newArgs.add(TaintedBooleanArrayWithIntTag.class);
					madeChange = true;
				}
			} else if (returnType.isPrimitive() && returnType != Void.TYPE) {
				if (returnType == Integer.TYPE)
					newArgs.add(TaintedIntWithIntTag.class);
				else if (returnType == Short.TYPE)
					newArgs.add(TaintedShortWithIntTag.class);
				else if (returnType == Float.TYPE)
					newArgs.add(TaintedFloatWithIntTag.class);
				else if (returnType == Double.TYPE)
					newArgs.add(TaintedDoubleWithIntTag.class);
				else if (returnType == Long.TYPE)
					newArgs.add(TaintedLongWithIntTag.class);
				else if (returnType == Character.TYPE)
					newArgs.add(TaintedCharWithIntTag.class);
				else if (returnType == Byte.TYPE)
					newArgs.add(TaintedByteWithIntTag.class);
				else if (returnType == Boolean.TYPE)
					newArgs.add(TaintedBooleanWithIntTag.class);
				madeChange = true;
			}
		} else {
			if (returnType.isArray()) {
				if (returnType.getComponentType().isPrimitive()) {
					Class comp = returnType.getComponentType();
					if (comp == Integer.TYPE)
						newArgs.add(TaintedIntArrayWithObjTag.class);
					else if (comp == Short.TYPE)
						newArgs.add(TaintedShortArrayWithObjTag.class);
					else if (comp == Float.TYPE)
						newArgs.add(TaintedFloatArrayWithObjTag.class);
					else if (comp == Double.TYPE)
						newArgs.add(TaintedDoubleArrayWithObjTag.class);
					else if (comp == Long.TYPE)
						newArgs.add(TaintedLongArrayWithObjTag.class);
					else if (comp == Character.TYPE)
						newArgs.add(TaintedCharArrayWithObjTag.class);
					else if (comp == Byte.TYPE)
						newArgs.add(TaintedByteArrayWithObjTag.class);
					else if (comp == Boolean.TYPE)
						newArgs.add(TaintedBooleanArrayWithObjTag.class);
					madeChange = true;
				}
			} else if (returnType.isPrimitive() && returnType != Void.TYPE) {
				if (returnType == Integer.TYPE)
					newArgs.add(TaintedIntWithObjTag.class);
				else if (returnType == Short.TYPE)
					newArgs.add(TaintedShortWithObjTag.class);
				else if (returnType == Float.TYPE)
					newArgs.add(TaintedFloatWithObjTag.class);
				else if (returnType == Double.TYPE)
					newArgs.add(TaintedDoubleWithObjTag.class);
				else if (returnType == Long.TYPE)
					newArgs.add(TaintedLongWithObjTag.class);
				else if (returnType == Character.TYPE)
					newArgs.add(TaintedCharWithObjTag.class);
				else if (returnType == Byte.TYPE)
					newArgs.add(TaintedByteWithObjTag.class);
				else if (returnType == Boolean.TYPE)
					newArgs.add(TaintedBooleanWithObjTag.class);
				madeChange = true;
			}
		}

		if (madeChange) {
			Class[] args = new Class[newArgs.size()];
			newArgs.toArray(args);
			Method ret = null;
			try {
				ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORTAGGED", args);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			return ret;
		} else {
			return m;
		}
	}

	@SuppressWarnings("rawtypes")
	public static Method getUnTaintMethod(Method m, boolean isObjTags) {
		//		if (m.INVIVO_PC_TAINTmarked)
		//			return m;
		if (m.getDeclaringClass().isAnnotation())
			return m;

		final char[] chars = m.getName().toCharArray();
		if (chars.length > SUFFIX_LEN + 2) {
			boolean isEq = true;
			int x = 0;
			for (int i = chars.length - SUFFIX_LEN -2; i < chars.length; i++) {
				if (chars[i] != SUFFIX2CHARS[x]) {
					isEq = false;
				}
				x++;
			}
			if (isEq) {
				return m;
			}
		}

		ArrayList<Class> newArgs = new ArrayList<Class>();
		boolean madeChange = false;
		for (final Class c : m.getParameterTypes()) {
			if (c.isArray()) {
				if (c.getComponentType().isPrimitive()) {
					newArgs.add(c);
					continue;
				} else {
					Class elementType = c.getComponentType();
					String s = "";
					while (elementType.isArray())
						elementType = elementType.getComponentType();
					if (elementType.isPrimitive()) {
						//2d primtiive array
						madeChange = true;
						if (isObjTags)
							newArgs.add(
									MultiDTaintedArrayWithObjTag
									.getUnderlyingBoxClassForUnderlyingClass(c));
						else
							newArgs.add(
									MultiDTaintedArrayWithIntTag
									.getUnderlyingBoxClassForUnderlyingClass(c));
						continue;
					} else {
						// reference array
						newArgs.add(c);
						continue;
					}
				}
			} else if (c.isPrimitive()) {
				newArgs.add(c);
				continue;
			} else {
				// anything else
				newArgs.add(c);
				continue;
			}
		}		

		if (madeChange) {
			Class[] args = new Class[newArgs.size()];
			newArgs.toArray(args);
			Method ret = null;
			try {
				ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORUNTAGGED", args);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			return ret;
		} else {
			return m;
		}
	}

	@SuppressWarnings("rawtypes")
	public static Method getOrigMethod(Method m, boolean isObjTracking) {
		//		System.out.println("Get orig method: " + m);
		//		new Exception().printStackTrace();
		if (Configuration.GENERATE_UNINST_STUBS
				&& m.getName().endsWith("$$PHOSPHORUNTAGGED")) {
			String origName = m.getName().replace("$$PHOSPHORUNTAGGED", "");
			try {
				return m.getDeclaringClass()
						.getDeclaredMethod(origName, m.getParameterTypes());
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		} else if (m.getName().endsWith("$$PHOSPHORTAGGED")) {
			String origName = m.getName().replace("$$PHOSPHORTAGGED", "");
			ArrayList<Class> origArgs = new ArrayList<Class>();
			boolean dontIgnorePrimitive = false;

			for (Class c : m.getParameterTypes()) {
				Type t = Type.getType(c);
				if (c.getName().startsWith("edu.columbia.cs.psl.phosphor.struct.multid")) {
					// Remove multid
					String s = "[";
					if(isObjTracking)
						s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
					else
						s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
					try {
						origArgs.add(Class.forName(s));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				} else if (t.getSort() == Type.ARRAY
						&& t.getElementType().getSort() == Type.OBJECT
						&& t.getElementType().getInternalName()
								.startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
					// Remove multid
					String s = "";
					for (int i = 0; i < t.getDimensions(); i++)
						s += "[";
					s += "[";
					if(isObjTracking)
						s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
					else
						s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
					try {
						origArgs.add(Class.forName(s));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				} else if (c.isArray()
						&& (c.getComponentType().isPrimitive()
							|| c.getComponentType().equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
					if (dontIgnorePrimitive)
						origArgs.add(c);
					dontIgnorePrimitive = !dontIgnorePrimitive;
				} else if (c.isPrimitive()
						|| c.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
					if (dontIgnorePrimitive)
						origArgs.add(c);
					dontIgnorePrimitive = !dontIgnorePrimitive;
				} else {
					if (!TaintedPrimitiveWithIntTag.class.isAssignableFrom(c)
							&& !TaintedPrimitiveArrayWithIntTag.class.isAssignableFrom(c)
							&& !TaintedPrimitiveWithObjTag.class.isAssignableFrom(c)
							&& !TaintedPrimitiveArrayWithObjTag.class.isAssignableFrom(c)
							&& !c.equals(ControlTaintTagStack.class))
						origArgs.add(c);
				}
			}

			Class[] args = new Class[origArgs.size()];
			origArgs.toArray(args);
			Method ret = null;
			try {
				ret = m.getDeclaringClass().getDeclaredMethod(origName, args);
			} catch (NoSuchMethodException e) {
				try {
					ret = m.getDeclaringClass().getMethod(origName, args);
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				} catch (SecurityException e1) {
					e1.printStackTrace();
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			return ret;
		}
		return m;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Constructor getOrigMethod(Constructor m, boolean isObjTags) {
		ArrayList<Class> origArgs = new ArrayList<Class>();
		boolean hasSentinel = false;
		boolean dontIgnorePrimitive = false;

		for (Class c : m.getParameterTypes()) {
			Type t = Type.getType(c);
			if (c.getName().startsWith("edu.columbia.cs.psl.phosphor.struct.multid")) {
				//Remove multid
				String s = "[";
				if (isObjTags) 
					s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
				else 
					s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
				try {
					origArgs.add(Class.forName(s));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (t.getSort() == Type.ARRAY
					&& t.getElementType().getSort() == Type.OBJECT
					&& t.getElementType().getInternalName()
							.startsWith("edu/columbia/cs/psl/phosphor/struct/multid")) {
				// Remove multid
				String s = "";
				for (int i = 0; i < t.getDimensions(); i++)
					s += "[";
				s += "[";
				if (isObjTags)
					s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
				else
					s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
				try {
					origArgs.add(Class.forName(s));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (c.isArray()
					&& (c.getComponentType().isPrimitive()
						|| c.getComponentType().equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
				if (dontIgnorePrimitive)
					origArgs.add(c);
				dontIgnorePrimitive = !dontIgnorePrimitive;
			} else if (c.isPrimitive()
					|| c.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
				if (dontIgnorePrimitive)
					origArgs.add(c);
				dontIgnorePrimitive = !dontIgnorePrimitive;
			} else if (c.equals(TaintSentinel.class)) {
				hasSentinel = true;
			} else if (!c.equals(ControlTaintTagStack.class))
				origArgs.add(c);
		}

		if (hasSentinel) {
			Class[] args = new Class[origArgs.size()];
			origArgs.toArray(args);
			try {
				return m.getDeclaringClass().getDeclaredConstructor(args);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		return m;
	}

	@SuppressWarnings("rawtypes")
	public static Class[] addTypeParams(Class[] params, boolean implicitTracking, boolean isObjTags) {
		boolean needsChange = false;
		if (params == null)
			return null;
		for (Class c : params) {
			if (c != null && (c.isPrimitive() || (c.isArray() && isPrimitiveArray(c))))
				needsChange = true;
		}

		if(implicitTracking)
			needsChange = true;
		if (needsChange) {
			ArrayList<Class> newParams = new ArrayList<Class>();
			for (Class c : params) {
				Type t = Type.getType(c);
				if (t.getSort() == Type.ARRAY) {
					if (t.getElementType().getSort() != Type.OBJECT) {
						if (t.getDimensions() == 1) {
							if (isObjTags)
								newParams.add(Configuration.TAINT_TAG_OBJ_ARRAY_CLASS);
							else
								newParams.add(int[].class);
						} else {
							Type newType = null;
							if(isObjTags)
								newType = MultiDTaintedArrayWithObjTag.getTypeForType(t);
							else
								newType = MultiDTaintedArrayWithIntTag.getTypeForType(t);
							try {
								newParams.add(Class.forName(newType.getInternalName().replace("/", ".")));
								continue;
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
					}
				} else if (t.getSort() != Type.OBJECT) {
					if(isObjTags)
						newParams.add(Configuration.TAINT_TAG_OBJ_CLASS);
					else
						newParams.add(Integer.TYPE);
				}
				newParams.add(c);
			}
			if(implicitTracking)
				newParams.add(ControlTaintTagStack.class);
			newParams.add(TaintSentinel.class);
			Class[] ret = new Class[newParams.size()];
			newParams.toArray(ret);
			return ret;
		}
		return params;
	}

	public static Method getDeclaredMethod(Class czz, String name, Class[] params,
			boolean isObjTags) throws NoSuchMethodException {
		return czz.getDeclaredMethod(name, params);
	}

	public static Method getMethod(Class czz, String name, Class[] params,
			boolean isObjTags) throws NoSuchMethodException {
		if (czz == Object.class)
			return czz.getMethod(name, params);
		if (czz.isAnnotation()) {
			return czz.getMethod(name, params);
		}

		if(Instrumenter.isIgnoredClass(czz.getName().replace('.', '/')))
			return czz.getMethod(name, params);
		try {
			Method m = czz.getMethod(name, params);
			m = getTaintMethod(m, isObjTags);
			return m;
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}

		System.err.println("Bailing just in case.");
		System.exit(-1);
		return null;
	}

	@SuppressWarnings("rawtypes")
	static boolean isPrimitiveArray(Class c) {
		if (c.isArray())
			return isPrimitiveArray(c.getComponentType());
		return c.isPrimitive();
	}

	@SuppressWarnings("rawtypes")
	public static Class[] removeTaintedInterface(Class[] in) {
		if (in == null)
			return null;
		boolean found = false;
		for (int i = 0; i < in.length; i++) {
			if (in[i].equals(TaintedWithIntTag.class)
					|| in[i].equals(TaintedWithObjTag.class))
				found = true;
		}

		if (!found)
			return in;
		Class[] ret = new Class[in.length - 1];
		int idx = 0;
		for (int i = 0; i < in.length; i++) {
			if (!in[i].equals(TaintedWithIntTag.class)
					&& !in[i].equals(TaintedWithObjTag.class)) {
				ret[idx] = in[i];
				idx++;
			}
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public static StackTraceElement[] removeExtraStackTraceElements(
			StackTraceElement[] in, Class clazz) {
		int depthToCut = 0;
		String toFind = clazz.getName();
		if (clazz == null || in == null)
			return in;

		for (int i = 0; i < in.length; i++) {
			if (in[i].getClassName().equals(toFind)
					&& !(i + 1 < in.length
						&& in[i + 1].getClassName().equals(toFind))) {
				depthToCut = i + 1;
				break;
			}
		}

		StackTraceElement[] ret = new StackTraceElement[in.length - depthToCut];
		System.arraycopy(in, depthToCut, ret, 0, ret.length);
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public static Object[] fixAllArgs(Object[] in, Constructor c, boolean isObjTags) {
		if (c != null
				&& in != null
				&& c.getParameterTypes().length != in.length) {
			Object[] ret = new Object[c.getParameterTypes().length];
			
			int j = 0;
			for (int i = 0; i < in.length; i++) {
				if (c.getParameterTypes()[j].isPrimitive()) {
					if (in[i] instanceof TaintedWithIntTag)
						ret[j] = ((TaintedWithIntTag) in[i]).getPHOSPHOR_TAG();
					else if(in[i] instanceof TaintedWithObjTag)
						ret[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret[j] = 0;
					j++;
				} else if (c.getParameterTypes()[j] == Configuration.TAINT_TAG_OBJ_CLASS){
					if(in[i] instanceof TaintedWithObjTag)
						ret[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret[j] = null;
					j++;
				} else if (c.getParameterTypes()[j].isArray()
						&& (c.getParameterTypes()[j].getComponentType().isPrimitive()
							|| c.getParameterTypes()[j].getComponentType()
									.equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
					if (!isObjTags) {
						MultiDTaintedArrayWithIntTag arr = ((MultiDTaintedArrayWithIntTag) in[i]);
						ret[j] = arr.taint;
						j++;
						ret[j] = arr.getVal();
						j++;
					} else {
						MultiDTaintedArrayWithObjTag arr = ((MultiDTaintedArrayWithObjTag) in[i]);
						ret[j] = arr.taint;
						j++;
						ret[j] = arr.getVal();
						j++;
					}
					continue;
				}
				ret[j] = in[i];
				j++;
			}
			return ret;
		} else if (in == null && c.getParameterTypes().length == 1) {
			Object[] ret = new Object[1];
			ret[0] = null;
			return ret;
		} else if (in == null && c.getParameterTypes().length == 2) {
			Object[] ret = new Object[2];
			ret[0] = new ControlTaintTagStack();
			ret[1] = null;
			return ret;
		}

		return in;
	}

	public static Object[] fixAllArgs(Object[] in, Constructor c, ControlTaintTagStack ctrl) {
		if (c != null
				&& in != null
				&& c.getParameterTypes().length != in.length) {
			Object[] ret = new Object[c.getParameterTypes().length];
			int j = 0;
			for (int i = 0; i < in.length; i++) {
				if (c.getParameterTypes()[j].isPrimitive()) {
					if (in[i] instanceof TaintedWithIntTag)
						ret[j] = ((TaintedWithIntTag) in[i]).getPHOSPHOR_TAG();
					else if(in[i] instanceof TaintedWithObjTag)
						ret[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret[j] = 0;
					j++;
				} else if (c.getParameterTypes()[j] == Configuration.TAINT_TAG_OBJ_CLASS){
					if(in[i] instanceof TaintedWithObjTag)
						ret[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret[j] = null;
					j++;
				} else if (c.getParameterTypes()[j].isArray()
						&& (c.getParameterTypes()[j].getComponentType().isPrimitive()
							|| c.getParameterTypes()[j].getComponentType()
									.equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
					MultiDTaintedArrayWithObjTag arr = ((MultiDTaintedArrayWithObjTag) in[i]);
					ret[j] = arr.taint;
					j++;
					ret[j] = arr.getVal();
					j++;
					continue;
				}
				ret[j] = in[i];
				j++;
			}
			ret[ret.length - 2] = ctrl;
			return ret;
		} else if (in == null && c.getParameterTypes().length == 1) {
			Object[] ret = new Object[1];
			ret[0] = null;
			return ret;
		} else if (in == null && c.getParameterTypes().length == 2) {
			Object[] ret = new Object[2];
			ret[0] = ctrl;
			ret[1] = null;
			return ret;
		}

		return in;
	}

	@SuppressWarnings("rawtypes")
	public static Object[] fixAllArgsFast(Method m, Object[] in, boolean isObjTags) {
		System.out.println("Making fast call to  " + m);
		Object[] ret = in;
		m.setAccessible(true);
		int j = 0;
		if (in != null && m.getParameterTypes().length != in.length) {
			ret = new Object[m.getParameterTypes().length];
			for (int i = 0; i < in.length; i++) {
				if (m.getParameterTypes()[j].isPrimitive()) {
					if (in[i] instanceof TaintedWithIntTag)
						ret[j] = ((TaintedWithIntTag) in[i]).getPHOSPHOR_TAG();
					else if(in[i] instanceof TaintedWithObjTag)
						ret[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret[j] = 0;
					j++;
				} else if (m.getParameterTypes()[j].isArray()
						&& m.getParameterTypes()[j].getComponentType().isPrimitive()) {
					if (!isObjTags) {
						MultiDTaintedArrayWithIntTag arr = ((MultiDTaintedArrayWithIntTag) in[i]);
						ret[j] = arr.taint;
						j++;
						ret[j] = arr.getVal();
						j++;
					} else {
						MultiDTaintedArrayWithObjTag arr = ((MultiDTaintedArrayWithObjTag) in[i]);
						ret[j] = arr.taint;
						j++;
						ret[j] = arr.getVal();
						j++;
					}
					continue;
				}
				ret[j] = in[i];
				j++;
			}
		}

		if ((in == null
					&& m.getParameterTypes().length == 1)
				|| (in != null
					&& j != in.length - 1)) {
			if (in == null)
				ret = new Object[1];
			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveArrayWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveArrayWithObjTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType))  {
				try {
					ret[j] = returnType.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	public static Object[] fixAllArgsUninst(Object[] in, Constructor c, boolean isObjTags) {
		Class[] params = c.getParameterTypes();
		if (params.length == 0)
			return in;
		if (params[params.length - 1] == UninstrumentedTaintSentinel.class) {
			Object[] ret = new Object[in.length + 1];
			System.arraycopy(in, 0, ret, 0, in.length);
			return ret;
		} else if (params[params.length - 1] == TaintSentinel.class) {
			return fixAllArgs(in, c, isObjTags);
		}
		return in;
	}

	public static Object[] fixAllArgsUninst(Object[] in, Constructor c, ControlTaintTagStack ctrl) {
		return in;
	}

	public static MethodInvoke fixAllArgsUninst(Method m, Object owner, Object[] in, boolean isObjTags) {
		MethodInvoke ret = new MethodInvoke();
		if (m == null) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}

		if (Configuration.WITH_SELECTIVE_INST) {
			ret.m = getOrigMethod(m, isObjTags);
			ret.o = owner;
			ret.a = in;
			if (ret.a != null
					&& ret.a.getClass().isArray()
					&& ret.a.getClass().getComponentType() == Object.class)
				for (int i = 0; i < ret.a.length; i++) {
					if(ret.a[i] instanceof MultiDTaintedArrayWithIntTag) {
						ret.a[i] = ((MultiDTaintedArrayWithIntTag)ret.a[i]).getVal();
					} else if(ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
						ret.a[i] = ((MultiDTaintedArrayWithObjTag)ret.a[i]).getVal();
					}
				}
		} else {
			ret = fixAllArgs(m, owner, in, isObjTags);
		}
		return ret;
	}

	public static MethodInvoke fixAllArgsUninst(Method m, Object owner,
			Object[] in, ControlTaintTagStack ctrl) {
		MethodInvoke ret = new MethodInvoke();
		if (m == null) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}

		ret.m = getOrigMethod(m, true);
		ret.o = owner;
		ret.a = in;
		for (int i = 0; i < ret.a.length; i++) {
			if (ret.a[i] instanceof MultiDTaintedArrayWithIntTag) {
				ret.a[i] = ((MultiDTaintedArrayWithIntTag)ret.a[i]).getVal();
			} else if (ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
				ret.a[i] = ((MultiDTaintedArrayWithObjTag)ret.a[i]).getVal();
			}
		}
		return ret;
	}

	public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, boolean isObjTags) {
		//		System.out.println("Making slow call to  " + m);
		MethodInvoke ret = new MethodInvoke();
		if (m == null) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}

		if (Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}

		m.setAccessible(true);
		if (((IS_KAFFE)
					|| !m.PHOSPHOR_TAGmarked)
				&& !"java.lang.Object".equals(m.getDeclaringClass().getName())) {
			if (IS_KAFFE) {
				if (in.length > 0)
					m = getTaintMethod(m, isObjTags);
			} else
				m = getTaintMethod(m, isObjTags);
		}

		m.setAccessible(true);
		ret.a = in;
		ret.o = owner;
		ret.m = m;

		int j = 0;
		if (in != null && m.getParameterTypes().length != in.length) {
			ret.a = new Object[m.getParameterTypes().length];
			for (int i = 0; i < in.length; i++) {
				if (m.getParameterTypes()[j].isPrimitive()) {
					if (in[i] instanceof TaintedWithIntTag)
						ret.a[j] = ((TaintedWithIntTag) in[i]).getPHOSPHOR_TAG();
					else if(in[i] instanceof TaintedWithObjTag)
						ret.a[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else if(isObjTags)
						ret.a[j] = null;
					else
						ret.a[j] = 0;
					j++;
				} else if (m.getParameterTypes()[j] == Configuration.TAINT_TAG_OBJ_CLASS){
					if(in[i] instanceof TaintedWithObjTag)
						ret.a[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret.a[j] = null;
					j++;
				} else if (m.getParameterTypes()[j].isArray()
						&& (m.getParameterTypes()[j].getComponentType().isPrimitive()
							|| m.getParameterTypes()[j].getComponentType()
									.equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
					if (!isObjTags) {
						MultiDTaintedArrayWithIntTag arr = ((MultiDTaintedArrayWithIntTag) in[i]);
						ret.a[j] = arr.taint;
						j++;
						ret.a[j] = arr.getVal();
						j++;
					} else {
						MultiDTaintedArrayWithObjTag arr = ((MultiDTaintedArrayWithObjTag) in[i]);
						ret.a[j] = arr.taint;
						j++;
						ret.a[j] = arr.getVal();
						j++;
					}
					continue;
				}
				ret.a[j] = in[i];
				j++;
			}
		}

		if ((in == null
					&& m != null
					&& m.getParameterTypes().length == 1)
				|| (in != null
					&& j != in.length - 1)) {
			if (in == null)
				ret.a = new Object[1];

			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveArrayWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveArrayWithObjTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType))  {
				try {
					ret.a[j] = returnType.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, ControlTaintTagStack ctrl) {
		//		System.out.println("Making slow call to  " + m);
		MethodInvoke ret = new MethodInvoke();
		m.setAccessible(true);
		if (Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}

		if ((IS_KAFFE) || !m.PHOSPHOR_TAGmarked) {
			if (IS_KAFFE) {
				if (in.length > 0)
					m = getTaintMethodControlTrack(m);
			} else {
				m = getTaintMethodControlTrack(m);
			}
		}

		m.setAccessible(true);
		ret.a = in;
		ret.o = owner;
		ret.m = m;

		int j = 0;
		if (in != null && m.getParameterTypes().length != in.length) {
			ret.a = new Object[m.getParameterTypes().length];
			for (int i = 0; i < in.length; i++) {
				if (m.getParameterTypes()[j].isPrimitive()) {
					if (in[i] instanceof TaintedWithIntTag)
						ret.a[j] = ((TaintedWithIntTag) in[i]).getPHOSPHOR_TAG();
					else if(in[i] instanceof TaintedWithObjTag)
						ret.a[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret.a[j] = 0;
					j++;
				} else if (m.getParameterTypes()[j] == Configuration.TAINT_TAG_OBJ_CLASS){
					if(in[i] instanceof TaintedWithObjTag)
						ret.a[j] = ((TaintedWithObjTag) in[i]).getPHOSPHOR_TAG();
					else
						ret.a[j] = null;
					j++;
				} else if (m.getParameterTypes()[j].isArray()
						&& (m.getParameterTypes()[j].getComponentType().isPrimitive()
							|| m.getParameterTypes()[j].getComponentType()
									.equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
					MultiDTaintedArrayWithObjTag arr = ((MultiDTaintedArrayWithObjTag) in[i]);
					ret.a[j] = arr.taint;
					j++;
					ret.a[j] = arr.getVal();
					j++;
					continue;
				}
				ret.a[j] = in[i];
				j++;
			}
		}

		if (ret.a != null) {
			ret.a[j] = ctrl;
			j++;
		}

		if (in == null && m != null && m.getParameterTypes().length == 1) {
			ret.a = new Object[1];
			ret.a[0] = ctrl;
		} else if ((in == null
					&& m != null
					&& m.getParameterTypes().length == 2) 
				|| (in != null
					&& j != in.length - 1)) {
			if (in == null) {
				ret.a = new Object[2];
				ret.a[0] = ctrl;
				j++;
			}

			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveArrayWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveArrayWithObjTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
				try {
					ret.a[j] = returnType.newInstance();
					if (ret.a[j].getClass().equals(Boolean.class)) {
						System.exit(-1);
					}
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public static Class removeTaintClass(Class clazz, boolean isObjTags) {

		if (cachedClasses.containsKey(clazz))
			return cachedClasses.get(clazz);
		Type t = Type.getType(clazz);
		if (t.getSort() == Type.ARRAY) {
			String cmp = t.getElementType().getDescriptor();
			if (cmp.length() >= "Ledu.columbia.cs.psl.phosphor.struct.multid".length()
					&& cmp.subSequence(0, "Ledu.columbia.cs.psl.phosphor.struct.multid".length())
							.equals("Ledu/columbia/cs/psl/phosphor/struct/multid")) {
				String innerType = null;

				if(isObjTags)
					innerType = (MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(clazz));
				else
					innerType = (MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(clazz));

				String newName = "[";
				for (int i = 0; i < t.getDimensions(); i++)
					newName += "[";
				try {
					Class ret = Class.forName(newName + innerType);
					cachedClasses.put(clazz, ret);
					return ret;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			cachedClasses.put(clazz, clazz);
			return clazz;
		}
		String cmp = t.getDescriptor();
		if (cmp.length() >= "Ledu.columbia.cs.psl.phosphor.struct.multid".length()
				&& cmp.subSequence(0, "Ledu.columbia.cs.psl.phosphor.struct.multid".length())
						.equals("Ledu/columbia/cs/psl/phosphor/struct/multid")) {
			String innerType = null;
			if(isObjTags)
				innerType = (MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(clazz));
			else
				innerType = (MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(clazz));

			try {
				Class ret = Class.forName("[" + innerType);
				cachedClasses.put(clazz, ret);
				return ret;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		cachedClasses.put(clazz, clazz);
		return clazz;
	}

	public static Field[] removeTaintFields(Field[] in) {

		ArrayList<Field> ret = new ArrayList<Field>();
		for (Field f : in) {
			if (f.getName().equals("taint") || f.getName().endsWith(TaintUtils.TAINT_FIELD)) {

			} else
				ret.add(f);
		}
		Field[] retz = new Field[ret.size()];
		ret.toArray(retz);
		return retz;
	}

	private static ReflectionFactory getReflectionFactory() {
		if (reflectionFactory == null) {
			reflectionFactory = java.security.AccessController.doPrivileged(
							new sun.reflect.ReflectionFactory.GetReflectionFactoryAction());
		}
		return reflectionFactory;
	}


	public static Method[] copyMethods(Method[] arg) {
		return arg;
	}

	@SuppressWarnings("rawtypes")
	public static Constructor[] copyConstructors(Constructor[] arg) {
		return arg;
	}

	@SuppressWarnings("rawtypes")
	public static Field[] copyFields(Field[] arg) {
		return arg;
	}

	public static Method[] removeTaintMethods(Method[] in) {

		ArrayList<Method> ret = new ArrayList<Method>();
		for (Method f : in) {
			final char[] chars = f.getName().toCharArray();
			boolean match = false;
			if (chars.length == GETSETLEN) {
				match = true;
				for (int i = 3; i < GETSETLEN; i++) {
					if (chars[i] != GETSETCHARS[i]) {
						match = false;
						break;
					}
				}
			}

			if (!match && chars.length > SUFFIX_LEN) {
				int x = 0;
				boolean matched = true;
				for (int i = chars.length - SUFFIX_LEN; i < chars.length; i++) {
					if (chars[i] != SUFFIXCHARS[x]) {
						matched = false;
						break;
					}
					x++;
				}

				x = 0;
				if (!matched
						&& Configuration.GENERATE_UNINST_STUBS
						&& chars.length > SUFFIX_LEN + 2)
					for (int i = chars.length - SUFFIX_LEN -2; i < chars.length; i++) {
						if (chars[i] != SUFFIX2CHARS[x]) {
							ret.add(f);
							break;
						}
						x++;
					}
				else if(!matched)
					ret.add(f);
			} else if (!match) {
				ret.add(f);
			}
		}

		Method[] retz = new Method[ret.size()];
		ret.toArray(retz);
		//		System.out.println("returning taint methods: " + Arrays.toString(retz));
		return retz;
	}

	@SuppressWarnings("rawtypes")
	public static Constructor[] removeTaintConstructors(Constructor[] in) {
		ArrayList<Constructor> ret = new ArrayList<Constructor>();
		for (Constructor f : in) {
			Class[] params = f.getParameterTypes();
			if (params.length > 0
					&& (params[params.length - 1].getName()
							.equals("edu.columbia.cs.psl.phosphor.runtime.TaintSentinel")
						|| params[params.length - 1].getName()
							.equals("edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel"))) {

			} else
				ret.add(f);
		}

		Constructor[] retz = new Constructor[ret.size()];
		ret.toArray(retz);
		return retz;
	}
}
