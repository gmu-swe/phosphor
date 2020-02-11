package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;

public class CharacterUtils {

    private CharacterUtils() {
        // Prevents this class from being instantiated
    }

    public static TaintedCharWithObjTag reverseBytes$$PHOSPHORTAGGED(char c, Taint t, TaintedCharWithObjTag ret) {
        ret.val = Character.reverseBytes(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toLowerCase$$PHOSPHORTAGGED(char c, Taint t, TaintedCharWithObjTag ret) {
        ret.val = Character.toLowerCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toLowerCase$$PHOSPHORTAGGED(int c, Taint t, TaintedIntWithObjTag ret) {
        ret.val = Character.toLowerCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toTitleCase$$PHOSPHORTAGGED(char c, Taint t, TaintedCharWithObjTag ret) {
        ret.val = Character.toTitleCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toTitleCase$$PHOSPHORTAGGED(int c, Taint t, TaintedIntWithObjTag ret) {
        ret.val = Character.toTitleCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toUpperCase$$PHOSPHORTAGGED(char c, Taint t, TaintedCharWithObjTag ret) {
        ret.val = Character.toUpperCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toUpperCase$$PHOSPHORTAGGED(int c, Taint t, TaintedIntWithObjTag ret) {
        ret.val = Character.toUpperCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(tags.val, i);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(CharSequence seq, Taint seqTaint, int i, Taint t, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(seq, i);
            if(seq instanceof String && ((String) seq).valuePHOSPHOR_WRAPPER != null && ((String) seq).valuePHOSPHOR_WRAPPER.taints != null) {
                ret.taint = ((String) seq).valuePHOSPHOR_WRAPPER.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, int i2, Taint t2, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(tags.val, i, i2);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }


    public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointBefore(tags.val, i);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(CharSequence seq, Taint seqTaint, int i, Taint t, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointBefore(seq, i);
            if(seq instanceof String && ((String) seq).valuePHOSPHOR_WRAPPER != null && ((String) seq).valuePHOSPHOR_WRAPPER.taints != null) {
                ret.taint = ((String) seq).valuePHOSPHOR_WRAPPER.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointBefore$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, Taint t2, int i2, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointBefore(tags.val, i, i2);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedReferenceWithObjTag toChars$$PHOSPHORTAGGED(int idx, Taint idxTaint, TaintedReferenceWithObjTag _ret) {

        char[] v = Character.toChars(idx);
        LazyCharArrayObjTags ret = new LazyCharArrayObjTags(v);

        if(idxTaint != null) {
            ret.taints = new Taint[v.length];
            for(int i = 0; i < v.length; i++) {
                ret.taints[i] = idxTaint;
            }
        }
        _ret.val = ret;
        _ret.taint = idxTaint;
        return _ret;
    }

    public static TaintedIntWithObjTag toChars$$PHOSPHORTAGGED(int idx, Taint idxTaint, LazyCharArrayObjTags array, Taint t, int dstIdx, Taint dstIdxTaint, TaintedIntWithObjTag ret) {

        ret.val = Character.toChars(idx, array.val, dstIdx);

        if(idxTaint != null) {
            ret.taint = idxTaint;
        }
        return ret;
    }


    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(tags.val, i);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union(ctrl.copyTag());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(CharSequence seq, Taint seqTaint, int i, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(seq, i);
            if(seq instanceof String && ((String) seq).valuePHOSPHOR_WRAPPER != null) {
                ret.taint = seq.toString().valuePHOSPHOR_WRAPPER.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union((Taint) ((TaintedWithObjTag) seq).getPHOSPHOR_TAG());
            _t = _t.union(ctrl.copyTag());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedIntWithObjTag codePointAt$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, Taint t2, int i2, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        try {
            ret.val = Character.codePointAt(tags.val, i, i2);
            ret.taint = Taint.emptyTaint();
            if(tags.taints != null) {
                ret.taint = tags.taints[i];
            }
            return ret;
        } catch(StringIndexOutOfBoundsException ex) {
            Taint _t = Taint.withLabel(t);
            _t = _t.union(ctrl.copyTag());
            ((TaintedWithObjTag) ex).setPHOSPHOR_TAG(_t);
            throw ex;
        }
    }

    public static TaintedReferenceWithObjTag toChars$$PHOSPHORTAGGED(int idx, Taint idxTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag _ret) {
        char[] v = Character.toChars(idx);
        LazyCharArrayObjTags ret = new LazyCharArrayObjTags(v);
        if(idxTaint != null) {
            ret.taints = new Taint[v.length];
            for(int i = 0; i < v.length; i++) {
                ret.taints[i] = idxTaint;
            }
        }
        _ret.taint = Taint.emptyTaint();
        _ret.val = ret;
        return _ret;
    }

    public static TaintedIntWithObjTag toChars$$PHOSPHORTAGGED(int idx, Taint idxTaint, LazyCharArrayObjTags tags, Taint t, int dstIdx, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.toChars(idx, tags.val, dstIdx);
        if(idxTaint != null) {
            ret.taint = idxTaint;
        }
        return ret;
    }

    public static TaintedCharWithObjTag reverseBytes$$PHOSPHORTAGGED(char c, Taint t, ControlFlowStack ctrl, TaintedCharWithObjTag ret) {
        ret.val = Character.reverseBytes(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toLowerCase$$PHOSPHORTAGGED(char c, Taint t, ControlFlowStack ctrl, TaintedCharWithObjTag ret) {
        ret.val = Character.toLowerCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toLowerCase$$PHOSPHORTAGGED(int c, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.toLowerCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toTitleCase$$PHOSPHORTAGGED(char c, Taint t, ControlFlowStack ctrl, TaintedCharWithObjTag ret) {
        ret.val = Character.toTitleCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toTitleCase$$PHOSPHORTAGGED(int c, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.toTitleCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedCharWithObjTag toUpperCase$$PHOSPHORTAGGED(char c, Taint t, ControlFlowStack ctrl, TaintedCharWithObjTag ret) {
        ret.val = Character.toUpperCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag toUpperCase$$PHOSPHORTAGGED(int c, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.toUpperCase(c);
        if(t == null) {
            ret.taint = Taint.emptyTaint();
        } else {
            ret.taint = t;
        }
        return ret;
    }

    public static TaintedIntWithObjTag codePointBeforeImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, Taint t2, int i2, TaintedIntWithObjTag ret) {
        ret.val = Character.codePointBeforeImpl(tags.val, i, i2);
        ret.taint = Taint.emptyTaint();
        if(tags.taints != null) {
            ret.taint = tags.taints[i];
        }
        return ret;
    }

    public static TaintedIntWithObjTag codePointBeforeImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags tags, Taint tagsTaint, int i, Taint t, Taint t2, int i2, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.codePointBeforeImpl(tags.val, i, i2);
        ret.taint = Taint.emptyTaint();
        if(tags.taints != null) {
            ret.taint = tags.taints[i];
        }
        return ret;
    }

    public static TaintedIntWithObjTag toUpperCaseEx$$PHOSPHORTAGGED(int cp, Taint t, TaintedIntWithObjTag ret) {
        ret.val = Character.toUpperCaseEx(cp);
        if(t != null) {
            ret.taint = t;
        } else {
            ret.taint = Taint.emptyTaint();
        }
        return ret;
    }

    public static TaintedReferenceWithObjTag toUpperCaseCharArray$$PHOSPHORTAGGED(int c, Taint t, TaintedReferenceWithObjTag _ret) {
        LazyCharArrayObjTags ret = new LazyCharArrayObjTags(Character.toUpperCaseCharArray(c));
        if(t != null) {
            ret.taints = new Taint[ret.val.length];
            for(int i = 0; i < ret.taints.length; i++) {
                if(t != null) {
                    ret.taints[i] = t;
                }
            }
        } else {
            ret.taints = null;
        }
        _ret.val = ret;
        _ret.taint = Taint.emptyTaint();
        return _ret;
    }

    public static TaintedIntWithObjTag toUpperCaseEx$$PHOSPHORTAGGED(int c, Taint t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.toUpperCaseEx(c);
        if(t != null) {
            ret.taint = t;
        } else {
            ret.taint = Taint.emptyTaint();
        }
        return ret;
    }

    public static TaintedReferenceWithObjTag toUpperCaseCharArray$$PHOSPHORTAGGED(Taint t, ControlFlowStack ctrl, int cp, TaintedReferenceWithObjTag _ret) {
        LazyCharArrayObjTags ret = new LazyCharArrayObjTags(Character.toUpperCaseCharArray(cp));
        if(t != null) {
            ret.taints = new Taint[ret.val.length];
            for(int i = 0; i < ret.taints.length; i++) {
                if(t != null) {
                    ret.taints[i] = t;
                }
            }
        } else {
            ret.taints = null;
        }
        _ret.taint = Taint.emptyTaint();
        _ret.val = ret;
        return _ret;
    }

    public static TaintedIntWithObjTag codePointAtImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags t, Taint refTaint, int index, Taint ti, int limit, Taint tl, TaintedIntWithObjTag ret) {
        ret.val = Character.codePointAtImpl(t.val, index, limit);
        ret.taint = Taint.emptyTaint();
        if(t.taints != null && t.taints[index] != null) {
            ret.taint = t.taints[index];
        }
        return ret;
    }

    public static TaintedIntWithObjTag codePointAtImpl$$PHOSPHORTAGGED(LazyCharArrayObjTags t, Taint refTaint, int index, Taint ti, int limit, Taint tl, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        ret.val = Character.codePointAtImpl(t.val, index, limit);
        ret.taint = Taint.emptyTaint();
        if(t.taints != null && t.taints[index] != null) {
            ret.taint = t.taints[index];
        }
        return ret;
    }
}
