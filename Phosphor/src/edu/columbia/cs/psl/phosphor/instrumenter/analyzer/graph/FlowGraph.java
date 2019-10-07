package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.jgrapht.alg.util.Pair;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.Vertex.addEdge;

/**
 * A flow graph composed of a set of vertices connected by directed edges. The graph is unweighted and rooted at a
 * single entry point with a single point of exit.
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
     * The vertices in this graph that are reachable from the entry point in reverse post order, the elements of
     * vertices are unique
     */
    private final SinglyLinkedList<Vertex<V>> reachableVertices;

    /**
     * Vertex designated to be the single entry point for this graph
     */
    private final Vertex<V> entryPoint;

    /**
     * Vertex designated to be the single exit point for this graph
     */
    private final Vertex<V> exitPoint;

    /**
     * The vertices in this graph that are not reachable from the entry point
     */
    private final SinglyLinkedList<Vertex<V>> unreachableVertices;

    /**
     * Constructs a new flow graph with the specified entry point, exit point and edges.
     *
     * @param edges a mapping from each vertex in the graph being constructed to a set containing all the successors of
     *              the vertex
     * @param entryPoint a vertex designated to be the single entry point for this graph
     * @param exitPoint a vertex designated to be the single exit point for this graph
     */
    FlowGraph(Map<V, Set<V>> edges, V entryPoint , V exitPoint) {
        this.reachableVertices = new SinglyLinkedList<>();
        this.unreachableVertices = new SinglyLinkedList<>();
        Map<V, Vertex<V>> vertexMap = createVertices(edges, entryPoint, exitPoint);
        this.entryPoint = vertexMap.get(entryPoint);
        this.exitPoint = vertexMap.get(exitPoint);
        for(V source : edges.keySet()) {
            for(V target : edges.get(source)) {
                addEdge(vertexMap.get(source), vertexMap.get(target));
            }
        }
    }

    /**
     * Creates vertices for the keys in the specified edges map. Performs a depth first traversal of the graph defined
     * by the specified edges from the specified entry point. Stores reachable vertices in this instances
     * reachableVertices list in their reverse post order. Store unreachable vertices in this instances
     * unreachableVertices list.
     *
     * @param edges a mapping from each vertex in the graph being constructed to a set containing all the successors of
     *              the vertex
     * @param entryPoint a vertex designated to be the single entry point for this graph
     * @param exitPoint a vertex designated to be the single exit point for this graph
     * @return a mapping from vertex values to the vertex created for the value
     */
    private Map<V, Vertex<V>>createVertices(Map<V, Set<V>> edges, V entryPoint , V exitPoint) {
        SinglyLinkedList<V> stack = new SinglyLinkedList<>();
        Set<V> marked = new HashSet<>(); // Vertices that have been visited
        depthFirstSearch(entryPoint, edges, stack, marked);
        int i = 0;
        Map<V, Vertex<V>> vertexMap = new HashMap<>();
        for(V value : stack) {
            Vertex<V> vertex = new Vertex<>(value, i++);
            reachableVertices.enqueue(vertex);
            vertexMap.put(value, vertex);
        }
        if(marked.size() != edges.size()) {
            // Some vertex was unreachable
            for(V value : edges.keySet()) {
                Vertex<V> vertex = new Vertex<>(value, -1);
                unreachableVertices.enqueue(vertex);
                vertexMap.put(value, vertex);
            }
        }
        return vertexMap;
    }

    /**
     *  Recursively performs a depth first search of the graph defined by the specified edges. Creates a reverse
     *  post-ordering of the reachable edges in the graph.
     *
     * @param edges a mapping from each vertex in the graph being constructed to a set containing all the successors of
     *              the vertex
     * @param vertex the vertex currently being visited
     * @param stack used to track the reverse post-ordering
     * @param marked set of vertices that have been visited
     */
    private static <V> void depthFirstSearch(V vertex, Map<V, Set<V>> edges, SinglyLinkedList<V> stack, Set<V> marked ) {
        marked.add(vertex);
        for(V child : edges.get(vertex)) {
            if(marked.add(child)) {
                depthFirstSearch(child, edges, stack, marked);
            }
        }
        stack.push(vertex);
    }

    /**
     * @return the vertex designated to be the single entry point for this graph
     */
    public V getEntryPoint() {
        return entryPoint.getValue();
    }

    /**
     * @return the vertex designated to be the single exit point for this graph
     */
    public V getExitPoint() {
        return exitPoint.getValue();
    }

    /**
     * @return a mapping from each vertex in this graph to a set containing all the successors of the vertex
     */
    public Map<V, Set<V>> getEdges() {
        Map<V, Set<V>> edges = new HashMap<>();
        for(Vertex<V> vertex : reachableVertices) {
            edges.put(vertex.getValue(), vertex.getSuccessorValues());
        }
        for(Vertex<V> vertex : unreachableVertices) {
            edges.put(vertex.getValue(), vertex.getSuccessorValues());
        }
        return edges;
    }

    /**
     * @return the transverse of this graph, a directed graph with the same set of vertices as this one, but
     *          with all of the edges reversed compared to their orientation in this graph.
     */
    public FlowGraph<V> reverse() {
        Map<V, Set<V>> edges = new HashMap<>();
        for(Vertex<V> vertex : reachableVertices) {
            edges.put(vertex.getValue(), vertex.getPredecessorValues());
        }
        for(Vertex<V> vertex : unreachableVertices) {
            edges.put(vertex.getValue(), vertex.getPredecessorValues());
        }
        return new FlowGraph<>(edges, exitPoint.getValue(), entryPoint.getValue());
    }

    /**
     * @return a mapping from each reachable vertex in this graph to the set of vertices in its dominance frontier
     */
    public Map<V, Set<V>> calculateDominanceFrontiers() {
        Map<V, Set<V>> dominanceFrontiers = new HashMap<>();
        // TODO
        return dominanceFrontiers;
    }

    /**
     * @return @return a mapping from each reachable vertex in this graph to its immediate dominator or null if the vertex
     *          is the entry point
     */
    public Map<V, V> calculateImmediateDominators() {
        Map<V, V> immediateDominators = new HashMap<>();
        // TODO
        return immediateDominators;
    }

    /**
     * @return a mapping from each reachable vertex in this graph to the set of vertices that dominate it
     */
    public Map<V, Set<V>> calculateDominatorSets() {
        Map<V, Set<V>> dominatorSets = new HashMap<>();
        Map<V, V> immediateDominators = calculateImmediateDominators();
        for(V key : immediateDominators.keySet()) {
            Set<V> dominators = new HashSet<>();
            for(V current = key; current != null; current = immediateDominators.get(current)) {
                dominators.add(current);
            }
            dominatorSets.put(key, dominators);
        }
        return dominatorSets;
    }

    /**
     * @return a set contains a pair for each back edge in this graph
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
}
