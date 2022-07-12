package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREMethodHelper;
import edu.columbia.cs.psl.phosphor.struct.LazyByteArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

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

    public static int getChars(long l, int idx, char[] ar, PhosphorStackFrame phosphorStackFrame) {
        LazyCharArrayObjTags ta = phosphorStackFrame.getArgWrapper(2, ar);
        Taint lt = phosphorStackFrame.getArgTaint(0);
        int ret = InstrumentedJREMethodHelper.java_lang_Long_getChars(l, idx, ta.val);
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
        return ret;
    }

    public static int getChars(int i, int idx, char[] ar, PhosphorStackFrame phosphorStackFrame) {
        LazyCharArrayObjTags ta = phosphorStackFrame.getArgWrapper(2, ar);
        Taint it = phosphorStackFrame.getArgTaint(0);
        int ret = InstrumentedJREMethodHelper.java_lang_Integer_getChars(i, idx, ta.val);
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
        return ret;
    }

    public static int getChars(int i, int idx, byte[] ar, PhosphorStackFrame phosphorStackFrame) {
        LazyByteArrayObjTags ta = phosphorStackFrame.getArgWrapper(2, ar);
        Taint it = phosphorStackFrame.getArgTaint(0);
        int ret = InstrumentedJREMethodHelper.java_lang_Integer_getChars(i, idx, ta.val);
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
        return ret;
    }

    public static int getChars(long i, int idx, byte[] ar, PhosphorStackFrame phosphorStackFrame) {
        LazyByteArrayObjTags ta = phosphorStackFrame.getArgWrapper(2, ar);
        Taint it = phosphorStackFrame.getArgTaint(0);
        int ret = InstrumentedJREMethodHelper.java_lang_Long_getChars(i, idx, ta.val);
        if (it != null) {
            int nChars;
            if (i < 0) {
                nChars = stringSize(-i) + 1;
            } else {
                nChars = stringSize(i);
            }
            if (ta.taints == null) {
                ta.taints = new Taint[ta.val.length];
            }
            for (int k = idx - nChars; k < Math.min(idx, ta.taints.length); k++) {
                ta.taints[k] = it;
            }
        }
        return ret;
    }


    public static String toString(byte i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Byte.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Byte.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(char i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Character.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Character.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(int i,  int r, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toString(i, r);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toString(i, r).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toUnsignedString(int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toUnsignedString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toUnsignedString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toUnsignedString(int i,  int r, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toUnsignedString(i, r);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toUnsignedString(i, r).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toOctalString(int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toOctalString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toOctalString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toHexString(int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toHexString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toHexString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(short i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Short.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(boolean i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Boolean.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Boolean.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(float i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Float.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Float.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toHexString(float i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Float.toHexString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Float.toHexString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(double i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Double.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Double.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toHexString(double i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Double.toHexString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Double.toHexString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(long i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toString(long i,  int r, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toString(i, r);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toString(i, r).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toBinaryString(long i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toBinaryString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toBinaryString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toBinaryString(int i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Integer.toBinaryString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Integer.toBinaryString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toHexString(long i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toHexString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toHexString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toUnsignedString(long i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toUnsignedString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toUnsignedString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toUnsignedString(long i,  int r, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toUnsignedString(i, r);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toUnsignedString(i, r).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static String toOctalString(long i, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        String ret;
        if(t == null) {
            ret = Long.toOctalString(i);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;
        }
        ret = new String(Long.toOctalString(i).toCharArray());
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(t);
        phosphorStackFrame.setReturnTaint(t);
        return ret;
    }

    public static int digit(char c,  int radix,  PhosphorStackFrame phosphorStackFrame) {
        int ret = Character.digit(c, radix);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static int digit(int codePoint,  int radix,  PhosphorStackFrame phosphorStackFrame) {
        int ret = Character.digit(codePoint, radix);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Long valueOf(long l, PhosphorStackFrame phosphorStackFrame) {
        Taint t = phosphorStackFrame.getArgTaint(0);
        if(t == null) {
            Long ret = Long.valueOf(l);
            phosphorStackFrame.setReturnTaint(Taint.emptyTaint());
            return ret;

        } else {
            phosphorStackFrame.setArgTaint(t, 1);
            Long ret = new Long(l);
            phosphorStackFrame.setReturnTaint(t);
            return ret;
        }
    }

    public static Boolean valueOfZ(String s, PhosphorStackFrame phosphorStackFrame) {
        Boolean ret = valueOfZ(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Byte valueOfB(String s, PhosphorStackFrame phosphorStackFrame) {
        Byte ret = valueOfB(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Short valueOfS(String s, PhosphorStackFrame phosphorStackFrame) {
        Short ret = valueOfS(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Boolean valueOfZ(boolean s, PhosphorStackFrame phosphorStackFrame) {
        Boolean ret = Boolean.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Byte valueOfB(byte s, PhosphorStackFrame phosphorStackFrame) {
        Byte ret = Byte.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }
    public static Byte valueOfB(String s, int radix, PhosphorStackFrame phosphorStackFrame) {
        Byte ret = Byte.valueOf(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(1)).union(phosphorStackFrame.getArgTaint(0)));
        return ret;
    }


    public static Short valueOfS(short s, PhosphorStackFrame phosphorStackFrame) {
        Short ret = Short.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Double valueOfD(String s, PhosphorStackFrame phosphorStackFrame) {
        Double ret = Double.valueOf(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Double valueOfD(double s, PhosphorStackFrame phosphorStackFrame) {
        Double ret = Double.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Integer valueOfI(String s, PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.valueOf(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Integer valueOfI(String s,  int radix, PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.valueOf(s, radix);
        Taint taint = phosphorStackFrame.getArgTaint(0);
        Taint radixTaint = phosphorStackFrame.getArgTaint(1);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, taint).union(radixTaint));
        return ret;
    }

    public static Integer valueOfI(int s, PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Float valueOfF(String s, PhosphorStackFrame phosphorStackFrame) {
        Float ret = Float.valueOf(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Float valueOfF(float s, PhosphorStackFrame phosphorStackFrame) {
        Float ret = Float.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Long valueOfJ(String s, PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.valueOf(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    public static Long valueOfJ(long s, PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static Long valueOfJ(String s,  int radix, PhosphorStackFrame phosphorStackFrame) {
        Taint taint = phosphorStackFrame.getArgTaint(0);
        Taint radixTaint = phosphorStackFrame.getArgTaint(1);
        Long ret = Long.valueOf(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, taint).union(radixTaint));
        return ret;
    }

    public static Character valueOfC(char s, PhosphorStackFrame phosphorStackFrame) {
        Character ret = Character.valueOf(s);
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static boolean booleanValue(Boolean v, PhosphorStackFrame phosphorStackFrame) {
        boolean ret = v.booleanValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static byte byteValue(Byte v, PhosphorStackFrame phosphorStackFrame) {
        byte ret = v.byteValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static char charValue(Character v, PhosphorStackFrame phosphorStackFrame) {
        char ret = v.charValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static double doubleValue(Double v, PhosphorStackFrame phosphorStackFrame) {
        double ret = v.doubleValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static int intValue(Integer v, PhosphorStackFrame phosphorStackFrame) {
        int ret = v.intValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static short shortValue(Short v, PhosphorStackFrame phosphorStackFrame) {
        short ret = v.shortValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static float floatValue(Float v, PhosphorStackFrame phosphorStackFrame) {
        float ret = v.floatValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        return ret;
    }

    public static long longValue(Long v, PhosphorStackFrame phosphorStackFrame) {
        long ret = v.longValue();
        phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
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
    public static Short valueOfS(String s, int radix, PhosphorStackFrame phosphorStackFrame) {
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        Short ret = Short.parseShort(s, radix);
        return ret;
    }

    @SuppressWarnings("unused")
    public static boolean parseBoolean(String s, PhosphorStackFrame phosphorStackFrame) {
        Boolean ret = Boolean.parseBoolean(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static byte parseByte(String s, PhosphorStackFrame phosphorStackFrame) {
        Byte ret = Byte.parseByte(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static double parseDouble(String s, PhosphorStackFrame phosphorStackFrame) {
        Double ret = Double.parseDouble(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static float parseFloat(String s, PhosphorStackFrame phosphorStackFrame) {
        Float ret = Float.parseFloat(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static int parseInt(String s, PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.parseInt(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static long parseLong(String s, PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.parseLong(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static short parseShort(String s, PhosphorStackFrame phosphorStackFrame) {
        Short ret = Short.parseShort(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static int parseUnsignedInt(String s, PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.parseInt(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static long parseUnsignedLong(String s, PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.parseLong(s);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static byte parseByte(String s, int radix,  PhosphorStackFrame phosphorStackFrame) {
        Byte ret = Byte.parseByte(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static int parseInt(String s, int radix,  PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.parseInt(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static long parseLong(String s, int radix,  PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.parseLong(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static int parseInt(CharSequence s, int beginIndex, int endIndex, int radix, PhosphorStackFrame phosphorStackFrame) {
        int ret = Integer.parseInt(s, beginIndex, endIndex, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix, PhosphorStackFrame phosphorStackFrame) {
        long ret = Long.parseLong(s, beginIndex, endIndex, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static short parseShort(String s, int radix,  PhosphorStackFrame phosphorStackFrame) {
        Short ret = Short.parseShort(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static int parseUnsignedInt(String s,  int radix,  PhosphorStackFrame phosphorStackFrame) {
        Integer ret = Integer.parseInt(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static long parseUnsignedLong(String s,  int radix,  PhosphorStackFrame phosphorStackFrame) {
        Long ret = Long.parseLong(s, radix);
        phosphorStackFrame.setReturnTaint(getCombinedTaint(s, phosphorStackFrame.getArgTaint(0)));
        return ret;
    }

    @SuppressWarnings("unused")
    public static <T> char forDigit(int digit, int radix, PhosphorStackFrame phosphorStackFrame) {
        char ret = Character.forDigit(digit, radix);
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
            phosphorStackFrame.setReturnTaint(Taint.combineTags(phosphorStackFrame.getArgTaint(0), phosphorStackFrame.getArgTaint(1)));
        } else {
            phosphorStackFrame.setReturnTaint(phosphorStackFrame.getArgTaint(0));
        }
        return ret;
    }

    public static String toString(Boolean value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Byte value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Character value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Float value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Integer value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Long value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Short value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    public static String toString(Double value, PhosphorStackFrame phosphorStackFrame) {
        String ret = value.toString();
        Taint tag = phosphorStackFrame.getArgTaint(0);
        ((TaintedWithObjTag) (Object) ret).setPHOSPHOR_TAG(tag);
        phosphorStackFrame.setReturnTaint(tag);
        return ret;
    }

    /* Returns a taint tag that contains the labels of the specified String's tag and the labels of any tags for its
     * characters. */
    private static Taint getCombinedTaint(CharSequence str, Taint referenceTaint) {
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
