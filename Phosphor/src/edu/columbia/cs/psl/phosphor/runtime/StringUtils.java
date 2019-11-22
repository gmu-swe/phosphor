package edu.columbia.cs.psl.phosphor.runtime;


import edu.columbia.cs.psl.phosphor.PreMain;

/**
 * Adapted from Apache harmony's String.java, under the
 * Apache License v2.
 */
public class StringUtils {

    private StringUtils() {
        // Prevents this class from being instantiated
    }

    /**
     * Compares the specified string to this string and compares the specified
     * range of characters to determine if they are the same.
     *
     * @param thisStr   the string to match against
     * @param thisStart the starting offset in this string.
     * @param string    the string to compare.
     * @param start     the starting offset in the specified string.
     * @param length    the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false}
     * otherwise
     * @throws NullPointerException if {@code string} is {@code null}.
     */
    public static boolean regionMatches(String thisStr, int thisStart, String string, int start,
                                        int length) {
        if(string.value.length - start < length || start < 0) {
            return false;
        }
        if(thisStart < 0 || thisStr.value.length - thisStart < length) {
            return false;
        }
        if(length <= 0) {
            return true;
        }
        for(int i = 0; i < length; ++i) {
            if(thisStr.value[thisStart + i] != string.value[start + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean _startsWith(String thisStr, String string) {
        if(thisStr.value.length < string.value.length) {
            return false;
        }
        for(int i = 0; i < string.value.length; ++i) {
            if(thisStr.value[i] != string.value[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWith(String str, String prefix) {
        if(PreMain.RUNTIME_INST) {
            return _startsWith(str, prefix);
        }
        return str.startsWith(prefix);
    }
}
