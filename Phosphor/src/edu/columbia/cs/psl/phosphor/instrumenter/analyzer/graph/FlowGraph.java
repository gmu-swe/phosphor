package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.IntSinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;

/**
 * A flow graph composed of a set of vertices connected by directed edges. The graph is unweighted and has single point
 * of entry (source) and a single point of exit (sink).
 *
 *  Uses algorithms for calculating dominators, immediate dominators, and dominance frontiers from the following:
 *      K.D. Cooper, T.J. Harvey, and K. Kennedy, “A Simple, Fast Dominance Algorithm,” Rice University,
 *      Department of Computer Science Technical Report 06-33870, 2006.
 *      http://www.cs.rice.edu/~keith/EMBED/dom.pdf
 *
 * @param <V> the type of this graph's vertices
 */

public final class FlowGraph<V> {

    /**
     * Vertex designated to be the single entry point for this graph.
     */
    private final V entryPoint;

    /**
     * Vertex designated to be the single exit point for this graph
     */
    private final V exitPoint;

    /**
     * Unmodifiable set containing all of the vertices in this graph
     */
    private final Set<V> vertices;

    /**
     * Unmodifiable mapping from each vertex in this graph to an unmodifiable set containing all of its immediate
     * successors (i.e., the vertices from which there is an edge to the vertex in the graph).
     */
    private final Map<V, Set<V>> successors;

    /**
     * Unmodifiable mapping from each vertex in this graph to an unmodifiable set of containing all of its immediate
     * predecessors (i.e., the vertices to which there is an edge from the vertex in the graph).
     */
    private final Map<V, Set<V>> predecessors;

    /**
     * The transverse of this graph if it has been created, otherwise null (this value is lazily created).
     */
    private FlowGraph<V> transverseGraph = null;

    /**
     * Unmodifiable list containing information about the vertices in this graph that are reachable from the entry point.
     * Elements are in ordered by the reverse post order index of the vertices starting from the entry point. Otherwise,
     * this value is null if this list has not yet been calculated (this value is lazily calculated).
     */
    private List<VertexInfo<V>> reachableVertices = null;

    /**
     * The "doms" array as described in Cooper et al. if it has been calculated, otherwise null (this value is lazily
     * calculated). If non-null, for all 0 <= i <= reachableVertices.size(): ((i = 0 -> dominators[i] = 0)
     *          && (i != 0 -> dominators[i] = the reverse post order index of the immediate dominator of
     *          reachableVertices.get(i)))
     */
    private int[] dominators = null;

    /**
     * A unmodifiable mapping from each reachable vertex in this graph to its immediate dominator null if the immediate
     * dominators for this graph have not yet been calculated (this value is lazily calculated).
     */
    private Map<V, V> immediateDominators = null;

    /**
     * A unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set containing the vertices
     * that it dominates or null if the dominator sets for this graph have not yet been calculated (this value is lazily
     * calculated).
     */
    private Map<V, Set<V>> dominatorSets = null;

    /**
     * A unmodifiable mapping from each reachable vertex in this graph to a unmodifiable set containing the vertices in
     * its dominance frontier or null if the dominance frontiers for this graph have not yet been calculated
     * (this value is lazily calculated).
     */
    private Map<V, Set<V>> dominanceFrontiers = null;

    /**
     * Constructs a new flow graph with the specified entry point, exit point and edges.
     *
     * @param edges a mapping from each vertex in the graph being constructed to a set containing all the immediate
     *              successors of the vertex - every vertex must have an entry in the map including the entry point and the exit point
     * @param entryPoint a vertex designated to be the single entry point for the graph being constructed
     * @param exitPoint a vertex designated to be the single exit point for the graph being constructed
     */
    FlowGraph(Map<V, Set<V>> edges, V entryPoint, V exitPoint) {
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
        this.vertices = Collections.unmodifiableSet(new HashSet<>(edges.keySet()));
        this.successors = createSuccessorsMap(edges);
        this.predecessors = createPredecessorsMap(edges);
    }

