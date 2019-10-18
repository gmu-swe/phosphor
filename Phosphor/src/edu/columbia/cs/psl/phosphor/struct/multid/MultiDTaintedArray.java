package edu.columbia.cs.psl.phosphor.struct.multid;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import org.objectweb.asm.Type;
import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static org.objectweb.asm.Opcodes.*;

public abstract class MultiDTaintedArray {

    public static CK_ATTRIBUTE[] unboxCK_ATTRIBUTE(CK_ATTRIBUTE[] in) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if(in == null || in[0] == null) {
            return null;
        }
        boolean needsFix = false;
        Field f = in[0].getClass().getDeclaredField("pValue");
        for(Object a : in) {
            Object v = f.get(a);
            if(v instanceof LazyArrayObjTags) {
                f.set(a, MultiDTaintedArrayWithObjTag.unboxRaw(v));
            }
        }
        return in;
    }

    public static Object unbox1D(final Object in) {
        if(in instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) in).getVal();
        }
        return in;
    }

    /* If the specified object is a one dimensional array of primitives, boxes and returns the specified object. Otherwise
     * returns the specified object. */
    public static Object boxOnly1D(final Object obj) {
        return MultiDTaintedArrayWithObjTag.boxOnly1D(obj);
    }

    public static Object maybeUnbox(final Object in) {
        if(in == null) {
            return null;
        }
        if(null != isPrimitiveBoxClass(in.getClass())) {
            return unboxRaw(in);
        }
        return in;
    }

    public static Type getTypeForType(final Type originalElementType) {
        return MultiDTaintedArrayWithObjTag.getTypeForType(originalElementType);
    }

    public static String isPrimitiveBoxClass(Class c) {
    	return MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(c);
    }

    public static String getPrimitiveTypeForWrapper(Class c) {
    	return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
    }

    public static Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
        return MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(c);
    }

    public static Class getClassForComponentType(final int componentSort) {
        return MultiDTaintedArrayWithObjTag.getClassForComponentType(componentSort);
    }

    public static Object unboxRaw(final Object in) {
        return MultiDTaintedArrayWithObjTag.unboxRaw(in);
    }

    public static Object unboxVal(final Object _in, final int componentType, final int dims) {
        return MultiDTaintedArrayWithObjTag.unboxVal(_in, componentType, dims);
    }

    public static int getSortForBoxClass(Class c) {
        return MultiDTaintedArrayWithObjTag.getSortForBoxClass(c);
    }

    public static int getSort(Class c) {
        return MultiDTaintedArrayWithObjTag.getSort(c);
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = BOX_IF_NECESSARY)
    public static Object boxIfNecessary(final Object in) {
        return MultiDTaintedArrayWithObjTag.boxIfNecessary(in);
    }

    public static Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
        return MultiDTaintedArrayWithObjTag.initWithEmptyTaints(ar, componentType, dims);
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
        MultiDTaintedArrayWithObjTag.initLastDim(ar, lastDimSize, componentType);
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final Taint<?> dimTaint, final int lastDimSize, final int componentType) {
        MultiDTaintedArrayWithObjTag.initLastDim(ar, dimTaint, lastDimSize, componentType);
    }

    public static Type getPrimitiveTypeForWrapper(String internalName) {
        return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(internalName);
    }

    @SuppressWarnings("unused")
    public static Object unboxMethodReceiverIfNecessary(Method m, Object obj) {
        if(LazyArrayObjTags.class.isAssignableFrom(m.getDeclaringClass())) {
            return obj; // No unboxing is necessary
        } else {
            return unboxRaw(obj);
        }
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the internal name of the taint array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getTaintArrayInternalName(int typeOperand) {
        return MultiDTaintedArrayWithObjTag.getTaintArrayInternalName(typeOperand);
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the descriptor of the array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getArrayDescriptor(int typeOperand) {
        switch(typeOperand) {
            case T_BOOLEAN:
                return "[Z";
            case T_INT:
                return "[I";
            case T_BYTE:
                return "[B";
            case T_CHAR:
                return "[C";
            case T_DOUBLE:
                return "[D";
            case T_FLOAT:
                return "[F";
            case T_LONG:
                return "[J";
            case T_SHORT:
                return "[S";
            default:
                throw new IllegalArgumentException();
        }
    }
}
