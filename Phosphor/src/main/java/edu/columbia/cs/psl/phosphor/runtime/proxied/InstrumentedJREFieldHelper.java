package edu.columbia.cs.psl.phosphor.runtime.proxied;

import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.RuntimeJDKInternalUnsafePropagator;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

import java.lang.reflect.Method;

public class InstrumentedJREFieldHelper {

    private static RuntimeException _crash() {
        return new IllegalStateException("InstrumentedJREHelper not initialized");
    }

    public static Taint getPHOSPHOR_TAG(String s) {
        throw _crash();
    }

    public static void setPHOSPHOR_TAG(String s, Taint t) {
        throw _crash();
    }

    public static LazyCharArrayObjTags JAVA_8getvaluePHOSPHOR_WRAPPER(String str) {
        throw _crash();
    }

    public static void JAVA_8setvaluePHOSPHOR_WRAPPER(String str, LazyCharArrayObjTags v) {
        throw _crash();
    }

    public static char[] JAVA_8getvalue(String str) {
        throw _crash();
    }

    public static LazyByteArrayObjTags getvaluePHOSPHOR_WRAPPER(String str) {
        throw _crash();
    }

    public static void setvaluePHOSPHOR_WRAPPER(String str, LazyByteArrayObjTags v) {
        throw _crash();
    }

    public static byte[] getvalue(String str) {
        throw _crash();
    }

    public static SinglyLinkedList<RuntimeJDKInternalUnsafePropagator.OffsetPair> get$$PHOSPHOR_OFFSET_CACHE(Class<?> cl) {
        throw _crash();
    }

    public static void set$$PHOSPHOR_OFFSET_CACHE(Class<?> cl, SinglyLinkedList<RuntimeJDKInternalUnsafePropagator.OffsetPair> offsetPairs) {
        throw _crash();
    }

    public static boolean getvalueOf(Boolean z) {
        throw _crash();
    }

    public static boolean getvalueOf(Byte z) {
        throw _crash();
    }

    public static void setvalueOf(Boolean r, boolean b) {
        throw _crash();
    }

    public static void setvalueOf(Byte r, boolean b) {
        throw _crash();
    }

    public static void setvalueOf(Character r, boolean b) {
        throw _crash();
    }


    public static boolean getvalueOf(Character z) {
        throw _crash();
    }

    public static void setvalueOf(Short r, boolean b) {
        throw _crash();
    }

    public static boolean getvalueOf(Short z) {
        throw _crash();
    }

    public static Method getPHOSPHOR_TAGmethod(Method method) {
        throw _crash();
    }

    public static void setPHOSPHOR_TAGmethod(Method method, Method v) {
        throw _crash();
    }

    public static Class<?> getPHOSPHOR_TAGclass(Class<?> clazz) {
        throw _crash();
    }

    public static void setPHOSPHOR_TAGclass(Class<?> clazz, Class<?> v) {
        throw _crash();
    }

    public static void setPHOSPHOR_TAGmarked(Method method, boolean value) {
        throw _crash();
    }

    public static boolean getPHOSPHOR_TAGmarked(Method method) {
        throw _crash();
    }

    public static void setphosphorStackFrame(Thread thread, PhosphorStackFrame prevFrame) {
        throw _crash();
    }
    public static PhosphorStackFrame getphosphorStackFrame(Thread thread){
        throw _crash();
    }
}
