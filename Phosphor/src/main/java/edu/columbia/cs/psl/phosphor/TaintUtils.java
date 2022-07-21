package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaggedArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

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
        if (obj == null || Taint.IGNORE_TAINTING) {
            return null;
        }
        if (obj instanceof TaintedWithObjTag) {
            return (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
        } else if (ArrayHelper.engaged == 1) {
            return ArrayHelper.getTag(obj);
        } else if (obj instanceof Taint[]) {
            return Taint.combineTaintArray((Taint[]) obj);
        } else if (obj instanceof TaggedArray) {
            return ((TaggedArray) obj).lengthTaint;
        }
        return null;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = GET_TAINT_COPY_SIMPLE)
    public static Taint<?> getTaintCopySimple(Object obj) {
        if (obj == null || Taint.IGNORE_TAINTING) {
            return null;
        } else if (obj instanceof TaintedWithObjTag) {
            Taint<?> t = ((Taint<?>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG());
            return t;
        } else {
            return null;
        }
    }

    public static int[][] create2DTaintArray(Object in, int[][] ar) {
        for (int i = 0; i < Array.getLength(in); i++) {
            Object entry = Array.get(in, i);
            if (entry != null) {
                ar[i] = new int[Array.getLength(entry)];
            }
        }
        return ar;
    }

    public static int[][][] create3DTaintArray(Object in, int[][][] ar) {
        for (int i = 0; i < Array.getLength(in); i++) {
            Object entry = Array.get(in, i);
            if (entry != null) {
                ar[i] = new int[Array.getLength(entry)][];
                for (int j = 0; j < Array.getLength(entry); j++) {
                    Object e = Array.get(entry, j);
                    if (e != null) {
                        ar[i][j] = new int[Array.getLength(e)];
                    }
                }
            }
        }
        return ar;
    }

    public static void arraycopy(Object src, int srcPos,
            Object dest, int destPos,
            int length) {
        Object srcArray = src instanceof TaggedArray ? ((TaggedArray) src).getVal() : src;
        Object destArray = dest instanceof TaggedArray ? ((TaggedArray) dest).getVal() : dest;
        System.arraycopy(srcArray, srcPos, destArray, destPos, length);
        if (src instanceof TaggedArray && dest instanceof TaggedArray) {
            TaggedArray destArr = (TaggedArray) dest;
            TaggedArray srcArr = (TaggedArray) src;
            if (srcArr.taints != null) {
                if (destArr.taints == null) {
                    destArr.taints = new Taint[destArr.getLength()];
                }
                System.arraycopy(srcArr.taints, srcPos, destArr.taints, destPos, length);
            }
        }
    }

    public static String getShadowTaintType(String typeDesc) {
        Type t = Type.getType(typeDesc);
        if (isShadowedType(t)) {
            return Configuration.TAINT_TAG_DESC;
        }
        return null;
    }

    public static boolean isShadowedType(Type t) {
        return (Configuration.REFERENCE_TAINTING || (t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT))
                && t.getSort() != Type.VOID;
    }

    public static boolean isWrappedType(Type t) {
        return t.getSort() == Type.ARRAY;
    }

    public static boolean isErasedReturnType(Type t) {
        return isWrappedTypeWithErasedType(t) || (t.getSort() == Type.OBJECT
                && !t.getDescriptor().equals("Ledu/columbia/cs/psl/phosphor/struct/TaintedReferenceWithObjTag;"));
    }

    public static boolean isWrappedTypeWithErasedType(Type t) {
        return t.getSort() == Type.ARRAY && (t.getDimensions() > 1 || t.getElementType().getSort() == Type.OBJECT);
    }

    public static boolean isWrapperType(Type t) {
        return t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tagged");
    }

    public static boolean isWrappedTypeWithSeparateField(Type t) {
        return t.getSort() == Type.ARRAY;
    }

    public static Type getWrapperType(Type t) {
        return MultiDArrayUtils.getTypeForType(t);
    }

    public static Type getUnwrappedType(Type wrappedType) {
        if (wrappedType.getSort() != Type.OBJECT) {
            return wrappedType;
        }
        switch (wrappedType.getDescriptor()) {
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedBooleanArray;":
                return Type.getType("[Z");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedByteArray;":
                return Type.getType("[B");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedCharArray;":
                return Type.getType("[C");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedDoubleArray;":
                return Type.getType("[D");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedFloatArray;":
                return Type.getType("[F");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedIntArray;":
                return Type.getType("[I");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedLongArray;":
                return Type.getType("[J");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedShortArray;":
                return Type.getType("[S");
            case "Ledu/columbia/cs/psl/phosphor/struct/TaggedReferenceArray;":
                return Type.getType("[Ljava/lang/Object;");
            default:
                return wrappedType;
        }
    }

    public static Object getStackTypeForType(Type t) {
        if (t == null) {
            return Opcodes.NULL;
        }
        switch (t.getSort()) {
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

    private static <T> T shallowClone(T obj) {
        try {
            Method m = obj.getClass().getDeclaredMethod("clone");
            m.setAccessible(true);
            return (T) m.invoke(obj);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name) {
        T ret = Enum.valueOf(enumType, name);
        if (((Object) name) instanceof TaintedWithObjTag) {
            Object tag = ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
            if (tag != null) {
                ret = shallowClone(ret);
                ((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
            }
        }
        return ret;
    }

    public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name, PhosphorStackFrame ctrl) {
        T ret = Enum.valueOf(enumType, name);
        Taint tag = (Taint) ((TaintedWithObjTag) ((Object) name)).getPHOSPHOR_TAG();
        tag = Taint.combineTags(tag, ctrl);
        if (tag != null && !tag.isEmpty()) {
            ret = shallowClone(ret);
            ((TaintedWithObjTag) ret).setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = ENSURE_UNBOXED)
    public static Object ensureUnboxed(Object o) {
        if (o instanceof TaggedArray) {
            return ((TaggedArray) o).getVal();
        } else if (Configuration.WITH_ENUM_BY_VAL && o instanceof Enum<?>) {
            return Enum.valueOf(((Enum) o).getDeclaringClass(), ((Enum) o).name());
        }
        return o;
    }

    public static boolean isPrimitiveArrayType(Type t) {
        return t != null && t.getSort() == Type.ARRAY && t.getDimensions() == 1
                && t.getElementType().getSort() != Type.OBJECT;
    }

    public static boolean isPrimitiveType(Type t) {
        return t != null && t.getSort() != Type.ARRAY && t.getSort() != Type.OBJECT && t.getSort() != Type.VOID;
    }

    public static boolean isPrimitiveOrPrimitiveArrayType(Type t) {
        return isPrimitiveArrayType(t) || isPrimitiveType(t);
    }

}
