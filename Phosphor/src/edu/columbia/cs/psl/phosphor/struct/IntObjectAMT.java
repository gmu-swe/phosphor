package edu.columbia.cs.psl.phosphor.struct;

/* Simple array mapped trie implementation used to maps integers to objects. Implementation is not threadsafe. */
public class IntObjectAMT<V> {

    // At each node of the AMT, the lower SHIFT_AMOUNT bits are used to determine the correct index into the
    // child array from a given key. SHIFT_AMOUNT is then used to shift these bits out of the key before passing it to a
    // child node.
    private static final int SHIFT_AMOUNT = 5;
    // The maximum size of a child array
    private static final int ARRAY_MAX_SIZE = 1 << SHIFT_AMOUNT;
    // Initial size of a non-empty child array
    private static final int ARRAY_INIT_SIZE = 4;
    // Used to determine the index of an int in the child array in place of the modulus operation. Since ARRAY_MAX_SIZE
    // is a power of 2, x & (ARRAY_MAX_SIZE -1) == x % ARRAY_MAX_SIZE.
    private static final int ARRAY_INDEX_MASK = ARRAY_MAX_SIZE - 1;
    // Pre-calculated masks used to isolate the bits less than a particularly index for a population count. For example,
    // if trying to determine the number of 1 bits before but index five in some int, x, x & POP_COUNT_MASKS[5] would
    // 0-out bits 31-5, so that only bits 0-4 are potentially set to 1.
    private static final int[] POP_COUNT_MASKS = new int[ARRAY_MAX_SIZE];

    static {
        int x = 0;
        for(int i = 0; i < POP_COUNT_MASKS.length; i++) {
            POP_COUNT_MASKS[i] = x;
            x = x << 1;
            x++;
        }
    }

    // The number of non-null element in the child array
    private int childrenSize;
    // Used to track which indices in a non-condensed child array contain elements. By counting the number of bits set to 1
    // that are lower than a particular bit index, the index into the condensed child array for a given bit index can be
    // calculated
    private int bitSet;
    // Condensed array of non-null children. A children is either a mapping or another IntObjectAMT.
    private Object[] children;

    /* Constructs a new empty map. */
    public IntObjectAMT() {
        this.childrenSize = 0;
        this.bitSet = 0;
        this.children = null;
    }

    /* Removes all mappings. */
    public void clear() {
        this.childrenSize = 0;
        this.bitSet = 0;
        this.children = null;
    }

    /* Returns true if this map contains 0 mappings. */
    public boolean isEmpty() {
        return childrenSize == 0;
    }

    /* Return the index in the condensed child array corresponding to the specified index in the non-condensed array. */
    private int getChildIndex(int index) {
        return Integer.bitCount(bitSet & POP_COUNT_MASKS[index]);
    }

    /* Returns whether a mapping exists for the specified key. */
    @SuppressWarnings("unchecked")
    public boolean contains(int key) {
        int index = key & ARRAY_INDEX_MASK;
        if(children == null || (bitSet & (1 << index)) == 0) {
            return false;
        }
        Object child = children[getChildIndex(index)];
        int childKey = key >>> SHIFT_AMOUNT;
        if(child instanceof IntObjectAMT) {
            return ((IntObjectAMT<V>) child).contains(childKey);
        } else {
            return ((Mapping) child).key == childKey;
        }
    }

    /* Returns the value associated with the specified key or null if the specified key is not in the map. */
    @SuppressWarnings("unchecked")
    public V get(int key) {
        int index = key & ARRAY_INDEX_MASK;
        if(children == null || (bitSet & (1 << index)) == 0) {
            return null;
        }
        Object child = children[getChildIndex(index)];
        int childKey = key >>> SHIFT_AMOUNT;
        if(child instanceof IntObjectAMT) {
            return ((IntObjectAMT<V>) child).get(childKey);
        } else {
            Mapping m = (Mapping) child;
            return (m.key == childKey) ? m.value : null;
        }
    }

    /* Puts a mapping associating the specified key with the specified value. Any existing mapping for the specified key
     * is replaced. */
    @SuppressWarnings("unchecked")
    public void put(int key, V value) {
        int index = key & ARRAY_INDEX_MASK;
        int childKey = key >>> SHIFT_AMOUNT;
        int childIndex = getChildIndex(index);
        if(children == null) {
            // Map has no children
            children = new Object[ARRAY_INIT_SIZE];
            children[childrenSize++] = new Mapping(childKey, value);
            bitSet |= (1 << index);
        } else if((bitSet & (1 << index)) == 0) {
            // Map does not have a child for this key
            if(childrenSize == children.length) {
                // Resize the array
                Object[] temp = new Object[children.length * 2];
                System.arraycopy(children, 0, temp, 0, childrenSize);
                children = temp;
            }
            // Shift children to make space for new child
            System.arraycopy(children, childIndex, children, childIndex + 1, childrenSize - childIndex);
            children[childIndex] = new Mapping(childKey, value);
            bitSet |= (1 << index);
            childrenSize++;
        } else {
            // Map has a child for this key
            Object child = children[childIndex];
            if(child instanceof IntObjectAMT) {
                ((IntObjectAMT<V>) child).put(childKey, value);
            } else if(((Mapping) child).key == childKey) {
                // Map contains a mapping for this key, replace its value
                ((Mapping) child).value = value;
            } else {
                // Map contains a mapping where this key should go that is not for this key
                Mapping m = (Mapping) child;
                IntObjectAMT<V> map = new IntObjectAMT<>();
                map.put(m.key, m.value);
                map.put(childKey, value);
                children[childIndex] = map;
            }
        }
    }

    /* Removes the child for in the specified un-condensed index located at the specified child index in the child array.
     * Shifts remaining children up in the array. */
    private void removeChild(int index, int childIndex) {
        bitSet &= ~(1 << index);
        childrenSize--;
        System.arraycopy(children, childIndex + 1, children, childIndex, childrenSize - childIndex);
        children[childrenSize] = null;
    }

    /* Removes the mapping for the specified key if such a mapping exists. Return the value mapped to the specified key
     * or null if no value was mapped to the specified key. */
    @SuppressWarnings("unchecked")
    public V remove(int key) {
        int index = key & ARRAY_INDEX_MASK;
        if(children == null || (bitSet & (1 << index)) == 0) {
            return null;
        }
        int childIndex = getChildIndex(index);
        Object child = children[childIndex];
        int childKey = key >>> SHIFT_AMOUNT;
        if(child instanceof IntObjectAMT) {
            V value = ((IntObjectAMT<V>) child).remove(childKey);
            if(((IntObjectAMT<V>) child).isEmpty()) {
                // Delete the emptied child
                removeChild(index, childIndex);
            }
            return value;
        } else {
            Mapping m = (Mapping) child;
            if(m.key != childKey) {
                return null;
            } else {
                removeChild(index, childIndex);
                return m.value;
            }
        }
    }

    /* Returns a list containing all of the values in the map. */
    @SuppressWarnings("unchecked")
    public SinglyLinkedList<V> values() {
        SinglyLinkedList<V> ret = new SinglyLinkedList<>();
        if(!isEmpty()) {
            for(Object child : children) {
                if(child instanceof IntObjectAMT) {
                    for(V value : ((IntObjectAMT<V>) child).values()) {
                        ret.enqueue(value);
                    }
                } else if(child != null) {
                    ret.enqueue(((Mapping) child).value);
                }
            }
        }
        return ret;
    }

    /* Stores a mapping from a key to a value. */
    private class Mapping {
        int key;
        V value;

        Mapping(int key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