    /**
     * Constructs a new flow graph that is the transverse of the specified graph i.e. a directed graph with the same set
     * of vertices as the specified on, but with all of its edges reversed compared to their orientation in the
     * specified graph.
     *
     * @param originalGraph the graph whose transverse graph is to be constructed
     */
    private FlowGraph(FlowGraph<V> originalGraph) {
        this.entryPoint = originalGraph.exitPoint;
        this.exitPoint = originalGraph.entryPoint;
        this.vertices = originalGraph.vertices;
        this.predecessors = originalGraph.successors;
        this.successors = originalGraph.predecessors;
        this.transverseGraph = originalGraph;
    }

    /**
     * @param edges a mapping from each vertex in an graph to a set containing all the immediate successors of the
     *              vertex - every vertex must have an entry in the map including the entry point and the exit point
     * @param <V> the type of the vertices in the graph
     * @return an unmodifiable mapping from each vertex in the graph defined by the specified edges to an unmodifiable
     *          set containing all of its immediate successors (i.e., the vertices from which there is an edge to the
     *          vertex in the graph).
     */
    private static <V> Map<V, Set<V>> createSuccessorsMap(Map<V, Set<V>> edges) {
        Map<V, Set<V>> successors = new HashMap<>();
        for(V key : edges.keySet()) {
            // Make unmodifiable copies of all of the sets
            successors.put(key, Collections.unmodifiableSet(new HashSet<>(edges.get(key))));
        }
        // Put an unmodifiable wrapper around the map
        return Collections.unmodifiableMap(successors);
    }

    /**
     * @param edges a mapping from each vertex in an graph to a set containing all the immediate successors of the
     *              vertex - every vertex must have an entry in the map including the entry point and the exit point
     * @param <V> the type of the vertices in the graph
     * @return an unmodifiable mapping from each vertex in the graph defined by the specified edges to an unmodifiable
     *          set containing all of its immediate predecessors (i.e., the vertices to which there is an edge from the
     *          vertex in the graph).
     */
    private static <V> Map<V, Set<V>> createPredecessorsMap(Map<V, Set<V>> edges) {
        Map<V, Set<V>> predecessors = new HashMap<>();
        for(V key : edges.keySet()) {
            // Make empty sets for each vertex
            predecessors.put(key, new HashSet<V>());
        }
        for(V key : edges.keySet()) {
            for(V value : edges.get(key)) {
                // For each directed edge (key, value) add key to the predecessors of value
                predecessors.get(value).add(key);
            }
        }
        for(V key : predecessors.keySet()) {
            // Put an unmodifiable wrapper around all of the sets
            predecessors.put(key, Collections.unmodifiableSet(predecessors.get(key)));
        }
        // Put an unmodifiable wrapper around the map
        return Collections.unmodifiableMap(predecessors);
    }

    /**
     * @return the vertex designated to be the single entry point for this graph
     */
    public V getEntryPoint() {
        return entryPoint;
    }

    /**
     * @return the vertex designated to be the single exit point for this graph
     */
    public V getExitPoint() {
        return exitPoint;
    }

    /**
     * @return an unmodifiable mapping from each vertex in this graph to an unmodifiable set containing all the
     *          immediate successors of the vertex
     */
    public Map<V, Set<V>> getSuccessors() {
        return successors;
    }

    /**
     * @return an unmodifiable mapping from each vertex in this graph to an unmodifiable set containing all the
     *          immediate predecessors of the vertex
     */
    public Map<V, Set<V>> getPredecessors() {
        return predecessors;
    }

    /**
     * @return an unmodifiable set containing all of the vertices in this graph
     */
    public Set<V> getVertices() {
        return vertices;
    }

    /**
     * @param vertex the vertex whose immediate successors are to be returned
     * @return an unmodifiable set containing the vertices that immediately succeed the specified vertex
     * @throws IllegalArgumentException if the specified vertex is not a vertex in this graph
     */
    public Set<V> getSuccessors(V vertex) {
        if(!successors.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not a vertex in the graph");
        } else {
            return successors.get(vertex);
        }
    }

