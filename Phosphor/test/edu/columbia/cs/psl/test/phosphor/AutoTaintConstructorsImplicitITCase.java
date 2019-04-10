package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;
import org.junit.Test;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;

public class AutoTaintConstructorsImplicitITCase extends BasePhosphorTest {
    
    /* Tests that a constructor that takes a char argument and is marked as source properly taints its non-primitive arguments. */
    @Test
    public void testPrimitiveSourceConstructor() {
        String str = new String("testPrimitiveSourceConstructor");
        AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors ex = new AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors('x', str);
        assertNonNullTaint(str);
    }

    /* Tests that a constructor that takes a char array argument and is marked as source properly taints its argument. */
    @Test
    public void testPrimitiveArrSourceConstructor() {
        char[] cs = new char[]{'x', 'y'};
        AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors ex = new AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors(cs);
        assertNonNullTaint(MultiTainter.getTaint(cs[0]));
    }

    /* Tests that a constructor that takes a 2D char array argument and is marked as source properly taints its argument. */
    @Test
    public void testPrimitive2DArrSourceConstructor() {
        char[][] csArr = new char[][]{{'x', 'y'}, {'x', 'y'}};
        AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors ex = new AutoTaintConstructorsObjTagITCase.ExampleWithSourceConstructors(csArr);
        assertNonNullTaint(MultiTainter.getTaint(csArr[0][0]));
    }

    /* Tests that a constructor that takes a char argument  and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitiveSinkConstructor() {
        char c = MultiTainter.taintedChar('x', "testPrimitiveSinkConstructor");
        new AutoTaintConstructorsObjTagITCase.ExampleWithSinkConstructors(c);
    }

    /* Tests that a constructor that takes a char array argument  and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitiveArrSinkConstructor() {
        char[] cs = MultiTainter.taintedCharArray(new char[]{'x', 'y'}, "testPrimitiveSinkConstructor");
        new AutoTaintConstructorsObjTagITCase.ExampleWithSinkConstructors(cs);
    }

    /* Tests that a constructor that takes a 2D char array argument and is marked as sink properly checks its argument for
     * taint tags. */
    @Test(expected = TaintSinkError.class)
    public void testPrimitive2DArrSinkConstructor() {
        char[] cs = MultiTainter.taintedCharArray(new char[]{'x', 'y'}, "testPrimitiveSinkConstructor");
        char[][] csArr = new char[][]{cs, cs};
        new AutoTaintConstructorsObjTagITCase.ExampleWithSinkConstructors(csArr);
    }
}

