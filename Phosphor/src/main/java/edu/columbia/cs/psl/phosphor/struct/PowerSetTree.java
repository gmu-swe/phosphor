package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;

/* Provides access to a thread-safe collection of sets of objects by maintaining a trie-like tree structure. The set
 * represented by some node in the tree contains the objects associated with the keys of every node on the path from that
 * node to the root of the tree. Elements stored in the sets should be immutable and non-null. The elements of a set will
 *  be equivalent to the objects stored in the set with respect to the equals method but not necessarily referential equality.
 *
 * Each element in some set represented in the structure is assigned a consistent, unique rank which is used
 * to total order set elements. Ranks strictly decrease along any path from a node to the root of the tree, that is a node
 * will only have child nodes with higher ranks that its own. */
public class PowerSetTree {

    // Root of the tree, represents the empty set
    private final SetNode root;
    // Used to lazily reused ranks after the object assigned the rank is garbage collected
    private final IntSinglyLinkedList rankQueue;
    // Maps hash codes to a list of pairs of containing an object with that hashcode and a unique rank for that object.
    private IntObjectAMT<SinglyLinkedList<RankReference>> rankMap;
    // The next new rank that should be assigned to an object
    private int nextRank;

    /* Constructs a new empty pool. Initializes the root node that represents the empty set. */
    private PowerSetTree() {
        this.root = new SetNode(null, null);
        this.rankMap = new IntObjectAMT<>();
        this.rankQueue = new IntSinglyLinkedList();
        this.nextRank = Integer.MIN_VALUE;
    }

    /* Resets the tree to its initial state, turning all reachable SetNodes into quasi-empty sets.
    * THREAD SAFETY WARNING: Other threads running concurrently might check to see if taints are empty while this method
    * runs. If so, there is no guarantee that they will see taints emptied until after this method returns.
    * */
    public synchronized void reset() {
        this.rankMap.clear();
        this.rankQueue.clear();
        this.nextRank = Integer.MIN_VALUE;
        // Make all reachable nodes quasi-empty sets
        SinglyLinkedList<SetNode> nodeStack = new SinglyLinkedList<>();
        nodeStack.push(root);
        while(!nodeStack.isEmpty()) {
            SetNode node = nodeStack.pop();
            for(SetNode child : node.getChildren()) {
                nodeStack.push(child);
            }
            node.empty();
        }
    }

    /* If a rank can be reused from the rankQueue, returns that rank. Otherwise returns and increments nextRank. */
    private int getAvailableRank() {
        if(!rankQueue.isEmpty()) {
            // Try to reuse a rank
            return rankQueue.pop();
        } else {
            // There are no available ranks to be reused
            return nextRank++;
        }
    }

    /* Stores the specified object in the rankMap if an equal object is not already represented in the list. Returns
     * the record object for objects equal to the specified object. This record contains the an object equal to the specified
     * object and the rank assigned to objects equal to the specified object. */
    private synchronized RankedObject getRankedObject(Object object) {
        int hash = object.hashCode();
        if(!rankMap.contains(hash)) {
            SinglyLinkedList<RankReference> list = new SinglyLinkedList<>();
            rankMap.put(hash, list);
            RankedObject ret = new RankedObject(object, getAvailableRank());
            list.push(new RankReference(ret));
            return ret;
        } else {
            SinglyLinkedList<RankReference> list = rankMap.get(hash);
            Iterator<RankReference> it = list.iterator();
            while(it.hasNext()) {
                RankReference ref = it.next();
                RankedObject ro = ref.get();
                if(ro == null) {
                    // Remove reference with garbage collected referent from list
                    it.remove();
                    // Push the rank of the garbage collected object onto the stack so that it can be reused
                    rankQueue.push(ref.rank);
                } else if(object.equals(ro.object)) {
                    // Existing rank for the specified object was found
                    return ro;
                }
            }
            // No existing rank for the specified object was found
            RankedObject ret = new RankedObject(object, getAvailableRank());
            list.push(new RankReference(ret));
            return ret;
        }
    }

    /* Returns the node representing the empty set. */
    public SetNode emptySet() {
        return root;
    }

    /* Returns a node representing the set containing only the specified element. */
    public SetNode makeSingletonSet(Object element) {
        if(element == null) {
            // Null elements cannot be added to sets; return the node representing the empty set
            return emptySet();
        }
        return root.addChild(getRankedObject(element));
    }

    /* Returns the singleton instance of PowerSetTree. */
    public static PowerSetTree getInstance() {
        return PowerSetTreeSingleton.INSTANCE;
    }

    /* Represents some set in the collection. The set represented by some node contains the objects associated with
     * the keys of every node on the path from that node to the root of the tree. */
    public static final class SetNode extends Taint {

        private static final long serialVersionUID = 7385461591854398858L;
        // Holds an object and its associated rank. This object is the object with the highest rank in the set
        // represented by this node.
        private transient RankedObject key;
        // The node that represents the set difference between this set and the singleton set containing the object
        // associated with this node's key
        private transient volatile SetNode parent;
        // Stores child nodes that represent the union of the set represented by this node with a singleton set containing
        // the key of the child node. Children is null until at least one child node is added.
        private transient IntObjectAMT<WeakReference<SetNode>> children;

        /* Constructs a new set node with no child nodes. */
        private SetNode(RankedObject key, SetNode parent) {
            this.key = key;
            this.parent = parent;
            this.children = null;
        }

        /* Returns all non-null child nodes of this node. */
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

        /* Empties the set represented by this node. */
        private synchronized void empty() {
            this.key = null;
            this.parent = null;
            this.children = null;
        }

