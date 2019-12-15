package edu.columbia.cs.psl;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

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
}
