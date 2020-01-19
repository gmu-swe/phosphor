package edu.columbia.cs.psl.phosphor.util;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/* Provides utility methods ignored by Phosphor. */
public class IgnoredTestUtil {

    private IgnoredTestUtil() {
        // Prevents this class from being instantiated
    }
    static Unsafe unsafe;
    private static Unsafe getUnsafe(){
        if(unsafe == null){
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                unsafe = (Unsafe) f.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return unsafe;
    }

    /* Replaces the taint tag for each character of the specified with a taint tag containing only the specified label.
     * If the specified label is null clears the specified String's characters' tags */
    public static void setStringCharTaints(String str, Object label) {
        Taint<?> tag = label == null ? null : Taint.withLabel(label);
        Taint<?>[] tags = new Taint[str.length()];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = tag;
        }
        try {
            Field taintField = String.class.getDeclaredField("valuePHOSPHOR_WRAPPER");
            LazyCharArrayObjTags taints = (LazyCharArrayObjTags) getUnsafe().getObject(str,unsafe.objectFieldOffset(taintField));
            taints.taints = tags;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static void setStringCharTaints$$PHOSPHORTAGGED(String str, Object label, ControlFlowStack ctrl) {
        setStringCharTaints(str, label);
    }

    public static void setStringCharTaints(String str, Taint tag, Object label, Taint tag2) {
        setStringCharTaints(str, label);
    }
}