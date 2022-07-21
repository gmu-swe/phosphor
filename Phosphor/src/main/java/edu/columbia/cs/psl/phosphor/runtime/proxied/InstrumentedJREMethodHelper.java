package edu.columbia.cs.psl.phosphor.runtime.proxied;

public class InstrumentedJREMethodHelper {
    private static RuntimeException _crash() {
        return new IllegalStateException("InstrumentedJREHelper not initialized");
    }

    public static Object java_lang_reflect_Array_newArray(Class<?> componentType, int length) throws NegativeArraySizeException {
        throw _crash();
    }

    public static int java_lang_Integer_parseInt(CharSequence sequence, int v1, int v2, int v3){
        throw _crash();
    }
    public static long java_lang_Long_parseLong(CharSequence sequence, int v1, int v2, int v3){
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

    public static void java_lang_Long_getChars(long l, int idx, char[] val) {
        throw _crash();
    }

    public static void java_lang_Integer_getChars(int l, int idx, char[] val) {
        throw _crash();
    }

    public static int java_lang_Long_getChars(long l, int idx, byte[] val) {
        throw _crash();
    }

    public static int java_lang_Integer_getChars(int l, int idx, byte[] val) {
        throw _crash();
    }

}
