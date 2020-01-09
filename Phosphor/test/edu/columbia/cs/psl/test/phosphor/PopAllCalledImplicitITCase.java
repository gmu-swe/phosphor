package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PopAllCalledImplicitITCase extends BaseMultiTaintClass {

    private void throwsExceptionInFinally() {
        int[] a = null;
        int x = MultiTainter.taintedInt(8, "throwsExceptionInFinally");
        try {
            if(x > 0) {
                a[0] = 8;
            }
        } finally {
            a[0] = 7;
        }
    }

    @Test
    public void testExceptionThrownInFinally() {
        try {
            throwsExceptionInFinally();
        } catch(Exception e) {
            //
        }
        int y = 7;
        assertNullOrEmpty(MultiTainter.getTaint(y));
    }

    @Test
    public void testExceptionThrownBeforeSuperInit() {
        boolean b = MultiTainter.taintedBoolean(true, "b");
        Exception e = null;
        try {
            Child c = new Child(null, b);
        } catch(NullPointerException ex) {
            e = ex;
        }
        assertNotNull(e);
        int i = 7;
        assertNullOrEmpty(MultiTainter.getTaint(i));
    }

    @Test
    public void testBranchBeforeSuperInit() {
        boolean b = MultiTainter.taintedBoolean(true, "b");
        Child c = new Child(new int[1], b);
        int i = 7;
        assertNullOrEmpty(MultiTainter.getTaint(i));
        assertTaintHasOnlyLabel(MultiTainter.getTaint(c.i), "b");
    }

    private static class Parent {
        int i;

        Parent(int i) {
            this.i = i;
        }
    }

    private static class Child extends Parent {
        Child(int[] a, boolean b) {
            super(b ? a[0] : a[1]);
        }
    }
}
