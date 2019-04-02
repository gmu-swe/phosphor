package edu.columbia.cs.psl.phosphor.struct;

/* Provides access to a thread-safe pool of sets of objects by maintaining a tree structure. The set represented by
 * some node in the tree contains the key values of every node on the path from that node to the root of the tree.
 * Elements stored in the sets should be immutable or should not be mutated. Element in sets will be equivalent to the items
 * stored with respect to equals but not necessarily referential equality SetNodes originating from different pool instances
 * should not be used in operations together. */
public abstract class SetPool<K> {

    // The number of unique elements in sets in the pool, when updating uniqueElements synchronize on root.children first
    private volatile int uniqueElements;
    // Root of the tree, represents the empty set
    private final SetNode root;
    // Represents the type of the elements in the set
    private final Object type;

    /* Constructs a new empty pool. Initializes the root node that represents the empty set. And sets the number of unique
     * elements to be 0. */
    public SetPool() {
        this.root = new SetNode(null, -1, null);
        this.type = ((java.lang.reflect.ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.uniqueElements = 0;
    }

    /* Returns the node representing the empty set. */
    public SetNode emptySet() {
        return root;
    }

    /* Returns a node representing the set containing only the specified element. */
    public SetNode makeSingletonSet(K element) {
        if(element == null) {
            // Return the empty set
            return emptySet();
        }
        // Make a singleton set containing the element if one does not already exist in the pool
        synchronized(root.children) {
            if(!root.children.containsKey(element)) {
                root.children.put(element, new SetNode(element, uniqueElements++, root));
            }
            return root.children.get(element);
        }
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        } else if(object == null || !(object instanceof SetPool)) {
            return false;
        } else {
            SetPool<?> that = (SetPool<?>) object;
            return type.equals(that.type);
        }
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /* Represents some set in the pool. The set represented by a node contains the key values of every node on the path
     * from that node to the root node of the pool. */
    public class SetNode {

        // The largest element in the set represented by this node
        private final K key;
        // Used to impose a totally ordering on unique elements/keys
        private final int rank;
        // The node that represents the set difference between this set and the singleton set containing this node's key
        private final SetNode parent;
        // Maps a key whose associated rank is larger than this node's rank to a node representing the union of the set
        // represented by this node with a singleton set containing the key
        private final SimpleHashMap<K, SetNode> children;

        /* Construct a new set node. */
        private SetNode(K key, int rank, SetNode parent) {
            this.key = key;
            this.rank = rank;
            this.parent = parent;
            this.children = new SimpleHashMap<>();
        }

        /* Returns whether this node represents the empty set. */
        public boolean isEmpty() {
            return this == root;
        }

        /* Returns a node that represents the set union of the set represented by this node with the set represented by
         * the specified other node. Does not change the elements contained by the sets represented by this node or the
         * other node. */
        public SetNode union(SetNode other) {
            if(other == null) {
                return this;
            }
            LinkedList<K> mergedKeysList = new LinkedList<>();
            LinkedList<Integer> mergedRanksList = new LinkedList<>();
            SetNode cur = this;
            // Maintain a sorted list of unique values popped off from the two sets until one set is exhausted
            while(!cur.isEmpty() && !other.isEmpty()) {
                if(cur.rank == other.rank) {
                    mergedKeysList.addFast(cur.key);
                    mergedRanksList.addFast(cur.rank);
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.rank > other.rank) {
                    mergedKeysList.addFast(cur.key);
                    mergedRanksList.addFast(cur.rank);
                    cur = cur.parent;
                } else {
                    mergedKeysList.addFast(other.key);
                    mergedRanksList.addFast(other.rank);
                    other = other.parent;
                }
            }
            // Find the node for the non-exhausted set
            SetNode result = cur.isEmpty() ? other : cur;
            // Move down the path in the tree for the merged list adding nodes as necessary
            while(!mergedKeysList.isEmpty()) {
                K poppedKey = mergedKeysList.pop();
                int poppedRank = mergedRanksList.pop();
                synchronized(result.children) {
                    if(!result.children.containsKey(poppedKey)) {
                        result.children.put(poppedKey, new SetNode(poppedKey, poppedRank, result));
                    }
                    result = result.children.get(poppedKey);
                }
            }
            return result;
        }

        /* Return a node representing the set union of the set represented by this node and the singleton set containing
         * the specified element. Does not change the elements contained by the set represented by this node. */
        public SetNode singletonUnion(K element) {
            return union(makeSingletonSet(element));
        }

        /* Returns whether the set represented by this node contains the specified element. */
        public boolean contains(Object element) {
            if(element == null || isEmpty()) {
                return false;
            }
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                if(cur.key.equals(element)) {
                    return true;
                }
            }
            return false;
        }

        /* Returns whether the set represented by this node is a superset of the set represented by the specified other
         * node. */
        public boolean isSuperset(SetNode other) {
            if(other == null) {
                return true;
            }
            SetNode cur = this;

            while(!other.isEmpty()) {
                if(cur.isEmpty()) {
                    return false;
                }
                if(cur.rank == other.rank) {
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.rank > other.rank) {
                    cur = cur.parent;
                } else {
                    return false;
                }
            }
            return true;
        }

        /* Returns a list containing the elements of the set represented by this node. */
        public LinkedList<K> toList() {
            LinkedList<K> list = new LinkedList<>();
            // Walk to the root adding non-null key values to front of the list
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.add(cur.key);
            }
            return list;
        }
    }

    /* Provides access to the single instance of SetPool<Object> */
    public static SetPool<Object> getInstance() {
        return SetPoolSingleton.INSTANCE;
    }

    /* Inner class used to provide access to singleton instance of SetPool<Object> */
    private static class SetPoolSingleton {
        private static final SetPool<Object> INSTANCE = new SetPool<Object>(){};
    }
}