package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class GeneralBindingControlITCase extends BaseMultiTaintClass {

    public static void checkContainsInAnyOrder(Object[] actual, Object... expected) {
        Set<Object> actualSet = new HashSet<>(Arrays.asList(actual));
        Set<Object> expectedSet = new HashSet<>(Arrays.asList(expected));
        assertEquals(expectedSet, actualSet);
    }

    private static char[] copyDigits(char[] c, boolean noZeros) {
        char[] copy = new char[c.length];
        for(int i = 0; i < c.length; i++) {
            if(c[i] == '0') {
                if(noZeros) {
                    throw new IllegalArgumentException();
                }
                copy[i] = '%';
            } else {
                copy[i] = c[i];
            }
        }
        return copy;
    }

    public static char[] createDigitArray() {
        char[] c = "0123456789".toCharArray();
        for(int i = 0; i < c.length; i++) {
            c[i] = MultiTainter.taintedChar(c[i], i);
        }
        return c;
    }

    public static void checkDigitArray(char[] c) {
        for(int i = 0; i < c.length; i++) {
            Taint t = MultiTainter.getTaint(c[i]);
            assertNotNull(t);
            Object[] labels = t.getLabels();
            assertArrayEquals(new Object[]{i}, labels);
        }
    }

    public static void taintWithIndices(int[] a) {
        for(int i = 0; i < a.length; i++) {
            a[i] = MultiTainter.taintedInt(a[i], i);
        }
    }

    @Test
    public void testSimpleIfEqualTaken() {
        int a = MultiTainter.taintedInt(5, "a");
        int b = MultiTainter.taintedInt(5, "b");
        int c = 7;
        if(a == b) {
            c = 22;
        }
        checkContainsInAnyOrder(MultiTainter.getTaint(c).getLabels(), "a", "b");
    }

    @Test
    public void testSimpleIfNotEqualNotTaken() {
        int a = MultiTainter.taintedInt(5, "a");
        int b = MultiTainter.taintedInt(5, "b");
        int c = 7;
        int d = 0;
        if(a != b) {
            d = 88;
        } else {
            c = 22;
        }
        checkContainsInAnyOrder(MultiTainter.getTaint(c).getLabels(), "a", "b");
    }

    @Test
    public void testSimpleIfNotEqualTaken() {
        int a = MultiTainter.taintedInt(5, "a");
        int b = MultiTainter.taintedInt(7, "b");
        int c = 7;
        int d = 0;
        if(a != b) {
            d = 88;
        } else {
            c = 22;
        }
        assertNullOrEmpty(MultiTainter.getTaint(d));
    }

    @Test
    public void testForLoopMultipleReturns() {
        char[] c = createDigitArray();
        char[] copy = copyDigits(c, false);
        checkDigitArray(copy);
    }

    @Test
    public void testSimpleAnd() {
        int a = MultiTainter.taintedInt(5, "a");
        int b = MultiTainter.taintedInt(22, "b");
        int c = 7;
        if(a == 5 && b == 22) {
            c = 43;
        }
        checkContainsInAnyOrder(MultiTainter.getTaint(c).getLabels(), "a", "b");
    }
}
