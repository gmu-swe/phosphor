package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.SignatureReWriter;
import edu.columbia.cs.psl.phosphor.runtime.ArrayHelper;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import sun.misc.VM;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class TaintUtils {

    public static final int RAW_INSN = 201;
    public static final int NO_TAINT_STORE_INSN = 202;
    public static final int IGNORE_EVERYTHING = 203;
    public static final int TRACKED_LOAD = 204;
    public static final int DUP_TAINT_AT_0 = 205;
    public static final int DUP_TAINT_AT_1 = 206;
    public static final int DUP_TAINT_AT_2 = 207;
    public static final int DUP_TAINT_AT_3 = 208;
    public static final int NEVER_AUTOBOX = 209;
    public static final int ALWAYS_AUTOBOX = 210;
    public static final int ALWAYS_BOX_JUMP = 211;
    public static final int ALWAYS_UNBOX_JUMP = 212;
    public static final int IS_TMP_STORE = 213;
    public static final int BRANCH_START = 214;
    public static final int BRANCH_END = 215;
    public static final int FORCE_CTRL_STORE = 216;
    public static final int FORCE_CTRL_STORE_WIDE = 217;
    public static final int EXCEPTION_HANDLER_START = 205;
    public static final int EXCEPTION_HANDLER_END = 206;
    public static final int UNTHROWN_EXCEPTION = 215; //When we are returning but might have otherwise thrown some exception
    public static final int UNTHROWN_EXCEPTION_CHECK = 214; //When we are returning from a method and are covered directly by a "try"
    public static final int FORCE_CTRL_STORE_SFIELD = 217;
    public static final int FOLLOWED_BY_FRAME = 217;
    public static final int CUSTOM_SIGNAL_1 = 218;
    public static final int CUSTOM_SIGNAL_2 = 219;
    public static final int CUSTOM_SIGNAL_3 = 220;
    public static final int LOOP_HEADER = 221;
    // Indicates that tags pushed onto the control taint tag stack for a revisable branch should not to propagate
    // to the next store insn
    public static final int EXCLUDE_REVISABLE_BRANCHES = 222;
    // Marks the start of the scope of a revisable branch edge
    public static final int REVISABLE_BRANCH_START = 223;
    public static final String TAINT_FIELD = "PHOSPHOR_TAG";
    public static final String METHOD_SUFFIX = "$$PHOSPHORTAGGED";
    public static final String PHOSPHOR_ADDED_FIELD_PREFIX = "$$PHOSPHOR_";
    public static final String MARK_FIELD = PHOSPHOR_ADDED_FIELD_PREFIX + "MARK";
    public static final String ADDED_SVUID_SENTINEL = PHOSPHOR_ADDED_FIELD_PREFIX + "REMOVE_SVUID";
    public static final String CLASS_OFFSET_CACHE_ADDED_FIELD = PHOSPHOR_ADDED_FIELD_PREFIX + "OFFSET_CACHE";
    public static final boolean DEBUG_ALL = false;
    public static final boolean DEBUG_DUPSWAP = DEBUG_ALL;
    public static final boolean DEBUG_FRAMES = DEBUG_ALL;
    public static final boolean DEBUG_FIELDS = DEBUG_ALL;
    public static final boolean DEBUG_LOCAL = DEBUG_ALL;
    public static final boolean DEBUG_CALLS = DEBUG_ALL;
    public static final boolean DEBUG_OPT = false;
    public static final String METHOD_SUFFIX_UNINST = "$$PHOSPHORUNTAGGED";
    public static boolean VERIFY_CLASS_GENERATION = false;

    private TaintUtils() {
        // Prevents this class from being instantiated
    }

    public static boolean isPreAllocReturnType(String methodDescriptor) {
        Type retType = Type.getReturnType(methodDescriptor);
        if(retType.getSort() == Type.OBJECT || retType.getSort() == Type.VOID) {
            return false;
        } else {
            return retType.getSort() != Type.ARRAY;
        }
    }

    /* Returns whether the specified class is a taint sentinel type. */
    public static boolean isTaintSentinel(Class<?> clazz) {
        return TaintSentinel.class.equals(clazz);
    }

    /* Returns whether the specified type is for a taint sentinel. */
    public static boolean isTaintSentinel(Type type) {
        return type.equals(Type.getType(TaintSentinel.class));
    }

    /* Returns whether the specified method description contains a taint sentinel. */
    public static boolean containsTaintSentinel(String desc) {
        Type[] types = Type.getArgumentTypes(desc);
        for(Type type : types) {
            if(isTaintSentinel(type)) {
                return true;
            }
        }
        return false;
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
            Taint ret = new Taint();
            for(Taint t : ((Taint[]) obj)) {
                if(t != null) {
                    ret.addDependency(t);
                }
            }
            if(ret.isEmpty()) {
                return null;
            }
            return ret;
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
            return t == null ? null : t.copy();
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

    public static void arraycopy(Object src, Object srcPosTaint, int srcPos, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
        if(!src.getClass().isArray() && !dest.getClass().isArray()) {
            System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
            if(((LazyArrayObjTags) src).taints != null) {
                if(((LazyArrayObjTags) dest).taints == null) {
                    ((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).taints.length];
                }
                System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
            }
        } else if(!dest.getClass().isArray()) {
            System.arraycopy(src, srcPos, ((MultiDTaintedArrayWithObjTag) dest).getVal(), destPos, length);
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    public static void arraycopy(Object src, Object srcPosTaint, int srcPos, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length, ControlTaintTagStack ctrl) {
        if(!src.getClass().isArray() && !dest.getClass().isArray()) {
            System.arraycopy(((LazyArrayObjTags) src).getVal(), srcPos, ((LazyArrayObjTags) dest).getVal(), destPos, length);
            if(((LazyArrayObjTags) src).taints != null) {
                if(((LazyArrayObjTags) dest).taints == null) {
                    ((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).taints.length];
                }
                System.arraycopy(((LazyArrayObjTags) src).taints, srcPos, ((LazyArrayObjTags) dest).taints, destPos, length);
            }
            if(!ctrl.isEmpty()) {
                if(((LazyArrayObjTags) dest).taints == null) {
                    ((LazyArrayObjTags) dest).taints = new Taint[((LazyArrayObjTags) src).getLength()];
                }
                Taint[] taints = ((LazyArrayObjTags) dest).taints;
                for(int i = 0; i < taints.length; i++) {
                    if(taints[i] == null) {
                        taints[i] = ctrl.copyTag();
                    } else {
                        taints[i].addDependency(ctrl.copyTag());
                    }
                }
            }
        } else if(!dest.getClass().isArray()) {
            System.arraycopy(src, srcPos, ((MultiDTaintedArrayWithObjTag) dest).getVal(), destPos, length);
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    public static void arraycopy(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
        if(VM.isBooted$$PHOSPHORTAGGED(new TaintedBooleanWithObjTag()).val) {
            boolean srcTainted = (srcTaint != null && ((LazyArrayObjTags) srcTaint).taints != null);
            boolean dstTainted = (destTaint != null && ((LazyArrayObjTags) destTaint).taints != null);

            if(!srcTainted && !dstTainted) {
                // Fast path: No taints to copy to/from
                return;
            }

            if(!srcTainted && dstTainted) {
                // Source not tainted, reset dest
                for(int i = destPos; i < destPos + length; i++) {
                    ((LazyArrayObjTags) destTaint).taints[i] = null;
                }
            } else {
                // Source tainted, copy taint over
                if(((LazyArrayObjTags) destTaint).taints == null) {
                    ((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
                }
                System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
            }
        }
    }

    public static void arraycopyControlTrack(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length) {
        try {
            System.arraycopy(src, srcPos, dest, destPos, length);
            if(VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null && ((LazyArrayObjTags) srcTaint).taints != null) {
                if(((LazyArrayObjTags) destTaint).taints == null) {
                    ((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
                }
                System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
            }
        } catch(ArrayIndexOutOfBoundsException ex) {
            Taint t = null;
            if(srcPosTaint != null) {
                t = ((Taint) srcPosTaint).copy();
                t.addDependency((Taint) destPosTaint);
                t.addDependency((Taint) lengthTaint);
            } else if(destPosTaint != null) {

                t = ((Taint) destPosTaint).copy();
                t.addDependency((Taint) lengthTaint);
            } else if(lengthTaint != null) {

                t = ((Taint) lengthTaint).copy();
            }
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(t);
            throw ex;
        }
    }

    public static void arraycopyControlTrack(Object srcTaint, Object src, Object srcPosTaint, int srcPos, Object destTaint, Object dest, Object destPosTaint, int destPos, Object lengthTaint, int length, ControlTaintTagStack ctrl) {
        try {
            System.arraycopy(src, srcPos, dest, destPos, length);

            if(VM.isBooted$$PHOSPHORTAGGED(new ControlTaintTagStack(), new TaintedBooleanWithObjTag()).val && srcTaint != null && destTaint != null && ((LazyArrayObjTags) srcTaint).taints != null) {
                if(((LazyArrayObjTags) destTaint).taints == null) {
                    ((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
                }
                System.arraycopy(((LazyArrayObjTags) srcTaint).taints, srcPos, ((LazyArrayObjTags) destTaint).taints, destPos, length);
            }
            if(!ctrl.isEmpty()) {
                if(((LazyArrayObjTags) destTaint).taints == null) {
                    ((LazyArrayObjTags) destTaint).taints = new Taint[Array.getLength(dest)];
                }
                Taint[] taints = ((LazyArrayObjTags) destTaint).taints;
                for(int i = destPos; i < destPos + length; i++) {
                    if(taints[i] == null) {
                        taints[i] = ctrl.copyTag();
                    } else {
                        taints[i].addDependency(ctrl.copyTag());
                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException ex) {
            Taint t = null;
            if(srcPosTaint != null) {
                t = ((Taint) srcPosTaint).copy();
                t.addDependency((Taint) destPosTaint);
                t.addDependency((Taint) lengthTaint);
            } else if(destPosTaint != null) {

                t = ((Taint) destPosTaint).copy();
                t.addDependency((Taint) lengthTaint);
            } else if(lengthTaint != null) {

                t = ((Taint) lengthTaint).copy();
            }
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(t);
            throw ex;
        }
    }

    public static Object getShadowTaintTypeForFrame(Type t) {
        if(t.getSort() == Type.OBJECT || t.getSort() == Type.VOID) {
            return null;
        }
        if(t.getSort() == Type.ARRAY && t.getDimensions() > 1) {
            return null;
        }
        if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
            return MultiDTaintedArray.getTypeForType(t).getInternalName();
        }
        if(t.getSort() == Type.ARRAY) {
            return null;
        }
        return Configuration.TAINT_TAG_STACK_TYPE;
    }

    public static Object getShadowTaintTypeForFrame(String typeDesc) {
        return getShadowTaintTypeForFrame(Type.getType(typeDesc));
    }

    public static String getShadowTaintType(String typeDesc) {
        Type t = Type.getType(typeDesc);
        if(t.getSort() == Type.OBJECT || t.getSort() == Type.VOID) {
            return null;
        }
        if(t.getSort() == Type.ARRAY && t.getDimensions() > 1) {
            return null;
        }
        if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
            return MultiDTaintedArray.getTypeForType(Type.getType(typeDesc)).getDescriptor();
        }
        if(t.getSort() == Type.ARRAY) {
            return null;
        }
        return Configuration.TAINT_TAG_DESC;
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
            case Type.ARRAY:
                switch(originalReturnType.getElementType().getSort()) {
                    case Type.BYTE:
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.DOUBLE:
                    case Type.FLOAT:
                    case Type.INT:
                    case Type.LONG:
                    case Type.SHORT:
                        return MultiDTaintedArrayWithObjTag.getTypeForType(originalReturnType);
                    case Type.OBJECT:
                        return originalReturnType;
                }
            default:
                return originalReturnType;
        }
    }

    public static String remapMethodDesc(String desc) {
        String r = "(";
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            r += Type.getDescriptor(ControlTaintTagStack.class);
        }
        r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        return r;
    }

    public static String remapMethodDescWithoutAddingNewArgs(String desc) {
        String r = "(";
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        return r;
    }

    public static String remapMethodDescAndIncludeReturnHolder(String desc) {
        String r = "(";
        boolean ctrlAdded = !(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING);
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(!ctrlAdded && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                ctrlAdded = true;
                r += Type.getDescriptor(ControlTaintTagStack.class);
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        if(!ctrlAdded) {
            r += Type.getDescriptor(ControlTaintTagStack.class);
        }
        if(isPrimitiveType(Type.getReturnType(desc))) {
            r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        }
        r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        return r;
    }

    public static String remapMethodDescAndIncludeReturnHolderPrimitiveReturnBoxing(String desc) {
        String r = "(";
        boolean ctrlAdded = !(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING);
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(!ctrlAdded && t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                ctrlAdded = true;
                r += Type.getDescriptor(ControlTaintTagStack.class);
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        if(!ctrlAdded) {
            r += Type.getDescriptor(ControlTaintTagStack.class);
        }
        r += ")";
        if(isPrimitiveArrayType(Type.getReturnType(desc))) {
            r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        } else if(isPrimitiveType(Type.getReturnType(desc))) {
            switch(Type.getReturnType(desc).getSort()) {
                case Type.CHAR:
                    r += "Ljava/lang/Character;";
                    break;
                case Type.BOOLEAN:
                    r += "Ljava/lang/Boolean;";
                    break;
                case Type.DOUBLE:
                    r += "Ljava/lang/Double;";
                    break;
                case Type.FLOAT:
                    r += "Ljava/lang/Float;";
                    break;
                case Type.LONG:
                    r += "Ljava/lang/Long;";
                    break;
                case Type.INT:
                    r += "Ljava/lang/Integer;";
                    break;
                case Type.SHORT:
                    r += "Ljava/lang/Short;";
                    break;
                case Type.BYTE:
                    r += "Ljava/lang/Byte;";
            }
        } else {
            r += Type.getReturnType(desc).getDescriptor();
        }
        return r;
    }

    public static String remapMethodDescAndIncludeReturnHolderInit(String desc) {
        String r = "(";
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            r += Type.getDescriptor(ControlTaintTagStack.class);
        }
        r += Type.getDescriptor(TaintSentinel.class);
        if(isPrimitiveType(Type.getReturnType(desc))) {
            r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        }
        r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        return r;
    }

    public static String remapMethodDescAndIncludeReturnHolderNoControlStack(String desc) {
        String r = "(";
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY) {
                if(t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
                    r += getShadowTaintType(t.getDescriptor());
                }
            } else if(t.getSort() != Type.OBJECT) {
                r += getShadowTaintType(t.getDescriptor());
            }
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        if(isPrimitiveType(Type.getReturnType(desc))) {
            r += getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        }
        r += ")" + getContainerReturnType(Type.getReturnType(desc)).getDescriptor();
        return r;
    }

    public static String remapMethodDescForUninst(String desc) {
        String r = "(";
        for(Type t : Type.getArgumentTypes(desc)) {
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() > 1) {
                r += MultiDTaintedArray.getTypeForType(t);
            } else {
                r += t;
            }
        }
        Type ret = Type.getReturnType(desc);
        if(ret.getSort() == Type.ARRAY && ret.getDimensions() > 1 && ret.getElementType().getSort() != Type.OBJECT) {
            r += ")" + MultiDTaintedArray.getTypeForType(ret).getDescriptor();
        } else {
            r += ")" + ret.getDescriptor();
        }
        return r;
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

    public static <T extends Enum<T>> T enumValueOf(Class<T> enumType, String name, ControlTaintTagStack ctrl) {
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

    /* Returns whether the specified opcode is for a return instruction. */
    public static boolean isReturnOpcode(int opcode) {
        switch(opcode) {
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.RETURN:
            case Opcodes.DRETURN:
            case Opcodes.FRETURN:
            case Opcodes.LRETURN:
                return true;
            default:
                return false;
        }
    }

    /* Returns the class instance resulting from removing any phosphor taint wrapping from the specified class. */
    public static Class<?> getUnwrappedClass(Class<?> clazz) {
        try {
            return Class.forName(SourceSinkManager.remapReturnType(Type.getType(clazz)));
        } catch(ClassNotFoundException e) {
            return clazz;
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

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int max(int a, int b, int c) {
        return max(max(a, b), c);
    }

    public static void main(String[] args) {
        List<String> lst = new ArrayList<>();
        System.out.println(remapSignature("(Ljava/util/stream/AbstractPipeline<TP_OUT;TP_OUT;*>;Ljava/util/stream/PipelineHelper<TP_OUT;>;Ljava/util/Spliterator<TP_IN;>;Ljava/util/function/IntFunction<[TP_OUT;>;JJ)V", lst));
    }
}
