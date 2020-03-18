package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

public class ArrayCopyImplicitITCase extends BaseMultiTaintClass {

    @Test
    public void testControlTagPropagatesToCopiedElements() {
        int[] arr1 = new int[10];
        int[] arr2 = new int[]{1, 2, 3, 4, 5};
        boolean b = MultiTainter.taintedBoolean(true, 0);
        if(b) {
            System.arraycopy(arr2, 0, arr1, 2, arr2.length - 2);
        }
        for(int i : arr1) {
            if(i == 0) {
                assertNullOrEmpty(MultiTainter.getTaint(i));
            } else {
                assertTaintHasOnlyLabel(MultiTainter.getTaint(i), 0);
            }
        }
    }

    @Test
    public void testSourceTaintPropagatesToCopiedElements() {
        int[] arr1 = new int[10];
        int[] arr2 = new int[]{1, 2, 3, 4, 5};
        arr2 = MultiTainter.taintedReference(arr2, 0);
        System.arraycopy(arr2, 0, arr1, 2, arr2.length - 2);
        for(int i : arr1) {
            if(i == 0) {
                assertNullOrEmpty(MultiTainter.getTaint(i));
            } else {
                assertTaintHasOnlyLabel(MultiTainter.getTaint(i), 0);
            }
        }
    }
}
