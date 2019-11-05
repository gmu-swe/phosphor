package edu.columbia.cs.psl.test.phosphor.binding;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class GeneralBindingControlITCase extends BaseMultiTaintClass {

    @Test
    public void testSimpleIfEqualTaken() {
        int a = MultiTainter.taintedInt(5, "a");
        int b = MultiTainter.taintedInt(5, "b");
        int c = 7;
        if(a == b) {
            c = 22;
        }
        assertTaintHasOnlyLabels(MultiTainter.getTaint(c), "a", "b");
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
        assertTaintHasOnlyLabels(MultiTainter.getTaint(c), "a", "b");
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
        assertTaintHasOnlyLabels(MultiTainter.getTaint(c), "a", "b");
    }

    @Ignore(value = "Requires branch-not-taken semantics")
    @Test
    public void testArrayEqualsSingleExitTrue() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 4};
        boolean result = arrayEqualsSingleExit(a, b);
        assertTaintHasOnlyLabels(MultiTainter.getTaint(result), 0, 1, 2, 3, 4);
    }

    @Test
    public void testArrayEqualsSingleExitFalse() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 5};
        boolean result = arrayEqualsSingleExit(a, b);
        assertNullOrEmpty(MultiTainter.getTaint(result));
    }

    @Ignore(value = "Need different binding semantics")
    @Test
    public void testArrayEqualsMultipleExitTrue() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 4};
        boolean result = arrayEqualsMultipleExit(a, b);
        assertTaintHasOnlyLabels(MultiTainter.getTaint(result), 0, 1, 2, 3, 4);
    }

    @Test
    public void testArrayEqualsMultipleExitFalse() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 5};
        boolean result = arrayEqualsMultipleExit(a, b);
        assertNullOrEmpty(MultiTainter.getTaint(result));
    }

    @Test
    public void testRecursiveArrayEqualsTrue() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 4};
        boolean result = recursiveEquals(0, a, b);
        assertTaintHasOnlyLabels(MultiTainter.getTaint(result), 0, 1, 2, 3, 4);
    }

    @Ignore(value = "Loop formed via recursive call")
    @Test
    public void testRecursiveArrayEqualsFalse() {
        int[] a = new int[]{0, 1, 2, 3, 4};
        taintWithIndices(a);
        int[] b = new int[]{0, 1, 2, 3, 5};
        boolean result = recursiveEquals(0, a, b);
        assertNullOrEmpty(MultiTainter.getTaint(result));
    }

    public static boolean arrayEqualsSingleExit(int[] a, int[] b) {
        boolean result = true;
        if(a.length != b.length) {
            result = false;
        } else {
            for(int i = 0; i < a.length; i++) {
                if(a[i] != b[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public static boolean arrayEqualsMultipleExit(int[] a, int[] b) {
        if(a.length != b.length) {
            return false;
        }
        for(int i = 0; i < a.length; i++) {
            if(a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean recursiveEquals(int i, int[] a, int[] b) {
        if(i >= a.length) {
            return true;
        } else if(a[i] != b[i]) {
            return false;
        } else {
            return recursiveEquals(++i, a, b);
        }
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
}
