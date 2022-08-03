package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper;
import edu.columbia.cs.psl.phosphor.struct.TaggedCharArray;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class CharacterUtils {

    private CharacterUtils() {
        // Prevents this class from being instantiated
    }

    public static char reverseBytes(char c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        char ret = Character.reverseBytes(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static char toLowerCase(char c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        char ret = Character.toLowerCase(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static int toLowerCase(int c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        int ret = Character.toLowerCase(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static char toTitleCase(char c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        char ret = Character.toTitleCase(c);
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        }

    public static int toTitleCase(int c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        int ret = Character.toTitleCase(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static char toUpperCase(char c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        char ret = Character.toUpperCase(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static int toUpperCase(int c, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        int ret = Character.toUpperCase(c);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static int codePointAt(char[] tags, int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        try {
            TaggedCharArray tagsWrapper = phosphorStackFrame.getArgWrapper(0, tags);
            Taint retTaint = Taint.emptyTaint();
            if(tagsWrapper.taints != null) {
                retTaint = tagsWrapper.taints[i];
            }
            int ret = Character.codePointAt(tags, i);
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static int codePointAt(CharSequence seq, int i, PhosphorStackFrame phosphorStackFrame) {
        try {
            Taint retTaint = Taint.emptyTaint();
            int ret = Character.codePointAt(seq, i);
            if(Configuration.IS_JAVA_8){
                if (seq instanceof String && InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq) != null && InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq).taints != null) {
                    retTaint = InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq).taints[i];
                }
            } else {
                if (seq instanceof String && InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq) != null && InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq).taints != null) {
                    retTaint = InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq).taints[i];
                }
            }
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(phosphorStackFrame.getArgTaint(0));
            _t = _t.union((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static int codePointAt(char[] tags, int i, int i2, PhosphorStackFrame phosphorStackFrame) {
        try {
            Taint retTaint = Taint.emptyTaint();
            TaggedCharArray wrapper = phosphorStackFrame.getArgWrapper(0, tags);
            if(wrapper.taints != null) {
                retTaint = wrapper.taints[i];
            }
            int ret = Character.codePointAt(tags, i, i2);
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(phosphorStackFrame.getArgTaint(0));
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }


    public static int codePointBefore(char[] tags, int i, PhosphorStackFrame phosphorStackFrame) {
        try {
            int ret = Character.codePointBefore(tags, i);
            i--;
            Taint retTaint = Taint.emptyTaint();
            TaggedCharArray wrapper = phosphorStackFrame.getArgWrapper(0, tags);
            if(wrapper.taints != null) {
                retTaint = wrapper.taints[i];
            }
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(phosphorStackFrame.getArgTaint(0));
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static int codePointBefore(CharSequence seq, int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(1);
        try {
            int ret = Character.codePointBefore(seq, i);
            i--;
            Taint retTaint = Taint.emptyTaint();
            if(Configuration.IS_JAVA_8){
                if(seq instanceof String && InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq) != null && InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq).taints  != null) {
                    retTaint = InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) seq).taints[i];
                }
            } else {
                if(seq instanceof String && InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq) != null && InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq).taints  != null) {
                    retTaint = InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) seq).taints[i];
                }
            }
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static int codePointBefore(char[] tags, int i, int i2, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(1);
        try {
            int ret = Character.codePointBefore(tags, i, i2);
            i--;
            Taint retTaint = Taint.emptyTaint();
            TaggedCharArray wrapper = phosphorStackFrame.getArgWrapper(0, tags);
            if(wrapper.taints != null) {
                retTaint = wrapper.taints[i];
            }
            phosphorStackFrame.setReturnTaint(retTaint);
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static char[] toChars(int idx, PhosphorStackFrame phosphorStackFrame) {
        Taint idxTaint = phosphorStackFrame.getArgTaint(0);
        char[] v = Character.toChars(idx);
        TaggedCharArray ret = new TaggedCharArray(v);
        if(idxTaint != null) {
            ret.taints = new Taint[v.length];
            for(int i = 0; i < v.length; i++) {
                ret.taints[i] = idxTaint;
            }
        }
        phosphorStackFrame.setWrappedReturn(v);
        return v;
    }

    public static int toChars(int idx, char[] array, int dstIdx, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = phosphorStackFrame.getArgTaint(0);
        int ret = Character.toChars(idx, array, dstIdx);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static int codePointBeforeImpl(char[] tags, int i, int i2, PhosphorStackFrame phosphorStackFrame) {
        Taint retTaint = Taint.emptyTaint();
        TaggedCharArray tagsWrapper = phosphorStackFrame.getArgWrapper(0, tags);
        if(tagsWrapper != null) {
            retTaint = tagsWrapper.taints[i];
        }
        int ret = InstrumentedJREMethodHelper.java_lang_Character_codePointBeforeImpl(tags, i, i2);
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }

    public static int toUpperCaseEx(int cp, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        int ret = InstrumentedJREMethodHelper.java_lang_Character_toUpperCaseEx(cp);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static char[] toUpperCaseCharArray(int c, PhosphorStackFrame phosphorStackFrame) {
        Taint idxTaint = phosphorStackFrame.getArgTaint(0);
        char[] v = InstrumentedJREMethodHelper.java_lang_Character_toUpperCaseCharArray(c);
        TaggedCharArray ret = new TaggedCharArray(v);
        if(idxTaint != null) {
            ret.taints = new Taint[v.length];
            for(int i = 0; i < v.length; i++) {
                ret.taints[i] = idxTaint;
            }
        }
        phosphorStackFrame.setWrappedReturn(v);
        return v;
    }

    public static int codePointAtImpl(char[] t, int index, int limit, PhosphorStackFrame phosphorStackFrame) {
        TaggedCharArray wrapped = phosphorStackFrame.getArgWrapper(0, t);
        int ret = InstrumentedJREMethodHelper.java_lang_Character_codePointAtImpl(t, index, limit);
        Taint retTaint = Taint.emptyTaint();
        if(wrapped.taints != null && wrapped.taints[index] != null) {
            retTaint = wrapped.taints[index];
        }
        phosphorStackFrame.setReturnTaint(retTaint);
        return ret;
    }
}
