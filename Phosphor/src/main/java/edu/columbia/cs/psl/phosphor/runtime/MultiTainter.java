package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public final class MultiTainter {

    private MultiTainter() {
        // Prevents this class from being instantiated
    }

    public static ControlFlowStack getControlFlow() {
        throw new IllegalStateException();
    }

    public static TaintedReferenceWithObjTag getControlFlow$$PHOSPHORTAGGED(ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, ControlFlowStack erased) {
        ret.taint = Taint.emptyTaint();
        ret.val = ctrl;
        return ret;
    }

    public static boolean taintedBoolean(boolean in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static byte taintedByte(byte in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static char taintedChar(char in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static double taintedDouble(double in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static float taintedFloat(float in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static long taintedLong(long in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static int taintedInt(int in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static short taintedShort(short in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static boolean[] taintedBooleanArray(boolean[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static byte[] taintedByteArray(byte[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static char[] taintedCharArray(char[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static double[] taintedDoubleArray(double[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static float[] taintedFloatArray(float[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static long[] taintedLongArray(long[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static int[] taintedIntArray(int[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static short[] taintedShortArray(short[] in, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(boolean in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(byte in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(char in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(double in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(float in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(int in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(long in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(short in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static Taint getTaint(Object in) {
        throw new IllegalStateException("Calling uninstrumented Phosphor stubs!");
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(boolean b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(byte b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(char b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(double b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(float b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(int b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(long b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(short b, Taint t, TaintedReferenceWithObjTag ret, Taint erasedReturnType) {
        ret.val = t;
        ret.taint = Taint.emptyTaint();
        return ret;
    }

    public static TaintedBooleanWithObjTag taintedBoolean$$PHOSPHORTAGGED(boolean in, Taint oldTag, Object lbl, Taint lblTag, TaintedBooleanWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedByteWithObjTag taintedByte$$PHOSPHORTAGGED(byte in, Taint oldTag, Object lbl, Taint lblTag, TaintedByteWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedCharWithObjTag taintedChar$$PHOSPHORTAGGED(char in, Taint oldTag, Object lbl, Taint lblTag, TaintedCharWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedDoubleWithObjTag taintedDouble$$PHOSPHORTAGGED(double in, Taint oldTag, Object lbl, Taint lblTag, TaintedDoubleWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedFloatWithObjTag taintedFloat$$PHOSPHORTAGGED(float in, Taint oldTag, Object lbl, Taint lblTag, TaintedFloatWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedIntWithObjTag taintedInt$$PHOSPHORTAGGED(int in, Taint oldTag, Object lbl, Taint lblTag, TaintedIntWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedLongWithObjTag taintedLong$$PHOSPHORTAGGED(long in, Taint oldTag, Object lbl, Taint lblTag, TaintedLongWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    public static TaintedShortWithObjTag taintedShort$$PHOSPHORTAGGED(short in, Taint oldTag, Object lbl, Taint lblTag, TaintedShortWithObjTag ret) {
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
        return ret;
    }

    private static void taintedArray(LazyArrayObjTags in, Object lbl, TaintedReferenceWithObjTag ret) {
        if(in.taints == null) {
            in.taints = new Taint[in.getLength()];
        }
        for(int i = 0; i < in.getLength(); i++) {
            in.taints[i] = Taint.withLabel(lbl);
        }
        ret.taint = Taint.withLabel(lbl);
        ret.val = in;
    }

    public static TaintedReferenceWithObjTag taintedBooleanArray$$PHOSPHORTAGGED(LazyBooleanArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedByteArray$$PHOSPHORTAGGED(LazyByteArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedCharArray$$PHOSPHORTAGGED(LazyCharArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedDoubleArray$$PHOSPHORTAGGED(LazyDoubleArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedFloatArray$$PHOSPHORTAGGED(LazyFloatArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedIntArray$$PHOSPHORTAGGED(LazyIntArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedShortArray$$PHOSPHORTAGGED(LazyShortArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedLongArray$$PHOSPHORTAGGED(LazyLongArrayObjTags in, Taint arrayRefTag, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret) {
        taintedArray(in, lbl, ret);
        return ret;
    }

    public static TaintedReferenceWithObjTag taintedReference$$PHOSPHORTAGGED(Object in, Taint inTaint, Object lbl, Taint lblTag, TaintedReferenceWithObjTag ret, Object erased) {
        ret.val = in;
        ret.taint = Taint.withLabel(lbl);
        return ret;
    }

    public static <T> T taintedReference(T toTaint, Object lbl) {
        throw new IllegalStateException("Calling uninstrumented phosphor stubs!");
    }


    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(Object obj, Taint taint, TaintedReferenceWithObjTag ret, Taint erasedRet) {
        ret.taint = Taint.emptyTaint();
        ret.val = taint;
        return ret;
    }

    public static Taint[] getStringCharTaints(String str) {
        if(str == null) {
            return null;
        }
        return str.valuePHOSPHOR_WRAPPER.taints;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getTaint$$PHOSPHORTAGGED(Object obj, Taint theTaint, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, Taint erasedRet) {
        return getTaint$$PHOSPHORTAGGED(obj, theTaint, ret, erasedRet);
    }

    @Deprecated
    public static void taintedObject(Object obj, Taint tag) {
        if(obj instanceof MultiDTaintedArrayWithObjTag) {
            obj = ((MultiDTaintedArrayWithObjTag) obj).getVal();
        }
        if(obj instanceof TaintedWithObjTag) {
            ((TaintedWithObjTag) obj).setPHOSPHOR_TAG(tag);
        } else if(obj instanceof TaintedPrimitiveWithObjTag) {
            ((TaintedPrimitiveWithObjTag) obj).taint = tag;
        } else if(obj instanceof LazyArrayObjTags) {
            // TODO
        } else if(obj != null && ArrayHelper.engaged == 1) {
            ArrayHelper.setTag(obj, tag);
        }
    }

    @SuppressWarnings("unused")
    public static void taintedObject$$PHOSPHORTAGGED(Object obj, Taint oldTag, Taint tag, Taint tagTag, ControlFlowStack ctrl) {
        taintedObject(obj, tag);
    }

    @SuppressWarnings("unused")
    public static void taintedObject$$PHOSPHORTAGGED(Object obj, Taint oldTag, Taint tag, Taint tagTag) {
        taintedObject(obj, tag);
    }

    /* If the specified object is a one-dimensional primitive array sets the taint of each element in the array to be
     * the specified tag. */
    public static void setPrimitiveArrayTaints(Object obj, Taint tag) {
        if(obj instanceof LazyArrayObjTags) {
            ((LazyArrayObjTags) obj).setTaints(tag);
        }
    }

    @SuppressWarnings("unused")
    public static void setPrimitiveArrayTaints$$PHOSPHORTAGGED(Object obj, Taint oldTag, Taint tag, Taint tagTag) {
        setPrimitiveArrayTaints(obj, tag);
    }

    @SuppressWarnings("unused")
    public static void setPrimitiveArrayTaints$$PHOSPHORTAGGED(Object obj, Taint oldTag, Taint tag, Taint tagTag, ControlFlowStack ctrl) {
        setPrimitiveArrayTaints(obj, tag);
    }

    /* If multi-tainted and the specified object is a multi-dimensional array returns a Taint tag containing the taints of
     * all of its elements, otherwise returns the object's taint. */
    public static Taint getMergedTaint(Object obj) {
        //TODO reference taints?
        if(obj instanceof LazyReferenceArrayObjTags) {
            Object[] ar = ((LazyReferenceArrayObjTags) obj).val;
            Taint[] taints = new Taint[ar.length];
            int i = 0;
            for(Object el : ar) {
                taints[i] = getMergedTaint(el);
            }
            return Taint.combineTaintArray(taints);
        } else if(obj instanceof LazyArrayObjTags) {
            return Taint.combineTaintArray(((LazyArrayObjTags) obj).taints);
        } else if(obj instanceof Object[]) {
            throw new IllegalStateException("Object[] should not exist!");
        } else {
            // return getTaint(obj);
            return Taint.emptyTaint();
        }
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getMergedTaint$$PHOSPHORTAGGED(Object obj, Taint taint, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, Taint erasedReturn) {
        ret.taint = Taint.emptyTaint();
        ret.val = getMergedTaint(obj);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag getMergedTaint$$PHOSPHORTAGGED(Object obj, Taint taint, TaintedReferenceWithObjTag ret, Taint erasedReturn) {
        ret.taint = Taint.emptyTaint();
        ret.val = getMergedTaint(obj);
        return ret;
    }
}