    /**
     * @param vertex the vertex whose immediate predecessors are to be returned
     * @return an unmodifiable set containing the vertices that immediately precede the specified vertex
     * @throws IllegalArgumentException if the specified vertex is not a vertex in this graph
     */
    public Set<V> getPredecessors(V vertex) {
        if(!predecessors.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not a vertex in the graph");
        } else {
            return predecessors.get(vertex);
        }
    }

    /**
     * @return the transverse of this graph, a directed graph with the same set of vertices as this one, but
     *          with all of the edges reversed compared to their orientation in this graph.
     */
    public FlowGraph<V> getTransverseGraph() {
        if(this.transverseGraph == null) {
            this.transverseGraph = new FlowGraph<>(this);
        }
        return this.transverseGraph;
    }

    /**
     * Calculates this graph's reachable vertices list if has not yet been created by performing a depth first traversal
     * of this graph from its entry point.
     */
    private void ensureReachableVerticesListIsCalculated() {
        if(this.reachableVertices == null) {
            List<VertexInfo<V>> reachableVertices = new ArrayList<>();
            SinglyLinkedList<V> stack = new SinglyLinkedList<>();
            Set<V> marked = new HashSet<>(); // Set of vertices that have been visited
            depthFirstSearch(entryPoint, stack, marked);
            int i = 0;
            Map<V, VertexInfo<V>> vertexInfoMap = new HashMap<>();
            for(V value : stack) {
                VertexInfo<V> info = new VertexInfo<>(value, i++);
                reachableVertices.add(info);
                vertexInfoMap.put(value, info);
            }
            for(VertexInfo<V> vertexInfo : reachableVertices) {
                for(V successor : successors.get(vertexInfo.value)) {
                    if(vertexInfoMap.containsKey(successor)) {
                        vertexInfo.successors.enqueue(vertexInfoMap.get(successor).reversePostOrderIndex);
                    }
                }
                for(V predecessor : predecessors.get(vertexInfo.value)) {
                    if(vertexInfoMap.containsKey(predecessor)) {
                        vertexInfo.predecessors.enqueue(vertexInfoMap.get(predecessor).reversePostOrderIndex);
                    }
                }
            }
            this.reachableVertices = Collections.unmodifiableList(reachableVertices);
        }
    }

