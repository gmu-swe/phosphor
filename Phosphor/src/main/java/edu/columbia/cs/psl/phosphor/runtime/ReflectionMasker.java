package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;
import sun.reflect.misc.ConstructorUtil;

import java.lang.reflect.Field;
import java.lang.reflect.*;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class ReflectionMasker {

    private static final boolean IS_KAFFE = false;
    private static final String multiDDescriptor = "edu.columbia.cs.psl.phosphor.struct.Lazy";
    private static final int multiDDescriptorLength = multiDDescriptor.length();
    private static final char[] SET_TAG_METHOD_CHARS = "setPHOSPHOR_TAG".toCharArray();
    private static final int SET_TAG_METHOD_LEN = SET_TAG_METHOD_CHARS.length;

    static {
        System.setSecurityManager(null);
    }

    private ReflectionMasker() {
        // Prevents this class from being instantiated
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = IS_INSTANCE)
    public static boolean isInstance(Class<?> c1, Object o, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.returnTaint = Taint.emptyTaint();
        Object wrappedVersion = phosphorStackFrame.wrappedArgs[0];
        if(wrappedVersion != null){
            return c1.isInstance(wrappedVersion);
        }
        if(o instanceof LazyArrayObjTags && !LazyArrayObjTags.class.isAssignableFrom(c1)) {
            return c1.isInstance(MultiDTaintedArrayWithObjTag.unboxRaw(o));
        } else {
            return c1.isInstance(o);
        }
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

    private static boolean isErasedReturnType(Class c) {
        return (!c.isArray() && !c.isPrimitive()) || isWrappedErasedType(c);
    }

    /* Returns true if the specified class was ignored by Phosphor. */
    private static boolean isIgnoredClass(Class<?> clazz) {
        return clazz != null && (Instrumenter.isIgnoredClass(clazz.getName().replace('.', '/'))
                || Object.class.equals(clazz));
    }

    @InvokedViaInstrumentation(record = FIX_ALL_ARGS)
    public static Object[] fixAllArgs(Object[] args, Object receiver, PhosphorStackFrame phosphorStackFrame){
        boolean isInstanceMethod = false;
        if(receiver != null){
            isInstanceMethod = true;
            Taint thisTaint = phosphorStackFrame.getArgTaint(2);
            phosphorStackFrame.setArgTaint(thisTaint, 0);
        }
        if(args != null) {
            LazyReferenceArrayObjTags argTaints = (LazyReferenceArrayObjTags) phosphorStackFrame.getArgWrapper(2, args);
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof LazyArrayObjTags) {
                    phosphorStackFrame.setArgWrapper(args[i], i);
                    args[i] = MultiDTaintedArray.unbox1DOrNull(args[i]);
                }
                if(argTaints.taints != null) {
                    phosphorStackFrame.setArgTaint(argTaints.taints[i], i + (isInstanceMethod ? 1 : 0));
                }
            }
        }
        return args;
    }

    @InvokedViaInstrumentation(record = FIX_ALL_ARGS_CONSTRUCTOR)
    public static Object[] fixAllArgsConstructor(Object[] args, PhosphorStackFrame phosphorStackFrame){
        if(args != null) {
            LazyReferenceArrayObjTags argTaints = (LazyReferenceArrayObjTags) phosphorStackFrame.getArgWrapper(1, args);
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof LazyArrayObjTags) {
                    phosphorStackFrame.setArgWrapper(args[i], i);
                    args[i] = MultiDTaintedArray.unbox1DOrNull(args[i]);
                }
                if(argTaints.taints != null) {
                    phosphorStackFrame.setArgTaint(argTaints.taints[i], i + 1); //+1 to account for "this" taint
                }
            }
            Taint thisTaint = phosphorStackFrame.getArgTaint(0);
            phosphorStackFrame.setArgTaint(thisTaint, 0);
        }
        return args;
    }

    @InvokedViaInstrumentation(record =  UNWRAP_RETURN)
    public static Object unwrapReturn(Object ret, PhosphorStackFrame phosphorStackFrame){
        if(ret instanceof byte[]){
            return phosphorStackFrame.getReturnWrapper((byte[]) ret);
        } else if(ret instanceof boolean[]){
            return phosphorStackFrame.getReturnWrapper((boolean[]) ret);
        } else if(ret instanceof char[]){
            return phosphorStackFrame.getReturnWrapper((char[]) ret);
        }else if(ret instanceof short[]){
            return phosphorStackFrame.getReturnWrapper((short[]) ret);
        } else if(ret instanceof int[]){
            return phosphorStackFrame.getReturnWrapper((int[]) ret);
        }else if(ret instanceof float[]){
            return phosphorStackFrame.getReturnWrapper((float[]) ret);
        }else if(ret instanceof double[]){
            return phosphorStackFrame.getReturnWrapper((double[]) ret);
        }else if(ret instanceof Object[]){
            return phosphorStackFrame.getReturnWrapper((Object[]) ret);
        }
        return ret;
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
    public static Field[] removeTaintedFields(Field[] in) {
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
        return ret.toArray(new Field[ret.size()]);
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
    public static Method[] removeTaintedMethods(Method[] in) {
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
            if(!match) {
                ret.enqueue(f);
            }
        }
        return ret.toArray(new Method[ret.size()]);
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_CONSTRUCTORS)
    public static Constructor[] removeTaintedConstructors(Constructor[] in) {
        SinglyLinkedList<Constructor<?>> ret = new SinglyLinkedList<>();
        for(Constructor<?> f : in) {
            Class<?>[] params = f.getParameterTypes();
            if(params.length == 0 || !(params[0].equals(Configuration.TAINT_TAG_OBJ_CLASS))) {
                ret.enqueue(f);
            }
        }
        return ret.toArray(new Constructor<?>[ret.size()]);
    }

    @SuppressWarnings({"rawtypes", "unused"})
    @InvokedViaInstrumentation(record = REMOVE_TAINTED_INTERFACES)
    public static Class[] removeTaintedInterfaces(Class[] in) {
        if(in == null) {
            return null;
        }
        boolean found = false;
        for(Class aClass : in) {
            if(aClass.equals(TaintedWithObjTag.class)) {
                found = true;
                break;
            }
        }
        if(!found) {
            return in;
        }
        Class[] ret = new Class[in.length - 1];
        int idx = 0;
        for(Class aClass : in) {
            if(!aClass.equals(TaintedWithObjTag.class)) {
                ret[idx] = aClass;
                idx++;
            }
        }
        return ret;
    }

    @SuppressWarnings({"rawtypes", "unused"})
    @InvokedViaInstrumentation(record = REMOVE_EXTRA_STACK_TRACE_ELEMENTS)
    public static StackTraceElement[] removeExtraStackTraceElements(StackTraceElement[] in, Class<?> clazz) {
        if(in == null) {
            return null;
        }
        int depthToCut = 0;
        String toFind = clazz.getName();

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

    @InvokedViaInstrumentation(record = PREPARE_FOR_CALL_REFLECTIVE)
    public static void prepareForCall(Method m, PhosphorStackFrame phosphorStackFrame){
        int nArgs = m.getParameterTypes().length;
        String desc = Type.getMethodDescriptor(m);
        phosphorStackFrame.intendedNextMethodDebug = m.getName()+desc.substring(0, 1+desc.indexOf(')'));
        phosphorStackFrame.intendedNextMethodFast = PhosphorStackFrame.hashForDesc(phosphorStackFrame.intendedNextMethodDebug);
        Thread.currentThread().phosphorStackFrame = phosphorStackFrame;
    }

    @InvokedViaInstrumentation(record = PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR)
    public static void prepareForCall(Constructor m, PhosphorStackFrame phosphorStackFrame){
        if(!PhosphorStackFrame.isInitialized()){
            return;
        }
        String desc = Type.getConstructorDescriptor(m);
        phosphorStackFrame.intendedNextMethodDebug = "<init>"+desc.substring(0, 1+desc.indexOf(')'));
        phosphorStackFrame.intendedNextMethodFast = PhosphorStackFrame.hashForDesc(phosphorStackFrame.intendedNextMethodDebug);
        Thread.currentThread().phosphorStackFrame = phosphorStackFrame;
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
