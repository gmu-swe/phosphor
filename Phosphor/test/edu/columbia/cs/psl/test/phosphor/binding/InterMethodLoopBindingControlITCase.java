package edu.columbia.cs.psl.test.phosphor.binding;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

import static edu.columbia.cs.psl.test.phosphor.binding.GeneralBindingControlITCase.taintWithIndices;

public class InterMethodLoopBindingControlITCase extends BaseMultiTaintClass {

    @Test
    public void testReceiverInvariantParamInvariant() {
        int[] values = new int[]{1, 1, 0, 1};
        taintWithIndices(values);
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
        taintWithIndices(values);
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
        taintWithIndices(values);
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
        taintWithIndices(values);
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

    // Outer condition loop variant wrt outer loop
    // Inner condition loop variant wrt outer loop and inner loop
    // Assignment variant wrt outer loop
    @Test
    public void testNestedLoopsOuterVariantAssignment() {
        int[][] matrix = createTaintedMatrix();
        boolean[] conditions = createTaintedConditionArray();
        for(int r = 0; r < matrix.length; r++) { // outer loop
            if(conditions[r]) { // varies wrt outer loop
                // if conditions[r] && matrix[r] contains 0 then matrix[r][0] := 7
                setInvariantMemoryLocation(matrix[r]); // arg is variant wrt outer loop
            }
        }
        for(int[] row : matrix) {
            // only outer condition's tag should propagate
            assertTaintHasOnlyLabels(MultiTainter.getTaint(row[0]), -1);
        }
    }

    // Outer condition loop variant wrt outer loop
    // Inner condition loop variant wrt inner loop
    // Assignment variant wrt neither loop
    @Test
    public void testNestedLoopsInvariantAssignment() {
        int[][] matrix = createTaintedMatrix();
        boolean[] conditions = createTaintedConditionArray();
        for(int r = 0; r < matrix.length; r++) { // outer loop
            if(conditions[r]) { // varies wrt outer loop
                // if conditions[r] && matrix[0] contains 0 then matrix[0][0] := 7
                setInvariantMemoryLocation(matrix[0]); // arg is invariant wrt outer loop
            }
        }
        for(int[] row : matrix) {
            assertNullOrEmpty(MultiTainter.getTaint(row[0]));
        }
    }

    // Outer condition loop variant wrt outer loop
    // Inner condition loop variant wrt inner loop
    // Assignment variant wrt inner loop only
    @Test
    public void testNestedLoopsInnerVariantAssignment() {
        int[][] matrix = createTaintedMatrix();
        boolean[] conditions = createTaintedConditionArray();
        for(int r = 0; r < matrix.length; r++) { // outer loop
            if(conditions[r]) { // varies wrt outer loop
                // if conditions[r] && matrix[0][i] == 0 then matrix[0][i] := 7
                setVariantMemoryLocation(matrix[0]); // arg is invariant wrt outer loop
            }
        }
        for(int[] row : matrix) {
            assertTaintHasOnlyLabels(MultiTainter.getTaint(row[0]), 0);
        }
    }

    // Outer condition loop variant wrt outer loop
    // Inner condition loop variant wrt inner loop
    // Assignment variant wrt both the outer and the inner loop
    @Test
    public void testNestedLoopsVariantAssignment() {
        int[][] matrix = createTaintedMatrix();
        boolean[] conditions = createTaintedConditionArray();
        for(int r = 0; r < matrix.length; r++) { // outer loop
            if(conditions[r]) { // varies wrt outer loop
                // if conditions[r] && matrix[r][i] == 0 then matrix[r][i] := 7
                setVariantMemoryLocation(matrix[r]); // arg is invariant wrt outer loop
            }
        }
        for(int[] row : matrix) {
            assertTaintHasOnlyLabels(MultiTainter.getTaint(row[0]), 0, -1);
        }
    }

    private static void setInvariantMemoryLocation(int[] a) {
        for(int i = 0; i < a.length; i++) { // inner loop
            if(a[i] == 0) { // variant wrt inner loop, dependent on loop variance of a
                a[0] = 7; // invariant wrt inner loop, dependent on loop variance of a
                break;
            }
        }
    }

    private static void setVariantMemoryLocation(int[] a) {
        for(int i = 0; i < a.length; i++) { // inner loop
            if(a[i] == 0) { // variant wrt inner loop, dependent on loop variance of a
                a[i] = 7; // variant wrt to inner loop, dependent on loop variance of a
            }
        }
    }

    private static int[][] createTaintedMatrix() {
        int[][] matrix = new int[5][5];
        for(int r = 0; r < matrix.length; r++) {
            for(int c = 0; c < matrix[r].length; c++) {
                matrix[r][c] = MultiTainter.taintedInt(c, c);
            }
        }
        return matrix;
    }

    private static boolean[] createTaintedConditionArray() {
        boolean[] conditions = new boolean[5];
        for(int i = 0; i < conditions.length; i++) {
            conditions[i] = MultiTainter.taintedBoolean(true, -1);
        }
        return conditions;
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
