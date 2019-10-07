package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

/**
 * Represents a vertex in a directed, rooted, unweighted graph.
 *
 * @param <V> the type of the value of this vertex
 */
final class Vertex<V> {

    /**
     * The value stored at this vertex
     */
    private final V value;

    /**
     * The index of this vertex in the reverse post-order sequence for the graph or -1 if this vertex is unreachable
     * from the root of the graph
     */
    private final int reversePostOrderIndex;

    /**
     * Set of vertices from which there is an edge to this vertex in the graph
     */
    final Set<Vertex<V>> predecessors = new HashSet<>();;

    /**
     * Set of vertices to which there is an edge from this vertex in the graph
     */
    private final Set<Vertex<V>> successors = new HashSet<>();;

    Vertex(V value, int reversePostOrderIndex) {
        this.value = value;
        this.reversePostOrderIndex = reversePostOrderIndex;
    }

    /**
     * @return the value stored at this vertex
     */
    public V getValue() {
        return value;
    }

    /**
     * @return the reverse post-order index of this vertex in the graph or -1 if this vertex is unreachable from the
     *              root of the graph
     */
    public int getReversePostOrderIndex() {
        return reversePostOrderIndex;
    }

    /**
     * @return a set containing the value of each vertex that directly succeeds this vertex
     */
    public Set<V> getSuccessorValues() {
        Set<V> successorValues = new HashSet<>(successors.size());
        for(Vertex<V> successor : successors) {
            successorValues.add(successor.value);
        }
        return  successorValues;
    }

    /**
     * @return a set containing the value of each vertex that directly precedes this vertex
     */
    public Set<V> getPredecessorValues() {
        Set<V> predecessorValues = new HashSet<>(predecessors.size());
        for(Vertex<V> predecessor : predecessors) {
            predecessorValues.add(predecessor.value);
        }
        return  predecessorValues;
    }

    /**
     * @return a string representation of this vertex
     */
    @Override
    public String toString() {
        return String.format("<#%d %s>", reversePostOrderIndex, value);
    }

    /**
     * Adds an edge from the specified source vertex to the specified target vertex to the graph.
     *
     * @param source the source vertex of the edge to be added
     * @param target the target vertex of the edge to be added
     */
    static <V> void addEdge(Vertex<V> source, Vertex<V> target) {
        source.successors.add(target);
        target.predecessors.add(source);
    }

    /**
     * Removes the edge from the specified source vertex to the specified target vertex from the graph if such an edge
     * exists.
     *
     * @param source the source vertex of the edge to be removed
     * @param target the target vertex of the edge to be removed
     */
    static <V> void removeEdge(Vertex<V> source, Vertex<V> target) {
        source.successors.remove(target);
        target.predecessors.remove(source);
    }
}
