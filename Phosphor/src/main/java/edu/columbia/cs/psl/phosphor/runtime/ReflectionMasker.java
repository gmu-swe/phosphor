package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.*;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class ReflectionMasker {

    private static final boolean IS_KAFFE = false;
    private static final String multiDDescriptor = "edu.columbia.cs.psl.phosphor.struct.Lazy";
    private static final int multiDDescriptorLength = multiDDescriptor.length();
    private static final char[] SET_TAG_METHOD_CHARS = "setPHOSPHOR_TAG".toCharArray();
    private static final int SET_TAG_METHOD_LEN = SET_TAG_METHOD_CHARS.length;
    private static final char[] METHOD_SUFFIX_CHARS = TaintUtils.METHOD_SUFFIX.toCharArray();
    private static final int METHOD_SUFFIX_LEN = METHOD_SUFFIX_CHARS.length;

    static {
        System.setSecurityManager(null);
    }

    private ReflectionMasker() {
        // Prevents this class from being instantiated
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getObject$$PHOSPHORTAGGED(Unsafe u, Taint uT, Object obj, Taint<?> tag, long offset, Taint oT, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret) {
        RuntimeUnsafePropagator.getObject$$PHOSPHORTAGGED(u, uT, obj, tag, offset, oT, ret);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getObject$$PHOSPHORTAGGED(Unsafe u, Taint unsafeTaint, Object obj, Taint<?> tag, long offset, Taint offsetTag, TaintedReferenceWithObjTag ret) {
        RuntimeUnsafePropagator.getObject$$PHOSPHORTAGGED(u, unsafeTaint, obj, tag, offset, offsetTag, ret);
        return ret;
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe u, Taint unsafetaint, Object obj, Taint<?> tag, long fieldOffset, Taint offsetTag, Object val, Taint valTaint, ControlFlowStack ctrl) {
        RuntimeUnsafePropagator.putObject$$PHOSPHORTAGGED(u, unsafetaint, obj, tag, fieldOffset, offsetTag, val, valTaint);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe u, Taint unsafetaint, Object obj, Taint<?> tag, long fieldOffset, Taint offsetTag, Object val, Taint valTaint) {
        //TODO go straight to runtimeunsafeprop instead?
        RuntimeUnsafePropagator.putObject$$PHOSPHORTAGGED(u, unsafetaint, obj, tag, fieldOffset, offsetTag, val, valTaint);
    }


    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = IS_INSTANCE)
    public static TaintedBooleanWithObjTag isInstance(Class<?> c1, Taint c1Taint, Object o, Taint oTaint, TaintedBooleanWithObjTag ret) {
        ret.taint = null;
        if(o instanceof LazyArrayObjTags && !LazyArrayObjTags.class.isAssignableFrom(c1)) {
            ret.val = c1.isInstance(MultiDTaintedArrayWithObjTag.unboxRaw(o));
        } else {
            ret.val = c1.isInstance(o);
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static String getPropertyHideBootClasspath(String prop) {
        if(prop.equals("sun.boot.class.path")) {
            return null;
        } else if(prop.equals("os.name")) {
            return "linux";
        }
        return System.getProperty(prop);
    }

    private static boolean isWrappedErasedType(Class c) {
        if(c.isArray()) {
            return !c.getComponentType().isPrimitive();
        }
        return false;
    }

    private static Constructor getTaintConstructor(Constructor m, boolean controlTracking) {
        final char[] chars = m.getName().toCharArray();
        ArrayList<Class> newArgs = new ArrayList<>();
        boolean madeChange = true;
        madeChange = true;
        newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
        LinkedList<Class> wrappedArgs = new LinkedList<>();
        for(final Class c : m.getParameterTypes()) {
            if(c.isArray()) {
                if(!c.getComponentType().isArray()) {
                    // 1d array
                    madeChange = true;
                    newArgs.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
                } else {
                    Class elementType = c.getComponentType();
                    while(elementType.isArray()) {
                        elementType = elementType.getComponentType();
                    }
                    madeChange = true;
                    try {
                        newArgs.add(Class.forName(MultiDTaintedArray.getTypeForType(Type.getType(c)).getInternalName()));
                    } catch(ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                madeChange = true;
                newArgs.add(c);
            }
            if(isWrappedErasedType(c)) {
                wrappedArgs.add(c);
            }
            newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
        }
        if(controlTracking) {
            newArgs.add(ControlFlowStack.class);
        }
        newArgs.addAll(wrappedArgs);
        Class[] args = new Class[newArgs.size()];
        newArgs.toArray(args);
        Constructor ret = null;
        try {
            ret = m.getDeclaringClass().getDeclaredConstructor(args);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }


    private static Method getTaintMethod(Method m, boolean controlTracking) {
        if(isMarked(m) && getCachedMethod(m) != null) {
            return m;
        } else if(!isMarked(m) && getCachedMethod(m) != null) {
            return getCachedMethod(m);
        } else if(m.getDeclaringClass().isAnnotation()) {
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
                    setMark(m, true);
                }
                return m;
            }
        }
        ArrayList<Class> newArgs = new ArrayList<>();
        boolean madeChange = false;
        if(!Modifier.isStatic(m.getModifiers())) {
            madeChange = true;
            newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
        }
        LinkedList<Class> wrappedArgs = new LinkedList<>();
        for(final Class c : m.getParameterTypes()) {
            if(c.isArray()) {
                if(!c.getComponentType().isArray()) {
                    // 1d array
                    madeChange = true;
                    newArgs.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
                } else {
                    Class elementType = c.getComponentType();
                    while(elementType.isArray()) {
                        elementType = elementType.getComponentType();
                    }
                    madeChange = true;
                    try {
                        newArgs.add(Class.forName(MultiDTaintedArray.getTypeForType(Type.getType(c)).getInternalName()));
                    } catch(ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                madeChange = true;
                newArgs.add(c);
            }
            if(isWrappedErasedType(c)) {
                wrappedArgs.add(c);
            }
            newArgs.add(Configuration.TAINT_TAG_OBJ_CLASS);
        }
        if(controlTracking) {
            newArgs.add(ControlFlowStack.class);
        }
        final Class returnType = m.getReturnType();
        if(returnType != Void.TYPE) {
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
            } else {
                newArgs.add(TaintedReferenceWithObjTag.class);
            }
            madeChange = true;
        }
        newArgs.addAll(wrappedArgs);
        if(madeChange) {
            Class[] args = new Class[newArgs.size()];
            newArgs.toArray(args);
            Method ret = null;
            try {
                ret = m.getDeclaringClass().getDeclaredMethod(m.getName() + TaintUtils.METHOD_SUFFIX, args);
            } catch(NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
            setMark(ret, true);
            setCachedMethod(m, ret);
            setCachedMethod(ret, m);
            return ret;
        } else {
            setMark(m, false);
            setCachedMethod(m, m);
            return m;
        }
    }

    /* Returns the original list of the parameters for a method that would produce a phosphor-added method with the specified
     * tainted parameters. */
    private static ArrayList<Class<?>> getOriginalParamTypes(Class<?>[] taintedParamTypes) {
        ArrayList<Class<?>> originalParamTypes = new ArrayList<>(taintedParamTypes.length);
        SinglyLinkedList<Class<?>> originalWrappedTypes = new SinglyLinkedList<>();
        for(int i = 1; i < taintedParamTypes.length; i++) { //start from 1 to skip the taint for THIS
            Class<?> paramType = taintedParamTypes[i];
            if(paramType.isArray()) {
                originalWrappedTypes.enqueue(paramType);
            } else if(paramType == LazyReferenceArrayObjTags.class) {
                originalParamTypes.add(paramType);
            } else if(LazyArrayObjTags.class.isAssignableFrom(paramType)) {
                // Add the type of the 1D primitive array for which the current parameter is the taint array
                originalParamTypes.add(TaintUtils.getUnwrappedClass(paramType));
            } else if(!paramType.equals(ControlFlowStack.class)
                    && !TaintedPrimitiveWithObjTag.class.isAssignableFrom(paramType) && !paramType.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                // Add the type as is if it is not TaintSentinel, ControlFlowStack or a TaintedPrimitiveWithXTags
                originalParamTypes.add(paramType);
            }
        }
        for(int i = 0; i < originalParamTypes.size(); i++) {
            if(originalParamTypes.get(i) == LazyReferenceArrayObjTags.class) {
                originalParamTypes.set(i, originalWrappedTypes.dequeue());
            }
        }
        return originalParamTypes;
    }

    /**
     * Called for Class.getConstructor and Class.getDeclaredConstructor to remap the parameter types.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = ADD_TYPE_PARAMS)
    public static LazyReferenceArrayObjTags addTypeParams(Class<?> clazz, LazyReferenceArrayObjTags params, boolean implicitTracking) {
        if(isIgnoredClass(clazz) || params == null || params.val == null) {
            return params;
        }
        boolean needsChange = true;
        ArrayList<Class<?>> newParams = new ArrayList<>();
        newParams.add(Configuration.TAINT_TAG_OBJ_CLASS);
        LinkedList<Class<?>> wrapped = new LinkedList<>();
        for(Class<?> c : (Class[]) params.val) {
            Type t = Type.getType(c);
            if(t.getSort() == Type.ARRAY) {
                newParams.add(MultiDTaintedArray.getUnderlyingBoxClassForUnderlyingClass(c));
                newParams.add(Configuration.TAINT_TAG_OBJ_CLASS);
                if(isWrappedErasedType(c)) {
                    wrapped.add(c);
                }
                continue;
            }
            newParams.add(c);
            newParams.add(Configuration.TAINT_TAG_OBJ_CLASS);
        }
        if(implicitTracking) {
            newParams.add(ControlFlowStack.class);
        }
        newParams.addAll(wrapped);
        Class[] ret = new Class[newParams.size()];
        newParams.toArray(ret);
        return new LazyReferenceArrayObjTags(ret);
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_DECLARED_METHOD)
    public static TaintedReferenceWithObjTag getDeclaredMethod(Class<?> czz, Taint czzTaint, String name, Taint nameTaint, LazyReferenceArrayObjTags params, Taint paramsTaint, TaintedReferenceWithObjTag ret, Class[] unused) throws NoSuchMethodException {
        ret.taint = Taint.emptyTaint();
        ret.val = checkForSyntheticObjectMethod(czz.getDeclaredMethod(name, (params == null ? null : (Class[]) params.val)), true);
        return ret;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_METHOD)
    public static TaintedReferenceWithObjTag getMethod(Class<?> czz, Taint czzTaint, String name, Taint nameTaint, LazyReferenceArrayObjTags params, Taint paramsTaint, TaintedReferenceWithObjTag ret, Class[] unused) throws NoSuchMethodException {
        ret.taint = Taint.emptyTaint();
        ret.val = checkForSyntheticObjectMethod(czz.getMethod(name, (params == null ? null : (Class[]) params.val)), false);
        return ret;
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

    /* Returns true if the specified member was declared in a class ignored by Phosphor. */
    private static boolean declaredInIgnoredClass(Member member) {
        return member != null && member.getDeclaringClass() != null && isIgnoredClass(member.getDeclaringClass());
    }

    /* Returns true if the specified class was ignored by Phosphor. */
    private static boolean isIgnoredClass(Class<?> clazz) {
        return clazz != null && (Instrumenter.isIgnoredClass(clazz.getName().replace('.', '/'))
                || Object.class.equals(clazz));
    }

    private static boolean isPrimitiveOrPrimitiveArray(Class<?> c) {
        return c.isArray() ? isPrimitiveOrPrimitiveArray(c.getComponentType()) : c.isPrimitive();
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = FIX_ALL_ARGS_CONSTRUCTOR)
    public static MethodInvoke fixAllArgs(Constructor c, Taint cTaint, LazyReferenceArrayObjTags in, Taint argstaint, TaintedReferenceWithObjTag prealloc, Object[] unused) {
        return fixAllArgs(c, cTaint, in, argstaint, prealloc, false, null, unused);
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = FIX_ALL_ARGS_CONSTRUCTOR_CONTROL)
    public static MethodInvoke fixAllArgs(Constructor c, Taint cTaint, LazyReferenceArrayObjTags in, Taint argstaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag prealloc, Object[] unused) {
        ctrl = ctrl.copyTop();
        return fixAllArgs(c, cTaint, in, argstaint, prealloc, true, ctrl, unused);
    }

    private static MethodInvoke fixAllArgs(Constructor c, Taint cTaint, LazyReferenceArrayObjTags in, Taint argstaint, TaintedReferenceWithObjTag prealloc, boolean implicitTracking,
                                           ControlFlowStack ctrl, Object[] unused) {
        String cName = c.getDeclaringClass().getName();
        if(c!= null && c.getParameterTypes() != null && System.out != null && (c.getParameterTypes().length == 0 || Taint.class != c.getParameterTypes()[0])){
            //This is not the tainted constructor!
            if (!declaredInIgnoredClass(c)) // && !c.getDeclaringClass().getName().contains("Lambda"))
            {
                Constructor newC = getTaintConstructor(c, ctrl != null);
                if(newC != null){
                    newC.setAccessible(true);
                    c = newC;
                }
            }

        }
        MethodInvoke ret = new MethodInvoke();
        ret.c = c;
        ret.c_taint = cTaint;
        ret.a_taint = argstaint;
        ret.prealloc = prealloc;
        ret.a = in;
        if(declaredInIgnoredClass(c)) {
            if(in != null) {
                ret.a.val = getOriginalParams(c.getParameterTypes(), in.val);
            }
            return ret;
        }
        if(c == null) {
            return ret;
        } else if(in == null || in.val == null || c.getParameterTypes().length != in.val.length) {
            ret.a = new LazyReferenceArrayObjTags(new Object[c.getParameterTypes().length]);
            fillInParams(ret.a, in, c.getParameterTypes());
            if(implicitTracking && ret.a.val.length > 0) {
                ret.a.val[ret.a.val.length - 1] = ctrl;
            }
            return ret;
        } else if(in == null && c.getParameterTypes().length == 1) {
            ret.a = new LazyReferenceArrayObjTags(new Object[1]);
            ret.a.val[0] = Taint.emptyTaint();
            return ret;
        } else if(in == null && c.getParameterTypes().length == 2) {
            ret.a = new LazyReferenceArrayObjTags(new Object[2]);
            ret.a.val[0] = Taint.emptyTaint();
            ret.a.val[1] = implicitTracking ? ctrl : Configuration.controlFlowManager.getStack(false);
            return ret;
        }
        return ret;
    }

    /**
     * Returns an array of objects derived from the specified array of tainted parameters that match the specified
     * arrayof types.
     */
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

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = FIX_ALL_ARGS_METHOD)
    public static MethodInvoke fixAllArgs(Method m, Taint mTaint, Object owner, Taint ownerTaint, LazyReferenceArrayObjTags in, Taint inTaint, TaintedReferenceWithObjTag prealloc, Object[] unused) {
        MethodInvoke ret = new MethodInvoke();
        ret.m_taint = mTaint;
        ret.o_taint = ownerTaint;
        ret.a_taint = inTaint;
        ret.prealloc = prealloc;
        if(m == null || declaredInIgnoredClass(m)) {
            ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
        }
        m.setAccessible(true);
        if((!isMarked(m)) && !"java.lang.Object".equals(m.getDeclaringClass().getName())) {
            m = getTaintMethod(m, false);
        }
        m.setAccessible(true);
        ret.o = owner;
        ret.m = m;
        if(in == null || m.getParameterTypes().length != in.val.length) {
            ret.a = new LazyReferenceArrayObjTags(new Object[ret.m.getParameterTypes().length]);
        } else {
            ret.a = in;
        }
        int j = fillInParams(ret.a, in, ret.m.getParameterTypes());

        final Class returnType = m.getReturnType();
        if(TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
            try {
                ret.a.val[j] = returnType.newInstance();
            } catch(InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * The instrumentation may add calls to this method.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = FIX_ALL_ARGS_METHOD_CONTROL)
    public static MethodInvoke fixAllArgs(Method m, Taint mTaint, Object owner, Taint ownerTaint, LazyReferenceArrayObjTags in, Taint inTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag prealloc, Object[] unused) {
        MethodInvoke ret = new MethodInvoke();
        ret.m_taint = mTaint;
        ret.o_taint = ownerTaint;
        ret.a_taint = inTaint;
        ret.prealloc = prealloc;
        if(m == null || declaredInIgnoredClass(m)) {
            ret.a = in;
            ret.o = owner;
            ret.m = m;
            return ret;
        }
        m.setAccessible(true);
        if((!isMarked(m)) && !"java.lang.Object".equals(m.getDeclaringClass().getName())) {
            m = getTaintMethod(m, true);
        }
        m.setAccessible(true);
        ret.o = owner;
        ret.m = m;
        if(in == null || m.getParameterTypes().length != in.val.length) {
            ret.a = new LazyReferenceArrayObjTags(new Object[ret.m.getParameterTypes().length]);
        } else {
            ret.a = in;
        }
        int j = fillInParams(ret.a, in, ret.m.getParameterTypes());

        if(ret.a.val.length > 0) {
            ret.a.val[j] = ctrl;
            j++;
        }

        final Class returnType = m.getReturnType();
        if(TaintedPrimitiveWithObjTag.class.isAssignableFrom(returnType)) {
            try {
                ret.a.val[j] = returnType.newInstance();
            } catch(InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * Adds arguments to the target argument array from the specified array of provided arguments based on the
     * specified expected parameter types. Returns the number of arguments added.
     */
    private static int fillInParams(LazyReferenceArrayObjTags targetArgs, LazyReferenceArrayObjTags providedArgs, Class<?>[] paramTypes) {
        int targetParamIndex = 0;
        if(paramTypes.length > 0 && paramTypes[0] == Configuration.TAINT_TAG_OBJ_CLASS) {
            targetArgs.val[targetParamIndex] = Taint.emptyTaint();
            targetParamIndex++;
        }
        if(providedArgs != null) {
            int idx = 0;
            for(Object providedArg : providedArgs.val) {
                // Class<?> targetParamClass = paramTypes[targetParamIndex];
                targetArgs.val[targetParamIndex++] = providedArg;
                if(providedArgs.taints == null) {
                    targetArgs.val[targetParamIndex++] = Taint.emptyTaint();
                } else {
                    targetArgs.val[targetParamIndex++] = providedArgs.taints[idx];
                }
                idx++;
            }
        }
        return targetParamIndex;
    }

    /**
     * Masks calls to Object.getClass from ObjectOutputStream.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM)
    public static Class<?> getOriginalClassObjectOutputStream(Object obj) {
        // if (obj instanceof LazyArrayObjTags && ((LazyArrayObjTags) obj).taints != null) {
        return obj.getClass();
        // } else {
        //     return getOriginalClass(obj.getClass());
        // }
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_ORIGINAL_METHOD)
    public static Method getOriginalMethod(Method m) {
        if(getCachedMethod(m) != null && isMarked(m)) {
            return getCachedMethod(m);
        }
        return m;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_ORIGINAL_CONSTRUCTOR)
    public static Constructor<?> getOriginalConstructor(Constructor<?> cons) {
        if(declaredInIgnoredClass(cons)) {
            return cons;
        }
        boolean hasSentinel = false;
        for(Class<?> clazz : cons.getParameterTypes()) {
            if(clazz.equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
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

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_ORIGINAL_CLASS)
    public static Class<?> getOriginalClass(Class<?> clazz) {
        if(getCachedClass(clazz) != null) {
            return getCachedClass(clazz);
        } else if(clazz.isArray()) {
            String cmp;
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
                    setCachedClass(clazz, ret);
                    setCachedClass(ret, ret);
                    return ret;
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            setCachedClass(clazz, clazz);
            return clazz;
        } else {
            String cmp = clazz.getName();
            if(cmp.length() >= multiDDescriptorLength
                    && cmp.subSequence(0, multiDDescriptorLength).equals(multiDDescriptor)) {
                String innerType = MultiDTaintedArray.getPrimitiveTypeForWrapper(clazz);
                try {
                    Class ret = Class.forName("[" + innerType);
                    setCachedClass(clazz, ret);
                    setCachedClass(ret, ret);
                    return ret;
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            setCachedClass(clazz, clazz);
            return clazz;
        }
    }

    /**
     * Filters the fields returned by Class.getFields and Class.getDeclaredFields.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_FIELDS)
    public static TaintedReferenceWithObjTag removeTaintedFields(TaintedReferenceWithObjTag _in) {
        Field[] in = (Field[]) ((LazyReferenceArrayObjTags) _in.val).val;
        SinglyLinkedList<Field> ret = new SinglyLinkedList<>();
        boolean removeSVUIDField = containsSVUIDSentinelField(in);
        for(Field f : in) {
            if(!f.getName().equals("taint") && !f.getName().endsWith(TaintUtils.TAINT_FIELD)
                    && !f.getName().endsWith(TaintUtils.TAINT_WRAPPER_FIELD)
                    && !f.getName().startsWith(TaintUtils.PHOSPHOR_ADDED_FIELD_PREFIX)
                    && !(removeSVUIDField && f.getName().equals("serialVersionUID"))) {
                ret.enqueue(f);
            }
        }
        ((LazyReferenceArrayObjTags) _in.val).val = ret.toArray(new Field[ret.size()]);
        return _in;
    }

    /**
     * Returns whether the specified array of fields contains a sentinel field indicating that a SerialVersionUID was
     * added to the class by phosphor.
     */
    private static boolean containsSVUIDSentinelField(Field[] in) {
        for(Field f : in) {
            if(f.getName().equals(TaintUtils.ADDED_SVUID_SENTINEL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filters the methods returns by Class.getDeclaredMethods and Class.getMethods. If declaredOnly is true then
     * synthetic equals and hashCode methods are fully removed from the specified array, otherwise they are replaced with
     * Object.equals and Object.hashCode respectively.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_METHODS)
    public static TaintedReferenceWithObjTag removeTaintedMethods(TaintedReferenceWithObjTag _in, boolean declaredOnly) {
        Method[] in = (Method[]) ((LazyReferenceArrayObjTags) _in.val).val;
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
                if(!matched) {
                    ret.enqueue(f);
                }
            } else if(!match) {
                // Check for synthetic hashCode and equals methods added by Phosphor
                if(f.isSynthetic()) {
                    if(chars.length == 6 && chars[0] == 'e' && chars[1] == 'q' && chars[2] == 'u' && chars[3] == 'a'
                            && chars[4] == 'l' && chars[5] == 's') {
                        if(!declaredOnly) {
                            ret.enqueue(ObjectMethods.EQUALS.method);
                        }
                        continue;
                    } else if(chars.length == 8 && chars[0] == 'h' && chars[1] == 'a' && chars[2] == 's'
                            && chars[3] == 'h' && chars[4] == 'C' && chars[5] == 'o' && chars[6] == 'd'
                            && chars[7] == 'e') {
                        if(!declaredOnly) {
                            ret.enqueue(ObjectMethods.HASH_CODE.method);
                        }
                        continue;
                    }
                }
                ret.enqueue(f);
            }
        }
        ((LazyReferenceArrayObjTags) _in.val).val = ret.toArray(new Method[ret.size()]);
        return _in;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_CONSTRUCTORS)
    public static TaintedReferenceWithObjTag removeTaintedConstructors(TaintedReferenceWithObjTag _in) {
        SinglyLinkedList<Constructor<?>> ret = new SinglyLinkedList<>();
        LazyReferenceArrayObjTags ar = (LazyReferenceArrayObjTags) _in.val;
        for(Constructor<?> f : (((Constructor<?>[]) ar.val))) {
            Class<?>[] params = f.getParameterTypes();
            if(params.length == 0 || !(params[0].equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
                ret.enqueue(f);
            }
        }
        ar.val = ret.toArray(new Constructor<?>[ret.size()]);
        return _in;
    }

    @SuppressWarnings({"rawtypes", "unused"})
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_INTERFACES)
    public static TaintedReferenceWithObjTag removeTaintedInterfaces(TaintedReferenceWithObjTag _in) {
        if(_in.val == null) {
            return null;
        }
        boolean found = false;
        Class[] in = (Class[]) ((LazyReferenceArrayObjTags) _in.val).val;
        for(Class aClass : in) {
            if(aClass.equals(TaintedWithObjTag.class)) {
                found = true;
                break;
            }
        }
        if(!found) {
            return _in;
        }
        Class[] ret = new Class[in.length - 1];
        int idx = 0;
        for(Class aClass : in) {
            if(!aClass.equals(TaintedWithObjTag.class)) {
                ret[idx] = aClass;
                idx++;
            }
        }
        ((LazyReferenceArrayObjTags) _in.val).val = ret;
        return _in;
    }

    @SuppressWarnings({"rawtypes", "unused"})
    @InvokedViaInstrumentation(record = REMOVE_EXTRA_STACK_TRACE_ELEMENTS)
    public static TaintedReferenceWithObjTag removeExtraStackTraceElements(TaintedReferenceWithObjTag _in, Class<?> clazz) {
        int depthToCut = 0;
        String toFind = clazz.getName();
        StackTraceElement[] in = (StackTraceElement[]) ((LazyReferenceArrayObjTags) _in.val).val;
        if(in == null) {
            return null;
        }

        for(int i = 0; i < in.length; i++) {
            if(in[i].getClassName().equals(toFind) && !(i + 1 < in.length && in[i + 1].getClassName().equals(toFind))) {
                depthToCut = i + 1;
                break;
            }
        }
        StackTraceElement[] ret = new StackTraceElement[in.length - depthToCut];
        System.arraycopy(in, depthToCut, ret, 0, ret.length);
        ((LazyReferenceArrayObjTags) _in.val).val = ret;
        return _in;
    }

    @InvokedViaInstrumentation(record = ENUM_VALUE_OF)
    public static TaintedReferenceWithObjTag propagateEnumValueOf(TaintedReferenceWithObjTag ret, Taint<?> tag) {
        ret.taint = tag; //TODO also from string?
        return ret;
    }

    private static Method getCachedMethod(Method method) {
        return method.PHOSPHOR_TAGmethod;
    }

    private static void setCachedMethod(Method method, Method valueToCache) {
        method.PHOSPHOR_TAGmethod = valueToCache;
    }

    private static Class<?> getCachedClass(Class<?> clazz) {
        return clazz.PHOSPHOR_TAGclass;
    }

    private static void setCachedClass(Class<?> clazz, Class<?> valueToCache) {
        clazz.PHOSPHOR_TAGclass = valueToCache;
    }

    private static void setMark(Method method, boolean value) {
        method.PHOSPHOR_TAGmarked = value;
    }

    private static boolean isMarked(Method method) {
        return method.PHOSPHOR_TAGmarked;
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
