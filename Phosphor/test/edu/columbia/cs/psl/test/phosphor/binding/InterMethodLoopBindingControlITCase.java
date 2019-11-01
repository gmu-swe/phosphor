package edu.columbia.cs.psl.test.phosphor.binding;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

public class InterMethodLoopBindingControlITCase extends BaseMultiTaintClass {

    @Test
    public void testReceiverInvariantParamInvariant() {
        int[] values = new int[]{1, 1, 0, 1};
        GeneralBindingControlITCase.taintWithIndices(values);
        Example e = new Example();
        for(int i = 0; i < values.length; i++) {
            if(values[i] == 0) {
                // e refers to the same object on each loop iteration
                e.setX(5);
            }
        }
        assertNullOrEmpty(MultiTainter.getTaint(e));
    }

    @Test
    public void testReceiverVariantParamInvariant() {
        int[] values = new int[]{0, 1, 0, 1};
        GeneralBindingControlITCase.taintWithIndices(values);
        Example[] es = new Example[values.length];
        for(int i = 0; i < values.length; i++) {
            es[i] = new Example();
            if(values[i] == 0) {
                // es[i] refers to a different object on each loop iteration
                es[i].setX(5);
            }
        }
        for(int i = 0; i < values.length; i++) {
            Taint<?> tag = MultiTainter.getTaint(es[i]);
            if(i % 2 == 0) {
                assertTaintHasOnlyLabel(tag, i);
            } else {
                assertNullOrEmpty(tag);
            }
        }
    }

    @Test
    public void testReceiverInvariantParamVariant() {
        int[] values = new int[]{1, 1, 0, 1};
        GeneralBindingControlITCase.taintWithIndices(values);
        Example e = new Example();
        for(int i = 0; i < values.length; i++) {
            if(values[i] == 0) {
                // e refers to the same object on each loop iteration
                // i is a different value on each loop iteration
                e.setX(i);
            }
        }
        Taint<?> tag = MultiTainter.getTaint(e);
        assertTaintHasOnlyLabel(tag, 2);
    }

    @Test
    public void testReceiverVariantParamVariant() {
        int[] values = new int[]{0, 1, 0, 1};
        GeneralBindingControlITCase.taintWithIndices(values);
        Example[] es = new Example[values.length];
        for(int i = 0; i < values.length; i++) {
            es[i] = new Example();
            if(values[i] == 0) {
                // es[i] refers to a different object on each loop iteration
                // i is a different value on each loop iteration
                es[i].setX(i);
            }
        }
        for(int i = 0; i < values.length; i++) {
            Taint<?> tag = MultiTainter.getTaint(es[i]);
            if(i % 2 == 0) {
                assertTaintHasOnlyLabel(tag, i);
            } else {
                assertNullOrEmpty(tag);
            }
        }
    }

    private static class Example {
        private int x = 0;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }
}
