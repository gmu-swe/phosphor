package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;
import org.junit.Test;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;

public class AutoTaintConstructorsObjTagITCase extends BasePhosphorTest {
    public static class ExampleWithSourceConstructors {
        public char c;
        public char[] cs;
        public char[][] csArr;
        public String str;
        public ExampleWithSourceConstructors(char c, String s) {
            this.c = c;
            this.str = s;
        }

        public ExampleWithSourceConstructors(char[] cs) {
            this.cs = cs;
        }

        public ExampleWithSourceConstructors(char[][] csArr) {
            this.csArr = csArr;
        }
    }

    public static class ExampleWithSinkConstructors {
        public char c;
        public char[] cs;
        public char[][] csArr;
        public ExampleWithSinkConstructors(char c) {
            this.c = c;
        }

        public ExampleWithSinkConstructors(char[] cs) {
            this.cs = cs;
        }

        public ExampleWithSinkConstructors(char[][] csArr) {
            this.csArr = csArr;
        }
    }

    /* Tests that a constructor that takes a char argument and is marked as source properly taints its non-primitive arguments. */
    @Test
    public void testPrimitiveSourceConstructor() {
        String str = "testPrimitiveSourceConstructor";
        ExampleWithSourceConstructors ex = new ExampleWithSourceConstructors('x', str);
        assertNonNullTaint(ex.str);
    }

    /* Tests that a constructor that takes a char array argument and is marked as source properly taints its argument. */
    @Test
    public void testPrimitiveArrSourceConstructor() {
        char[] cs = new char[]{'x', 'y'};
        ExampleWithSourceConstructors ex = new ExampleWithSourceConstructors(cs);
        assertNonNullTaint(MultiTainter.getTaint(cs[0]));
    }

    /* Tests that a constructor that takes a 2D char array argument and is marked as source properly taints its argument. */
    @Test
    public void testPrimitive2DArrSourceConstructor() {
        char[][] csArr = new char[][]{{'x', 'y'}, {'x', 'y'}};
        ExampleWithSourceConstructors ex = new ExampleWithSourceConstructors(csArr);
        assertNonNullTaint(MultiTainter.getTaint(csArr[0][0]));
    }

    /* Tests that a constructor that takes a char argument  and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitiveSinkConstructor() {
        char c = MultiTainter.taintedChar('x', "testPrimitiveSinkConstructor");
       new ExampleWithSinkConstructors(c);
    }

    /* Tests that a constructor that takes a char array argument  and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitiveArrSinkConstructor() {
        char[] cs = MultiTainter.taintedCharArray(new char[]{'x', 'y'}, "testPrimitiveSinkConstructor");
        new ExampleWithSinkConstructors(cs);
    }

    /* Tests that a constructor that takes a 2D char array argument and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitive2DArrSinkConstructor() {
        char[] cs = MultiTainter.taintedCharArray(new char[]{'x', 'y'}, "testPrimitiveSinkConstructor");
        char[][] csArr = new char[][]{cs, cs};
        new ExampleWithSinkConstructors(csArr);
    }
}
