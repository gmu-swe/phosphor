package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.SignatureReWriter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;

public class TaintUtils {

    public static final int RAW_INSN = 201;
    public static final int NO_TAINT_STORE_INSN = 202;
    public static final int IGNORE_EVERYTHING = 203;
    public static final int IS_TMP_STORE = 213;
    public static final int FOLLOWED_BY_FRAME = 217;
    public static final String TAINT_FIELD = "PHOSPHOR_TAG";
    public static final String METHOD_SUFFIX = "$$PHOSPHORTAGGED";
    public static final String PHOSPHOR_ADDED_FIELD_PREFIX = "$$PHOSPHOR_";
    public static final String MARK_FIELD = PHOSPHOR_ADDED_FIELD_PREFIX + "MARK";
    public static final String ADDED_SVUID_SENTINEL = PHOSPHOR_ADDED_FIELD_PREFIX + "REMOVE_SVUID";
    public static final String CLASS_OFFSET_CACHE_ADDED_FIELD = PHOSPHOR_ADDED_FIELD_PREFIX + "OFFSET_CACHE";
    public static final String METHOD_SUFFIX_UNINST = "$$PHOSPHORUNTAGGED";
    public static final String TAINT_WRAPPER_FIELD = "PHOSPHOR_WRAPPER";
    public static boolean VERIFY_CLASS_GENERATION = false;

    private TaintUtils() {
        // Prevents this class from being instantiated
    }

    public static boolean isPreAllocReturnType(String methodDescriptor) {
        Type retType = Type.getReturnType(methodDescriptor);
        return retType.getSort() != Type.VOID;
    }

