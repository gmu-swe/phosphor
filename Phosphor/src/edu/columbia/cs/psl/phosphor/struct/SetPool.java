package edu.columbia.cs.psl.phosphor.struct;

import java.lang.ref.WeakReference;

/* Provides access to a thread-safe pool of sets of objects by maintaining a tree structure. The set represented by some
 * node in the tree contains the objects associated with the keys of every node on the path from that node to the root of
 * the tree. Elements stored in the sets should be immutable. The elements of a sets will be equivalent to the objects
 * stored in the set with respect to equals but not necessarily referential equality. */
public class SetPool {

    // Root of the tree, represents the empty set
    private final SetNode root;
    // The number that should be assigned as the next rank
    private volatile int nextId = Integer.MIN_VALUE;
    // Map objects to (object,rank) records
    private final SimpleHashMap<Object, RankedObject> rankMap = new SimpleHashMap<>();

    /* Constructs a new empty pool. Initializes the root node that represents the empty set. */
    private SetPool() {
        this.root = new SetNode(null, null);
    }

    /* Returns a new RankedObject if none existed for the specified object in the rankMap or the existing
     * RankedObject for the specified object if a mapping already existed in the rankMap. */
    private synchronized RankedObject getRankedObject(Object object) {
        if(!rankMap.containsKey(object)) {
            rankMap.put(object, new RankedObject(object, nextId++));
        }
        return rankMap.get(object);
    }

    /* Returns the node representing the empty set. */
    public SetNode emptySet() {
        return root;
    }

    /* Returns a node representing the set containing only the specified element. */
    public SetNode makeSingletonSet(Object element) {
        if(element == null) {
            // Return the empty set
            return emptySet();
        }
        return root.addChild(getRankedObject(element));
    }

    /* Represents some set in the pool. The set represented by some node contains the objects associated with
     * the keys of every node on the path from that node to the root of the tree. */
    public class SetNode {

        // Holds an object and its associated rank. This object is the object with the highest rank in the set
        // represented by this node.
        private final RankedObject key;
        // The node that represents the set difference between this set and the singleton set containing the object
        // associated with this node's key
        private final SetNode parent;
        // Maps a key with a rank larger than the rank of this node's key to a node representing the union of the set
        // represented by this node with a singleton set containing the object associated with that key.
        private final SimpleHashMap<RankedObject, WeakReference<SetNode>> children;

        /* Constructs a new set node with no child nodes. */
        private SetNode(RankedObject key, SetNode parent) {
            this.key = key;
            this.parent = parent;
            this.children = new SimpleHashMap<>();
        }

        /* Adds a new entry to this node's map of child nodes for the specified key if one does not already exist.
         * Returns the child node mapped to the specified key. */
        private SetNode addChild(RankedObject childKey) {
            synchronized(children) {
                if (!children.containsKey(childKey) || children.get(childKey).get() == null) {
                    // If there is no mapping for the specified key or the mapping's value has been garbage collected.
                    SetNode node = new SetNode(childKey, this);
                    children.put(childKey, new WeakReference<SetNode>(node));
                    return node;
                } else {
                    return children.get(childKey).get();
                }
            }
        }

        /* Returns whether this node represents the empty set. */
        public boolean isEmpty() {
            return this == root;
        }

        /* Returns a node that represents the set union of the set represented by this node with the set represented by
         * the specified other node. Does not change the elements contained by the sets represented by either original node */
        public SetNode union(SetNode other) {
            if(other == null) {
                return this;
            }
            LinkedList<RankedObject> mergedList = new LinkedList<>();
            SetNode cur = this;
            // Maintain a sorted list of objects popped off from the two sets until one set is exhausted
            while(!cur.isEmpty() && !other.isEmpty()) {
                if(cur.key.rank == other.key.rank) {
                    mergedList.addFast(cur.key);
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.key.rank > other.key.rank) {
                    mergedList.addFast(cur.key);
                    cur = cur.parent;
                } else {
                    mergedList.addFast(other.key);
                    other = other.parent;
                }
            }
            // Find the node for the non-exhausted set
            SetNode result = cur.isEmpty() ? other : cur;
            // Move down the path in the tree for the merged list adding child nodes as necessary
            while(!mergedList.isEmpty()) {
                result = result.addChild(mergedList.pop());
            }
            return result;
        }

        /* Return a node representing the set union of the set represented by this node and the singleton set containing
         * the specified element. Does not change the elements contained by the set represented by this node. */
        public SetNode singletonUnion(Object element) {
            return union(makeSingletonSet(element));
        }

        /* Returns whether the set represented by this node contains the specified element. */
        public boolean contains(Object element) {
            if(element == null || isEmpty()) {
                return false;
            }
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                if(cur.key.object.equals(element)) {
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
                if(cur.key.rank == other.key.rank) {
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.key.rank > other.key.rank) {
                    cur = cur.parent;
                } else {
                    return false;
                }
            }
            return true;
        }

        /* Returns a list containing the elements of the set represented by this node. */
        public LinkedList<Object> toList() {
            LinkedList<Object> list = new LinkedList<>();
            // Walk to the root adding the objects associated with the nodes' key values to the list
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.add(cur.key.object);
            }
            return list;
        }
    }

    /* Record type that associates an object with a unique integer. Used to maintain a consistent rank value for a
     * particular object. */
    private class RankedObject {

        // The object itself
        private Object object;
        // Unique integer used to order the object in sets
        private int rank;

        /* Constructs a new ranked object with the specified rank and object. */
        private RankedObject(Object object, int rank) {
            this.object = object;
            this.rank = rank;
        }
    }

    /* Provides access to the single instance of SetPool. */
    public static SetPool getInstance() {
        return SetPoolSingleton.INSTANCE;
    }

    /* Inner class used to provide access to singleton instance of SetPool. */
    private static class SetPoolSingleton {
        private static final SetPool INSTANCE = new SetPool();
    }
}