    /**
     *  Recursively performs a depth first search of this graph. Creates a reverse post-ordering of the reachable edges
     *  in the graph.
     *
     * @param vertex the vertex currently being visited
     * @param stack used to track the reverse post-ordering
     * @param marked set of vertices that have been visited
     */
    private void depthFirstSearch(V vertex, SinglyLinkedList<V> stack, Set<V> marked ) {
        marked.add(vertex);
        for(V child : successors.get(vertex)) {
            if(marked.add(child)) {
                depthFirstSearch(child, stack, marked);
            }
        }
        stack.push(vertex);
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to its immediate dominator or null if
     *          the vertex is the entry point
     */
    public Map<V, V> getImmediateDominators() {
        if(immediateDominators == null) {
            immediateDominators = new HashMap<>();
            ensureDominatorsArrayIsCalculated();
            for(int reversePostOrderIndex = 0; reversePostOrderIndex < dominators.length; reversePostOrderIndex++) {
                V vertex = reachableVertices.get(reversePostOrderIndex).value;
                if(reversePostOrderIndex == 0) {
                    immediateDominators.put(vertex, null);
                } else {
                    immediateDominators.put(vertex, reachableVertices.get(dominators[reversePostOrderIndex]).value);
                }
            }
            immediateDominators = Collections.unmodifiableMap(immediateDominators);
        }
        return immediateDominators;
    }

    /**
     * Calculates this graph's dominators array if it has not yet been calculated.
     */
    private void ensureDominatorsArrayIsCalculated() {
        ensureReachableVerticesListIsCalculated();
        if(dominators == null) {
            dominators = new int[reachableVertices.size()];
            for(int i = 1; i < dominators.length; i++) {
                // Initialize the dominators as undefined for each vertex, except for the entry vertex which should be
                // itself
                dominators[i] = -1;
            }
            boolean changed = true;
            while(changed) {
                changed = false;
                for(int i = 1; i < dominators.length; i++) {
                    int newImmediate = -1;
                    IntSinglyLinkedList.IntListIterator itr = reachableVertices.get(i).predecessors.iterator();
                    while(itr.hasNext()) {
                        int predecessorIndex = itr.nextInt();
                        if(dominators[predecessorIndex] != -1) {
                            if(newImmediate == -1) {
                                newImmediate = predecessorIndex;
                            } else {
                                newImmediate = intersect(dominators, predecessorIndex, newImmediate);
                            }
                        }
                    }
                    if(dominators[i] != newImmediate && newImmediate != -1) {
                        dominators[i] = newImmediate;
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     *  Helper function for creating the dominators array return the intersection point for the two specified vertices
     *  when moving up the dominator tree.
     *
     * @return the reverse post-order index of intersection point of the two specified vertices
     */
    private static int intersect(int[] dominators, int vertex1, int vertex2) {
        while(vertex1 != vertex2) {
            while(vertex1 > vertex2) {
                vertex1 = dominators[vertex1];
            }
            while(vertex2 > vertex1) {
                vertex2 = dominators[vertex2];
            }
        }
        return vertex1;
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of the
     *          vertices that dominate it
     */
    public Map<V, Set<V>> getDominatorSets() {
        if(dominatorSets == null) {
            dominatorSets = new HashMap<>();
            Map<V, V> immediateDominators = getImmediateDominators();
            for(V key : immediateDominators.keySet()) {
                Set<V> dominators = new HashSet<>();
                for(V current = key; current != null; current = immediateDominators.get(current)) {
                    dominators.add(current);
                }
                dominatorSets.put(key, Collections.unmodifiableSet(dominators));
            }
            dominatorSets = Collections.unmodifiableMap(dominatorSets);
        }
        return dominatorSets;
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of
     *          the vertices in its dominance frontier
     */
    public Map<V, Set<V>> getDominanceFrontiers() {
        if(dominanceFrontiers == null) {
            ensureDominatorsArrayIsCalculated();
            dominanceFrontiers = new HashMap<>();
            for(VertexInfo<V> vertexInfo : reachableVertices) {
                dominanceFrontiers.put(vertexInfo.value, new HashSet<V>());
            }
            for(int i = 0; i < reachableVertices.size(); i++) {
                if(reachableVertices.get(i).predecessors.size() > 1) {
                    IntSinglyLinkedList.IntListIterator itr = reachableVertices.get(i).predecessors.iterator();
                    while(itr.hasNext()) {
                        int runner = itr.nextInt();
                        while(runner != dominators[i]) {
                            dominanceFrontiers.get(reachableVertices.get(runner).value).add(reachableVertices.get(i).value);
                            runner = dominators[runner];
                        }
                    }
                }
            }
        }
        return dominanceFrontiers;
    }

    /**
     * @return a set that contains a pair for each back edge in this graph
     */
    private Set<Pair<V, V>> identifyBackEdges() {
        // TODO
        return null;
    }

    /**
     * @return a set of mappings from the loop header of each natural loop in this graph to the set of vertices
     *              contained in the natural loop
     */
    public Set<Map<V, Set<V>>> calculateLoopSets() {
        // TODO
        return null;
    }


    /**
     * Stores a reachable vertex with its reverse post order index and the reverse post order indices of its
     * immediate successors
     *
     * @param <T> the type of the vertex
     */
    private static final class VertexInfo<T> {

        /**
         * The value of the vertex
         */
        final T value;

        /**
         * The reverse post order index of the vertex
         */
        final int reversePostOrderIndex;

        /**
         * List containing the reverse post order indices of the immediate successors of the vertex
         */
        final IntSinglyLinkedList successors;

        /**
         * List containing the reverse post order indices of the immediate predecessors of the vertex
         */
        final IntSinglyLinkedList predecessors;

        VertexInfo(T value, int reversePostOrderIndex) {
            this.value = value;
            this.reversePostOrderIndex = reversePostOrderIndex;
            this.successors = new IntSinglyLinkedList();
            this.predecessors = new IntSinglyLinkedList();
        }
    }
}
