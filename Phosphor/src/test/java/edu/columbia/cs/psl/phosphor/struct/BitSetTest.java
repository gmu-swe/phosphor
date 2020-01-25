package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Test;

import static org.junit.Assert.*;

public class BitSetTest {

    /* Checks that BitSet.toList returns a list containing all of the bit indices added to the set. */
    @Test
    public void testBitSetToList() {
        BitSet set = new BitSet(2000);
        int[] addIndices = new int[]{63, 64, 100, 128, 199, 500, 1999};
        for(int index : addIndices) {
            set.add(index);
        }
        IntSinglyLinkedList list = set.toList();
        for(int index : addIndices) {
            assertTrue(list.contains(index));
        }
        assertEquals(addIndices.length, list.size());
    }

    /* Checks that the union of two BitSets contains any element contained in either of the two original sets. */
    @Test
    public void testBitSetUnion() {
        final int size = 20;
        BitSet set1 = new BitSet(size);
        set1.add(1);
        set1.add(2);
        set1.add(5);
        set1.add(14);
        BitSet set2 = new BitSet(size);
        set2.add(0);
        set2.add(2);
        set2.add(3);
        set2.add(19);
        BitSet union = BitSet.union(set1, set2);
        for(int i = 0; i < size; i++) {
            assertEquals(set1.contains(i) || set2.contains(i), union.contains(i));
        }
    }

    /* Checks that only the elements added to a BitSet are contained in the BitSet */
    @Test
    public void testBitSetAddContains() {
        boolean[] shouldAddIndex = new boolean[]{true, false, false, true, true, false, true, true, true, false, true};
        BitSet set = new BitSet(shouldAddIndex.length);
        for(int i = 0; i < shouldAddIndex.length; i++) {
            if(shouldAddIndex[i]) {
                set.add(i);
            }
        }
        for(int i = 0; i < shouldAddIndex.length; i++) {
            assertEquals(shouldAddIndex[i], set.contains(i));
        }
    }

    /* Checks that a large bit indices are correctly added. */
    @Test
    public void testBitSetLargeBitAdd() {
        BitSet set = new BitSet(2000);
        int[] addIndices = new int[]{63, 64, 100, 199, 128, 500, 1999};
        for(int index : addIndices) {
            set.add(index);
        }
        for(int index : addIndices) {
            assertTrue(set.contains(index));
        }
    }

    /* Checks that the copy of a BitSet is equal to the original with respect to the equals method, but not referentially
     * equal. Checks that changes to the copy of the BitSet do not impact the original set. */
    @Test
    public void testBitSetCopy() {
        BitSet set = new BitSet(10);
        set.add(0);
        set.add(3);
        set.add(5);
        set.add(9);
        BitSet copy = set.copy();
        assertEquals(set, copy);
        assertNotSame(set, copy);
        copy.add(4);
        assertNotEquals(set, copy);
    }

    /* Checks that isEmpty returns true for a BitSet with no bits set. */
    @Test
    public void testBitSetIsEmpty() {
        BitSet set = new BitSet(5);
        assertTrue(set.isEmpty());
    }

    /* Checks that isEmpty returns false for a BitSet with a bit set. */
    @Test
    public void testBitSetIsNotEmpty() {
        BitSet set = new BitSet(5);
        set.add(0);
        assertFalse(set.isEmpty());
    }

    /* Checks that isSuperset returns true when the BitSet instance is a superset of the BitSet argument. */
    @Test
    public void testBitSetIsSuperSet() {
        BitSet set1 = new BitSet(10);
        set1.add(1);
        set1.add(4);
        set1.add(9);
        BitSet set2 = new BitSet(10);
        set2.add(1);
        set2.add(9);
        assertTrue(set1.isSuperset(set2));
    }

    /* Checks that isSuperset returns false when the BitSet instance is a subset of the BitSet argument. */
    @Test
    public void testBitSetIsNotSuperSet() {
        BitSet set1 = new BitSet(10);
        set1.add(1);
        set1.add(4);
        set1.add(9);
        BitSet set2 = new BitSet(10);
        set2.add(1);
        set2.add(4);
        set2.add(5);
        set2.add(9);
        assertFalse(set1.isSuperset(set2));
    }

    /* Checks that two that two BitSets capable of storing the same maximum number of elements with the same bits set have
     * the same hashcode. */
    @Test
    public void testBitSetHashCode() {
        BitSet set1 = new BitSet(20);
        set1.add(1);
        set1.add(5);
        set1.add(19);
        BitSet set2 = new BitSet(20);
        set2.add(19);
        set2.add(5);
        set2.add(1);
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    /* Checks that two that two BitSets capable of storing the same maximum number of elements with the same bits set are
     * equal. */
    @Test
    public void testBitSetEquals() {
        BitSet set1 = new BitSet(20);
        set1.add(1);
        set1.add(5);
        set1.add(19);
        BitSet set2 = new BitSet(20);
        set2.add(19);
        set2.add(5);
        set2.add(1);
        assertEquals(set1, set2);
        // Check that equals is symmetric
        assertEquals(set2, set1);
    }


    /* Checks that two that two BitSets capable of storing the different maximum number of elements with the same bits set are
     * not equal. */
    @Test
    public void testBitSetEqualsDifferentMaxElements() {
        BitSet set1 = new BitSet(20);
        set1.add(1);
        set1.add(5);
        set1.add(19);
        BitSet set2 = new BitSet(200);
        set2.add(19);
        set2.add(5);
        set2.add(1);
        assertNotEquals(set1, set2);
    }

    /* Checks that the size of an empty BitSet is 0. */
    @Test
    public void testSizeOfEmptyBitSet() {
        BitSet set = new BitSet(10);
        assertEquals(0, set.size());
    }

    /* Checks that the size of a BitSet is the number of elements in the set. */
    @Test
    public void testSizeOfBitSet() {
        BitSet set = new BitSet(1000);
        int count = 0;
        for(int i = 1; i < 1000; i += 10) {
            set.add(i);
            count++;
        }
        assertEquals(count, set.size());
    }
}
