package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class IntObjectAMTTest {

    /* Checks that after adding random mappings to an IntObjectAMT, the map contains all of the keys of that mapping and return
     * the values of those mappings when given their keys. */
    @Test
    public void testPutRandomMappings() {
        int[] keys = (new Random(422719)).ints(Integer.MIN_VALUE, Integer.MAX_VALUE).distinct().limit(100000).toArray();
        IntObjectAMT<String> map = new IntObjectAMT<>();
        for(int key : keys) {
            map.put(key, ""+key);
        }
        for(int key : keys) {
           assertTrue(map.contains(key));
           assertEquals(""+key, map.get(key));
        }
    }

    /* Checks that a mapping for Integer.MAX_VALUE can be added to IntObjectAMT without issue. */
    @Test
    public void testPutMaxInt() {
        IntObjectAMT<Object> map = new IntObjectAMT<>();
        int key = Integer.MAX_VALUE;
        Object value = new Object();
        map.put(key, value);
        assertTrue(map.contains(key));
        assertEquals(value, map.get(key));
    }

    /* Checks that a mapping for Integer.MIN_VALUE can be added to IntObjectAMT without issue. */
    @Test
    public void testPutMinInt() {
        IntObjectAMT<Object> map = new IntObjectAMT<>();
        int key = Integer.MIN_VALUE;
        Object value = new Object();
        map.put(key, value);
        assertTrue(map.contains(key));
        assertEquals(value, map.get(key));
    }

    /* Checks that putting a mapping from a key that already has a mapping in an IntObjectAMT to a new value result in the
     * key being associated with the new value. */
    @Test
    public void testReplaceMapping() {
        IntObjectAMT<String> map = new IntObjectAMT<>();
        int key = 77777;
        String originalValue = "original";
        String replacementValue = "replacement";
        map.put(key, originalValue);
        assertTrue(map.contains(key));
        assertEquals(originalValue, map.get(key));
        map.put(key, replacementValue);
        assertTrue(map.contains(key));
        assertEquals(replacementValue, map.get(key));
    }

    /* Checks that mappings removed from an IntObjectAMT are not considered to be contained in the map. */
    @Test
    public void testRemoveMappings() {
        int[] keys = (new Random(4227)).ints(Integer.MIN_VALUE, Integer.MAX_VALUE).distinct().limit(100000).toArray();
        IntObjectAMT<String> map = new IntObjectAMT<>();
        for(int key : keys) {
            map.put(key, "" + key);
        }
        for(int i = 0; i < keys.length; i++) {
            if(i%2 == 0) {
                assertEquals("" + keys[i], map.remove(keys[i]));
            }
        }
        for(int i = 0; i < keys.length; i++) {
            if(i%2 == 0) {
                assertFalse(map.contains(keys[i]));
            } else {
                assertTrue(map.contains(keys[i]));
            }
        }
    }

    /* Checks that isEmpty returns true for a newly created IntObjectAMT and one for which every key that has been added has also
     * been removed. Also checks that isEmpty returns false for an IntObjectAMT for which every key but one has been removed. */
    @Test
    public void testIsEmpty() {
        IntObjectAMT<String> map = new IntObjectAMT<>();
        assertTrue(map.isEmpty());
        int[] keys = (new Random(2719)).ints(Integer.MIN_VALUE, Integer.MAX_VALUE).distinct().limit(100000).toArray();
        for(int key : keys) {
            map.put(key, ""+key);
        }
        for(int i = 0; i < keys.length-1; i++) {
            map.remove(keys[i]);
        }
        assertFalse(map.isEmpty());
        map.remove(keys[keys.length-1]);
        assertTrue(map.isEmpty());
    }
}
