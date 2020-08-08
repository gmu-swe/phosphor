package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;

public class RuntimeBoxUnboxPropagator {

    private static final int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999,
            99999999, 999999999, Integer.MAX_VALUE};

    private RuntimeBoxUnboxPropagator() {
        // Prevents this class from being instantiated
    }

    private static int stringSize(int x) {
        for(int i = 0; ; i++) {
            if(x <= sizeTable[i]) {
                return i + 1;
            }
        }
    }

    private static int stringSize(long x) {
        long p = 10;
        for(int i = 1; i < 19; i++) {
            if(x < p) {
                return i;
            }
            p = 10 * p;
        }
        return 19;
    }

    public static void getChars$$PHOSPHORTAGGED(long l, Taint lt, int idx, Taint idt, LazyCharArrayObjTags ta, Taint taTaint) {
        Long.getChars(l, idx, ta.val);
        if(lt != null) {
            int nChars;
            if(l < 0) {
                nChars = stringSize(-l) + 1;
            } else {
                nChars = stringSize(l);
            }
            if(ta.taints == null) {
                ta.taints = new Taint[ta.val.length];
            }
            for(int k = idx - nChars; k < idx; k++) {
                ta.taints[k] = lt;
            }
        }
    }

    public static void getChars$$PHOSPHORTAGGED(int i, Taint it, int idx, Taint idt, LazyCharArrayObjTags ta, Taint taTaint) {
        Integer.getChars(i, idx, ta.val);
        if(it != null) {
            int nChars;
            if(i < 0) {
                nChars = stringSize(-i) + 1;
            } else {
                nChars = stringSize(i);
            }
            if(ta.taints == null) {
                ta.taints = new Taint[ta.val.length];
            }
            for(int k = idx - nChars; k < Math.min(idx, ta.taints.length); k++) {
                ta.taints[k] = it;
            }
        }
    }

    public static void getChars$$PHOSPHORTAGGED(long l, Taint lt, int idx, Taint idt, LazyCharArrayObjTags ta, Taint taTaint, ControlFlowStack ctrl) {
        getChars$$PHOSPHORTAGGED(l, lt, idx, idt, ta, taTaint);
    }

    public static void getChars$$PHOSPHORTAGGED(int i, Taint it, int idx, Taint idt, LazyCharArrayObjTags ta, Taint taTaint, ControlFlowStack ctrl) {
        getChars$$PHOSPHORTAGGED(i, it, idx, idt, ta, taTaint);
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(byte i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Byte.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Byte.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(char i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Character.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Character.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(int i, Taint t, int r, Taint t2, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toUnsignedString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toUnsignedString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(Taint t, int i, Taint tr, int r, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toUnsignedString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toUnsignedString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toOctalString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toOctalString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toOctalString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(short i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Short.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(boolean i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Boolean.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Boolean.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(float i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Float.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Float.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(float i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Float.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Float.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(double i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Double.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Double.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(double i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Double.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Double.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(long i, Taint t, int r, Taint t2, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toBinaryString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toBinaryString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toBinaryString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toBinaryString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Integer.toBinaryString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toBinaryString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toBinaryString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        return toBinaryString$$PHOSPHORTAGGED(i, t, ret, e);
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toUnsignedString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toUnsignedString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(long i, Taint t, int r, Taint tr, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toUnsignedString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toUnsignedString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(long i, Taint t, int r, Taint tr, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toUnsignedString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toUnsignedString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toOctalString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toOctalString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toOctalString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(byte i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Byte.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Byte.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;

    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(char i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Character.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Character.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Integer.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Integer.toUnsignedString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toUnsignedString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(int i, Taint t, int r, Taint tr, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Integer.toUnsignedString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toUnsignedString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toOctalString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Integer.toOctalString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toOctalString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(int i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Integer.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(short i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Short.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Integer.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(boolean i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Boolean.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Boolean.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(float i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Float.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Float.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(float i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Float.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Float.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(double i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        Double.toString$$PHOSPHORTAGGED(i, t, ret, ctrl, e);
        ret.val = new String(((LazyCharArrayObjTags) ret.val), Taint.emptyTaint(), ctrl);
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(double i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Double.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Double.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(long i, Taint t, int r, Taint t2, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, String e) {
        if(t == null) {
            ret.val = Long.toString(i, r);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toString(i, r).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toBinaryString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toBinaryString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toBinaryString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toHexString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toHexString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toHexString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toUnsignedString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toUnsignedString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toUnsignedString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedReferenceWithObjTag toOctalString$$PHOSPHORTAGGED(long i, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, String e) {
        if(t == null) {
            ret.val = Long.toOctalString(i);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
        ret.val = new String(Long.toOctalString(i).toCharArray());
        ((String) ret.val).setPHOSPHOR_TAG(t);
        ret.taint = t;
        return ret;
    }

    public static TaintedIntWithObjTag digit$$PHOSPHORTAGGED(char c, Taint cTaint, int radix, Taint rTaint, TaintedIntWithObjTag ret) {
        ret.val = Character.digit(c, radix);
        if(cTaint != null) {
            ret.taint = cTaint;
        }
        return ret;
    }

    public static TaintedIntWithObjTag digit$$PHOSPHORTAGGED(int codePoint, Taint cTaint, int radix, Taint rTaint, TaintedIntWithObjTag ret) {
        ret.val = Character.digit(codePoint, radix);
        if(cTaint != null) {
            ret.taint = cTaint;
        }
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOf(long l, Taint t, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl) {
        if(t == null) {
            Long.valueOf$$PHOSPHORTAGGED(l, t, ret, ctrl, null);
            ret.taint = Taint.emptyTaint();
            return ret;
        } else {
            ret.val = new Long(l, t, ctrl);
            ret.taint = t;
            ((TaintedWithObjTag) (ret.val)).setPHOSPHOR_TAG(t);
            return ret;
        }
    }

    public static TaintedReferenceWithObjTag valueOf(long l, Taint t, TaintedReferenceWithObjTag ret) {
        if(t == null) {
            Long.valueOf$$PHOSPHORTAGGED(l, t, ret, null);
            ret.taint = Taint.emptyTaint();
            return ret;

        } else {
            ret.val = new Long(l, t, null);
            ret.taint = t;
            ((TaintedWithObjTag) ret.val).setPHOSPHOR_TAG(t);
            return ret;
        }
    }

    public static TaintedReferenceWithObjTag valueOfZ$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Boolean e) {
        ret.val = valueOfZ(s);
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfB$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Byte e) {
        ret.val = valueOfB(s);
        // ret.taint = taint;
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfS$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Short e) {
        ret.val = valueOfS(s);
        // ret.taint = taint;
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfZ$$PHOSPHORTAGGED(boolean s, Taint taint, TaintedReferenceWithObjTag ret, Boolean e) {
        ret.val = Boolean.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfB$$PHOSPHORTAGGED(byte s, Taint taint, TaintedReferenceWithObjTag ret, Byte e) {
        ret.val = Byte.valueOf(s);
        // ret.taint = taint;
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfS$$PHOSPHORTAGGED(short s, Taint taint, TaintedReferenceWithObjTag ret, Short e) {
        ret.val = Short.valueOf(s);
        // ret.taint = taint;
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfD$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Double e) {
        ret.val = Double.valueOf(s);
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfD$$PHOSPHORTAGGED(double s, Taint taint, TaintedReferenceWithObjTag ret, Double e) {
        ret.val = Double.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfI$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Integer e) {
        ret.val = Integer.valueOf(s);
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfI$$PHOSPHORTAGGED(String s, Taint taint, int radix, Taint radixTaint, TaintedReferenceWithObjTag ret, Integer e) {
        ret.val = Integer.valueOf(s, radix);
        ret.taint = getCombinedTaint(s, taint).union(radixTaint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfI$$PHOSPHORTAGGED(int s, Taint taint, TaintedReferenceWithObjTag ret, Integer erasedReturn) {
        ret.val = Integer.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfF$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Float erasedReturn) {
        ret.val = Float.valueOf(s);
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfF$$PHOSPHORTAGGED(float s, Taint taint, TaintedReferenceWithObjTag ret, Float erasedReturn) {
        ret.val = Float.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfJ$$PHOSPHORTAGGED(String s, Taint taint, TaintedReferenceWithObjTag ret, Long erasedReturn) {
        ret.val = Long.valueOf(s);
        ret.taint = getCombinedTaint(s, taint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfJ$$PHOSPHORTAGGED(long s, Taint taint, TaintedReferenceWithObjTag ret, Long erasedReturn) {
        ret.val = Long.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfJ$$PHOSPHORTAGGED(String s, Taint taint, int radix, Taint radixtaint, TaintedReferenceWithObjTag ret, Long erasedReturn) {
        ret.val = Long.valueOf(s, radix);
        ret.taint = getCombinedTaint(s, taint).union(radixtaint);
        return ret;
    }

    public static TaintedReferenceWithObjTag valueOfC$$PHOSPHORTAGGED(char s, Taint taint, TaintedReferenceWithObjTag ret, Character erasedReturn) {
        ret.val = Character.valueOf(s);
        ret.taint = taint;
        return ret;
    }

    public static TaintedBooleanWithObjTag booleanValue$$PHOSPHORTAGGED(Boolean v, Taint vTaint, TaintedBooleanWithObjTag ret) {
        ret.val = v.booleanValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedByteWithObjTag byteValue$$PHOSPHORTAGGED(Byte v, Taint vTaint, TaintedByteWithObjTag ret) {
        ret.val = v.byteValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedCharWithObjTag charValue$$PHOSPHORTAGGED(Character v, Taint vTaint, TaintedCharWithObjTag ret) {
        ret.val = v.charValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedDoubleWithObjTag doubleValue$$PHOSPHORTAGGED(Double v, Taint vTaint, TaintedDoubleWithObjTag ret) {
        ret.val = v.doubleValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedIntWithObjTag intValue$$PHOSPHORTAGGED(Integer v, Taint vTaint, TaintedIntWithObjTag ret) {
        ret.val = v.intValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedShortWithObjTag shortValue$$PHOSPHORTAGGED(Short v, Taint vTaint, TaintedShortWithObjTag ret) {
        ret.val = v.shortValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedFloatWithObjTag floatValue$$PHOSPHORTAGGED(Float v, Taint vTaint, TaintedFloatWithObjTag ret) {
        ret.val = v.floatValue();
        ret.taint = vTaint;
        return ret;
    }

    public static TaintedLongWithObjTag longValue$$PHOSPHORTAGGED(Long v, Taint vTaint, TaintedLongWithObjTag ret) {
        ret.val = v.longValue();
        ret.taint = vTaint;
        return ret;
    }

    public static Boolean valueOfZ(String s) {
        return s == null ? Boolean.FALSE : Boolean.parseBoolean(s);
    }

    public static Byte valueOfB(String s) {
        return Byte.parseByte(s);
    }

    public static Short valueOfS(String s) {
        return Short.parseShort(s);
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag valueOfB$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, Byte e) {
        ret.val = Byte.parseByte(s, radix);
        ret.taint = getCombinedTaint(s, tag).union(sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag valueOfB$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint t, TaintedReferenceWithObjTag ret, Byte e) {
        ret.val = Byte.parseByte(s, radix);
        ret.taint = getCombinedTaint(s, t).union(sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag valueOfS$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedReferenceWithObjTag ret, ControlFlowStack ctrl, Short e) {
        ret.val = Short.parseShort(s, radix);
        ret.taint = getCombinedTaint(s, tag).union(sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedReferenceWithObjTag valueOfS$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint t, TaintedReferenceWithObjTag ret, Short e) {
        ret.taint = getCombinedTaint(s, sTaint);
        ret.val = Short.parseShort(s, radix);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedBooleanWithObjTag parseBoolean$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        return parseBoolean$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedBooleanWithObjTag parseBoolean$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedBooleanWithObjTag ret) {
        ret.val = Boolean.parseBoolean(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedByteWithObjTag ret) {
        return parseByte$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedByteWithObjTag ret) {
        ret.val = Byte.parseByte(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedDoubleWithObjTag parseDouble$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedDoubleWithObjTag ret) {
        return parseDouble$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedDoubleWithObjTag parseDouble$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedDoubleWithObjTag ret) {
        ret.val = Double.parseDouble(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedFloatWithObjTag parseFloat$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedFloatWithObjTag ret) {
        return parseFloat$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedFloatWithObjTag parseFloat$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedFloatWithObjTag ret) {
        ret.val = Float.parseFloat(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        return parseInt$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedIntWithObjTag ret) {
        ret.val = Integer.parseInt(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedLongWithObjTag ret) {
        return parseLong$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedLongWithObjTag ret) {
        ret.val = Long.parseLong(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedShortWithObjTag ret) {
        return parseShort$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedShortWithObjTag ret) {
        ret.val = Short.parseShort(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        return parseUnsignedInt$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedIntWithObjTag ret) {
        ret.val = Integer.parseInt(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint sTaint, ControlFlowStack ctrl, TaintedLongWithObjTag ret) {
        return parseUnsignedLong$$PHOSPHORTAGGED(s, sTaint, ret);
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint sTaint, TaintedLongWithObjTag ret) {
        ret.val = Long.parseLong(s);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedByteWithObjTag ret) {
        try {
            ret.val = Byte.parseByte(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedByteWithObjTag parseByte$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedByteWithObjTag ret) {
        ret.val = Byte.parseByte(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        try {
            ret.val = Integer.parseInt(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseInt$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedIntWithObjTag ret) {
        ret.val = Integer.parseInt(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedLongWithObjTag ret) {
        try {
            ret.val = Long.parseLong(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseLong$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedLongWithObjTag ret) {
        ret.val = Long.parseLong(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedShortWithObjTag ret) {
        try {
            ret.val = Short.parseShort(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedShortWithObjTag parseShort$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedShortWithObjTag ret) {
        ret.val = Short.parseShort(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        try {
            ret.val = Integer.parseInt(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedIntWithObjTag parseUnsignedInt$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedIntWithObjTag ret) {
        ret.val = Integer.parseInt(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, ControlFlowStack ctrl, TaintedLongWithObjTag ret) {
        try {
            ret.val = Long.parseLong(s, radix);
            ret.taint = getCombinedTaint(s, tag);
            return ret;
        } catch(NumberFormatException ex) {

            throw ex;
        }
    }

    @SuppressWarnings("unused")
    public static TaintedLongWithObjTag parseUnsignedLong$$PHOSPHORTAGGED(String s, Taint sTaint, int radix, Taint<?> tag, TaintedLongWithObjTag ret) {
        ret.val = Long.parseLong(s, radix);
        ret.taint = getCombinedTaint(s, sTaint);
        return ret;
    }

    @SuppressWarnings("unused")
    public static <T> TaintedCharWithObjTag forDigit$$PHOSPHORTAGGED(int digit, Taint digitTag, int radix, Taint<T> radixTag,
                                                                     TaintedCharWithObjTag ret) {
        ret.val = Character.forDigit(digit, radix);
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
            ret.taint = Taint.combineTags(digitTag, radixTag);
        } else {
            ret.taint = digitTag;
        }
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Boolean value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Byte value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Character value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Float value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Integer value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Long value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Short value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    public static TaintedReferenceWithObjTag toString$$PHOSPHORTAGGED(Double value, Taint tag, TaintedReferenceWithObjTag ret, String e) {
        ret.val = value.toString();
        ((String) ret.val).setPHOSPHOR_TAG(tag);
        ret.taint = tag;
        return ret;
    }

    /* Returns a taint tag that contains the labels of the specified String's tag and the labels of any tags for its
     * characters. */
    private static Taint getCombinedTaint(String str, Taint referenceTaint) {
        if(str == null) {
            return null;
        } else {
            Taint charsTaint = MultiTainter.getMergedTaint(TaintSourceWrapper.getStringValueTag(str));
            return Taint.combineTags(referenceTaint, charsTaint);
        }
    }

    /* Returns a taint tag that contains the labels of the specified String's tag, the labels of any tags for its
     * characters, and in labels for the specified tag */
    @SuppressWarnings("unchecked")
    private static Taint getCombinedTaint(String str, Taint strTaint, Taint<?> tag) {
        if(str == null) {
            return Taint.emptyTaint();
        } else {
            Taint charsTaint = MultiTainter.getMergedTaint(TaintSourceWrapper.getStringValueTag(str));
            Taint result = Taint.combineTags(strTaint, charsTaint);
            if(result == null) {
                return tag;
            }
            if(tag == null) {
                return result;
            }
            return result.union(tag);
        }
    }

    private static Taint getTaint(Object obj) {
        return (obj == null) ? Taint.emptyTaint() : (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
    }
}
