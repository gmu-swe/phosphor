package edu.columbia.cs.psl.phosphor.struct;

import java.lang.ref.WeakReference;

/* Special case of PowerSetTree intended to maintain a collection of sets of integers. */
public class IntPowerSetTree {

    // Root of the tree, represents the empty set
    private final SetNode root;

    /* Constructs a new tree. Initializes the root node that represents the empty set. */
    private IntPowerSetTree() {
        this.root = new SetNode(0, null);
    }

    /* Returns the node representing the empty set. */
    public SetNode emptySet() {
        return root;
    }

    /* Returns a node representing the set containing only the specified element. */
    public SetNode makeSingletonSet(int element) {
        return root.addChild(element);
    }

    /* Represents some set in the collection. The set represented by some node contains the objects associated with
     * the keys of every node on the path from that node to the root of the tree. */
    public class SetNode {

        // The largest int in the set represented by this node
        private final int key;
        // The node that represents the set difference between this set and the singleton set containing this node's key
        private final SetNode parent;
        // Stores child nodes that represent the union of the set represented by this node with a singleton set containing
        // the key of the child node. Children is null until at least one child node is added.
        private IntObjectAMT<WeakReference<SetNode>> children;

        /* Constructs a new set node with no child nodes. */
        private SetNode(int key, SetNode parent) {
            this.key = key;
            this.parent = parent;
            this.children = null;
        }

        /* Adds a new entry to this node's map of child nodes for the specified key if one does not already exist.
         * Returns the child node for the specified key. */
        private SetNode addChild(int childKey) {
            synchronized(this) {
                if(children == null) {
                    // Initialize the child map
                    children = new IntObjectAMT<>();
                }
                if(!children.contains(childKey)) {
                    // There is no entry for child key
                    SetNode node = new SetNode(childKey, this);
                    children.put(childKey, new WeakReference<>(node));
                    return node;
                } else {
                    SetNode childNode = children.get(childKey).get();
                    if(childNode != null) {
                        // There is an existing non-garbage collected entry for the child key
                        return childNode;
                    } else {
                        // The entry for the child key has been garbage collected
                        SetNode node = new SetNode(childKey, this);
                        children.put(childKey, new WeakReference<>(node));
                        return node;
                    }
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
            IntSinglyLinkedList mergedList = new IntSinglyLinkedList();
            SetNode cur = this;
            // Maintain a sorted list of objects popped off from the two sets until one set is exhausted
            while(!cur.isEmpty() && !other.isEmpty()) {
                if(cur == other) {
                    break;
                } else if(cur.key == other.key) {
                    mergedList.push(cur.key);
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.key > other.key) {
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
        public SetNode add(int element) {
            IntSinglyLinkedList list = new IntSinglyLinkedList();
            SetNode cur = this;
            // Maintain a sorted list of objects popped off from this set until the right place to insert the new element
            // is found
            while(!cur.isEmpty()) {
                if(cur.key == element) {
                    // The specified element was already in the list
                    return this;
                } else if(cur.key > element) {
                    list.push(cur.key);
                    cur = cur.parent;
                } else {
                    // Found the correct spot to insert the new element into the path
                    break;
                }
            }
            list.push(element);
            // Move down the path in the tree for the list adding child nodes as necessary
            while(!list.isEmpty()) {
                cur = cur.addChild(list.pop());
            }
            return cur;
        }

        /* Returns whether the set represented by this node contains the specified element. */
        public boolean contains(int element) {
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                if(cur.key == element) {
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
                } else if(cur.key == other.key) {
                    cur = cur.parent;
                    other = other.parent;
                } else if(cur.key > other.key) {
                    cur = cur.parent;
                } else {
                    return false;
                }
            }
            return true;
        }

        /* Returns a list containing the elements of the set represented by this node. */
        public IntSinglyLinkedList toList() {
            IntSinglyLinkedList list = new IntSinglyLinkedList();
            // Walk to the root adding each visited nodes' key values to the list
            for(SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.push(cur.key);
            }
            return list;
        }
    }

    /* Returns the singleton instance of IntPowerSetTree. */
    public static IntPowerSetTree getInstance() {
        return IntPowerSetTreeSingleton.INSTANCE;
    }

    /* Inner class used to create the singleton instance of IntPowerSetTree. */
    private static class IntPowerSetTreeSingleton {
        private static final IntPowerSetTree INSTANCE = new IntPowerSetTree();
    }
}