package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.util.IgnoredTestUtil;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/* Check that primitive values parsed from Strings retain the taint tags from both the String and the chars of the
 * String from which they were parsed. */
@RunWith(Parameterized.class)
public class ParsePrimitiveObjTagITCase extends BaseMultiTaintClass {

    private static final String STRING_LABEL = "STRING";
    private static final String CHARS_LABEL = "CHARS";
    private final boolean taintString;
    private final boolean taintChars;

    @Parameterized.Parameters
    public static Collection taintingConfig() {
        return Arrays.asList(new Object[][] {
                {true, true},
                {true, false},
                {false, true},
                {false, false},
        });
    }

    public ParsePrimitiveObjTagITCase(Boolean taintString, Boolean taintChars) {
        this.taintString = taintString;
        this.taintChars = taintChars;
    }

    /* Returns a String made from the specified String with the String itself tainted if taintString is true and its
     * characters if taintChars is true. */
    private String taintString(String str) {
        str = str;
        if(taintString && taintChars) {
            MultiTainter.taintedObject(str, Taint.withLabel(STRING_LABEL));
            str = MultiTainter.taintedReference(str, STRING_LABEL);
            IgnoredTestUtil.setStringCharTaints(str, CHARS_LABEL);
            return str;
        } else if(taintString) {
            MultiTainter.taintedObject(str, Taint.withLabel(STRING_LABEL));
            str = MultiTainter.taintedReference(str, STRING_LABEL);
            IgnoredTestUtil.setStringCharTaints(str, null);
            return str;
        } else if(taintChars) {
            char[] c = str.toCharArray();
            MultiTainter.setPrimitiveArrayTaints(c, Taint.withLabel(CHARS_LABEL));
            return new String(c);
        } else {
            return str;
        }
    }

    /* Checks that the specified taint tag contains the String label only if taintString is true and that the
     * specified taint tag contains the chars label only is taintChars is true. */
    private void checkTaint(Taint<?> taint) {
        if(taintString || taintChars) {
            assertNonNullTaint(taint);
            assertEquals(taint.containsLabel(STRING_LABEL), taintString);
            assertEquals(taint.containsLabel(CHARS_LABEL), taintChars);
        } else {
            assertNullOrEmpty(taint);
        }
    }

    @Test
    public void testParseBoolean() {
        boolean bool = Boolean.parseBoolean(taintString("true"));
        checkTaint(MultiTainter.getTaint(bool));
    }

    @Test
    public void testValueOfBoolean() {
        Boolean bool = Boolean.valueOf(taintString("true"));
        checkTaint(MultiTainter.getTaint(bool));
    }

    @Test
    public void testParseByte() {
        byte b = Byte.parseByte(taintString("5"));
        checkTaint(MultiTainter.getTaint(b));
    }

    @Test
    public void testParseByteRadix() {
        byte b = Byte.parseByte(taintString("11"), 3);
        checkTaint(MultiTainter.getTaint(b));
    }

    @Test
    public void testValueOfByte() {
        Byte b = Byte.valueOf(taintString("5"));
        checkTaint(MultiTainter.getTaint(b));
    }

    @Test
    public void testValueOfByteRadix() {
        Byte b = Byte.valueOf(taintString("11"), 3);
        checkTaint(MultiTainter.getTaint(b));
    }

    @Test
    public void testParseDouble() {
        double db = Double.parseDouble(taintString("44.3"));
        checkTaint(MultiTainter.getTaint(db));
    }

    @Test
    public void testValueOfDouble() {
        Double db = Double.valueOf(taintString("10.2"));
        checkTaint(MultiTainter.getTaint(db));
    }

    @Test
    public void testParseFloat() {
        float f = Float.parseFloat(taintString("22.5"));
        checkTaint(MultiTainter.getTaint(f));
    }

    @Test
    public void testValueOfFloat() {
        Float f = Float.valueOf(taintString("88.9"));
        checkTaint(MultiTainter.getTaint(f));
    }

    @Test
    public void testParseInt() {
        int i = Integer.parseInt(taintString("-6"));
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testParseIntRadix() {
        int i = Integer.parseInt(taintString("101"), 3);
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testParseUnsignedInt() {
        int i = Integer.parseUnsignedInt(taintString("36"));
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testParseUnsignedIntRadix() {
        int i = Integer.parseUnsignedInt(taintString("10201"), 3);
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testValueOfInt() {
        Integer i = Integer.valueOf(taintString("9"));
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testValueOfIntRadix() {
        Integer i = Integer.valueOf(taintString("1011"), 3);
        checkTaint(MultiTainter.getTaint(i));
    }

    @Test
    public void testParseLong() {
        long j = Long.parseLong(taintString("-6"));
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testParseLongRadix() {
        long j = Long.parseLong(taintString("101"), 3);
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testParseUnsignedLong() {
        long j = Long.parseUnsignedLong(taintString("36"));
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testParseUnsignedLongRadix() {
        long j = Long.parseUnsignedLong(taintString("10201"), 3);
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testValueOfLong() {
        Long j = Long.valueOf(taintString("9"));
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testValueOfLongRadix() {
        Long j = Long.valueOf(taintString("1011"), 3);
        checkTaint(MultiTainter.getTaint(j));
    }

    @Test
    public void testParseShort() {
        short s = Short.parseShort(taintString("5"));
        checkTaint(MultiTainter.getTaint(s));
    }

    @Test
    public void testParseShortRadix() {
        short s = Short.parseShort(taintString("11"), 3);
        checkTaint(MultiTainter.getTaint(s));
    }

    @Test
    public void testValueOfShort() {
        Short s = Short.valueOf(taintString("5"));
        checkTaint(MultiTainter.getTaint(s));
    }

    @Test
    public void testValueOfShortRadix() {
        Short s = Short.valueOf(taintString("11"), 3);
        checkTaint(MultiTainter.getTaint(s));
    }
}
