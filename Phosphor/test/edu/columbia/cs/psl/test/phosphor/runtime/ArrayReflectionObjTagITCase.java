package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.LinkedList;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ArrayReflectionObjTagITCase extends BaseMultiTaintClass {

    @Test
    public void testArrayGet() {
        boolean[] b = new boolean[] {false};
        assertTrue(b.getClass().isArray());
        for (int i = 0; i < Array.getLength(b); i++) {
            Array.get(b, i);
        }
    }

    @Test
    public void testRefArraySet() {
        int[] arr = new int[]{18};
        arr = MultiTainter.taintedIntArray(arr, "arr");
        Object obj = arr;
        int ret = Array.getInt(obj, 0);
        assertEquals(MultiTainter.getTaint(arr[0]), MultiTainter.getTaint(ret));
    }

    @Test
    public void testNewInstancePrimitiveComponentMultiDimensionalArray() {
        int[] expectedDimensions = new int[]{33, 2, 9};
        Object arr = Array.newInstance(int.class, expectedDimensions);
        LinkedList<Integer> actual = new LinkedList<>();
        for(Class<?> clazz = arr.getClass(); clazz.isArray(); clazz = clazz.getComponentType()) {
            actual.add(Array.getLength(arr));
            arr = Array.get(arr, 0);
        }
        int[] actualDimensions = new int[actual.size()];
        for(int i = 0; i < actualDimensions.length; i++) {
            actualDimensions[i] = actual.pop();
        }
        assertArrayEquals(expectedDimensions, actualDimensions);
    }

    @Test
    public void testNewInstancePrimitiveArrayComponentMultiDimensionalArray() {
        Object arr = Array.newInstance(int[].class, 33, 2, 9);
        assertTrue(arr instanceof int[][][][]);
    }

    @Test
    public void testTaintedPrimitiveArraySet() {
        int[] ia = new int[2];
        int val = MultiTainter.taintedInt(23, "label");
        Array.set(ia, 0, val);
        assertNonNullTaint(ia[0]);

        byte[] ba = new byte[2];
        Array.set(ba, 0, MultiTainter.taintedByte((byte) 2,"label"));
        assertNonNullTaint(ba[0]);

        char[] ca = new char[2];
        Array.set(ca, 0, MultiTainter.taintedChar((char) 2,"label"));
        assertNonNullTaint(ca[0]);

        long[] ja = new long[2];
        Array.set(ja, 0, MultiTainter.taintedLong(2L,"label"));
        assertNonNullTaint(ja[0]);
    }
}
