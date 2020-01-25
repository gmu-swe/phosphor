package edu.columbia.cs.psl.phosphor.runtime;

public class ArrayHelper {

    public static int engaged = 0;

    private ArrayHelper() {
        // Prevents this class from being instantiated
    }

    private static native Taint _getTag(Object obj);

    private static native void _setTag(Object obj, Taint t);

    public static Taint getTag(Object obj) {
        if(engaged == 0) {
            throw new IllegalStateException("Attempting to use JVMTI features, but Phosphor native agent not loaded");
        }
        return _getTag(obj);
    }

    public static void setTag(Object obj, Taint t) {
        if(engaged == 0) {
            throw new IllegalStateException("Attempting to use JVMTI features, but Phosphor native agent not loaded");
        }
        _setTag(obj, t);
    }
}