        /* Adds a new entry to this node's map of child nodes for the specified key if one does not already exist.
         * Returns the child node for the specified key. */
        private SetNode addChild(RankedObject childKey) {
            synchronized (this) {
                if (children == null) {
                    // Initialize the child map
                    children = new IntObjectAMT<>();
                }
                if (!children.contains(childKey.rank)) {
                    // There is no entry for child key
                    SetNode node = new SetNode(childKey, this);
                    children.put(childKey.rank, new WeakReference<>(node));
                    return node;
                } else {
                    SetNode childNode = children.get(childKey.rank).get();
                    if (childNode != null) {
                        // There is an existing non-garbage collected entry for the child key
                        return childNode;
                    } else {
                        // The entry for the child key has been garbage collected
                        SetNode node = new SetNode(childKey, this);
                        children.put(childKey.rank, new WeakReference<>(node));
                        return node;
                    }
                }
            }
        }

        /* Returns whether this node represents the empty set or a quasi-empty set. */
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

        /* Returns a node that represents the set union of the set represented by this node with the set represented by
         * the specified other node. Does not change the elements contained by the sets represented by either original node */
        public SetNode union(Taint _other) {
            if (_other == null) {
                return this;
            }
            SetNode other = (SetNode) _other;
            SinglyLinkedList<RankedObject> mergedList = new SinglyLinkedList<>();
            // If the this set is empty ensure the node representing the empty set is used
            SetNode cur = this.isEmpty() ? PowerSetTree.getInstance().emptySet() : this;
            // If the other set is empty ensure the node representing the empty set is used
            other = other.isEmpty() ? PowerSetTree.getInstance().emptySet() : other;
            // Maintain a sorted list of objects popped off from the two sets until one set is exhausted
            while (!cur.isEmpty() && !other.isEmpty()) {
                if (cur == other) {
                    break;
                } else if (cur.key.rank == other.key.rank) {
                    mergedList.push(cur.key);
                    cur = cur.parent;
                    other = other.parent;
                } else if (cur.key.rank > other.key.rank) {
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

        /* Return a node representing the set union of the set represented by this node and the singleton set containing
         * the specified element. Does not change the elements contained by the set represented by this node. */
        public SetNode add(Object element) {
            if (element == null) {
                return this;
            }
            RankedObject obj = PowerSetTree.getInstance().getRankedObject(element);
            SinglyLinkedList<RankedObject> list = new SinglyLinkedList<>();
            // If the this set is empty ensure the node representing the empty set is used
            SetNode cur = this.isEmpty() ? PowerSetTree.getInstance().emptySet() : this;
            // Maintain a sorted list of objects popped off from this set until the right place to insert the new element
            // is found
            while (!cur.isEmpty()) {
                if (cur.key.rank == obj.rank) {
                    // The specified element was already in the list
                    return this;
                } else if (cur.key.rank > obj.rank) {
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

        /* Returns whether the set represented by this node contains the specified element. */
        public boolean contains(Object element) {
            if (element == null || isEmpty()) {
                return false;
            }
            for (SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                if (cur.key.object.equals(element)) {
                    return true;
                }
            }
            return false;
        }

        /* Returns whether the set represented by this node is a superset of the set represented by the specified other
         * node. */
        public boolean isSuperset(Taint _other) {
            if (_other == null) {
                return true;
            }
            SetNode other = (SetNode) _other;
            SetNode cur = this;
            while (!other.isEmpty()) {
                if (cur.isEmpty()) {
                    return false;
                }
                if (cur == other) {
                    return true;
                } else if (cur.key.rank == other.key.rank) {
                    cur = cur.parent;
                    other = other.parent;
                } else if (cur.key.rank > other.key.rank) {
                    cur = cur.parent;
                } else {
                    return false;
                }
            }
            return true;
        }

        /* Returns a list containing the elements of the set represented by this node. */
        public SinglyLinkedList<Object> toList() {
            SinglyLinkedList<Object> list = new SinglyLinkedList<>();
            // Walk to the root adding the objects associated with the nodes' key values to the list
            for (SetNode cur = this; !cur.isEmpty(); cur = cur.parent) {
                list.push(cur.key.object);
            }
            return list;
        }


        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeObject(toList());
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            SinglyLinkedList<Object> labels = (SinglyLinkedList<Object>) in.readObject();
            SetNode ret = (SetNode) Taint.emptyTaint();
            for(Object o : labels){
                ret = ret.union(Taint.withLabel(o));
            }
            this.parent = ret;
        }

        private Object readResolve(){
            return this.parent;
        }
    }

    /* Record type that associates an object with a unique integer. Used to maintain a consistent rank value for a
     * particular object. */
    private static class RankedObject {

        // The object itself
        private Object object;
        // Unique integer used to order the object in sets
        private int rank;

        /* Constructs a new ranked object with the specified rank and object. */
        private RankedObject(Object object, int rank) {
            this.object = object;
            this.rank = rank;
        }

        /* Returns a nicely formatted string representation of the object and its rank. */
        @Override
        public String toString() {
            return String.format("(%s -> %d)", object, rank);
        }
    }

    /* Stores information about the rank of the referent of a WeakReference so that the rank can be reused when the object
     * is garbage collected. */
    private static class RankReference extends WeakReference<RankedObject> {

        // The rank assigned to the referent
        int rank;

        RankReference(RankedObject referent) {
            super(referent);
            this.rank = referent.rank;
        }

        @Override
        public String toString() {
            return String.format("RankReference: rank: %d | referent: %s", rank, get());
        }
    }

    /* Inner class used to create the singleton instance of PowerSetTree. */
    private static class PowerSetTreeSingleton {
        private static final PowerSetTree INSTANCE = new PowerSetTree();
    }
}