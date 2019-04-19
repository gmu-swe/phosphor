package edu.columbia.cs.psl.phosphor.struct;

import java.lang.ref.WeakReference;

/* Provides access to a thread-safe collection of sets of objects by maintaining a trie-like tree structure. The set
 * represented by some node in the tree contains the objects associated with the keys of every node on the path from that
 * node to the root of the tree. Elements stored in the sets should be immutable and non-null. The elements of a set will
 *  be equivalent to the objects stored in the set with respect to the equals method but not necessarily referential equality.
 *
 * Each element in some set represented in the structure is assigned a consistent, unique rank which is used
 * to total order set elements. Ranks strictly decrease along any path from a node to the root of the tree, that is a node
 * will only have child nodes with higher ranks that its own. */
public class PowerSetTree {

    // The current capacity of elementRankList
    private volatile int currentCapacity;
    // Stores the unique elements across all of the sets in the collection with their rank. The position of an element
    // in this array determines its rank.
    private ArrayList<WeakReference<RankedObject>> elementRankList;
    // Root of the tree, represents the empty set
    private final SetNode root;

    /* Constructs a new empty pool. Initializes the root node that represents the empty set. */
    private PowerSetTree() {
        this.currentCapacity = 100;
        this.elementRankList = new ArrayList<>(currentCapacity);
        this.root = new SetNode(null, null);
    }

    /* Stores the specified object in the elementRankList if an equal object is not already represented in the list. Returns
     * the record object for objects equal to the specified object. This record contains the an object equal to the specified
     * object and the rank assigned to objects equal to the specified object. */
    private synchronized RankedObject getRankedObject(Object object) {
        // The lowest index in the array that was either null or set to null because the referent of the WeakReference
        // at that index was garbage collected
        int lowestGCEntry = -1;
        for(int i = 0; i < elementRankList.size(); i++) {
            WeakReference<RankedObject> ref = elementRankList.get(i);
            if(ref == null) {
                lowestGCEntry = (lowestGCEntry == -1) ? i : lowestGCEntry;
            } else {
                RankedObject rankedElement = ref.get();
                if(rankedElement == null) {
                    elementRankList.replace(i, null);
                    lowestGCEntry = (lowestGCEntry == -1) ? i : lowestGCEntry;
                } else if(object.equals(rankedElement.object)) {
                    // Found an object represented in the list equal to the specified object
                    return rankedElement;
                }
            }
        }
        if(lowestGCEntry == -1) {
            // No garbage collected or null entries were found
            RankedObject result = new RankedObject(object, elementRankList.size());
            elementRankList.add(new WeakReference<>(result));
            // Update the capacity in case it changed
            this.currentCapacity = elementRankList.getCapacity();
            return result;
        } else {
            RankedObject result = new RankedObject(object, lowestGCEntry);
            elementRankList.replace(lowestGCEntry, new WeakReference<>(result));
            return result;
        }
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

    /* Represents some set in the collection. The set represented by some node contains the objects associated with
     * the keys of every node on the path from that node to the root of the tree. */
    public class SetNode {

        // Holds an object and its associated rank. This object is the object with the highest rank in the set
        // represented by this node.
        private final RankedObject key;
        // The node that represents the set difference between this set and the singleton set containing the object
        // associated with this node's key
        private final SetNode parent;
        // Stores child nodes that represent the union of the set represented by this node with a singleton set containing the
        // object associated with the key of the child node.
        private WeakReference<SetNode>[] children;

        /* Constructs a new set node with no child nodes. */
        @SuppressWarnings("unchecked")
        private SetNode(RankedObject key, SetNode parent) {
            this.key = key;
            this.parent = parent;
            this.children = (WeakReference<SetNode>[]) new WeakReference[currentCapacity];
        }

        /* Adds a new entry to this node's array child nodes for the specified key if one does not already exist.
         * Returns the child node for the specified key. */
        @SuppressWarnings("unchecked")
        private SetNode addChild(RankedObject childKey) {
            synchronized(this) {
                int curRank = (key == null) ? -1 : key.rank;
                // Store the child at the difference between its rank and the lowest possible rank for a child of this node
                int childPosition = childKey.rank - (curRank + 1);
                if(childPosition >= children.length) {
                    // Resize the child array
                    WeakReference<SetNode>[] temp = this.children;
                    this.children = (WeakReference<SetNode>[]) new WeakReference[currentCapacity];
                    System.arraycopy(temp, 0, this.children, 0, temp.length);
                }
                if(children[childPosition] == null) {
                    // There is no entry at the child position
                    SetNode node = new SetNode(childKey, this);
                    children[childPosition] = new WeakReference<>(node);
                    return node;
                }
                SetNode childNode = children[childPosition].get();
                if(childNode == null || childNode.key != childKey) {
                    // The entry at the child position has been garbage collected or is going to be garbage collected
                    SetNode node = new SetNode(childKey, this);
                    children[childPosition] = new WeakReference<>(node);
                    return node;
                } else {
                    return childNode;
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
            SimpleLinkedList<RankedObject> mergedList = new SimpleLinkedList<>();
            SetNode cur = this;
            // Maintain a sorted list of objects popped off from the two sets until one set is exhausted
            while(!cur.isEmpty() && !other.isEmpty()) {
                if(cur == other) {
                    break;
                }
                if(cur.key.rank == other.key.rank) {
                    mergedList.push(cur.key);
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.key.rank > other.key.rank) {
                    mergedList.push(cur.key);
                    cur = cur.parent;
                } else {
                    mergedList.push(other.key);
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
                if(cur == other) {
                    return true;
                } else if(cur.key.rank == other.key.rank) {
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
        public SimpleLinkedList<Object> toList() {
            SimpleLinkedList<Object> list = new SimpleLinkedList<>();
            // Walk to the root adding the objects associated with the nodes' key values to the list
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.push(cur.key.object);
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

    /* Returns the singleton instance of PowerSetTree. */
    public static PowerSetTree getInstance() {
        return PowerSetTreeSingleton.INSTANCE;
    }

    /* Inner class used to create the singleton instance of PowerSetTree. */
    private static class PowerSetTreeSingleton {
        private static final PowerSetTree INSTANCE = new PowerSetTree();
    }
}