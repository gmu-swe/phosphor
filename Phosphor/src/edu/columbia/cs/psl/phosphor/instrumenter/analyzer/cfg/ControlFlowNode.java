package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

abstract class ControlFlowNode {

    /**
     * The index of this node in the reverse post-order sequence for the graph or -1 if the index has not yet been
     * calculated
     */
    int reversePostOrderIndex = -1;

    /**
     * The index of this node in the reverse post-order sequence for the transpose graph or -1 if the index has not
     * yet been calculated
     */
    int transposeReversePostOrderIndex = -1;

    /**
     * Tracks whether this node has been visited by an algorithm
     */
    private boolean marked = false;

    /**
     * Set of nodes to which there is an edge from this node in the control flow graph
     */
    final Set<ControlFlowNode> successors = new HashSet<>();

    /**
     * Set of nodes from which there is an edge to this node in the control flow graph
     */
    final Set<ControlFlowNode> predecessors = new HashSet<>();

    /**
     * Makes the specified source node a predecessor of the specified destination node and the specified destination node
     * a successor of the specified source node. In other words, adds the directed edge (source, destination) to the graph.
     *
     * @param source the node at which the edge being added starts
     * @param destination the node at which the edge being added ends
     */
    static void addEdge(ControlFlowNode source, ControlFlowNode destination) {
        source.successors.add(destination);
        destination.predecessors.add(source);
    }

    /**
     * If the edge (source, destination) is part of the graph removes it.
     *
     * @param source the node at which the edge being removed starts
     * @param destination the node at which the edge being removed ends
     */
    static void removeEdge(ControlFlowNode source, ControlFlowNode destination) {
        source.successors.remove(destination);
        destination.predecessors.remove(source);
    }

    /**
     * @param source the start of the edge being checked for
     * @param destination the end of the edge being checked for
     * @return true is there is an edge from the specified source node to the specified destination node
     */
    public static boolean isConnected(ControlFlowNode source, ControlFlowNode destination) {
        return destination.predecessors.contains(source) && source.successors.contains(destination);
    }

    public boolean mark() {
        boolean prev = marked;
        this.marked = true;
        return prev;
    }

    public boolean unmark() {
        boolean prev = marked;
        this.marked = false;
        return prev;
    }

    public boolean isMarked() {
        return marked;
    }

    @Override
    public String toString() {
        return super.toString() + " - index: " + reversePostOrderIndex;
    }
}
