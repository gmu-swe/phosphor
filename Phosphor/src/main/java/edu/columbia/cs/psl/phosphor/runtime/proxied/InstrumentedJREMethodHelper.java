package edu.columbia.cs.psl.phosphor.runtime.proxied;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;

import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;

public class InstrumentedJREMethodHelper {
    private static RuntimeException _crash() {
        return new IllegalStateException("InstrumentedJREHelper not initialized");
    }

    public static Object java_lang_reflect_Array_newArray(Class<?> componentType, int length) throws NegativeArraySizeException {
        throw _crash();
    }

    public static int java_lang_Character_codePointBeforeImpl(char[] val, int i, int i2) {
        throw _crash();
    }

    public static int java_lang_Character_toUpperCaseEx(int cp) {
        throw _crash();
    }

    public static char[] java_lang_Character_toUpperCaseCharArray(int c) {
        throw _crash();
    }

    public static int java_lang_Character_codePointAtImpl(char[] val, int index, int limit) {
        throw _crash();
    }

    public static TaintedBooleanWithObjTag java_util_HashSet_add$$PHOSPHORTAGGED_i(HashSet _this, Object obj, TaintedBooleanWithObjTag ret) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_io_ObjectInputStream_readObject$$PHOSPHORTAGGED_i(ObjectInputStream _this, Taint<Object> emptyTaint, ControlFlowStack dummy, TaintedReferenceWithObjTag ret, Object erased) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_io_ObjectInputStream_readObject$$PHOSPHORTAGGED_i(ObjectInputStream _this, Taint<Object> emptyTaint, TaintedReferenceWithObjTag ret, Object erased) {
        throw _crash();
    }

    public static TaintedBooleanWithObjTag java_util_Collection_add$$PHOSPHORTAGGED_i(Collection _this, Taint referenceTaint, Object toAdd, Taint objTaint, TaintedBooleanWithObjTag ret) {
        throw _crash();
    }

    public static TaintedBooleanWithObjTag java_util_Collection_add$$PHOSPHORTAGGED_i(Collection _this, Taint referenceTaint, Object toAdd, Taint objTaint, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        throw _crash();
    }

    public static int java_lang_Long_getChars(long l, int idx, char[] val) {
        throw _crash();
    }

    public static int java_lang_Integer_getChars(int l, int idx, char[] val) {
        throw _crash();
    }

    public static int java_lang_Long_getChars(long l, int idx, byte[] val) {
        throw _crash();
    }

    public static int java_lang_Integer_getChars(int l, int idx, byte[] val) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_lang_Double_toString$$PHOSPHORTAGGED(double d, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String erased) {
        throw _crash();
    }

    public static String java_lang_String_init(Taint refTaint, LazyCharArrayObjTags chars, Taint tag, ControlFlowStack ctrl) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_lang_Long_valueOf$$PHOSPHORTAGGED(long l, Taint t, TaintedReferenceWithObjTag ret, Long erased) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_lang_Long_valueOf$$PHOSPHORTAGGED(long l, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, Long erased) {
        throw _crash();
    }

    public static Long java_lang_Long_init(Taint refTaint, long l, Taint t, ControlFlowStack ctrl) {
        throw _crash();
    }

    public static Long java_lang_Long_init(Taint refTaint, long l, Taint t) {
        throw _crash();
    }

    public static TaintedReferenceWithObjTag java_lang_reflect_Field_get$$PHOSPHORTAGGED_i(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, Object erasedReturn) {
        throw _crash();
    }
    public static TaintedReferenceWithObjTag java_lang_reflect_Field_get$$PHOSPHORTAGGED_i(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedReferenceWithObjTag ret, Object erasedReturn) {
        throw _crash();
    }

    public static TaintedBooleanWithObjTag java_util_Collection_add$$PHOSPHORTAGGED_i(Collection tmp, ControlFlowStack stack, TaintedBooleanWithObjTag taintedBooleanWithObjTag) {
        throw _crash();
    }
}
