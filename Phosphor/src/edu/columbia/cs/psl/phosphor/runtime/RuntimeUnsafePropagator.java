package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class RuntimeUnsafePropagator {

    private static Object unwrap(Object o) {
        if(o instanceof LazyArrayObjTags[] | o instanceof LazyArrayObjTags) {
            return MultiDTaintedArrayWithObjTag.unboxRawOnly1D(o);
        } else {
            return o;
        }
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, boolean x, ControlTaintTagStack ctrl) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, boolean x, ControlTaintTagStack ctrl) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBoolean(Unsafe unsafe, Object o, int offset, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putBoolean(Unsafe unsafe, Object o, long offset, boolean x) {
        unsafe.putBoolean(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putBooleanVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBooleanVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBooleanVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, boolean x, ControlTaintTagStack ctrl) {
        unsafe.putBooleanVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBooleanVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, boolean x) {
        unsafe.putBooleanVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, boolean.class);
    }

    @SuppressWarnings("unused")
    public static void putBooleanVolatile(Unsafe unsafe, Object o, long offset, boolean x) {
        unsafe.putBooleanVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, byte x, ControlTaintTagStack ctrl) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, byte x, ControlTaintTagStack ctrl) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByte(Unsafe unsafe, Object o, int offset, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putByte(Unsafe unsafe, Object o, long offset, byte x) {
        unsafe.putByte(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putByteVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByteVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByteVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, byte x, ControlTaintTagStack ctrl) {
        unsafe.putByteVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByteVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, byte x) {
        unsafe.putByteVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, byte.class);
    }

    @SuppressWarnings("unused")
    public static void putByteVolatile(Unsafe unsafe, Object o, long offset, byte x) {
        unsafe.putByteVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, char x) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, char x, ControlTaintTagStack ctrl) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, char x) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, char x, ControlTaintTagStack ctrl) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, char x) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, char x) {
        unsafe.putChar(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putChar(Unsafe unsafe, Object o, int offset, char x) {
        unsafe.putChar(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putChar(Unsafe unsafe, Object o, long offset, char x) {
        unsafe.putChar(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putCharVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, char x) {
        unsafe.putCharVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putCharVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, char x, ControlTaintTagStack ctrl) {
        unsafe.putCharVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putCharVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, char x) {
        unsafe.putCharVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, char.class);
    }

    @SuppressWarnings("unused")
    public static void putCharVolatile(Unsafe unsafe, Object o, long offset, char x) {
        unsafe.putCharVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, double x, ControlTaintTagStack ctrl) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, double x, ControlTaintTagStack ctrl) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDouble(Unsafe unsafe, Object o, int offset, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putDouble(Unsafe unsafe, Object o, long offset, double x) {
        unsafe.putDouble(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putDoubleVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, double x) {
        unsafe.putDoubleVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDoubleVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, double x, ControlTaintTagStack ctrl) {
        unsafe.putDoubleVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDoubleVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, double x) {
        unsafe.putDoubleVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, double.class);
    }

    @SuppressWarnings("unused")
    public static void putDoubleVolatile(Unsafe unsafe, Object o, long offset, double x) {
        unsafe.putDoubleVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, float x, ControlTaintTagStack ctrl) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, float x, ControlTaintTagStack ctrl) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloat(Unsafe unsafe, Object o, int offset, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putFloat(Unsafe unsafe, Object o, long offset, float x) {
        unsafe.putFloat(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putFloatVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloatVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloatVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, float x, ControlTaintTagStack ctrl) {
        unsafe.putFloatVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloatVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, float x) {
        unsafe.putFloatVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, float.class);
    }

    @SuppressWarnings("unused")
    public static void putFloatVolatile(Unsafe unsafe, Object o, long offset, float x) {
        unsafe.putFloatVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, int x) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, int x, ControlTaintTagStack ctrl) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x, ControlTaintTagStack ctrl) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, int x) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, int x) {
        unsafe.putInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putInt(Unsafe unsafe, Object o, int offset, int x) {
        unsafe.putInt(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putInt(Unsafe unsafe, Object o, long offset, int x) {
        unsafe.putInt(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putIntVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x) {
        unsafe.putIntVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putIntVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x, ControlTaintTagStack ctrl) {
        unsafe.putIntVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putIntVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, int x) {
        unsafe.putIntVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putIntVolatile(Unsafe unsafe, Object o, long offset, int x) {
        unsafe.putIntVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, long x) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, long x, ControlTaintTagStack ctrl) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x, ControlTaintTagStack ctrl) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, long x) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, long x) {
        unsafe.putLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLong(Unsafe unsafe, Object o, int offset, long x) {
        unsafe.putLong(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putLong(Unsafe unsafe, Object o, long offset, long x) {
        unsafe.putLong(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putLongVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x) {
        unsafe.putLongVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLongVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x, ControlTaintTagStack ctrl) {
        unsafe.putLongVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLongVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, long x) {
        unsafe.putLongVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putLongVolatile(Unsafe unsafe, Object o, long offset, long x) {
        unsafe.putLongVolatile(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Object x, ControlTaintTagStack ctrl) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x, ControlTaintTagStack ctrl) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject(Unsafe unsafe, Object o, int offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObject(Unsafe unsafe, Object o, long offset, Object x) {
        unsafe.putObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putObjectVolatile(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x, ControlTaintTagStack ctrl) {
        unsafe.putObjectVolatile(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObjectVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putObjectVolatile(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putObjectVolatile(Unsafe unsafe, Object o, long offset, Object x) {
        unsafe.putObjectVolatile(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x) {
        unsafe.putOrderedInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, int x, ControlTaintTagStack ctrl) {
        unsafe.putOrderedInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedInt$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, int x) {
        unsafe.putOrderedInt(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, int.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedInt(Unsafe unsafe, Object o, long offset, int x) {
        unsafe.putOrderedInt(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x) {
        unsafe.putOrderedLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, long x, ControlTaintTagStack ctrl) {
        unsafe.putOrderedLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedLong$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, long x) {
        unsafe.putOrderedLong(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, long.class);
    }

    @SuppressWarnings("unused")
    public static void putOrderedLong(Unsafe unsafe, Object o, long offset, long x) {
        unsafe.putOrderedLong(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putOrderedObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Object x, ControlTaintTagStack ctrl) {
        unsafe.putOrderedObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedObject$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, Object x) {
        unsafe.putOrderedObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putOrderedObject(Unsafe unsafe, Object o, long offset, Object x) {
        unsafe.putOrderedObject(unwrap(o), offset, unwrap(x));
        putTag(unsafe, o, offset, x);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, short x) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, int offset, Taint x$$PHOSPHORTAGGED, short x, ControlTaintTagStack ctrl) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, short x) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, short x, ControlTaintTagStack ctrl) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, int offset, int x$$PHOSPHORTAGGED, short x) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, short x) {
        unsafe.putShort(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShort(Unsafe unsafe, Object o, int offset, short x) {
        unsafe.putShort(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putShort(Unsafe unsafe, Object o, long offset, short x) {
        unsafe.putShort(unwrap(o), offset, x);
    }

    @SuppressWarnings("unused")
    public static void putShortVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, short x) {
        unsafe.putShortVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShortVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, Taint offset$$PHOSPHORTAGGED, long offset, Taint x$$PHOSPHORTAGGED, short x, ControlTaintTagStack ctrl) {
        unsafe.putShortVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShortVolatile$$PHOSPHORTAGGED(Unsafe unsafe, Object o, int offset$$PHOSPHORTAGGED, long offset, int x$$PHOSPHORTAGGED, short x) {
        unsafe.putShortVolatile(unwrap(o), offset, x);
        putTag(unsafe, o, offset, x$$PHOSPHORTAGGED, short.class);
    }

    @SuppressWarnings("unused")
    public static void putShortVolatile(Unsafe unsafe, Object o, long offset, short x) {
        unsafe.putShortVolatile(unwrap(o), offset, x);
    }

    private static long getTagFieldOffset(Unsafe unsafe, Object o, long offset, Class<?> primitiveClazz) {
        for(Class<?> clazz = o.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for(Field field : declaredFields) {
                try {
                    if(field.getType().equals(primitiveClazz) && unsafe.objectFieldOffset(field) == offset) {
                        // Get the associated taint field
                        Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                        if (Configuration.MULTI_TAINTING && taintField.getType().equals(Configuration.TAINT_TAG_OBJ_CLASS)) {
                            return unsafe.objectFieldOffset(taintField);
                        } else if (!Configuration.MULTI_TAINTING && taintField.getType().equals(int.class)) {
                            return unsafe.objectFieldOffset(taintField);
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }
        }
        return Unsafe.INVALID_FIELD_OFFSET;
    }

    private static long getLazyArrayTagFieldOffset(Unsafe unsafe, Object o, long offset) {
        for(Class<?> clazz = o.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for(Field field : declaredFields) {
                try {
                    if(field.getType().isArray() && unsafe.objectFieldOffset(field) == offset) {
                        // Get the associated taint field
                        Field taintField = clazz.getField(field.getName() + TaintUtils.TAINT_FIELD);
                        if (Configuration.MULTI_TAINTING && LazyArrayObjTags.class.isAssignableFrom(taintField.getType())) {
                            return unsafe.objectFieldOffset(taintField);
                        } else if (!Configuration.MULTI_TAINTING && LazyArrayIntTags.class.isAssignableFrom(taintField.getType())) {
                            return unsafe.objectFieldOffset(taintField);
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }
        }
        return Unsafe.INVALID_FIELD_OFFSET;
    }

    private static void putTag(Unsafe unsafe, Object o, long offset, Taint x$$PHOSPHORTAGGED, Class<?> clazz) {
        try {
            long tagOffset = getTagFieldOffset(unsafe, o, offset, clazz);
            if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                unsafe.putObject(o, tagOffset, x$$PHOSPHORTAGGED);
            }
        } catch(Exception e) {
            //
        }
    }

    private static void putTag(Unsafe unsafe, Object o, long offset, int x$$PHOSPHORTAGGED, Class<?> clazz) {
        try {
            long tagOffset = getTagFieldOffset(unsafe, o, offset, clazz);
            if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                unsafe.putInt(o, tagOffset, x$$PHOSPHORTAGGED);
            }
        } catch(Exception e) {
            //
        }
    }

    private static void putTag(Unsafe unsafe, Object o, long offset, Object x) {
        if(x == null || x instanceof LazyArrayObjTags || x instanceof LazyArrayIntTags) {
            try {
                long tagOffset = getLazyArrayTagFieldOffset(unsafe, o, offset);
                if(tagOffset != Unsafe.INVALID_FIELD_OFFSET) {
                    unsafe.putObject(o, tagOffset, x);
                }
            } catch(Exception e) {
                //
            }
        }
    }
}
