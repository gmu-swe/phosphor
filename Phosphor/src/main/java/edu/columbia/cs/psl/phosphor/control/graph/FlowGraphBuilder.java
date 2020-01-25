package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

/**
 * A mutable set of vertices and directed edges used to construct FlowGraph instances. Exactly one vertex must be
 * designated as the entry point or source. Exactly one vertex must be designated as the exit point or sink.
 *
 * @param <V> the type of the vertices of the flow graphs built by this builder
 */
public final class FlowGraphBuilder<V> {

    /**
     * Maps from a vertex u to a set containing each vertex v such that there is an edge from u to v in the graph being
     * built. Contains a key for each of the vertices in the graph being built.
     * Invariant: (edges != null) &&
     * (for all s in edges.values(): (s != null && for all e in s: edges.containsKey(e)) &&
     * (entryPointSet -> edges.containsKey(entryPoint))
     * (exitPointSet -> edges.containsKey(exitPoint))
     */
    private final Map<V, Set<V>> edges;

    /**
     * Vertex designated to be the entry point for the graph being built.
     * Invariant: (!entryPointSet -> entryPoint == null) &&
     * (entryPointSet -> edges.containsKey(entryPoint))
     */
    private V entryPoint = null;

    /**
     * True only if a vertex has been designated to be the entry point for the graph being built.
     * Invariant: (!entryPointSet -> entryPoint == null) &&
     * (entryPointSet -> edges.containsKey(entryPoint))
     */
    private boolean entryPointSet = false;

    /**
     * Vertex designated to be the exit point for the graph being built.
     * Invariant: (!exitPointSet -> exitPoint == null) &&
     * (exitPointSet -> edges.containsKey(exitPoint))
     */
    private V exitPoint = null;

    /**
     * True only if a vertex has been designated to be the exit point for the graph being built.
     * Invariant: (!exitPointSet -> exitPoint == null) &&
     * (exitPointSet -> edges.containsKey(exitPoint))
     */
    private boolean exitPointSet = false;

    /**
     * Constructs a new graph builder for a graph that initially contains no vertices and no edges.
     */
    public FlowGraphBuilder() {
        this.edges = new HashMap<>();
    }

    /**
     * Constructs a new graph builder for a graph that initially contains all of the vertices and edges in the specified
     * graph
     */
    public FlowGraphBuilder(FlowGraph<V> graph) {
        this();
        this.edges.putAll(graph.getSuccessors());
        this.entryPoint = graph.getEntryPoint();
        this.entryPointSet = true;
        this.exitPoint = graph.getExitPoint();
        this.exitPointSet = true;
    }

    /**
     * Adds the specified vertex to the graph being built by this if the graph does not already contain the vertex.
     *
     * @param vertex the vertex to be add to the graph
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> addVertex(V vertex) {
        if(!edges.containsKey(vertex)) {
            edges.put(vertex, new HashSet<V>());
        }
        return this;
    }

    /**
     * Removes the specified vertex from the graph being built by this if the graph contains the vertex. If the graph
     * does not contain the specified vertex, does nothing.
     *
     * @param vertex the vertex to be removed from the graph
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> removeVertex(V vertex) {
        if(edges.containsKey(vertex)) {
            edges.remove(vertex);
            for(V key : edges.keySet()) {
                edges.get(key).remove(vertex);
            }
            if(entryPointSet && entryPoint == vertex) {
                entryPointSet = false;
                entryPoint = null;
            }
            if(exitPointSet && exitPoint == vertex) {
                exitPointSet = false;
                exitPoint = null;
            }
        }
        return this;
    }

    /**
     * @param vertex the vertex being checked for
     * @return true is the graph being built contains the specified vertex
     */
    public boolean containsVertex(V vertex) {
        return edges.containsKey(vertex);
    }

    /**
     * Ensures that the graph being built by this contains both the specified source and target vertices by adding them
     * if needed. Adds an edge from the specified source vertex to the specified target vertex to the graph being built
     * by this.
     *
     * @param source the source vertex of the edge to be added
     * @param target the target vertex of the edge to be added
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> addEdge(V source, V target) {
        addVertex(source);
        addVertex(target);
        edges.get(source).add(target);
        return this;
    }

    /**
     * Removes the edge from the specified source vertex to the specified target vertex from the graph being built by this
     * if such an edge exists. If the graph does not contain such an edge, does nothing.
     *
     * @param source the source vertex of the edge to be removed
     * @param target the target vertex of the edge to be removed
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> removeEdge(V source, V target) {
        if(edges.containsKey(source)) {
            edges.get(source).remove(target);
        }
        return this;
    }

    /**
     * @param source the source vertex of the edge being checked for
     * @param target the target vertex of the edge being checked for
     * @return true if the graph contains the specified source and target vertices and an edge from the specified source
     * vertex to the specified target vertex
     */
    public boolean containsEdge(V source, V target) {
        return edges.containsKey(source) && edges.get(source).contains(target);
    }

    /**
     * Ensures that the specified vertex is in the graph being built by this builder by adding it if necessary. Marks
     * the specified vertex as the entry point for the graph being built.
     *
     * @param vertex the vertex to be set as the designated entry point for the graph being built
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> addEntryPoint(V vertex) {
        addVertex(vertex);
        entryPoint = vertex;
        entryPointSet = true;
        return this;
    }

    /**
     * Ensures that the specified vertex is in the graph being built by this builder by adding it if necessary. Marks
     * the specified vertex as the exit point for the graph being built.
     *
     * @param vertex the vertex to be set as the designated exit point for the graph being built
     * @return a reference to this builder
     */
    public FlowGraphBuilder<V> addExitPoint(V vertex) {
        addVertex(vertex);
        exitPoint = vertex;
        exitPointSet = true;
        return this;
    }

    /**
     * @return the entry point for the graph being built
     * @throws IllegalStateException if the entry point for the graph being built has not yet been set
     */
    public V getEntryPoint() {
        if(!entryPointSet) {
            throw new IllegalStateException("Entry point cannot be accessed before it is set");
        } else {
            return entryPoint;
        }
    }

    /**
     * @return exit point for the graph being built
     * @throws IllegalStateException if the exit point for the graph being built has not yet been set
     */
    public V getExitPoint() {
        if(!exitPointSet) {
            throw new IllegalStateException("Exit point cannot be accessed before it is set");
        } else {
            return exitPoint;
        }
    }

    /**
     * @return a FlowGraph created from this builder's vertices and edges
     * @throws IllegalStateException if an entry and exit point have not been set for this builder
     */
    public FlowGraph<V> build() {
        if(!entryPointSet || !exitPointSet) {
            throw new IllegalStateException("Cannot build a flow graph if an entry and exit point have not been set");
        }
        return new FlowGraph<>(edges, entryPoint, exitPoint);
    }
}
