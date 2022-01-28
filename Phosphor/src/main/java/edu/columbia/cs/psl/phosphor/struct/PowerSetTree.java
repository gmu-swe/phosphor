package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

/**
 * Provides access to a thread-safe collection of sets of objects by maintaining a trie-like tree structure. The set
 * represented by some node in the tree contains the objects associated with the keys of every node on the path from that
 * node to the root of the tree. Elements stored in the sets should be immutable and non-null. The elements of a set will
 * be equivalent to the objects stored in the set with respect to {@link Object#equals(Object)} method but not
 * necessarily referential equality.
 * <p>
 * Each element in some set represented in the tree is assigned a consistent, unique rank which is used
 * to totally order elements. Ranks strictly decrease along any path from a node to the root of the tree, i.e., a node
 * will only have child nodes with ranks that are greater than its own.
 */
public class PowerSetTree {
    /**
     * Root of the tree, represents the empty set.
     */
    private final SetNode root = new SetNode(null, null);

    /**
     * Constructs a new tree that contains only the empty set.
     */
    private PowerSetTree() {
    }

    /**
     * Resets the tree to its initial state, turning all reachable SetNodes into quasi-empty sets.
     * THREAD SAFETY WARNING: Other threads running concurrently might check to see if sets are empty while this method
     * runs. If so, there is no guarantee that they will see sets emptied until after this method returns.
     */
    public synchronized void reset() {
        RankPoolSingleton.POOL.reset();
        // Make all reachable nodes quasi-empty sets
        SinglyLinkedList<SetNode> nodeStack = new SinglyLinkedList<>();
        nodeStack.push(root);
        while (!nodeStack.isEmpty()) {
            SetNode node = nodeStack.pop();
            for (SetNode child : node.getChildren()) {
                nodeStack.push(child);
            }
            node.empty();
        }
    }

    /**
     * Returns the node representing the empty set.
     */
    public SetNode emptySet() {
        return root;
    }

    /**
     * Returns a node representing the set containing only the specified element.
     */
    public SetNode makeSingletonSet(Object element) {
        if (element == null) {
            // Null elements cannot be added to sets; return the node representing the empty set
            return emptySet();
        }
        return root.addChild(RankPoolSingleton.POOL.getRankedElement(element));
    }

    /* Returns the singleton tree instance. */
    public static PowerSetTree getInstance() {
        return PowerSetTreeSingleton.INSTANCE;
    }

    /**
     * Represents the set containing the elements associated with the keys of every node on the path from this node to
     * the root of the tree.
     */
    public static final class SetNode extends Taint {
        private static final long serialVersionUID = 7385461591854398858L;
        /**
         * The element with the greater rank in this set.
         */
        private transient RankedElement key;
        /**
         * The set containing every element of this set except for this set's {@code key}'s element.
         */
        private transient volatile SetNode parent;
        /**
         * Maps ranks to sets.
         * The set mapped to a rank is the union of this set and the singleton set containing the element associated
         * with that rank.
         * If this set has no child sets, then {@code children} is null.
         */
        private transient IntObjectAMT<WeakReference<SetNode>> children;

        /**
         * Constructs a new set that is the union of the specified parent set and the singleton set containing the
         * element associated with the specified key.
         */
        private SetNode(RankedElement key, SetNode parent) {
            this.key = key;
            this.parent = parent;
            this.children = null;
        }

        /**
         * Returns all non-null child of this set.
         */
        private synchronized SinglyLinkedList<SetNode> getChildren() {
            SinglyLinkedList<SetNode> list = new SinglyLinkedList<>();
            if (children != null) {
                for (WeakReference<SetNode> ref : children.values()) {
                    SetNode node = ref.get();
                    if (node != null) {
                        list.enqueue(node);
                    }
                }
            }
            return list;
        }

        /**
         * Empties the set represented by this node.
         */
        private synchronized void empty() {
            this.key = null;
            this.parent = null;
            this.children = null;
        }

        /**
         * Adds a new entry to this node's map of child nodes for the specified key if one does not already exist.
         * Returns the child node for the specified key.
         */
        private SetNode addChild(RankedElement childKey) {
            synchronized (this) {
                if (children == null) {
                    // Initialize the child map
                    children = new IntObjectAMT<>();
                }
                if (!children.contains(childKey.getRank())) {
                    // There is no entry for child key
                    SetNode node = new SetNode(childKey, this);
                    children.put(childKey.getRank(), new WeakReference<>(node));
                    return node;
                } else {
                    SetNode childNode = children.get(childKey.getRank()).get();
                    if (childNode != null) {
                        // There is an existing non-garbage collected entry for the child key
                        return childNode;
                    } else {
                        // The entry for the child key has been garbage collected
                        SetNode node = new SetNode(childKey, this);
                        children.put(childKey.getRank(), new WeakReference<>(node));
                        return node;
                    }
                }
            }
        }

        /**
         * Returns true if this set does not contain any elements.
         *
         * @return true if this set does not contain any elements
         */
        @Override
        public boolean isEmpty() {
            return this.parent == null;
        }

