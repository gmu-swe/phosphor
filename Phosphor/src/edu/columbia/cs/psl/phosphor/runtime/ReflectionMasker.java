package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionMasker {

	static {
		System.setSecurityManager(null);
	}
	//	static WeakHashMap<Method, Method> methodCache = new WeakHashMap<Method, Method>();

	public static final boolean IS_KAFFE = false;

	public static Class<?> getClassOOS(Object o)
	{
		if(o instanceof LazyArrayObjTags && ((LazyArrayObjTags) o).taints != null)
			return o.getClass();
		else if(o instanceof LazyArrayIntTags && ((LazyArrayIntTags) o).taints != null)
			return o.getClass();
		return removeTaintClass(o.getClass(), Configuration.MULTI_TAINTING);
	}
	public static Object getObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long offset, ControlTaintTagStack ctrl)
	{
		return MultiDTaintedArrayWithObjTag.boxIfNecessary(u.getObject(obj, offset));
	}
	public static Object getObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long offset)
	{
		return MultiDTaintedArrayWithObjTag.boxIfNecessary(u.getObject(obj, offset));
	}
	public static Object getObject$$PHOSPHORTAGGED(Unsafe u, Object obj, int tag, long offset)
	{
		return MultiDTaintedArrayWithIntTag.boxIfNecessary(u.getObject(obj, offset));
	}
	public static void putObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long fieldOffset, Object val, ControlTaintTagStack ctrl)
	{
		putObject$$PHOSPHORTAGGED(u, obj, tag, fieldOffset, val);
	}
	public static void putObject$$PHOSPHORTAGGED(Unsafe u, Object obj, int tag, long fieldOffset, Object val)
	{
		//Need to put the actual obj if its a lazyarray and the offset points to the tag field
		if(val instanceof LazyArrayIntTags)
		{
			for (Field f : obj.getClass().getDeclaredFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObject(obj, fieldOffset, ((LazyArrayIntTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObject(obj, fieldOffset, val);
					return;
				}
			for (Field f : obj.getClass().getFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObject(obj, fieldOffset, ((LazyArrayIntTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObject(obj, fieldOffset, val);
					return;
				}
			u.putObject(obj, fieldOffset, ((LazyArrayIntTags) val).getVal());
		}
		u.putObject(obj, fieldOffset, val);
	}

	public static void putObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint<?> tag, long fieldOffset, Object val)
	{
		//Need to put the actual obj if its a lazyarray and the offset points to the tag field
		if(val instanceof LazyArrayObjTags)
		{
			for (Field f : obj.getClass().getDeclaredFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObject(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObject(obj, fieldOffset, val);
					return;
				}
			for (Field f : obj.getClass().getFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObject(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObject(obj, fieldOffset, val);
					return;
				}
			u.putObject(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
		}
		u.putObject(obj, fieldOffset, val);
	}
	
	private static void putTaintWrapper(Unsafe u, Field origField, Object obj, Object val)
	{
		if(origField.getType().isArray() && origField.getType().getComponentType().isPrimitive())
			try {
				Field taintField = origField.getDeclaringClass().getDeclaredField(origField.getName()+TaintUtils.TAINT_FIELD);
				u.putObject(obj, u.objectFieldOffset(taintField), val);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException e) {
//				e.printStackTrace();
			}
		
	}
	public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint<?> tag, long fieldOffset, Object val, ControlTaintTagStack ctrl){
		putObjectVolatile$$PHOSPHORTAGGED(u, obj, tag, fieldOffset, val);
	}
	public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint<?> tag, long fieldOffset, Object val)
	{
		//Need to put the actual obj if its a lazyarray and the offset points to the tag field
		if(val instanceof LazyArrayObjTags)
		{
			for (Field f : obj.getClass().getDeclaredFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObjectVolatile(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObjectVolatile(obj, fieldOffset, val);
					return;
				}
			for (Field f : obj.getClass().getFields())
				if (!Modifier.isStatic(f.getModifiers()) && u.objectFieldOffset(f) == fieldOffset)
				{
					if(f.getType().isArray())
					{
						u.putObjectVolatile(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
						putTaintWrapper(u, f, obj, val);
					}
					else
						u.putObjectVolatile(obj, fieldOffset, val);
					return;
				}
			u.putObjectVolatile(obj, fieldOffset, ((LazyArrayObjTags) val).getVal());
		}
		u.putObjectVolatile(obj, fieldOffset, val);
	}

	
	public static TaintedBooleanWithIntTag isInstance(Class<?> c1, Object o, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		if (o instanceof LazyArrayIntTags || o instanceof LazyArrayIntTags[]) {
			if (LazyArrayIntTags.class.isAssignableFrom(c1))
				ret.val = c1.isInstance(o);
			else
				ret.val = c1.isInstance(MultiDTaintedArrayWithIntTag.unboxRaw(o));
		} else
			ret.val = c1.isInstance(o);
		return ret;
	}
	public static TaintedBooleanWithObjTag isInstance(Class<?> c1, Object o, ControlTaintTagStack ctr, TaintedBooleanWithObjTag ret) {
		return isInstance(c1, o, ret);
	}
	public static TaintedBooleanWithObjTag isInstance(Class<?> c1, Object o, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		if (o instanceof LazyArrayObjTags || o instanceof LazyArrayObjTags[]) {
			if (LazyArrayObjTags.class.isAssignableFrom(c1))
				ret.val = c1.isInstance(o);
			else
				ret.val = c1.isInstance(MultiDTaintedArrayWithObjTag.unboxRaw(o));
		} else
			ret.val = c1.isInstance(o);
		return ret;
	}
	public static TaintedBooleanWithObjTag isInstanceOOS(Class<?> c1, Object o, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		if (o instanceof LazyArrayObjTags || o instanceof LazyArrayObjTags[]) {
			if (LazyArrayObjTags.class.isAssignableFrom(c1))
				ret.val = c1.isInstance(o);
			else
				ret.val = c1.isInstance(MultiDTaintedArrayWithObjTag.unboxRaw(o));
		} else
			ret.val = c1.isInstance(o);
		return ret;
	}
	public static TaintedBooleanWithIntTag isInstanceOOS(Class<?> c1, Object o, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		if (o instanceof LazyArrayIntTags || o instanceof LazyArrayIntTags[]) {
			if (LazyArrayIntTags.class.isAssignableFrom(c1))
				ret.val = c1.isInstance(o);
			else
				ret.val = c1.isInstance(MultiDTaintedArrayWithIntTag.unboxRaw(o));
		} else
			ret.val = c1.isInstance(o);
		return ret;
	}

	
	public static String getPropertyHideBootClasspath(String prop)
	{
		if(prop.equals("sun.boot.class.path"))
			return null;
		else if(prop.equals("os.name"))
			return "linux";
		return System.getProperty(prop);
	}

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
		if(Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/')))
		{
			return m;
		}
		//		if (methodCache.containsKey(m))
		//			return methodCache.get(m);
		ArrayList<Class> newArgs = new ArrayList<Class>();
		boolean madeChange = false;
		for (final Class c : m.getParameterTypes()) {
			if (c.isArray()) {
				if (c.getComponentType().isPrimitive()) {
					//1d primitive array
					madeChange = true;
					try {
						newArgs.add(Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(Type.getType(c)).getInternalName().replace('/','.')));
					}catch(ClassNotFoundException e){
						e.printStackTrace();
					}
					newArgs.add(c);
					continue;
				} else {
					Class elementType = c.getComponentType();
					while (elementType.isArray())
						elementType = c.getComponentType();
					if (elementType.isPrimitive()) {
						//2d primtiive array
						madeChange = true;
						try {
							newArgs.add(Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(Type.getType(c)).getInternalName().replace('/','.')));
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						continue;
					} else {
						//reference array
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
				//anything else
				newArgs.add(c);
				continue;
			}
		}
			madeChange = true;
			newArgs.add(ControlTaintTagStack.class);
		final Class returnType = m.getReturnType();
		
		if (returnType.isPrimitive() && returnType != Void.TYPE) {
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
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = ret;
			//			methodCache.put(m, ret);
			//			ret.INVIVO_PC_TAINTmarked = true;
			return ret;
		} else {
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = m;
			//			methodCache.put(m, m);
			//			m.INVIVO_PC_TAINTmarked = true;
			return m;
		}
	}
	static boolean isSelectiveInstManagerInit = false;

	@SuppressWarnings("rawtypes")
	public static Method getTaintMethod(Method m, boolean isObjTags) {
		//		if (m.INVIVO_PC_TAINTmarked)
		//			return m;
		if(m.PHOSPHOR_TAGmarked && m.PHOSPHOR_TAGmethod != null)
			return m;
		else if(!m.PHOSPHOR_TAGmarked && m.PHOSPHOR_TAGmethod != null)
			return m.PHOSPHOR_TAGmethod;
		
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
		//		if (methodCache.containsKey(m))
		//			return methodCache.get(m);
		ArrayList<Class> newArgs = new ArrayList<Class>();
		boolean madeChange = false;
		for (final Class c : m.getParameterTypes()) {
			if (c.isArray()) {
				if (c.getComponentType().isPrimitive()) {
					//1d primitive array
					madeChange = true;
					newArgs.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
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
								newArgs.add(Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(Type.getType(c)).getInternalName()));
							else
								newArgs.add(Class.forName(MultiDTaintedArrayWithIntTag.getTypeForType(Type.getType(c)).getInternalName()));
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						continue;
					} else {
						//reference array
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
				//anything else
				newArgs.add(c);
				continue;
			}
		}
		final Class returnType = m.getReturnType();
		if (!isObjTags) {
			if (returnType.isArray()) {
				
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
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = ret;
			//			methodCache.put(m, ret);
			//			ret.INVIVO_PC_TAINTmarked = true;
			ret.PHOSPHOR_TAGmarked = true;
			m.PHOSPHOR_TAGmethod = ret;
			ret.PHOSPHOR_TAGmethod = m;
			return ret;
		} else {
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = m;
			//			methodCache.put(m, m);
			//			m.INVIVO_PC_TAINTmarked = true;
			m.PHOSPHOR_TAGmarked = false;
			m.PHOSPHOR_TAGmethod = m;
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
							newArgs.add(MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(c));
						else
							newArgs.add(MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(c));
						continue;
					} else {
						//reference array
						newArgs.add(c);
						continue;
					}
				}
			} else if (c.isPrimitive()) {
				newArgs.add(c);
				continue;
			} else {
				//anything else
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
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = ret;
			//			methodCache.put(m, ret);
			//			ret.INVIVO_PC_TAINTmarked = true;
			return ret;
		} else {
			//			lastTaintMethodOrig = m;
			//			lastTaintMethod = m;
			//			methodCache.put(m, m);
			//			m.INVIVO_PC_TAINTmarked = true;
			return m;
		}
	}
	//	static Method lastOrigMethod;
	//	static Method lastMethod;

	@SuppressWarnings("rawtypes")
	public static Method getOrigMethod(Method m, boolean isObjTracking) {
//		if(VM.isBooted())
//				System.out.println("Get orig method: " + m);
		//		new Exception().printStackTrace();
		//		if (m == lastOrigMethod)
		//		{
		//			System.out.println("Returning " + lastMethod);
		//			return lastMethod;
		//		}
		if(Configuration.GENERATE_UNINST_STUBS && m.getName().endsWith("$$PHOSPHORUNTAGGED"))
		{
			String origName = m.getName().replace("$$PHOSPHORUNTAGGED", "");
			try {
				return m.getDeclaringClass().getDeclaredMethod(origName, m.getParameterTypes());
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		else if(m.PHOSPHOR_TAGmethod != null && m.PHOSPHOR_TAGmarked)
			return m.PHOSPHOR_TAGmethod;
		return m;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Constructor getOrigMethod(Constructor m, boolean isObjTags) {
		ArrayList<Class> origArgs = new ArrayList<Class>();
		boolean hasSentinel = false;
		boolean dontIgnorePrimitive = false;
//		if(VM.isBooted())
//		System.out.println("GOOM " + m);
		for (Class c : m.getParameterTypes()) {
			Type t = Type.getType(c);
			if (c.getName().startsWith("edu.columbia.cs.psl.phosphor.struct.Lazy")) {
				//Remove multid
				String s = "[";
				if(isObjTags) 
					s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
				else 
					s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
				try {
					origArgs.add(Class.forName(s));
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT && t.getElementType().getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/Lazy")) {
				//Remove multid
				String s = "";
				for (int i = 0; i < t.getDimensions(); i++)
					s += "[";
				s += "[";
				if(isObjTags)
					s += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
				else
					s += MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
				try {
					origArgs.add(Class.forName(s));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if ((c.isArray() &&(c.getComponentType().isPrimitive()) || c == Configuration.TAINT_TAG_OBJ_ARRAY_CLASS)) {
				if (dontIgnorePrimitive)
					origArgs.add(c);
				dontIgnorePrimitive = !dontIgnorePrimitive;
			} else if (c.isPrimitive() || c.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
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
							newParams.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
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
//						System.out.println("Adding type params from " + Arrays.toString(params)+ " to " + Arrays.toString(ret));
			return ret;
		}
		return params;
	}

	public static Method getDeclaredMethod(Class czz, String name, Class[] params, boolean isObjTags) throws NoSuchMethodException {
		return czz.getDeclaredMethod(name, params);
	}
	public static Method getDeclaredMethod$$PHOSPHORTAGGED(Class czz, String name, Class[] params, ControlTaintTagStack ctrl) throws NoSuchMethodException {
		return czz.getDeclaredMethod(name, params);
	}

	public static Method getMethod$$PHOSPHORTAGGED(Class czz, String name, Class[] params, ControlTaintTagStack ctrl) throws NoSuchMethodException {
		if (czz == Object.class || czz.isAnnotation() || Instrumenter.isIgnoredClass(czz.getName().replace('.', '/')))
			return czz.getMethod(name, params);
		//also, return original method if we are calling wait()
		if (name.equals("wait")) {
			if (params.length == 0 || (params.length == 1 && params[0] == Long.TYPE) || (params.length == 2 && params[0] == Long.TYPE && params[1] == Integer.TYPE))
				return czz.getMethod(name, params);
		}
		try {
			Method m = czz.getMethod(name, params);
			if (Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
				//maybe "this" class is not ignored, but super, which implements method is ignored
				return czz.getMethod(name, params);
			}
			m = getTaintMethodControlTrack(m);
			return m;
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}
		System.err.println("Bailing just in case.");
		System.exit(-1);
		return null;
	}
	public static Method getMethod(Class czz, String name, Class[] params, boolean isObjTags) throws NoSuchMethodException {
		if (czz == Object.class || czz.isAnnotation() || Instrumenter.isIgnoredClass(czz.getName().replace('.', '/')))
		    return czz.getMethod(name, params);
		//also, return original method if we are calling wait()
		if (name.equals("wait")) {
			if (params.length == 0 || (params.length == 1 && params[0] == Long.TYPE) || (params.length == 2 && params[0] == Long.TYPE && params[1] == Integer.TYPE))
				return czz.getMethod(name, params);
		}
		try {
			Method m = czz.getMethod(name, params);
			if (Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
				//maybe "this" class is not ignored, but super, which implements method is ignored
				return czz.getMethod(name, params);
			}
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
			if (in[i].equals(TaintedWithIntTag.class) || in[i].equals(TaintedWithObjTag.class))
				found = true;
		}
		if (!found)
			return in;
		Class[] ret = new Class[in.length - 1];
		int idx = 0;
		for (int i = 0; i < in.length; i++) {
			if (!in[i].equals(TaintedWithIntTag.class) && !in[i].equals(TaintedWithObjTag.class)) {
				ret[idx] = in[i];
				idx++;
			}
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public static StackTraceElement[] removeExtraStackTraceElements(StackTraceElement[] in, Class clazz) {
		int depthToCut = 0;
		String toFind = clazz.getName();
		if (clazz == null || in == null)
			return in;

		for (int i = 0; i < in.length; i++) {
			if (in[i].getClassName().equals(toFind) && !(i + 1 < in.length && in[i + 1].getClassName().equals(toFind))) {
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
		if (c != null && in != null && c.getParameterTypes().length != in.length) {
			Object[] ret = new Object[c.getParameterTypes().length];
			fillInParams(ret, in, c.getParameterTypes());
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
		if (c != null && in != null && c.getParameterTypes().length != in.length) {
			Object[] ret = new Object[c.getParameterTypes().length];
			int j = 0;

			fillInParams(ret, in, c.getParameterTypes());
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
				} else if (m.getParameterTypes()[j].isArray() && m.getParameterTypes()[j].getComponentType().isPrimitive()) {
					if (!isObjTags) {
						LazyArrayIntTags arr = ((LazyArrayIntTags) in[i]);
						ret[j] = arr.taints;
						j++;
						ret[j] = arr.getVal();
						j++;
					} else {
						LazyArrayObjTags arr = ((LazyArrayObjTags) in[i]);
						ret[j] = arr.taints;
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
		if ((in == null && m.getParameterTypes().length == 1) || (in != null && j != in.length - 1)) {
			if (in == null) {
				ret = new Object[1];
			}
			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType))  {
				try {
					ret[j] = returnType.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		}
		return ret;
	}
	
	public static Object[] fixAllArgsUninst(Object[] in, Constructor c, boolean isObjTags) {
		Class[] params = c.getParameterTypes();
		if(params.length == 0) {
			return in;
		}
		if(params[params.length - 1] == UninstrumentedTaintSentinel.class) {
			Object[] ret = new Object[in.length + 1];
			System.arraycopy(in, 0, ret, 0, in.length);
			return ret;
		} else if(params[params.length - 1] == TaintSentinel.class) {
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
		if(Configuration.WITH_SELECTIVE_INST) /* && Instrumenter.isIgnoredMethodFromOurAnalysis(m.getDeclaringClass().getName().replace(".", "/"), m.getName(), Type.getMethodDescriptor(m))) */ {
//			ret.m = getUnTaintMethod(m, isObjTags);
			ret.m = getOrigMethod(m, isObjTags);
			ret.o = owner;
			ret.a = in;
			if(ret.a != null && ret.a.getClass().isArray() && ret.a.getClass().getComponentType() == Object.class)
			for(int i = 0; i < ret.a.length; i++) {
				if(ret.a[i] instanceof LazyArrayIntTags) {
					ret.a[i] = ((LazyArrayIntTags)ret.a[i]).getVal();
				} else if(ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
					ret.a[i] = ((MultiDTaintedArrayWithObjTag)ret.a[i]).getVal();
				}
			}
		} else {
			ret = fixAllArgs(m, owner, in, isObjTags);
		}
		return ret;
	}

	public static MethodInvoke fixAllArgsUninst(Method m, Object owner, Object[] in, ControlTaintTagStack ctrl) {
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
		for(int i = 0; i < ret.a.length; i++) {
			if(ret.a[i] instanceof LazyArrayIntTags) {
				ret.a[i] = ((LazyArrayIntTags)ret.a[i]).getVal();
			} else if(ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
				ret.a[i] = ((MultiDTaintedArrayWithObjTag)ret.a[i]).getVal();
			}
		}
		return ret;
	}


	public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, boolean isObjTags) {
		MethodInvoke ret = new MethodInvoke();
		if(m == null || Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
		    ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
		}
		m.setAccessible(true);
		if ((!m.PHOSPHOR_TAGmarked) && !"java.lang.Object".equals(m.getDeclaringClass().getName())) {
			m = getTaintMethod(m, isObjTags);
		}
		m.setAccessible(true);
		ret.o = owner;
		ret.m = m;
		if(in != null && m.getParameterTypes().length != in.length) {
			ret.a = new Object[ret.m.getParameterTypes().length];
		} else {
			ret.a = in;
		}
		int j = fillInParams(ret.a, in, ret.m.getParameterTypes());
		if ((in == null && m.getParameterTypes().length == 1) || (in != null && j != in.length - 1)) {
			ret.a = (in != null) ? ret.a :  new Object[1];
			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType) || TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType))  {
				try {
					ret.a[j] = returnType.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, ControlTaintTagStack ctrl) {
		MethodInvoke ret = new MethodInvoke();
		m.setAccessible(true);
		if(Instrumenter.isIgnoredClass(m.getDeclaringClass().getName().replace('.', '/'))) {
			ret.a = in;
			ret.o = owner;
			ret.m = m;
			return ret;
		}
		if (!m.PHOSPHOR_TAGmarked) {
			m = getTaintMethodControlTrack(m);
		}
		m.setAccessible(true);
		ret.o = owner;
		ret.m = m;
		if(in != null && m.getParameterTypes().length != in.length) {
			ret.a = new Object[ret.m.getParameterTypes().length];
		} else {
			ret.a = in;
		}
		int j = fillInParams(ret.a, in, ret.m.getParameterTypes());
		if(ret.a != null && ret.a.length > j) {
			ret.a[j++] = ctrl;
		}
		if (in == null && m.getParameterTypes().length == 1) {
			ret.a = new Object[1];
			ret.a[0] = ctrl;
		} else if ((in == null && m.getParameterTypes().length == 2)
				|| (in != null && j != in.length - 1)) {
			if (in == null) {
				ret.a = new Object[2];
				ret.a[0] = ctrl;
				j++;
			}
			final Class returnType = m.getReturnType();
			if (TaintedPrimitiveWithIntTag.class.isAssignableFrom(returnType)
					|| TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
				try {
					ret.a[j] = returnType.newInstance();
					if(ret.a[j].getClass().equals(Boolean.class))
					{
						System.exit(-1);
					}
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/* Adds arguments to the target argument array from the specified array of provided arguments based on the specified
	 * expected parameter types. Returns the number of arguments added. */
	private static int fillInParams(Object[] targetArgs, Object[] providedArgs, Class<?>[] paramTypes) {
		int targetParamIndex = 0;
		if (providedArgs != null && paramTypes.length != providedArgs.length) {
			for(Object providedArg : providedArgs) {
				Class<?> targetParamClass = paramTypes[targetParamIndex];
				if(targetParamClass.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
					// Add an object taint
					targetArgs[targetParamIndex++] = MultiTainter.getTaint(providedArg);
					// Add a boxed primitive to the args
					targetArgs[targetParamIndex++] = providedArg;
				} else if(targetParamClass.equals(Integer.TYPE)) {
					// Add an int taint to the args
					if(providedArg instanceof TaintedWithObjTag) {
						targetArgs[targetParamIndex++] = ((TaintedWithObjTag) providedArg).getPHOSPHOR_TAG();
					} else if(providedArg instanceof Boolean) {
						targetArgs[targetParamIndex++] = BoxedPrimitiveStoreWithIntTags.booleanValue((Boolean)providedArg).taint;
					} else if(providedArg instanceof Byte) {
						targetArgs[targetParamIndex++] = BoxedPrimitiveStoreWithIntTags.byteValue((Byte)providedArg).taint;
					} else if(providedArg instanceof Short) {
						targetArgs[targetParamIndex++] = BoxedPrimitiveStoreWithIntTags.shortValue((Short)providedArg).taint;
					} else if(providedArg instanceof Character) {
						targetArgs[targetParamIndex++]= BoxedPrimitiveStoreWithIntTags.charValue((Character)providedArg).taint;
					} else {
						targetArgs[targetParamIndex++] = 0;
					}
					// Add a boxed primitive to the args
					targetArgs[targetParamIndex++] = providedArg;
				} else if(LazyArrayIntTags.class.isAssignableFrom(targetParamClass)) {
					// Add a LazyArray to the args
					LazyArrayIntTags arr = ((LazyArrayIntTags) providedArg);
					targetArgs[targetParamIndex++] = arr;
					// Add a primitive array to the args
					targetArgs[targetParamIndex++] = (arr == null) ? null : arr.getVal();
				} else if(LazyArrayObjTags.class.isAssignableFrom(targetParamClass)) {
					// Add a LazyArray to the args
					LazyArrayObjTags arr = ((LazyArrayObjTags) providedArg);
					targetArgs[targetParamIndex++] = arr;
					// Add a primitive array to the args
					targetArgs[targetParamIndex++] = (arr == null) ? null : arr.getVal();
				} else {
					// Add the provided argument as is to the args
					targetArgs[targetParamIndex++] = providedArg;
				}
			}
		}
		return targetParamIndex;
	}

	final static String multiDDescriptor = "edu.columbia.cs.psl.phosphor.struct.Lazy";
	final static int multiDDescriptorLength = multiDDescriptor.length();

	@SuppressWarnings("rawtypes")
	public static Class removeTaintClass(Class clazz, boolean isObjTags) {
		if(clazz.PHOSPHOR_TAGclass != null)
			return clazz.PHOSPHOR_TAGclass;
		if (clazz.isArray()) {
			String cmp = null;
			Class c = clazz.getComponentType();
			while(c.isArray())
				c = c.getComponentType();
			cmp = c.getName();
			if (cmp.length() >= multiDDescriptorLength
					&& cmp.subSequence(0, multiDDescriptorLength).equals(multiDDescriptor)) {
				String innerType = null;
				Type t = Type.getType(clazz);

				if(isObjTags)
					innerType = (MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(clazz));
				else
					innerType = (MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(clazz));
				String newName = "[";
				for (int i = 0; i < t.getDimensions(); i++)
					newName += "[";
				try {
					Class ret = Class.forName(newName + innerType);
					clazz.PHOSPHOR_TAGclass=ret;
					ret.PHOSPHOR_TAGclass=ret;
					return ret;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			clazz.PHOSPHOR_TAGclass = clazz;
			return clazz;
		}
		String cmp = clazz.getName();
		if (cmp.length() >= multiDDescriptorLength
				&& cmp.subSequence(0, multiDDescriptorLength).equals(multiDDescriptor)) {
			String innerType = null;
			if(isObjTags)
				innerType = (MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(clazz));
			else
				innerType = (MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(clazz));
			try {
				Class ret = Class.forName("[" + innerType);
				clazz.PHOSPHOR_TAGclass=ret;
				ret.PHOSPHOR_TAGclass=ret;
				return ret;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		clazz.PHOSPHOR_TAGclass = clazz;
		return clazz;
	}

	public static Field[] removeTaintFields(Field[] in) {
		ArrayList<Field> ret = new ArrayList<Field>();
		boolean removeSVUIDField = containsSVUIDSentinelField(in);
		for (Field f : in) {
			if(!f.getName().equals("taint") && !f.getName().endsWith(TaintUtils.TAINT_FIELD) && !f.getName().equals(TaintUtils.ADDED_SVUID_SENTINEL)
					&& !(removeSVUIDField && f.getName().equals("serialVersionUID"))
					/* && !f.getName().equals(TaintUtils.IS_TAINT_SEATCHING_FIELD) && !f.getName().equals(TaintUtils.HAS_TAINT_FIELD) */) {
				ret.add(f);
			}
		}
		Field[] retz = new Field[ret.size()];
		ret.toArray(retz);
		return retz;
	}

	/* Returns whether the specified array of fields contains a sentinel field indicating that a SerialVersionUID was
	 * added to the class by phosphor. */
	private static boolean containsSVUIDSentinelField(Field[] in) {
		for(Field f : in) {
			if(f.getName().equals(TaintUtils.ADDED_SVUID_SENTINEL)) {
				return true;
			}
		}
		return false;
	}

	private static ReflectionFactory getReflectionFactory() {
		if (reflectionFactory == null) {
			reflectionFactory = java.security.AccessController.doPrivileged(new sun.reflect.ReflectionFactory.GetReflectionFactoryAction());
		}
		return reflectionFactory;
	}

	private static ReflectionFactory reflectionFactory;

	public static Method[] copyMethods(Method[] arg) {
		//		if(arg == null || arg.length>0)
		//			throw new IllegalStateException("this is disabled for testing");
		//		ArrayList<Method> ret = new ArrayList<Method>();
		//		ReflectionFactory fact = getReflectionFactory();
		//		for (Method f : arg) {
		//			final char[] chars = f.getName().toCharArray();
		//			boolean match = false;
		//			if (chars.length == GETSETLEN) {
		//				match = true;
		//				for (int i = 3; i < GETSETLEN; i++) {
		//					if (chars[i] != GETSETCHARS[i]) {
		//						match = false;
		//						break;
		//					}
		//				}
		//			}
		//			if (!match && chars.length > SUFFIX_LEN) {
		//				int x = 0;
		//				for (int i = chars.length - SUFFIX_LEN; i < chars.length; i++) {
		//					if (chars[i] != SUFFIXCHARS[x]) {
		//						ret.add(fact.copyMethod(f));
		//						break;
		//					}
		//					x++;
		//				}
		//			} else if (!match) {
		//				ret.add(fact.copyMethod(f));
		//			}
		//		}
		//		Method[] retz = new Method[ret.size()];
		//		ret.toArray(retz);
		//		return retz;
		return arg;
	}

	@SuppressWarnings("rawtypes")
	public static Constructor[] copyConstructors(Constructor[] arg) {
		//		ArrayList<Constructor> ret = new ArrayList<Constructor>();
		//		ReflectionFactory fact = getReflectionFactory();
		//
		//		for (Constructor f : arg) {
		//			Class[] params = f.getParameterTypes();
		//			if (params.length > 0
		//					&& (params[params.length - 1].getName().equals("edu.columbia.cs.psl.phosphor.runtime.TaintSentinel") || params[params.length - 1].getName().equals(
		//							"edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel"))) {
		//
		//			} else
		//				ret.add(fact.copyConstructor(f));
		//		}
		//		Constructor[] retz = new Constructor[ret.size()];
		//		ret.toArray(retz);
		//		return retz;
		return arg;
	}

	@SuppressWarnings("rawtypes")
	public static Field[] copyFields(Field[] arg) {
		//		ArrayList<Field> ret = new ArrayList<Field>();
		//		ReflectionFactory fact = getReflectionFactory();
		//		//        System.out.println("Fields in : " +Arrays.toString(arg));
		//		for (Field f : arg) {
		//			final char[] chars = f.getName().toCharArray();
		//			if (chars.length >= FIELDSUFFIXLEN) {
		//				int x = 0;
		//				for (int i = chars.length - FIELDSUFFIXLEN; i < chars.length; i++) {
		//					if (chars[i] != FIELDSUFFIXCHARS[x]) {
		//						ret.add(fact.copyField(f));
		//						break;
		//					}
		//					x++;
		//				}
		//			} else if (!chars.equals("taint")) {
		//				ret.add(fact.copyField(f));
		//			}
		//		}
		//		Field[] retz = new Field[ret.size()];
		//		ret.toArray(retz);
		//		//        System.out.println("Fields out : " +Arrays.toString(retz));
		//		return retz;
		return arg;
	}

	static final int SUFFIX_LEN = "$$PHOSPHORTAGGED".length();
	static final int GETSETLEN = "setPHOSPHOR_TAG".length();
	static final char[] GETSETCHARS = "setPHOSPHOR_TAG".toCharArray();
	static final char[] SUFFIXCHARS = "$$PHOSPHORTAGGED".toCharArray();
	static final char[] SUFFIX2CHARS = "$$PHOSPHORUNTAGGED".toCharArray();
	
	static final char[] FIELDSUFFIXCHARS = TaintUtils.TAINT_FIELD.toCharArray();
	static final int FIELDSUFFIXLEN = FIELDSUFFIXCHARS.length;

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
				if (!matched && Configuration.GENERATE_UNINST_STUBS && chars.length > SUFFIX_LEN + 2)
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
				if(chars.length == 6 && chars[0] == 'e' && chars[1] == 'q'
				&& chars[2] == 'u' && chars[3] == 'a' && chars[4] == 'l' && chars[5] == 's' && f.isSynthetic())
					continue;
				ret.add(f);
			}
		}
		Method[] retz = new Method[ret.size()];
		ret.toArray(retz);
		return retz;
	}

	@SuppressWarnings("rawtypes")
	public static Constructor[] removeTaintConstructors(Constructor[] in) {
		ArrayList<Constructor> ret = new ArrayList<Constructor>();
		for (Constructor f : in) {
			Class[] params = f.getParameterTypes();
			if (params.length > 0
					&& (params[params.length - 1].getName().equals("edu.columbia.cs.psl.phosphor.runtime.TaintSentinel") || params[params.length - 1].getName().equals(
							"edu.columbia.cs.psl.phosphor.runtime.UninstrumentedTaintSentinel"))) {

			} else {
				ret.add(f);
			}
		}
		Constructor[] retz = new Constructor[ret.size()];
		ret.toArray(retz);
		return retz;
	}
}