    @InvokedViaInstrumentation(record = GET_TAINT_OBJECT)
    public static Taint getTaintObj(Object obj) {
        if(obj == null || Taint.IGNORE_TAINTING) {
            return null;
        }
        if(obj instanceof TaintedWithObjTag) {
            return (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
        } else if(ArrayHelper.engaged == 1) {
            return ArrayHelper.getTag(obj);
        } else if(obj instanceof Taint[]) {
            return Taint.combineTaintArray((Taint[]) obj);
        } else if(obj instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) obj).lengthTaint;
        }
        return null;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_TAINT_COPY_SIMPLE)
    public static Taint<?> getTaintCopySimple(Object obj) {
        if(obj == null || Taint.IGNORE_TAINTING) {
            return null;
        } else if(obj instanceof TaintedWithObjTag) {
            Taint<?> t = ((Taint<?>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
            return t;
        } else {
            return null;
        }
    }

    public static int[][] create2DTaintArray(Object in, int[][] ar) {
        for(int i = 0; i < Array.getLength(in); i++) {
            Object entry = Array.get(in, i);
            if(entry != null) {
                ar[i] = new int[Array.getLength(entry)];
            }
        }
        return ar;
    }

    public static int[][][] create3DTaintArray(Object in, int[][][] ar) {
        for(int i = 0; i < Array.getLength(in); i++) {
            Object entry = Array.get(in, i);
            if(entry != null) {
                ar[i] = new int[Array.getLength(entry)][];
                for(int j = 0; j < Array.getLength(entry); j++) {
                    Object e = Array.get(entry, j);
                    if(e != null) {
                        ar[i][j] = new int[Array.getLength(e)];
                    }
                }
            }
        }
        return ar;
    }

    public static void generateMultiDTaintArray(Object in, Object taintRef) {
        //Precondition is that taintArrayRef is an array with the same number of dimensions as obj, with each allocated.
        for(int i = 0; i < Array.getLength(in); i++) {
            Object entry = Array.get(in, i);
            Class<?> clazz = entry.getClass();
            if(clazz.isArray()) {
                //Multi-D array
                int innerDims = Array.getLength(entry);
                Array.set(taintRef, i, Array.newInstance(Integer.TYPE, innerDims));
            }
        }
    }

    public static void arraycopy$$PHOSPHORTAGGED(Object src, Taint<?> srctaint, int srcPos, Taint<?> srcPosTaint,
                                                 Object dest, Taint<?> destTaint, int destPos, Taint<?> destPosTaint,
                                                 int length, Taint<?> lengthTaint) {
        if(!src.getClass().isArray() && !dest.getClass().isArray()) {
            System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
            if(((LazyArrayObjTags) src).taints != null) {
                if(((LazyArrayObjTags) dest).taints == null) {
                    ((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) dest).getLength()];
                }
                System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
            }
        } else if(!dest.getClass().isArray()) {
            System.arraycopy(src, srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    public static void arraycopy$$PHOSPHORTAGGED(Object src, Taint<?> srcTaint, int srcPos, Taint<?> srcPosTaint,
                                                 Object dest, Taint<?> destTaint, int destPos, Taint<?> destPosTaint,
                                                 int length, Taint<?> lengthTaint, ControlFlowStack ctrl) {
        if(!src.getClass().isArray() && !dest.getClass().isArray()) {
            System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
            if(((LazyArrayObjTags) src).taints != null) {
                if(((LazyArrayObjTags) dest).taints == null) {
                    ((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) dest).getLength()];
                }
                System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
            }
            if(srcTaint != null && !srcTaint.isEmpty()) {
                LazyArrayObjTags destTags = (LazyArrayObjTags) dest;
                if(destTags.taints == null) {
                    destTags.taints = new Taint[destTags.getLength()];
                }
                for(int i = destPos; i < destPos + length; i++) {
                    destTags.taints[i] = Taint.combineTags(destTags.taints[i], srcTaint);
                }
            }
            if(!ctrl.copyTag().isEmpty()) {
                LazyArrayObjTags destTags = (LazyArrayObjTags) dest;
                if(destTags.taints == null) {
                    destTags.taints = new Taint[destTags.getLength()];
                }
                for(int i = destPos; i < destPos + length; i++) {
                    destTags.taints[i] = Taint.combineTags(destTags.taints[i], ctrl.copyTag());
                }
            }
        } else if(!dest.getClass().isArray()) {
            System.arraycopy(src, srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    public static String getShadowTaintType(String typeDesc) {
        Type t = Type.getType(typeDesc);
        if(isShadowedType(t)) {
            return Configuration.TAINT_TAG_DESC;
        }
        return null;
    }

    public static Type getContainerReturnType(String originalReturnType) {
        return getContainerReturnType(Type.getType(originalReturnType));
    }

    public static Type getContainerReturnType(Type originalReturnType) {
        switch(originalReturnType.getSort()) {
            case Type.BYTE:
                return Type.getType(TaintedByteWithObjTag.class);
            case Type.BOOLEAN:
                return Type.getType(TaintedBooleanWithObjTag.class);
            case Type.CHAR:
                return Type.getType(TaintedCharWithObjTag.class);
            case Type.DOUBLE:
                return Type.getType(TaintedDoubleWithObjTag.class);
            case Type.FLOAT:
                return Type.getType(TaintedFloatWithObjTag.class);
            case Type.INT:
                return Type.getType(TaintedIntWithObjTag.class);
            case Type.LONG:
                return Type.getType(TaintedLongWithObjTag.class);
            case Type.SHORT:
                return Type.getType(TaintedShortWithObjTag.class);
            case Type.OBJECT:
            case Type.ARRAY:
                return Type.getType(TaintedReferenceWithObjTag.class);
            default:
                return originalReturnType;
        }
    }

    public static boolean isShadowedType(Type t) {
        return (Configuration.REFERENCE_TAINTING || (t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT)) && t.getSort() != Type.VOID;
    }

    public static boolean isWrappedType(Type t) {
        return t.getSort() == Type.ARRAY;
    }

    public static boolean isErasedReturnType(Type t){
        return isWrappedTypeWithErasedType(t) || (t.getSort() == Type.OBJECT && !t.getDescriptor().equals("Ledu/columbia/cs/psl/phosphor/struct/TaintedReferenceWithObjTag;"));
    }

    public static boolean isWrappedTypeWithErasedType(Type t) {
        return t.getSort() == Type.ARRAY && (t.getDimensions() > 1 || t.getElementType().getSort() == Type.OBJECT);
    }

    public static boolean isWrapperType(Type t) {
        return t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy");
    }

    public static boolean isWrappedTypeWithSeparateField(Type t) {
        return t.getSort() == Type.ARRAY;
    }

    public static Type getWrapperType(Type t) {
        return MultiDTaintedArray.getTypeForType(t);
    }

    public static Type getUnwrappedType(Type wrappedType) {
        if(wrappedType.getSort() != Type.OBJECT) {
            return wrappedType;
        }
        switch(wrappedType.getDescriptor()) {
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyBooleanArrayObjTags;":
                return Type.getType("[Z");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyByteArrayObjTags;":
                return Type.getType("[B");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyCharArrayObjTags;":
                return Type.getType("[C");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyDoubleArrayObjTags;":
                return Type.getType("[D");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyFloatArrayObjTags;":
                return Type.getType("[F");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyIntArrayObjTags;":
                return Type.getType("[I");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyLongArrayObjTags;":
                return Type.getType("[J");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyShortArrayObjTags;":
                return Type.getType("[S");
            case "Ledu/columbia/cs/psl/phosphor/struct/LazyReferenceArrayObjTags;":
                return Type.getType("[Ljava/lang/Object;");
            default:
                return wrappedType;
        }
    }

    public static String remapMethodDesc(boolean isVirtual, String desc) {
        StringBuilder ret = new StringBuilder();
        ret.append('(');
        StringBuilder wrapped = new StringBuilder();
        if(isVirtual) {
            ret.append(Configuration.TAINT_TAG_DESC);
        }
        for(Type t : Type.getArgumentTypes(desc)) {
            if(isWrappedType(t)) {
                ret.append(getWrapperType(t));
            } else {
                ret.append(t);
            }
            if(isWrappedTypeWithErasedType(t)) {
                wrapped.append(t.getDescriptor());
            }
            if(isShadowedType(t)) {
                ret.append(Configuration.TAINT_TAG_DESC);
            }
        }
        Type returnType = Type.getReturnType(desc);
        if(isErasedReturnType(returnType)){
            wrapped.append(returnType.getDescriptor());
        }
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            ret.append(CONTROL_STACK_DESC);
        }
        ret.append(wrapped);
        ret.append(')');
        ret.append(getContainerReturnType(returnType).getDescriptor());
        return ret.toString();
    }

    public static String remapMethodDescAndIncludeReturnHolder(boolean isVirtual, String desc) {
        return remapMethodDescAndIncludeReturnHolder(isVirtual, desc, true, true);
    }

    public static String remapMethodDescAndIncludeReturnHolder(boolean isVirtual, String desc, boolean addErasedReturnType, boolean addErasedParamTypes) {
        return remapMethodDescAndIncludeReturnHolder(isVirtual ? 0 : -1, desc, addErasedReturnType, addErasedParamTypes);
    }

    public static String remapMethodDescAndIncludeReturnHolder(int addTaintAtPos, String desc, boolean addErasedReturnType, boolean addErasedParamTypes) {
        StringBuilder ret = new StringBuilder();
        ret.append('(');
        StringBuilder erasedTypes = new StringBuilder();
        if(addTaintAtPos == 0) {
            ret.append(Configuration.TAINT_TAG_DESC);
        }
        boolean ctrlAdded = !(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING);
        int pos = 0;
        for(Type t : Type.getArgumentTypes(desc)) {
            if(!ctrlAdded && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                ctrlAdded = true;
                ret.append(CONTROL_STACK_DESC);
            }
            if(isWrappedType(t)) {
                ret.append(getWrapperType(t));
            } else {
                ret.append(t);
            }
            if(isWrappedTypeWithErasedType(t)) {
                erasedTypes.append(t.getDescriptor());
            }
            if(isShadowedType(t)) {
                ret.append(Configuration.TAINT_TAG_DESC);
            }
            pos++;
            if(pos == addTaintAtPos && pos > 0){
                if(addErasedParamTypes) {
                    ret.append(erasedTypes.toString());
                    erasedTypes = new StringBuilder();
                }
                ret.append(Configuration.TAINT_TAG_DESC);
            }
        }
        Type returnType = Type.getReturnType(desc);

        if(!ctrlAdded) {
            ret.append(CONTROL_STACK_DESC);
        }
        if (returnType.getSort() != Type.VOID) {
            ret.append(getContainerReturnType(returnType).getDescriptor());
        }
        if(addErasedParamTypes) {
            ret.append(erasedTypes);
        }
        if(addErasedReturnType && isErasedReturnType(returnType)){
            ret.append(returnType.getDescriptor());
        }
        ret.append(')');
        ret.append(getContainerReturnType(returnType).getDescriptor());
        return ret.toString();
    }

    public static String remapMethodDescAndIncludeReturnHolderNoControlStack(boolean isVirtual, String desc, boolean addErasedTypes) {
        StringBuilder ret = new StringBuilder();
        ret.append('(');
        StringBuilder wrapped = new StringBuilder();
        if(isVirtual) {
            ret.append(Configuration.TAINT_TAG_DESC);
        }
        for(Type t : Type.getArgumentTypes(desc)) {
            if(isWrappedType(t)) {
                ret.append(getWrapperType(t));
            } else {
                ret.append(t);
            }
            if(isWrappedTypeWithErasedType(t)) {
                wrapped.append(t.getDescriptor());
            }
            if(isShadowedType(t)) {
                ret.append(Configuration.TAINT_TAG_DESC);
            }
        }
        Type returnType = Type.getReturnType(desc);
        if(isErasedReturnType(returnType)){
            wrapped.append(returnType.getDescriptor());
        }
        if(returnType.getSort() != Type.VOID) {
            ret.append(getContainerReturnType(returnType).getDescriptor());
        }
        if(addErasedTypes) {
            ret.append(wrapped);
        }
        ret.append(')');
        ret.append(getContainerReturnType(returnType).getDescriptor());
        return ret.toString();
    }

    public static Object getStackTypeForType(Type t) {
        if(t == null) {
            return Opcodes.NULL;
        }
        switch(t.getSort()) {
            case Type.ARRAY:
            case Type.OBJECT:
                return t.getInternalName();
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.INTEGER;
            case Type.DOUBLE:
                return Opcodes.DOUBLE;
            case Type.FLOAT:
                return Opcodes.FLOAT;
            case Type.LONG:
                return Opcodes.LONG;

            default:
                throw new IllegalArgumentException("Got: " + t);
        }
    }

    public static Object[] newTaintArray(int len) {
        return (Object[]) Array.newInstance(Configuration.TAINT_TAG_OBJ_CLASS, len);
    }

    private static <T> T shallowClone(T obj) {
        try {
            Method m = obj.getClass().getDeclaredMethod("clone");
            m.setAccessible(true);
            return (T) m.invoke(obj);
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name) {
        T ret = Enum.valueOf(enumType, name);
        if(((Object) name) instanceof TaintedWithObjTag) {
            Object tag = ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
            if(tag != null) {
                ret = shallowClone(ret);
                ((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
            }
        }
        return ret;
    }

    public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name, ControlFlowStack ctrl) {
        T ret = Enum.valueOf(enumType, name);
        Taint tag = (Taint) ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
        tag = Taint.combineTags(tag, ctrl);
        if(tag != null && !tag.isEmpty()) {
            ret = shallowClone(ret);
            ((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = ENSURE_UNBOXED)
    public static Object ensureUnboxed(Object o) {
        if(o instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) o).getVal();
        } else if(Configuration.WITH_ENUM_BY_VAL && o instanceof Enum<?>) {
            return Enum.valueOf(((Enum) o).getDeclaringClass(), ((Enum) o).name());
        }
        return o;
    }

    public static boolean isPrimitiveArrayType(Type t) {
        return t != null && t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT;
    }

    public static boolean isPrimitiveType(Type t) {
        return t != null && t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT && t.getSort() != Type.VOID;
    }

    public static boolean isPrimitiveOrPrimitiveArrayType(Type t) {
        return isPrimitiveArrayType(t) || isPrimitiveType(t);
    }

    public static String remapSignature(String sig, final List<String> extraArgs) {
        if(sig == null) {
            return null;
        }
        SignatureReWriter sw = new SignatureReWriter() {
            int isInArray = 0;
            boolean isInReturnType;
            boolean isInParam;

            @Override
            public SignatureVisitor visitArrayType() {
                isInArray++;
                return super.visitArrayType();
            }

            @Override
            public void visitBaseType(char descriptor) {
                if(descriptor == 'V') {
                    super.visitBaseType(descriptor);
                    return;
                }
                if(isInParam) {
                    if(isInArray == 0) {
                        super.visitClassType(Configuration.TAINT_TAG_INTERNAL_NAME);
                        super.visitEnd();
                        super.visitParameterType();
                        super.visitBaseType(descriptor);
                    } else if(isInArray == 1) {
                        super.pop();
                        super.visitClassType(MultiDTaintedArray.getTypeForType(Type.getType("[" + descriptor)).getInternalName());
                        super.visitEnd();
                        super.visitArrayType();
                        super.visitParameterType();
                        super.visitBaseType(descriptor);
                    } else {
                        super.pop();
                        super.visitClassType(MultiDTaintedArray.getTypeForType(Type.getType("[" + descriptor)).getInternalName());
                        super.visitEnd();
                    }
                } else {
                    if(isInArray > 0) {
                        super.pop();//reduce dimensions by 1
                        super.visitClassType(TaintUtils.getContainerReturnType("[" + descriptor).getInternalName());
                        super.visitEnd();
                    } else {
                        super.visitClassType(TaintUtils.getContainerReturnType("" + descriptor).getInternalName());
                        super.visitEnd();
                    }
                }
                isInParam = false;
                isInArray = 0;
            }

            @Override
            public SignatureVisitor visitReturnType() {
                //Add in extra stuff as needed.
                for(String s : extraArgs) {
                    super.visitParameterType();
                    super.visitClassType(s);
                    super.visitEnd();
                }
                isInReturnType = true;
                return super.visitReturnType();
            }

            @Override
            public void visitTypeVariable(String name) {
                isInParam = false;
                isInArray = 0;
                super.visitTypeVariable(name);
            }

            @Override
            public void visitClassType(String name) {
                isInArray = 0;
                isInParam = false;
                super.visitClassType(name);
            }

            @Override
            public SignatureVisitor visitParameterType() {
                isInParam = true;
                return super.visitParameterType();
            }
        };
        SignatureReader sr = new SignatureReader(sig);
        sr.accept(sw);
        sig = sw.toString();
        return sig;
    }

    /* Returns the class instance resulting from removing any phosphor taint wrapping from the specified class. */
    public static Class<?> getUnwrappedClass(Class<?> wrappedClass) {
        if(wrappedClass.equals(LazyBooleanArrayObjTags.class)) {
            return boolean[].class;
        } else if(wrappedClass.equals(LazyByteArrayObjTags.class)) {
            return byte[].class;
        } else if(wrappedClass.equals(LazyCharArrayObjTags.class)) {
            return char[].class;
        } else if(wrappedClass.equals(LazyDoubleArrayObjTags.class)) {
            return double[].class;
        } else if(wrappedClass.equals(LazyFloatArrayObjTags.class)) {
            return float[].class;
        } else if(wrappedClass.equals(LazyIntArrayObjTags.class)) {
            return int[].class;
        } else if(wrappedClass.equals(LazyLongArrayObjTags.class)) {
            return long[].class;
        } else if(wrappedClass.equals(LazyReferenceArrayObjTags.class)) {
            return Object[].class;
        } else if(wrappedClass.equals(LazyShortArrayObjTags.class)) {
            return short[].class;
        } else if(wrappedClass.equals(TaintedBooleanWithObjTag.class)) {
            return boolean.class;
        } else if(wrappedClass.equals(TaintedByteWithObjTag.class)) {
            return byte.class;
        } else if(wrappedClass.equals(TaintedCharWithObjTag.class)) {
            return char.class;
        } else if(wrappedClass.equals(TaintedDoubleWithObjTag.class)) {
            return double.class;
        } else if(wrappedClass.equals(TaintedFloatWithObjTag.class)) {
            return float.class;
        } else if(wrappedClass.equals(TaintedIntWithObjTag.class)) {
            return int.class;
        } else if(wrappedClass.equals(TaintedLongWithObjTag.class)) {
            return long.class;
        } else if(wrappedClass.equals(TaintedReferenceWithObjTag.class)) {
            return Object.class;
        } else if(wrappedClass.equals(TaintedShortWithObjTag.class)) {
            return short.class;
        } else {
            return wrappedClass;
        }
    }

    /* Returns whether the specified type is a primitive wrapper type. */
    public static boolean isTaintedPrimitiveType(Type type) {
        if(type == null) {
            return false;
        } else {
            return type.equals(Type.getType(TaintedByteWithObjTag.class)) || type.equals(Type.getType(TaintedBooleanWithObjTag.class))
                    || type.equals(Type.getType(TaintedCharWithObjTag.class)) || type.equals(Type.getType(TaintedDoubleWithObjTag.class))
                    || type.equals(Type.getType(TaintedFloatWithObjTag.class)) || type.equals(Type.getType(TaintedIntWithObjTag.class))
                    || type.equals(Type.getType(TaintedLongWithObjTag.class)) || type.equals(Type.getType(TaintedShortWithObjTag.class));
        }
    }

    public static boolean containsTaint(String desc) {
        return desc.contains(Configuration.TAINT_TAG_DESC);
    }

    /**
     * Constructs and returns the bytecode method signature from the specified pieces;
     * removes any phosphor-added suffixes and tainted types from the signature.
     */
    public static String getOriginalMethodSignatureWithoutReturn(String owner, String name, String desc) {
        if (name.endsWith(METHOD_SUFFIX) || containsTaint(desc)) {
            return owner + "." + name.replace(METHOD_SUFFIX, "") + getOriginalMethodDescWithoutReturn(desc);
        } else {
            return owner + "." + name + desc;
        }
    }

    public static String getOriginalMethodDescWithoutReturn(String desc) {
        StringBuilder builder = new StringBuilder("(");
        Type[] args = Type.getArgumentTypes(desc);
        Type returnType = Type.getReturnType(desc);
        int lastArg = args.length;
        if (returnType.equals(Type.getType(TaintedReferenceWithObjTag.class))) {
            lastArg--;
        }
        for (int i = 0; i < lastArg; i++) {
            Type arg = args[i];
            String argDesc = arg.getDescriptor();
            if(argDesc.equals(CONTROL_STACK_DESC) || argDesc.equals(Configuration.TAINT_TAG_DESC) ||
                arg.equals(Type.getType(LazyReferenceArrayObjTags.class)) || isTaintedPrimitiveType(arg) ||
                arg.equals(Type.getType(TaintedReferenceWithObjTag.class))) {
                continue;
            }
            if (argDesc.startsWith("Ledu/columbia/cs/psl/phosphor/struct/multid")
                        || argDesc.startsWith("Ledu/columbia/cs/psl/phosphor/struct")) {
                arg = getUnwrappedType(arg);
            }
           builder.append(arg.getDescriptor());
        }
        return builder.append(")").toString(); //  + getOriginalMethodReturnTypeDesc(desc);
    }

    public static String getOriginalMethodReturnTypeDesc(String desc) {
        Type returnType = Type.getReturnType(desc);
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            if (returnType.getInternalName().equals(Type.getInternalName(TaintedByteWithObjTag.class))) {
                return "B";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyByteArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyByteArrayObjTags.class),
                        "[B");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedBooleanWithObjTag.class))) {
                return "Z";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyBooleanArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyBooleanArrayObjTags.class),
                        "[Z");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedCharWithObjTag.class))) {
                return "C";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyCharArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyCharArrayObjTags.class),
                        "[C");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedDoubleWithObjTag.class))) {
                return "D";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyDoubleArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyDoubleArrayObjTags.class),
                        "[D");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedIntWithObjTag.class))) {
                return "I";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyIntArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyIntArrayObjTags.class),
                        "[I");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedFloatWithObjTag.class))) {
                return "F";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyFloatArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyFloatArrayObjTags.class),
                        "[F");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedLongWithObjTag.class))) {
                return "J";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyLongArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyLongArrayObjTags.class),
                        "[J");
            } else if (returnType.getInternalName().equals(Type.getInternalName(TaintedShortWithObjTag.class))) {
                return "S";
            } else if (returnType.getDescriptor().contains(Type.getDescriptor(LazyShortArrayObjTags.class))) {
                return returnType.getDescriptor().replace(Type.getDescriptor(LazyShortArrayObjTags.class),
                        "[S");
            } else if (returnType.getDescriptor().equals(Type.getDescriptor(TaintedReferenceWithObjTag.class))) {
                Type[] args = Type.getArgumentTypes(desc);
                return args[args.length - 1].getDescriptor();
            }
        }
        return returnType.getDescriptor();
    }
}
