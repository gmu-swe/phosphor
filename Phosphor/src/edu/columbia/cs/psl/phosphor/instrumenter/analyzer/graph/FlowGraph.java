package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.ArrayList;
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
     * Vertex designated to be the single entry point for this graph
     */
    private final Vertex<V> entryPoint;

    /**
     * Vertex designated to be the single exit point for this graph
     */
    private final Vertex<V> exitPoint;

    /**
     * The vertices in this graph that are reachable from the entry point in reverse post order
     */
    private final ArrayList<Vertex<V>> reachableVertices;

    /**
     * The vertices in this graph that are not reachable from the entry point
     */
    private final ArrayList<Vertex<V>> unreachableVertices;

    /**
     * Maps the value of vertices in this graph to the vertex created for the value
     */
    private final Map<V, Vertex<V>> vertexMap;

    /**
     * The "doms" array as described in Cooper et al. if it has been calculated, otherwise null. If non-null,
     * for all 0 <= i <= reachableVertices.size(): ((i = 0 -> dominators[i] = 0)
     *          && (i != 0 -> dominators[i] = the reverse post order index of the immediate dominator of
     *          reachableVertices.get(i)))
     */
    private int[] dominators = null;

    /**
     * A mapping from each reachable vertex in this graph to the set of vertices in its dominance frontier or null if
     * the dominance frontiers for this graph have yet been calculated
     */
    private  Map<Vertex<V>, Set<Vertex<V>>> dominanceFrontiers = null;

    /**
     * Constructs a new flow graph with the specified entry point, exit point and edges.
     *
     * @param edges a mapping from each vertex in the graph being constructed to a set containing all the successors of
     *              the vertex
     * @param entryPoint a vertex designated to be the single entry point for this graph
     * @param exitPoint a vertex designated to be the single exit point for this graph
     */
    FlowGraph(Map<V, Set<V>> edges, V entryPoint , V exitPoint) {
        this.reachableVertices = new ArrayList<>();
        this.unreachableVertices = new ArrayList<>();
        this.vertexMap = createVertices(edges, entryPoint);
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
     * @return a mapping from vertex values to the vertex created for the value
     */
    private Map<V, Vertex<V>> createVertices(Map<V, Set<V>> edges, V entryPoint) {
        SinglyLinkedList<V> stack = new SinglyLinkedList<>();
        Set<V> marked = new HashSet<>(); // Vertices that have been visited
        depthFirstSearch(entryPoint, edges, stack, marked);
        int i = 0;
        Map<V, Vertex<V>> vertexMap = new HashMap<>();
        for(V value : stack) {
            Vertex<V> vertex = new Vertex<>(value, i++);
            reachableVertices.add(vertex);
            vertexMap.put(value, vertex);
        }
        if(marked.size() != edges.size()) {
            // Some vertex was unreachable
            for(V value : edges.keySet()) {
                if(!marked.contains(value)) {
                    Vertex<V> vertex = new Vertex<>(value, -1);
                    unreachableVertices.add(vertex);
                    vertexMap.put(value, vertex);
                }
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
        for(Vertex<V> vertex : vertexMap.values()) {
            edges.put(vertex.getValue(), vertex.getSuccessorValues());
        }
        return edges;
    }

    /**
     * @return a set containing all of the vertices in this graph
     */
    public Set<V> getVertices() {
        return new HashSet<>(vertexMap.keySet());
    }

    /**
     * @param vertex the vertex whose successors are to be returned
     * @return a set containing the vertices that immediately succeed the specified vertex
     * @throws IllegalArgumentException if the specified vertex is not a vertex in this graph
     */
    public Set<V> getSuccessors(V vertex) {
        if(!vertexMap.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not a vertex in the graph");
        } else {
            return vertexMap.get(vertex).getSuccessorValues();
        }
    }

    /**
     * @param vertex the vertex whose predecessors are to be returned
     * @return a set containing the vertices that immediately precede the specified vertex
     * @throws IllegalArgumentException if the specified vertex is not a vertex in this graph
     */
    public Set<V> getPredecessors(V vertex) {
        if(!vertexMap.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not a vertex in the graph");
        } else {
            return vertexMap.get(vertex).getPredecessorValues();
        }
    }

    /**
     * @return the transverse of this graph, a directed graph with the same set of vertices as this one, but
     *          with all of the edges reversed compared to their orientation in this graph.
     */
    public FlowGraph<V> reverse() {
        Map<V, Set<V>> edges = new HashMap<>();
        for(Vertex<V> vertex : vertexMap.values()) {
            edges.put(vertex.getValue(), vertex.getPredecessorValues());
        }
        return new FlowGraph<>(edges, exitPoint.getValue(), entryPoint.getValue());
    }

    /**
     * @return a mapping from each reachable vertex in this graph to its immediate dominator or null if the vertex
     *          is the entry point
     */
    public Map<V, V> getImmediateDominators() {
        Map<V, V> immediateDominators = new HashMap<>();
        ensureDominatorsArrayIsCalculated();
        for(int reversePostOrderIndex = 0; reversePostOrderIndex < dominators.length; reversePostOrderIndex++) {
            V vertex = reachableVertices.get(reversePostOrderIndex).getValue();
            if(reversePostOrderIndex == 0) {
                immediateDominators.put(vertex, null);
            } else {
                immediateDominators.put(vertex, reachableVertices.get(dominators[reversePostOrderIndex]).getValue());
            }
        }
        return immediateDominators;
    }

    /**
     * Calculates this graph's dominators array if it has not yet been calculated.
     */
    private void ensureDominatorsArrayIsCalculated() {
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
                    for(Vertex<V> predecessor : reachableVertices.get(i).predecessors) {
                        if(predecessor.getReversePostOrderIndex() != -1 && dominators[predecessor.getReversePostOrderIndex()] != -1) {
                            if(newImmediate == -1) {
                                newImmediate = predecessor.getReversePostOrderIndex();
                            } else {
                                newImmediate = intersect(dominators, predecessor.getReversePostOrderIndex(), newImmediate);
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
     *  when move up the dominator tree.
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
     * @return a mapping from each reachable vertex in this graph to the set of vertices that dominate it
     */
    public Map<V, Set<V>> gatherDominatorSets() {
        Map<V, Set<V>> dominatorSets = new HashMap<>();
        Map<V, V> immediateDominators = getImmediateDominators();
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
     * @return a mapping from each reachable vertex in this graph to the set of vertices in its dominance frontier
     */
    public Map<V, Set<V>> getDominanceFrontiers() {
        Map<V, Set<V>> dominanceFrontiersValues = new HashMap<>();
        ensureDominanceFrontiersAreCalculated();
        for(Vertex<V> vertex : dominanceFrontiers.keySet()) {
            Set<V> frontierValue = new HashSet<>();
            for(Vertex<V> frontier : dominanceFrontiers.get(vertex)) {
                frontierValue.add(frontier.getValue());
            }
            dominanceFrontiersValues.put(vertex.getValue(), frontierValue);
        }
        return dominanceFrontiersValues;
    }

    /**
     * Calculates this graph's dominance frontiers if they have not yet been calculated.
     */
    private void ensureDominanceFrontiersAreCalculated() {
        if(dominanceFrontiers == null) {
            ensureDominatorsArrayIsCalculated();
            dominanceFrontiers = new HashMap<>();
            for(Vertex<V> vertex : reachableVertices) {
                dominanceFrontiers.put(vertex, new HashSet<Vertex<V>>());
            }
            for(int i = 0; i < reachableVertices.size(); i++) {
                if(reachableVertices.get(i).predecessors.size() > 1) {
                    for(Vertex<V> predecessor : reachableVertices.get(i).predecessors) {
                        if(predecessor.getReversePostOrderIndex() != -1) {
                            Vertex<V> runner = predecessor;
                            while(runner.getReversePostOrderIndex() != dominators[i]) {
                                dominanceFrontiers.get(runner).add(reachableVertices.get(i));
                                runner = reachableVertices.get(dominators[runner.getReversePostOrderIndex()]);
                            }
                        }
                    }
                }
            }
        }
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
}
