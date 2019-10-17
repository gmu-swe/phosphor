package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class ReflectionMasker {

    private static final boolean IS_KAFFE = false;
    private static final String multiDDescriptor = "edu.columbia.cs.psl.phosphor.struct.Lazy";
    private static final int multiDDescriptorLength = multiDDescriptor.length();
    private static final char[] SET_TAG_METHOD_CHARS = "setPHOSPHOR_TAG".toCharArray();
    private static final int SET_TAG_METHOD_LEN = SET_TAG_METHOD_CHARS.length;
    private static final char[] METHOD_SUFFIX_CHARS = TaintUtils.METHOD_SUFFIX.toCharArray();
    private static final int METHOD_SUFFIX_LEN = METHOD_SUFFIX_CHARS.length;
    private static final char[] METHOD_SUFFIX_UNINST_CHARS = TaintUtils.METHOD_SUFFIX_UNINST.toCharArray();
    private static final char[] FIELD_SUFFIX_CHARS = TaintUtils.TAINT_FIELD.toCharArray();
    private static final int FIELD_METHOD_SUFFIX_LEN = FIELD_SUFFIX_CHARS.length;
    static boolean isSelectiveInstManagerInit = false;
    private static ReflectionFactory reflectionFactory;

    static {
        System.setSecurityManager(null);
    }
    //	static Method lastOrigMethod;
    //	static Method lastMethod;

    @SuppressWarnings("unused")
    public static Object getObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long offset, ControlTaintTagStack ctrl) {
        return getObject$$PHOSPHORTAGGED(u, obj, null, offset);
    }

    @SuppressWarnings("unused")
    public static Object getObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long offset) {
        return RuntimeUnsafePropagator.get(u, obj, offset, null);
    }

    public static void putObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint tag, long fieldOffset, Object val, ControlTaintTagStack ctrl) {
        putObject$$PHOSPHORTAGGED(u, obj, tag, fieldOffset, val);
    }

    public static void putObject$$PHOSPHORTAGGED(Unsafe u, Object obj, Taint<?> tag, long fieldOffset, Object val) {
        if(val instanceof LazyArrayObjTags) {
            try {
                RuntimeUnsafePropagator.OffsetPair pair = RuntimeUnsafePropagator.getOffsetPair(u, obj, fieldOffset);
                if(pair != null) {
                    if(pair.origFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                        u.putObject(obj, pair.origFieldOffset, MultiDTaintedArray.unbox1D(val));
                    }
                    if(pair.tagFieldOffset != Unsafe.INVALID_FIELD_OFFSET) {
                        u.putObject(obj, pair.tagFieldOffset, val);
                    }
                    return;
                }
            } catch(Exception e) {
                //
            }
        }
        u.putObject(obj, fieldOffset, val);
    }

    @SuppressWarnings("unused")
    public static TaintedBooleanWithObjTag isInstance(Class<?> c1, Object o, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
        return isInstance(c1, o, ret);
    }

    public static TaintedBooleanWithObjTag isInstance(Class<?> c1, Object o, TaintedBooleanWithObjTag ret) {
        ret.taint = null;
        if(o instanceof LazyArrayObjTags && !LazyArrayObjTags.class.isAssignableFrom(c1)) {
            ret.val = c1.isInstance(MultiDTaintedArrayWithObjTag.unboxRaw(o));
        } else {
            ret.val = c1.isInstance(o);
        }
        return ret;
    }

    public static String getPropertyHideBootClasspath(String prop) {
        if(prop.equals("sun.boot.class.path")) {
            return null;
        } else if(prop.equals("os.name")) {
            return "linux";
        }
        return System.getProperty(prop);
    }

    public static Method getTaintMethodControlTrack(Method m) {
        if(m.getDeclaringClass().isAnnotation()) {
            return m;
        }
        final char[] chars = m.getName().toCharArray();
        if(chars.length > METHOD_SUFFIX_LEN) {
            boolean isEq = true;
            int x = 0;
            for(int i = chars.length - METHOD_SUFFIX_LEN; i < chars.length; i++) {
                if(chars[i] != METHOD_SUFFIX_CHARS[x]) {
                    isEq = false;
                }
                x++;
            }
            if(isEq) {
                if(!IS_KAFFE) {
                    m.PHOSPHOR_TAGmarked = true;
                }
                return m;
            }
            x = 0;
            if(Configuration.GENERATE_UNINST_STUBS && chars.length > METHOD_SUFFIX_LEN + 2) {
                for(int i = chars.length - METHOD_SUFFIX_LEN - 2; i < chars.length; i++) {
                    if(chars[i] != METHOD_SUFFIX_UNINST_CHARS[x]) {
                        isEq = false;
                    }
                    x++;
                }
            }
            if(isEq) {
                if(!IS_KAFFE) {
                    m.PHOSPHOR_TAGmarked = true;
                }
                return m;
            }
        }
        if(declaredInIgnoredClass(m)) {
            return m;
        }
        //		if (methodCache.containsKey(m))
        //			return methodCache.get(m);
        ArrayList<Class> newArgs = new ArrayList<Class>();
        boolean madeChange = false;
        for(final Class c : m.getParameterTypes()) {
            if(c.isArray()) {
                if(c.getComponentType().isPrimitive()) {
                    //1d primitive array
                    madeChange = true;
                    try {
                        newArgs.add(Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(Type.getType(c)).getInternalName().replace('/', '.')));
                    } catch(ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    newArgs.add(c);
                    continue;
                } else {
                    Class elementType = c.getComponentType();
                    while(elementType.isArray()) {
                        elementType = c.getComponentType();
                    }
                    if(elementType.isPrimitive()) {
                        //2d primtiive array
                        madeChange = true;
                        try {
                            newArgs.add(Class.forName(MultiDTaintedArrayWithObjTag.getTypeForType(Type.getType(c)).getInternalName().replace('/', '.')));
                        } catch(ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        continue;
                    } else {
                        //reference array
                        newArgs.add(c);
                        continue;
                    }
                }
            } else if(c.isPrimitive()) {
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

        if(returnType.isPrimitive() && returnType != Void.TYPE) {
            if(returnType == Integer.TYPE) {
                newArgs.add(TaintedIntWithObjTag.class);
            } else if(returnType == Short.TYPE) {
                newArgs.add(TaintedShortWithObjTag.class);
            } else if(returnType == Float.TYPE) {
                newArgs.add(TaintedFloatWithObjTag.class);
            } else if(returnType == Double.TYPE) {
                newArgs.add(TaintedDoubleWithObjTag.class);
            } else if(returnType == Long.TYPE) {
                newArgs.add(TaintedLongWithObjTag.class);
            } else if(returnType == Character.TYPE) {
                newArgs.add(TaintedCharWithObjTag.class);
            } else if(returnType == Byte.TYPE) {
                newArgs.add(TaintedByteWithObjTag.class);
            } else if(returnType == Boolean.TYPE) {
                newArgs.add(TaintedBooleanWithObjTag.class);
            }
            madeChange = true;
        }

        if(madeChange) {
            Class[] args = new Class[newArgs.size()];
            newArgs.toArray(args);
            Method ret = null;
            try {
                ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORTAGGED", args);
            } catch(NoSuchMethodException e) {
                e.printStackTrace();
            } catch(SecurityException e) {
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

    @SuppressWarnings("rawtypes")
    public static Method getTaintMethod(Method m, boolean isObjTags) {
        //		if (m.INVIVO_PC_TAINTmarked)
        //			return m;
        if(m.PHOSPHOR_TAGmarked && m.PHOSPHOR_TAGmethod != null) {
            return m;
        } else if(!m.PHOSPHOR_TAGmarked && m.PHOSPHOR_TAGmethod != null) {
            return m.PHOSPHOR_TAGmethod;
        }

        if(m.getDeclaringClass().isAnnotation()) {
            return m;
        }

        final char[] chars = m.getName().toCharArray();
        if(chars.length > METHOD_SUFFIX_LEN) {
            boolean isEq = true;
            int x = 0;
            for(int i = chars.length - METHOD_SUFFIX_LEN; i < chars.length; i++) {
                if(chars[i] != METHOD_SUFFIX_CHARS[x]) {
                    isEq = false;
                }
                x++;
            }
            if(isEq) {
                if(!IS_KAFFE) {
                    m.PHOSPHOR_TAGmarked = true;
                }
                return m;
            }
            x = 0;
            if(Configuration.GENERATE_UNINST_STUBS && chars.length > METHOD_SUFFIX_LEN + 2) {
                for(int i = chars.length - METHOD_SUFFIX_LEN - 2; i < chars.length; i++) {
                    if(chars[i] != METHOD_SUFFIX_UNINST_CHARS[x]) {
                        isEq = false;
                    }
                    x++;
                }
            }
            if(isEq) {
                if(!IS_KAFFE) {
                    m.PHOSPHOR_TAGmarked = true;
                }
                return m;
            }
        }
        ArrayList<Class> newArgs = new ArrayList<>();
        boolean madeChange = false;
        for(final Class c : m.getParameterTypes()) {
            if(c.isArray()) {
                if(c.getComponentType().isPrimitive()) {
                    //1d primitive array
                    madeChange = true;
                    newArgs.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
                    newArgs.add(c);
                } else {
                    Class elementType = c.getComponentType();
                    while(elementType.isArray()) {
                        elementType = elementType.getComponentType();
                    }
                    if(elementType.isPrimitive()) {
                        //2d primtiive array
                        madeChange = true;
                        try {
                            newArgs.add(Class.forName(MultiDTaintedArray.getTypeForType(Type.getType(c)).getInternalName()));
                        } catch(ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //reference array
                        newArgs.add(c);
                    }
                }
            } else if(c.isPrimitive()) {
                madeChange = true;
                if(isObjTags) {
                    newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
                } else {
                    newArgs.add(Integer.TYPE);
                }
                newArgs.add(c);
            } else {
                //anything else
                newArgs.add(c);
            }
        }
        final Class returnType = m.getReturnType();
        if(returnType.isArray()) {

        } else if(returnType.isPrimitive() && returnType != Void.TYPE) {
            if(returnType == Integer.TYPE) {
                newArgs.add(TaintedIntWithObjTag.class);
            } else if(returnType == Short.TYPE) {
                newArgs.add(TaintedShortWithObjTag.class);
            } else if(returnType == Float.TYPE) {
                newArgs.add(TaintedFloatWithObjTag.class);
            } else if(returnType == Double.TYPE) {
                newArgs.add(TaintedDoubleWithObjTag.class);
            } else if(returnType == Long.TYPE) {
                newArgs.add(TaintedLongWithObjTag.class);
            } else if(returnType == Character.TYPE) {
                newArgs.add(TaintedCharWithObjTag.class);
            } else if(returnType == Byte.TYPE) {
                newArgs.add(TaintedByteWithObjTag.class);
            } else if(returnType == Boolean.TYPE) {
                newArgs.add(TaintedBooleanWithObjTag.class);
            }
            madeChange = true;
        }
        if(madeChange) {
            Class[] args = new Class[newArgs.size()];
            newArgs.toArray(args);
            Method ret = null;
            try {
                ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORTAGGED", args);
            } catch(NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
            ret.PHOSPHOR_TAGmarked = true;
            m.PHOSPHOR_TAGmethod = ret;
            ret.PHOSPHOR_TAGmethod = m;
            return ret;
        } else {
            m.PHOSPHOR_TAGmarked = false;
            m.PHOSPHOR_TAGmethod = m;
            return m;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Method getUnTaintMethod(Method m, boolean isObjTags) {
        //		if (m.INVIVO_PC_TAINTmarked)
        //			return m;
        if(m.getDeclaringClass().isAnnotation()) {
            return m;
        }

        final char[] chars = m.getName().toCharArray();
        if(chars.length > METHOD_SUFFIX_LEN + 2) {
            boolean isEq = true;
            int x = 0;
            for(int i = chars.length - METHOD_SUFFIX_LEN - 2; i < chars.length; i++) {
                if(chars[i] != METHOD_SUFFIX_UNINST_CHARS[x]) {
                    isEq = false;
                }
                x++;
            }
            if(isEq) {

                return m;
            }
        }
        ArrayList<Class> newArgs = new ArrayList<Class>();
        boolean madeChange = false;
        for(final Class c : m.getParameterTypes()) {
            if(c.isArray()) {
                if(c.getComponentType().isPrimitive()) {
                    newArgs.add(c);
                    continue;
                } else {
                    Class elementType = c.getComponentType();
                    String s = "";
                    while(elementType.isArray()) {
                        elementType = elementType.getComponentType();
                    }
                    if(elementType.isPrimitive()) {
                        //2d primtiive array
                        madeChange = true;
                        newArgs.add(MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(c));
                        continue;
                    } else {
                        //reference array
                        newArgs.add(c);
                        continue;
                    }
                }
            } else if(c.isPrimitive()) {
                newArgs.add(c);
                continue;
            } else {
                //anything else
                newArgs.add(c);
                continue;
            }
        }
        if(madeChange) {
            Class[] args = new Class[newArgs.size()];
            newArgs.toArray(args);
            Method ret = null;
            try {
                ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + "$$PHOSPHORUNTAGGED", args);
            } catch(NoSuchMethodException e) {
                e.printStackTrace();
            } catch(SecurityException e) {
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
        if(Configuration.GENERATE_UNINST_STUBS && m.getName().endsWith("$$PHOSPHORUNTAGGED")) {
            String origName = m.getName().replace("$$PHOSPHORUNTAGGED", "");
            try {
                return m.getDeclaringClass().getDeclaredMethod(origName, m.getParameterTypes());
            } catch(NoSuchMethodException e) {
                e.printStackTrace();
            } catch(SecurityException e) {
                e.printStackTrace();
            }
        } else if(m.PHOSPHOR_TAGmethod != null && m.PHOSPHOR_TAGmarked) {
            return m.PHOSPHOR_TAGmethod;
        }
        return m;
    }

    /* Returns the original list of the parameters for a method that would produce a phosphor-added method with the specified
     * tainted parameters. */
    private static SinglyLinkedList<Class<?>> getOriginalParamTypes(Class<?>[] taintedParamTypes) {
        SinglyLinkedList<Class<?>> originalParamTypes = new SinglyLinkedList<>();
        for(int i = 0; i < taintedParamTypes.length; i++) {
            Class<?> paramType = taintedParamTypes[i];
            if(paramType.equals(Taint.class) || paramType.equals(Integer.TYPE)) {
                // Add the type of the primitive for which the current parameter is the taint tag
                originalParamTypes.enqueue(taintedParamTypes[++i]);
            } else if(LazyArrayObjTags.class.isAssignableFrom(paramType)) {
                // Add the type of the 1D primitive array for which the current parameter is the taint array
                originalParamTypes.enqueue(taintedParamTypes[++i]);
            } else if(paramType.getName().contains("edu.columbia.cs.psl.phosphor.struct.Lazy")) {
                // Add the original multidimensional primitive array for the current LazyArray array
                originalParamTypes.enqueue(TaintUtils.getUnwrappedClass(paramType));
            } else if(!paramType.equals(TaintSentinel.class) && !paramType.equals(ControlTaintTagStack.class) &&
                    !TaintedPrimitiveWithObjTag.class.isAssignableFrom(paramType)) {
                // Add the type as is if it is not TaintSentinel, ControlTaintTagStack or a TaintedPrimitiveWithXTags
                originalParamTypes.enqueue(paramType);
            }
        }
        return originalParamTypes;
    }

    @SuppressWarnings({"unused"})
    public static Constructor<?> getOrigMethod(Constructor<?> cons, boolean isObjTags) {
        if(declaredInIgnoredClass(cons)) {
            return cons;
        }
        boolean hasSentinel = false;
        for(Class<?> clazz : cons.getParameterTypes()) {
            if(clazz.equals(TaintSentinel.class)) {
                hasSentinel = true;
                break;
            }
        }
        if(hasSentinel) {
            Class<?>[] origParams = getOriginalParamTypes(cons.getParameterTypes()).toArray(new Class<?>[0]);
            try {
                return cons.getDeclaringClass().getDeclaredConstructor(origParams);
            } catch(NoSuchMethodException | SecurityException e) {
                return cons;
            }
        } else {
            return cons;
        }
    }

    /* Called for Class.getConstructor and Class.getDeclaredConstructor to remap the parameter types. */
    @SuppressWarnings("unused")
    public static Class<?>[] addTypeParams(Class<?> clazz, Class<?>[] params, boolean implicitTracking, boolean isObjTags) {
        if(isIgnoredClass(clazz) || params == null) {
            return params;
        }
        boolean needsChange = false;
        for(Class<?> c : params) {
            if(c != null && (c.isPrimitive() || (c.isArray() && isPrimitiveArray(c)))) {
                needsChange = true;
            }
        }
        if(implicitTracking) {
            needsChange = true;
        }
        if(needsChange) {
            ArrayList<Class<?>> newParams = new ArrayList<>();
            for(Class<?> c : params) {
                Type t = Type.getType(c);
                if(t.getSort() == Type.ARRAY) {
                    if(t.getElementType().getSort() != Type.OBJECT) {
                        if(t.getDimensions() == 1) {
                            newParams.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
                        } else {
                            Type newType = null;
                            newType = MultiDTaintedArrayWithObjTag.getTypeForType(t);
                            try {
                                newParams.add(Class.forName(newType.getInternalName().replace("/", ".")));
                                continue;
                            } catch(ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if(t.getSort() != Type.OBJECT) {
                    if(isObjTags) {
                        newParams.add(Configuration.TAINT_TAG_OBJ_CLASS);
                    } else {
                        newParams.add(Integer.TYPE);
                    }
                }
                newParams.add(c);
            }
            if(implicitTracking) {
                newParams.add(ControlTaintTagStack.class);
            }
            newParams.add(TaintSentinel.class);
            Class[] ret = new Class[newParams.size()];
            newParams.toArray(ret);
//						System.out.println("Adding type params from " + Arrays.toString(params)+ " to " + Arrays.toString(ret));
            return ret;
        }
        return params;
    }

    public static Method getDeclaredMethod(Class<?> czz, String name, Class<?>[] params, boolean isObjTags) throws NoSuchMethodException {
        return checkForSyntheticObjectMethod(czz.getDeclaredMethod(name, params), true);
    }

    public static Method getDeclaredMethod$$PHOSPHORTAGGED(Class<?> czz, String name, Class<?>[] params, ControlTaintTagStack ctrl) throws NoSuchMethodException {
        return checkForSyntheticObjectMethod(czz.getDeclaredMethod(name, params), true);
    }

    public static Method getMethod$$PHOSPHORTAGGED(Class<?> czz, String name, Class<?>[] params, ControlTaintTagStack ctrl) throws NoSuchMethodException {
        return checkForSyntheticObjectMethod(czz.getMethod(name, params), false);
    }

    public static Method getMethod(Class<?> czz, String name, Class<?>[] params, boolean isObjTags) throws NoSuchMethodException {
        return checkForSyntheticObjectMethod(czz.getMethod(name, params), false);
    }

    /* If the specified method is a synthetic hashCode or equals method added by Phosphor and declaredOnly is true,
     * finds and returns a suitable replacement for the method. If the specified method is a synthetic hashCode or
     * equals method added by Phosphor and declaredOnly is false, throws a NoSuchMethodException. */
    private static Method checkForSyntheticObjectMethod(Method m, boolean declaredOnly) throws NoSuchMethodException {
        if(m.isSynthetic()) {
            if("equals".equals(m.getName())) {
                if(declaredOnly) {
                    throw new NoSuchMethodException();
                } else {
                    return ObjectMethods.EQUALS.method;
                }
            } else if("hashCode".equals(m.getName())) {
                if(declaredOnly) {
                    throw new NoSuchMethodException();
                } else {
                    return ObjectMethods.HASH_CODE.method;
                }
            }
        }
        return m;
    }

    /* Returns whether the specified member was declared in a class ignored by Phosphor. */
    private static boolean declaredInIgnoredClass(Member member) {
        return member != null && member.getDeclaringClass() != null && isIgnoredClass(member.getDeclaringClass());
    }

    /* Returns whether the specified class if ignored by Phosphor. */
    private static boolean isIgnoredClass(Class<?> clazz) {
        return clazz != null && (Instrumenter.isIgnoredClass(clazz.getName().replace('.', '/')) || Object.class.equals(clazz));
    }

    static boolean isPrimitiveArray(Class<?> c) {
        return c.isArray() ? isPrimitiveArray(c.getComponentType()) : c.isPrimitive();
    }

    @SuppressWarnings("rawtypes")
    public static Class[] removeTaintedInterface(Class[] in) {
        if(in == null) {
            return null;
        }
        boolean found = false;
        for(int i = 0; i < in.length; i++) {
            if(in[i].equals(TaintedWithObjTag.class)) {
                found = true;
            }
        }
        if(!found) {
            return in;
        }
        Class[] ret = new Class[in.length - 1];
        int idx = 0;
        for(int i = 0; i < in.length; i++) {
            if(!in[i].equals(TaintedWithObjTag.class)) {
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
        if(clazz == null || in == null) {
            return in;
        }

        for(int i = 0; i < in.length; i++) {
            if(in[i].getClassName().equals(toFind) && !(i + 1 < in.length && in[i + 1].getClassName().equals(toFind))) {
                depthToCut = i + 1;
                break;
            }
        }
        StackTraceElement[] ret = new StackTraceElement[in.length - depthToCut];
        System.arraycopy(in, depthToCut, ret, 0, ret.length);
        return ret;
    }

    public static Object[] fixAllArgs(Object[] in, Constructor<?> c, boolean isObjTags) {
        return fixAllArgs(in, c, false, null);
    }

    public static Object[] fixAllArgs(Object[] in, Constructor c, ControlTaintTagStack ctrl) {
        return fixAllArgs(in, c, true, ctrl);
    }

    private static Object[] fixAllArgs(Object[] in, Constructor<?> c, boolean implicitTracking, ControlTaintTagStack ctrl) {
        if(declaredInIgnoredClass(c)) {
            return getOriginalParams(c.getParameterTypes(), in);
        }
        if(c == null) {
            return in;
        } else if(in != null && c.getParameterTypes().length != in.length) {
            Object[] ret = new Object[c.getParameterTypes().length];
            fillInParams(ret, in, c.getParameterTypes());
            if(implicitTracking) {
                ret[ret.length - 2] = ctrl;
            }
            return ret;
        } else if(in == null && c.getParameterTypes().length == 1) {
            Object[] ret = new Object[1];
            ret[0] = null;
            return ret;
        } else if(in == null && c.getParameterTypes().length == 2) {
            Object[] ret = new Object[2];
            ret[0] = implicitTracking ? ctrl : new ControlTaintTagStack();
            ret[1] = null;
            return ret;
        }
        return in;
    }

    /* Returns an array of objects derived from the specified array of tainted parameters that match the specified array
     * of types. */
    private static Object[] getOriginalParams(Class<?>[] types, Object[] taintedParams) {
        Object[] originalParams = new Object[types.length];
        for(int i = 0; i < types.length; i++) {
            if(types[i].isPrimitive()) {
                if(taintedParams[i] instanceof TaintedPrimitiveWithObjTag) {
                    originalParams[i] = ((TaintedPrimitiveWithObjTag) taintedParams[i]).toPrimitiveType();
                } else {
                    originalParams[i] = taintedParams[i];
                }
            } else if(types[i].isArray()) {
                Object obj = MultiDTaintedArray.maybeUnbox(taintedParams[i]);
                originalParams[i] = obj;

            } else {
                originalParams[i] = taintedParams[i];
            }
        }
        return originalParams;
    }

    @SuppressWarnings("rawtypes")
    public static Object[] fixAllArgsFast(Method m, Object[] in, boolean isObjTags) {
        System.out.println("Making fast call to  " + m);
        Object[] ret = in;
        m.setAccessible(true);
        int j = 0;
        if(in != null && m.getParameterTypes().length != in.length) {
            ret = new Object[m.getParameterTypes().length];
            for(Object o : in) {
                if(m.getParameterTypes()[j].isPrimitive()) {
                    if(o instanceof TaintedWithObjTag) {
                        ret[j] = ((TaintedWithObjTag) o).getPHOSPHOR_TAG();
                    } else {
                        ret[j] = 0;
                    }
                    j++;
                } else if(m.getParameterTypes()[j].isArray() && m.getParameterTypes()[j].getComponentType().isPrimitive()) {
                    LazyArrayObjTags arr = ((LazyArrayObjTags) o);
                    ret[j] = arr.taints;
                    j++;
                    ret[j] = arr.getVal();
                    j++;
                    continue;
                }
                ret[j] = o;
                j++;
            }
        }
        if((in == null && m.getParameterTypes().length == 1) || (in != null && j != in.length - 1)) {
            if(in == null) {
                ret = new Object[1];
            }
            final Class returnType = m.getReturnType();
            if(TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
                try {
                    ret[j] = returnType.newInstance();
                } catch(InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
        return ret;
    }

    public static Object[] fixAllArgsUninst(Object[] in, Constructor c, boolean isObjTags) {
        Class<?>[] params = c.getParameterTypes();
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
        if(m == null) {
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
            if(ret.a != null && ret.a.getClass().isArray() && ret.a.getClass().getComponentType() == Object.class) {
                for(int i = 0; i < ret.a.length; i++) {
                    if(ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
                        ret.a[i] = ((MultiDTaintedArrayWithObjTag) ret.a[i]).getVal();
                    }
                }
            }
        } else {
            ret = fixAllArgs(m, owner, in, isObjTags);
        }
        return ret;
    }

    public static MethodInvoke fixAllArgsUninst(Method m, Object owner, Object[] in, ControlTaintTagStack ctrl) {
        MethodInvoke ret = new MethodInvoke();
        if(m == null) {
            ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
        }
        ret.m = getOrigMethod(m, true);
        ret.o = owner;
        ret.a = in;
        for(int i = 0; i < ret.a.length; i++) {
            if(ret.a[i] instanceof MultiDTaintedArrayWithObjTag) {
                ret.a[i] = ((MultiDTaintedArrayWithObjTag) ret.a[i]).getVal();
            }
        }
        return ret;
    }

    public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, boolean isObjTags) {
        MethodInvoke ret = new MethodInvoke();
        if(m == null || declaredInIgnoredClass(m)) {
            ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
        }
        m.setAccessible(true);
        if((!m.PHOSPHOR_TAGmarked) && !"java.lang.Object".equals(m.getDeclaringClass().getName())) {
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
        if((in == null && m.getParameterTypes().length == 1) || (in != null && j != in.length - 1)) {
            ret.a = (in != null) ? ret.a : new Object[1];
            final Class returnType = m.getReturnType();
            if(TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
                try {
                    ret.a[j] = returnType.newInstance();
                } catch(InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    public static MethodInvoke fixAllArgs(Method m, Object owner, Object[] in, ControlTaintTagStack ctrl) {
        MethodInvoke ret = new MethodInvoke();
        m.setAccessible(true);
        if(declaredInIgnoredClass(m)) {
            ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
        }
        if(!m.PHOSPHOR_TAGmarked) {
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
        if(in == null && m.getParameterTypes().length == 1) {
            ret.a = new Object[1];
            ret.a[0] = ctrl;
        } else if((in == null && m.getParameterTypes().length == 2)
                || (in != null && j != in.length - 1)) {
            if(in == null) {
                ret.a = new Object[2];
                ret.a[0] = ctrl;
                j++;
            }
            final Class returnType = m.getReturnType();
            if(TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
                try {
                    ret.a[j] = returnType.newInstance();
                    if(ret.a[j].getClass().equals(Boolean.class)) {
                        System.exit(-1);
                    }
                } catch(InstantiationException | IllegalAccessException e) {
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
        if(providedArgs != null && paramTypes.length != providedArgs.length) {
            for(Object providedArg : providedArgs) {
                Class<?> targetParamClass = paramTypes[targetParamIndex];
                if(targetParamClass.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                    // Add an object taint
                    targetArgs[targetParamIndex++] = MultiTainter.getTaint(providedArg);
                    // Add a boxed primitive to the args
                    targetArgs[targetParamIndex++] = providedArg;
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

    /* Called after calls to Object.getClass. */
    public static Class<?> removeTaintClass(Class<?> clazz) {
        if(clazz.PHOSPHOR_TAGclass != null) {
            return clazz.PHOSPHOR_TAGclass;
        } else if(clazz.isArray()) {
            String cmp = null;
            Class c = clazz.getComponentType();
            while(c.isArray()) {
                c = c.getComponentType();
            }
            cmp = c.getName();
            if(cmp.length() >= multiDDescriptorLength
                    && cmp.subSequence(0, multiDDescriptorLength).equals(multiDDescriptor)) {
                Type t = Type.getType(clazz);
                String innerType = MultiDTaintedArray.getPrimitiveTypeForWrapper(clazz);
                String newName = "[";
                for(int i = 0; i < t.getDimensions(); i++) {
                    newName += "[";
                }
                try {
                    Class ret = Class.forName(newName + innerType);
                    clazz.PHOSPHOR_TAGclass = ret;
                    ret.PHOSPHOR_TAGclass = ret;
                    return ret;
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            clazz.PHOSPHOR_TAGclass = clazz;
            return clazz;
        } else {
            String cmp = clazz.getName();
            if(cmp.length() >= multiDDescriptorLength
                    && cmp.subSequence(0, multiDDescriptorLength).equals(multiDDescriptor)) {
                String innerType = MultiDTaintedArray.getPrimitiveTypeForWrapper(clazz);
                try {
                    Class ret = Class.forName("[" + innerType);
                    clazz.PHOSPHOR_TAGclass = ret;
                    ret.PHOSPHOR_TAGclass = ret;
                    return ret;
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            clazz.PHOSPHOR_TAGclass = clazz;
            return clazz;
        }
    }

    /* Masks calls to Object.getClass from ObjectOutputStream. */
    @SuppressWarnings("unused")
    public static Class<?> getClassOOS(Object o) {
        if(o instanceof LazyArrayObjTags && ((LazyArrayObjTags) o).taints != null) {
            return o.getClass();
        } else {
            return removeTaintClass(o.getClass());
        }
    }

    /* Filters the fields returned by Class.getFields and Class.getDeclaredFields. */
    @SuppressWarnings("unused")
    public static Field[] removeTaintFields(Field[] in) {
        SinglyLinkedList<Field> ret = new SinglyLinkedList<>();
        boolean removeSVUIDField = containsSVUIDSentinelField(in);
        for(Field f : in) {
            if(!f.getName().equals("taint") && !f.getName().endsWith(TaintUtils.TAINT_FIELD) && !f.getName().startsWith(TaintUtils.PHOSPHOR_ADDED_FIELD_PREFIX)
                    && !(removeSVUIDField && f.getName().equals("serialVersionUID"))) {
                ret.enqueue(f);
            }
        }
        return ret.toArray(new Field[ret.size()]);
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

    /* Filters the methods returns by Class.getDeclaredMethods and Class.getMethods. If declaredOnly is true then
     * synthetic equals and hashCode methods are fully removed from the specified array, otherwise they are replaced with
     * Object.equals and Object.hashCode respectively. */
    @SuppressWarnings("unused")
    public static Method[] removeTaintMethods(Method[] in, boolean declaredOnly) {
        SinglyLinkedList<Method> ret = new SinglyLinkedList<>();
        for(Method f : in) {
            final char[] chars = f.getName().toCharArray();
            boolean match = false;
            if(chars.length == SET_TAG_METHOD_LEN) {
                match = true;
                for(int i = 3; i < SET_TAG_METHOD_LEN; i++) {
                    if(chars[i] != SET_TAG_METHOD_CHARS[i]) {
                        match = false;
                        break;
                    }
                }
            }
            if(!match && chars.length > METHOD_SUFFIX_LEN) {
                int x = 0;
                boolean matched = true;
                for(int i = chars.length - METHOD_SUFFIX_LEN; i < chars.length; i++) {
                    if(chars[i] != METHOD_SUFFIX_CHARS[x]) {
                        matched = false;
                        break;
                    }
                    x++;
                }
                x = 0;
                if(!matched && Configuration.GENERATE_UNINST_STUBS && chars.length > METHOD_SUFFIX_LEN + 2) {
                    for(int i = chars.length - METHOD_SUFFIX_LEN - 2; i < chars.length; i++) {
                        if(chars[i] != METHOD_SUFFIX_UNINST_CHARS[x]) {
                            ret.enqueue(f);
                            break;
                        }
                        x++;
                    }
                } else if(!matched) {
                    ret.enqueue(f);
                }
            } else if(!match) {
                // Check for synthetic hashCode and equals methods added by Phosphor
                if(f.isSynthetic()) {
                    if(chars.length == 6 && chars[0] == 'e' && chars[1] == 'q' && chars[2] == 'u' && chars[3] == 'a' &&
                            chars[4] == 'l' && chars[5] == 's') {
                        if(!declaredOnly) {
                            ret.enqueue(ObjectMethods.EQUALS.method);
                        }
                        continue;
                    } else if(chars.length == 8 && chars[0] == 'h' && chars[1] == 'a' && chars[2] == 's' && chars[3] == 'h' &&
                            chars[4] == 'C' && chars[5] == 'o' && chars[6] == 'd' && chars[7] == 'e') {
                        if(!declaredOnly) {
                            ret.enqueue(ObjectMethods.HASH_CODE.method);
                        }
                        continue;
                    }
                }
                ret.enqueue(f);
            }
        }
        return ret.toArray(new Method[ret.size()]);
    }

    /* Filters the constructors returned by Class.getConstructors and Class.getDeclaredConstructors. */
    @SuppressWarnings({"unused"})
    public static Constructor<?>[] removeTaintConstructors(Constructor<?>[] in) {
        SinglyLinkedList<Constructor<?>> ret = new SinglyLinkedList<>();
        for(Constructor<?> f : in) {
            Class<?>[] params = f.getParameterTypes();
            if(params.length == 0 || !(TaintUtils.isTaintSentinel(params[params.length - 1]))) {
                ret.enqueue(f);
            }
        }
        return ret.toArray(new Constructor<?>[ret.size()]);
    }

    private static ReflectionFactory getReflectionFactory() {
        if(reflectionFactory == null) {
            reflectionFactory = java.security.AccessController.doPrivileged(new sun.reflect.ReflectionFactory.GetReflectionFactoryAction());
        }
        return reflectionFactory;
    }

    public static Method[] copyMethods(Method[] arg) {
        //		if(arg == null || arg.length>0)
        //			throw new IllegalStateException("this is disabled for testing");
        //		ArrayList<Method> ret = new ArrayList<Method>();
        //		ReflectionFactory fact = getReflectionFactory();
        //		for (Method f : arg) {
        //			final char[] chars = f.getName().toCharArray();
        //			boolean match = false;
        //			if (chars.length == SET_TAG_METHOD_LEN) {
        //				match = true;
        //				for (int i = 3; i < SET_TAG_METHOD_LEN; i++) {
        //					if (chars[i] != SET_TAG_METHOD_CHARS[i]) {
        //						match = false;
        //						break;
        //					}
        //				}
        //			}
        //			if (!match && chars.length > METHOD_SUFFIX_LEN) {
        //				int x = 0;
        //				for (int i = chars.length - METHOD_SUFFIX_LEN; i < chars.length; i++) {
        //					if (chars[i] != METHOD_SUFFIX_CHARS[x]) {
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

    public static Constructor<?>[] copyConstructors(Constructor<?>[] arg) {
        //		ArrayList<Constructor> ret = new ArrayList<Constructor>();
        //		ReflectionFactory fact = getReflectionFactory();
        //
        //		for (Constructor f : arg) {
        //			Class<?>[] params = f.getParameterTypes();
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

    public static Field[] copyFields(Field[] arg) {
        //		ArrayList<Field> ret = new ArrayList<Field>();
        //		ReflectionFactory fact = getReflectionFactory();
        //		//        System.out.println("Fields in : " +Arrays.toString(arg));
        //		for (Field f : arg) {
        //			final char[] chars = f.getName().toCharArray();
        //			if (chars.length >= FIELD_METHOD_SUFFIX_LEN) {
        //				int x = 0;
        //				for (int i = chars.length - FIELD_METHOD_SUFFIX_LEN; i < chars.length; i++) {
        //					if (chars[i] != FIELD_SUFFIX_CHARS[x]) {
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

    /* Used to create singleton references to Methods of the Object class. */
    private enum ObjectMethods {
        EQUALS("equals", Object.class),
        HASH_CODE("hashCode");

        public Method method;

        ObjectMethods(String name, Class<?>... parameterTypes) {
            try {
                this.method = Object.class.getDeclaredMethod(name, parameterTypes);
            } catch(NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }
}