        @Override
        public String toString() {
            return "Taint [Labels = [" + toList() + "]";
        }

        @Override
        public boolean containsLabel(Object label) {
            if (label == null) {
                return true;
            }
            return contains(label);
        }

        @Override
        public Object[] getLabels() {
            return toList().toArray();
        }

        /**
         * Returns the union of this set and the specified other set.
         * Does not modify this set or the specified other set.
         *
         * @param other the other set
         * @return the union of this set and the specified other set.
         */
        public SetNode union(SetNode other) {
            SinglyLinkedList<RankedElement> mergedList = new SinglyLinkedList<>();
            // If this set is empty, ensure the canonical empty set is used
            SetNode cur = this.isEmpty() ? PowerSetTree.getInstance().emptySet() : this;
            // If the other set is empty ensure the node representing the empty set is used
            other = other.isEmpty() ? PowerSetTree.getInstance().emptySet() : other;
            // Maintain a sorted list of objects popped off from the two sets until one set is exhausted
            while (!cur.isEmpty() && !other.isEmpty()) {
                if (cur == other) {
                    break;
                } else if (cur.key.getRank() == other.key.getRank()) {
                    mergedList.push(cur.key);
                    cur = cur.parent;
                    other = other.parent;
                } else if (cur.key.getRank() > other.key.getRank()) {
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
            while (!mergedList.isEmpty()) {
                result = result.addChild(mergedList.pop());
            }
            return result;
        }

        @Override
        public SetNode union(Taint other) {
            if (other == null) {
                return this;
            }
            return this.union((SetNode) other);
        }

        /**
         * Returns the union of this set and the singleton set containing the specified element.
         * Does not modify this set.
         *
         * @return the union of this set and the singleton set containing the specified element
         */
        public SetNode add(Object element) {
            if (element == null) {
                return this;
            }
            RankedElement obj = RankPoolSingleton.POOL.getRankedElement(element);
            SinglyLinkedList<RankedElement> list = new SinglyLinkedList<>();
            // If this set is empty, ensure the canonical empty set is used
            SetNode cur = this.isEmpty() ? PowerSetTree.getInstance().emptySet() : this;
            // Maintain a sorted list of objects popped off from this set until the right place to insert the new element
            // is found
            while (!cur.isEmpty()) {
                if (cur.key.getRank() == obj.getRank()) {
                    // The specified element was already in the list
                    return this;
                } else if (cur.key.getRank() > obj.getRank()) {
                    list.push(cur.key);
                    cur = cur.parent;
                } else {
                    // Found the correct spot to insert the new element into the path
                    break;
                }
            }
            list.push(obj);
            // Move down the path in the tree for the list adding child nodes as necessary
            while (!list.isEmpty()) {
                cur = cur.addChild(list.pop());
            }
            return cur;
        }

        /**
         * Returns true if the specified element is in this set.
         *
         * @return true if the specified element is in this set
         */
        public boolean contains(Object element) {
            if (element == null || isEmpty()) {
                return false;
            }
            for (SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                if (cur.key.getElement().equals(element)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns true is this set is a superset of the specified other set.
         *
         * @return true is this set is a superset of the specified other set.
         */
        public boolean isSuperset(SetNode other) {
            SetNode cur = this;
            while (!other.isEmpty()) {
                if (cur.isEmpty()) {
                    return false;
                }
                if (cur == other) {
                    return true;
                } else if (cur.key.getRank() == other.key.getRank()) {
                    cur = cur.parent;
                    other = other.parent;
                } else if (cur.key.getRank() > other.key.getRank()) {
                    cur = cur.parent;
                } else {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns true is this set is a superset of the labels in taint tag.
         *
         * @return true is this set is a superset of the labels in taint tag
         */
        @Override
        public boolean isSuperset(Taint other) {
            if (other == null) {
                return true;
            }
            return isSuperset((SetNode) other);
        }

        /**
         * Returns a list containing the elements of this set.
         *
         * @return a list containing the elements of this set
         */
        public SinglyLinkedList<Object> toList() {
            SinglyLinkedList<Object> list = new SinglyLinkedList<>();
            // Walk to the root adding the objects associated with the nodes' key values to the list
            for (SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.push(cur.key.getElement());
            }
            return list;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeObject(toList());
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            SinglyLinkedList<?> labels = (SinglyLinkedList<?>) in.readObject();
            SetNode ret = (SetNode) Taint.emptyTaint();
            for (Object o : labels) {
                ret = ret.union(Taint.withLabel(o));
            }
            this.parent = ret;
        }

        private Object readResolve() {
            return this.parent;
        }
    }

    /**
     * Inner class used to create the singleton instance of PowerSetTree.
     */
    private static class PowerSetTreeSingleton {
        private static final PowerSetTree INSTANCE = new PowerSetTree();
    }

    private static class RankPoolSingleton {
        /**
         * Pool of ranks assigned to set elements. Maintains consistent ranks for elements that are in reachable sets.
         */
        private static final RankPool POOL = new RankPool();
    }
}