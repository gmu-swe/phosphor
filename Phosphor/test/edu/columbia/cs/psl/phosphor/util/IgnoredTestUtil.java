package edu.columbia.cs.psl.phosphor.util;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;

/* Provides utility methods ignored by Phosphor. */
public class IgnoredTestUtil {

    private IgnoredTestUtil() {
        // Prevents this class from being instantiated
    }

    /* Replaces the taint tag for each character of the specified with a taint tag containing only the specified label.
     * If the specified label is null clears the specified String's characters' tags */
    public static void setStringCharTaints(String str, Object label) {
        Taint<?> tag = label == null ? null : Taint.withLabel(label);
        Taint<?>[] tags = new Taint[str.length()];
        for(int i = 0 ; i < tags.length; i++) {
            tags[i] = tag;
        }
        TaintSourceWrapper.setStringValueTag(str, new LazyCharArrayObjTags(str.toCharArray(), tags));
    }

    @SuppressWarnings("unused")
    public static void setStringCharTaints$$PHOSPHORTAGGED(String str, Object label, ControlTaintTagStack ctrl) {
        setStringCharTaints(str, label);
    }
}
