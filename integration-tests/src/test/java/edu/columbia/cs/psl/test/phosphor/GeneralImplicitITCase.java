package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class GeneralImplicitITCase extends BaseMultiTaintClass {
    String labelA = "a";
    String labelFoo = "Foo";

    @Test
    public void testTaintingOnFieldWrites() {
        int var = MultiTainter.taintedInt(1, "testTaintingOnFieldWrites");
        Holder h = new Holder();

        if(var == 1) {
            h.x = 40;
            h.j = 40L;
        }
        assertTaintHasOnlyLabel(MultiTainter.getTaint(h.x), "testTaintingOnFieldWrites");
        //TODO i don't think we should propogate in this way any more if we are planning to get rid of the $$PHOSPHORRTAG field of each object- JB
        // assertTaintHasOnlyLabel(MultiTainter.getTaint(h), "testTaintingOnFieldWrites");
    }

    public void testStringBuilderAppend() {
        int var = MultiTainter.taintedInt(1, "testStringBuilderAppend");
        StringBuilder sb = new StringBuilder();
        if(var == 1) {
            sb.append("ah ha!");
        }
        String ret = sb.toString();
        assertTaintHasOnlyLabel(MultiTainter.getStringCharTaints(ret)[0], "testStringBuilderAppend");
    }

    @Test
    public void testSimpleIf() {
        resetState();
        int i = MultiTainter.taintedInt(1, labelA);

        int k;

		if(i > 0)//control-tag-add-i
		{
			k = 5;
			assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelA);
		} else
		//control-tag-add-i
		{
			k = 6;
		}
        int f = MultiTainter.taintedInt(4, labelFoo);
//		System.out.println("F: " + f); //somehow causes f's taint to get stuck on the control flow tags
//		System.out.println(MultiTainter.getTaint(f));
        //control-tag-remove-i
        int r = 54;
        assertNullOrEmpty(MultiTainter.getTaint(r));

        switch(f) {
            case 0:
                r = 5;
                break;
            case 1:
                r = 6;
                break;
            case 2:
                r = 7;
                break;
            default:
                assertNullOrEmpty(MultiTainter.getTaint(r));

                foo(r);
                r = 111;
//			System.out.println(MultiTainter.getTaint(r));
                assertTaintHasOnlyLabel(MultiTainter.getTaint(r), labelFoo);
        }
        assertTaintHasOnlyLabel(MultiTainter.getTaint(i), labelA);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(f), labelFoo);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelA);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(r), labelFoo);
    }

    int foo(int in) {
        int k = 5;
        assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelFoo);
        assertNullOrEmpty(MultiTainter.getTaint(in));
		if(in > 5) {
			return 10;
		}
        return 12;
    }

    @Test
    public void testLoops() {
        boolean A = MultiTainter.taintedBoolean(true, "A");

        boolean x = false;
        int i = MultiTainter.taintedInt(5, "I");

        boolean z = false;
        boolean y = false;
        int j = MultiTainter.taintedInt(10, "J");
        while(i != 0) {
            i--;
            y = true;
            while(j != 0) {
                x = true;
                j--;
            }
            j++;
        }

        x = true;

        assertTaintHasOnlyLabels(MultiTainter.getTaint(j), "J", "I");
        assertTaintHasOnlyLabel(MultiTainter.getTaint(i), "I");
        assertTaintHasOnlyLabel(MultiTainter.getTaint(y), "I");
        assertNullOrEmpty(MultiTainter.getTaint(x));
    }

    @Test
    public void testRelationalConditional() {
        int x = -1;
        int y = -1;
        boolean b = true;

        Object labelX = "x";
        Object labelY = "y";
        Object labelB = "b";
        int xt = MultiTainter.taintedInt(x, labelX);
        int yt = MultiTainter.taintedInt(y, labelY);
        boolean bt = MultiTainter.taintedBoolean(b, labelB);

        int e = xt + 1;
        int f = yt + 2;
        assertTaintHasOnlyLabel(MultiTainter.getTaint(e), labelX);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(f), labelY);

        boolean b1 = e > 0;
        boolean b2 = f > -1;
        boolean b3 = e > -1 && bt;
        System.out.println(MultiTainter.getTaint(e));

        System.out.println(MultiTainter.getTaint(b1));
        assertTaintHasOnlyLabel(MultiTainter.getTaint(b1), labelX);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(b2), labelY);

        assertTaintHasOnlyLabels(MultiTainter.getTaint(b3), labelX, labelB);

        testA(xt, yt, bt);
    }

    @Test
    public void testStringContains() {
        resetState();
        String x = "afoo";
        String lbl = "taintedStr";
        MultiTainter.taintedObject(x, Taint.withLabel(lbl));
        boolean a = x.contains("a");
        assertTaintHasLabel(MultiTainter.getTaint(a), lbl);
        String x2 = "foo";
        MultiTainter.taintedObject(x, Taint.withLabel(lbl));
        boolean b = x.contains("a");
        assertTaintHasLabel(MultiTainter.getTaint(b), lbl);
    }

    @Test
    public void testBranchNotTaken() {
        resetState();
        boolean A = MultiTainter.taintedBoolean(false, "A");
        int i = 2;

        if(A) {
            i = 1;
        }
        assertTaintHasOnlyLabel(MultiTainter.getTaint(i), "A");

        double d = 2;

        if(A) {
            d = 1;
        }
        assertTaintHasOnlyLabel(MultiTainter.getTaint(d), "A");

    }

    @After
    public void resetState() {
        MultiTainter.getControlFlow().reset();
    }

    @Test
    public void testForLoopWithBreak() {
        char[] c = createDigitArray();
        for(int i = 0; i < c.length; i++) {
            if(c[i] == '0') {
                c[i] = '!';
                break;
            }
        }
        checkDigitArray(c);
    }

    @Test
    public void testForLoopWithoutBreak() {
        char[] c = createDigitArray();
        for(int i = 0; i < c.length; i++) {
            if(c[i] == '0') {
                c[i] = '!';
            }
        }
        checkDigitArray(c);
    }

    private char[] copyDigits(char[] c) {
        char[] copy = new char[c.length];
        for(int i = 0; i < c.length; i++) {
            if(c[i] == 'a') {
                throw new IllegalArgumentException();
            } else {
                copy[i] = c[i];
            }
        }
        return copy;
    }

    @Test
    public void testForLoopMultipleReturns() {
        char[] c = createDigitArray();
        char[] copy = copyDigits(c);
        for(int i = 0; i < copy.length; i++) {
            List<Object> labels = Arrays.asList(MultiTainter.getTaint(copy[i]).getLabels());
            assertEquals(i + 1, labels.size());
            for(int j = 0; j <= i; j++) {
                assertTrue(labels.contains(j));
            }
        }
    }

    public static void testA(int x, int y, boolean b) {

    }

    private static char[] createDigitArray() {
        char[] c = "0123456789".toCharArray();
        for(int i = 0; i < c.length; i++) {
            c[i] = MultiTainter.taintedChar(c[i], i);
        }
        return c;
    }

    private static void checkDigitArray(char[] c) {
        for(int i = 0; i < c.length; i++) {
            Taint t = MultiTainter.getTaint(c[i]);
            assertNotNull(t);
            Object[] labels = t.getLabels();
            assertArrayEquals(new Object[]{i}, labels);
        }
    }

    class Holder {
        int x;
        long j;
    }
}
