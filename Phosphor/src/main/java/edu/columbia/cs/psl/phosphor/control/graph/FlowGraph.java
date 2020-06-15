package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.IntSinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

/**
 * A flow graph composed of a set of vertices connected by directed edges. The graph is unweighted and has single point
 * of entry (source) and a single point of exit (sink).
 *
 * <p>Uses algorithms for calculating dominators, immediate dominators, and dominance frontiers from the following:<br>
 * K.D. Cooper, T.J. Harvey, and K. Kennedy, “A Simple, Fast Dominance Algorithm,” Rice University,
 * Department of Computer Science Technical Report 06-33870, 2006.
 * http://www.cs.rice.edu/~keith/EMBED/dom.pdf
 *
 * <p>Uses the algorithm for gathering the set of nodes in a natural loop from the following:<br>
 * Compilers: Principles, Techniques, and
 * Tools (2nd Edition) by Alfred V. Aho, Monica S. Lam, Ravi Sethi, and Jeffrey D. Ullman in section 9.6.6.
 *
 * @param <V> the type of the graph's vertices
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
     * && (i != 0 -> dominators[i] = the reverse post order index of the immediate dominator of
     * reachableVertices.get(i)))
     */
    private int[] dominators = null;

    /**
     * An unmodifiable mapping from each reachable vertex in this graph to its immediate dominator null if the immediate
     * dominators of this graph have not yet been calculated (this value is lazily calculated).
     */
    private Map<V, V> immediateDominators = null;

    /**
     * An unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of the vertices that it
     * immediately dominates (i.e., its children in the dominator tree) or null if the dominator tree of this graph
     * have not yet been calculated (this value is lazily calculated).
     */
    private Map<V, Set<V>> dominatorTree = null;

    /**
     * An unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set containing the vertices
     * that it dominates or null if the dominator sets of this graph have not yet been calculated (this value is lazily
     * calculated).
     */
    private Map<V, Set<V>> dominatorSets = null;

    /**
     * An unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set containing the vertices in
     * its dominance frontier or null if the dominance frontiers of this graph have not yet been calculated
     * (this value is lazily calculated).
     */
    private Map<V, Set<V>> dominanceFrontiers = null;

    /**
     * An unmodifiable set containing the natural loops associated with each back edge in this graph or null if the
     * natural loops of this graph have not yet been calculated (this value is lazily calculated).
     */
    private Set<NaturalLoop<V>> naturalLoops = null;

    /**
     * Constructs a new flow graph with the specified entry point, exit point and edges.
     *
     * @param edges      a mapping from each vertex in the graph being constructed to a set containing all the immediate
     *                   successors of the vertex - every vertex must have an entry in the map including the entry point and the exit point
     * @param entryPoint a vertex designated to be the single entry point for the graph being constructed
     * @param exitPoint  a vertex designated to be the single exit point for the graph being constructed
     */
    FlowGraph(Map<V, Set<V>> edges, V entryPoint, V exitPoint) {
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
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
        this.predecessors = originalGraph.successors;
        this.successors = originalGraph.predecessors;
        this.transverseGraph = originalGraph;
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
     * immediate successors of the vertex
     */
    public Map<V, Set<V>> getSuccessors() {
        return successors;
    }

    /**
     * @return an unmodifiable mapping from each vertex in this graph to an unmodifiable set containing all the
     * immediate predecessors of the vertex
     */
    public Map<V, Set<V>> getPredecessors() {
        return predecessors;
    }

    /**
     * @return an unmodifiable set containing all of the vertices in this graph
     */
    public Set<V> getVertices() {
        return Collections.unmodifiableSet(successors.keySet());
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
     * with all of the edges reversed compared to their orientation in this graph.
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
            List<VertexInfo<V>> tempReachableVertices = new ArrayList<>();
            SinglyLinkedList<V> stack = new SinglyLinkedList<>();
            Set<V> marked = new HashSet<>(); // Set of vertices that have been visited
            depthFirstSearch(entryPoint, stack, marked);
            int i = 0;
            Map<V, VertexInfo<V>> vertexInfoMap = new HashMap<>();
            for(V value : stack) {
                VertexInfo<V> info = new VertexInfo<>(value, i++);
                tempReachableVertices.add(info);
                vertexInfoMap.put(value, info);
            }
            for(VertexInfo<V> vertexInfo : tempReachableVertices) {
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
            this.reachableVertices = Collections.unmodifiableList(tempReachableVertices);
        }
    }

    /**
     * Recursively performs a depth first search of this graph. Creates a reverse post-ordering of the reachable edges
     * in the graph.
     *
     * @param vertex the vertex currently being visited
     * @param stack  used to track the reverse post-ordering
     * @param marked set of vertices that have been visited
     */
    private void depthFirstSearch(V vertex, SinglyLinkedList<V> stack, Set<V> marked) {
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
     * the vertex is the entry point
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
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of the vertices
     * that it immediately dominates (i.e., its children in the dominator tree)
     */
    public Map<V, Set<V>> getDominatorTree() {
        if(dominatorTree == null) {
            getImmediateDominators(); // ensure that immediate dominators have been calculated
            dominatorTree = new HashMap<>();
            for(V child : immediateDominators.keySet()) {
                dominatorTree.put(child, new HashSet<>());
            }
            for(V child : immediateDominators.keySet()) {
                V parent = immediateDominators.get(child);
                if(parent != null) {
                    dominatorTree.get(parent).add(child);
                }
            }
            for(V key : dominatorTree.keySet()) {
                dominatorTree.put(key, Collections.unmodifiableSet(dominatorTree.get(key)));
            }
            dominatorTree = Collections.unmodifiableMap(dominatorTree);
        }
        return dominatorTree;
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of the
     * vertices that dominate it
     */
    public Map<V, Set<V>> getDominatorSets() {
        if(dominatorSets == null) {
            getImmediateDominators(); // ensure that immediate dominators have been calculated
            dominatorSets = new HashMap<>();
            for(V key : immediateDominators.keySet()) {
                Set<V> tempDominators = new HashSet<>();
                for(V current = key; current != null; current = immediateDominators.get(current)) {
                    tempDominators.add(current);
                }
                dominatorSets.put(key, Collections.unmodifiableSet(tempDominators));
            }
            dominatorSets = Collections.unmodifiableMap(dominatorSets);
        }
        return dominatorSets;
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of
     * the vertices in its dominance frontier
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
            for(V key : dominanceFrontiers.keySet()) {
                dominanceFrontiers.put(key, Collections.unmodifiableSet(dominanceFrontiers.get(key)));
            }
            dominanceFrontiers = Collections.unmodifiableMap(dominanceFrontiers);
        }
        return dominanceFrontiers;
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to its immediate post-dominator or null
     * if the vertex is the exit point
     */
    public Map<V, V> getImmediatePostDominators() {
        return getTransverseGraph().getImmediateDominators();
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of the
     * vertices that post-dominate it
     */
    public Map<V, Set<V>> getPostDominatorSets() {
        return getTransverseGraph().getDominatorSets();
    }

    /**
     * @return an unmodifiable mapping from each reachable vertex in this graph to an unmodifiable set of
     * the vertices in its post-dominance frontier
     */
    public Map<V, Set<V>> getPostDominanceFrontiers() {
        return getTransverseGraph().getDominanceFrontiers();
    }

    /**
     * @return an unmodifiable set containing the natural loops associated with each back edge in this graph
     */
    public Set<NaturalLoop<V>> getNaturalLoops() {
        if(naturalLoops == null) {
            HashMap<V, NaturalLoop<V>> naturalLoopMap = new HashMap<>();
            ensureReachableVerticesListIsCalculated();
            getDominatorSets(); // ensure that the dominator sets have been calculated
            // Add a natural loop to the set for each back edge
            for(V source : dominatorSets.keySet()) {
                for(V target : successors.get(source)) {
                    if(dominatorSets.get(source).contains(target)) {
                        // There is an edge from source to target and source is dominated by target
                        if(naturalLoopMap.containsKey(target)) {
                            naturalLoopMap.get(target).tails.add(source);
                        } else {
                            naturalLoopMap.put(target, new NaturalLoop<>(source, target));
                        }
                    }
                }
            }
            for(NaturalLoop<V> loop : naturalLoopMap.values()) {
                loop.vertices.add(loop.header); // Mark the loop's header as visited
                for(V tail : loop.tails) {
                    if(tail != loop.header) {
                        transverseDepthFirstSearch(tail, loop.vertices);
                    }
                }
            }
            naturalLoops = Collections.unmodifiableSet(new HashSet<>(naturalLoopMap.values()));
        }
        return naturalLoops;
    }

    /**
     * Recursively performs a depth first search on the transverse of this graph.
     *
     * @param vertex the vertex currently being visited
     * @param marked set of vertices that have been visited
     */
    private void transverseDepthFirstSearch(V vertex, Set<V> marked) {
        marked.add(vertex);
        for(V child : predecessors.get(vertex)) {
            if(marked.add(child)) {
                transverseDepthFirstSearch(child, marked);
            }
        }
    }

    /**
     * @param source the start vertex of the path being checked for
     * @param target the end vertex of the path being checked for
     * @return true if there is a path in this graph from the specified source vertex to the specified target vertex,
     * if source.equals(target) only returns true if there is a path from source to itself
     * @throws IllegalArgumentException if either of the specified vertices are not in this graph
     */
    public boolean containsPath(V source, V target) {
        if(!successors.containsKey(source) || !successors.containsKey(target)) {
            throw new IllegalArgumentException("At least one supplied vertex is not a vertex in the graph");
        }
        Set<V> visited = new HashSet<>();
        SinglyLinkedList<V> queue = new SinglyLinkedList<>();
        queue.enqueue(source);
        visited.add(source);
        while(!queue.isEmpty()) {
            for(V successor : successors.get(queue.removeFirst())) {
                if(successor.equals(target)) {
                    return true;
                } else if(visited.add(successor)) {
                    queue.enqueue(successor);
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof FlowGraph)) {
            return false;
        }
        FlowGraph<?> flowGraph = (FlowGraph<?>) o;
        if(entryPoint != null ? !entryPoint.equals(flowGraph.entryPoint) : flowGraph.entryPoint != null) {
            return false;
        }
        if(exitPoint != null ? !exitPoint.equals(flowGraph.exitPoint) : flowGraph.exitPoint != null) {
            return false;
        }
        return successors.equals(flowGraph.successors);
    }

    @Override
    public int hashCode() {
        int result = entryPoint != null ? entryPoint.hashCode() : 0;
        result = 31 * result + (exitPoint != null ? exitPoint.hashCode() : 0);
        result = 31 * result + successors.hashCode();
        return result;
    }

    /**
     * Writes a representation of this graph in the DOT language to the specified writer.
     *
     * @param writer           the writer to which the graph will be written
     * @param graphName        name used as a label for the graph, this should adhere to DOT label requirements
     * @param vertexComparator comparator that determine the order in which vertices appear in the DOT output, if null
     *                         the order if undefined
     * @param printer          function used to create labels for the vertices, this should produce labels that
     *                         adhere to DOT label requirements
     * @param fontSize         value used for the fontsize attribute of the graph
     * @throws NullPointerException     if writer is null or printer is null
     * @throws IOException              if an I/O error occurs while writing to the specified writer
     * @throws IllegalArgumentException if fontSize <= 0
     */
    public void write(Writer writer, String graphName, Comparator<V> vertexComparator,
                      Function<V, String> printer, int fontSize) throws IOException {
        if(fontSize <= 0) {
            throw new IllegalArgumentException("Invalid font size: " + fontSize);
        }
        List<V> sortedVertices = new LinkedList<>(successors.keySet());
        if(vertexComparator != null) {
            Collections.sort(sortedVertices, vertexComparator);
        }
        Map<V, Integer> vertexIndexMap = new HashMap<>();
        writer.write(String.format("digraph {%n"));
        writer.write(String.format("\tnode [shape=box]%n"));
        int i = 0;
        for(V vertex : sortedVertices) {
            writer.write(String.format("\t%d [label=%s]%n", i, escapeDotLabel(printer.apply(vertex))));
            vertexIndexMap.put(vertex, i++);
        }
        for(V vertex : sortedVertices) {
            int vertexIndex = vertexIndexMap.get(vertex);
            for(V successor : successors.get(vertex)) {
                int successorIndex = vertexIndexMap.get(successor);
                writer.write(String.format("\t%d -> %d%n", vertexIndex, successorIndex));
            }
        }
        writer.write(String.format("\tlabel=%s%n", graphName));
        writer.write(String.format("\tfontsize=%d%n}%n", fontSize));
    }

    private static String escapeDotLabel(String label) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if(i == 0 && c != '"') {
                builder.append('"');
            }
            switch(c) {
                case '"':
                    if(i == 0 || i == label.length() - 1) {
                        builder.append('"');
                        break;
                    }
                case '{':
                case '[':
                case ';':
                case '#':
                case '}':
                case ']':
                    builder.append("\\");
                default:
                    builder.append(c);
            }
            if(i == label.length() - 1 && c != '"') {
                builder.append('"');
            }
        }
        return builder.toString();
    }

    /**
     * @param edges a mapping from each vertex in an graph to a set containing all the immediate successors of the
     *              vertex - every vertex must have an entry in the map including the entry point and the exit point
     * @param <V>   the type of the vertices in the graph
     * @return an unmodifiable mapping from each vertex in the graph defined by the specified edges to an unmodifiable
     * set containing all of its immediate successors (i.e., the vertices from which there is an edge to the
     * vertex in the graph).
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
     * @param <V>   the type of the vertices in the graph
     * @return an unmodifiable mapping from each vertex in the graph defined by the specified edges to an unmodifiable
     * set containing all of its immediate predecessors (i.e., the vertices to which there is an edge from the
     * vertex in the graph).
     */
    private static <V> Map<V, Set<V>> createPredecessorsMap(Map<V, Set<V>> edges) {
        Map<V, Set<V>> predecessors = new HashMap<>();
        for(V key : edges.keySet()) {
            // Make empty sets for each vertex
            predecessors.put(key, new HashSet<>());
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
     * Helper function for creating the dominators array return the intersection point for the two specified vertices
     * when moving up the dominator tree.
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
     * Stores a reachable vertex with its reverse post order index and the reverse post order indices of its
     * immediate successors
     *
     * @param <T> the type of the vertex whose information is being stored
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

        @Override
        public String toString() {
            return String.format("{Vertex #%d: value=%s, successors=%s}", reversePostOrderIndex, value, successors);
        }
    }

    /**
     * Represents a natural loop in a flow graph as described in Compilers: Principles, Techniques, and Tools (2nd Edition)
     * by Alfred V. Aho, Monica S. Lam, Ravi Sethi, and Jeffrey D. Ullman. Specifically, a natural loop is defined with
     * respect to at least one back edge (i.e., an edge in the flow graph whose target dominates its source). The set of
     * back edges with the same target vertex form a single natural loop. For some control flow graph G = (V, E) the set
     * of vertices contained within the natural loop defined by a set of back edges S that target a vertex d is defined
     * as {@code d + {n | (n -> d) in S} + {v | v in V, (n -> d) in S, there is a path from v to n in G that does not
     * pass through d}}.
     *
     * @param <V> the type of the vertices of the flow graph in which this loop exists
     */
    public static final class NaturalLoop<V> {

        /**
         * The single point of entry into this loop i.e., the target of the back edges that define this loop
         */
        private final V header;

        /**
         * The source vertices of the back edges that define this loop
         */
        private final Set<V> tails;

        /**
         * A set containing all of vertices that are considered to be part of this loop including this loop's
         * tails and header
         */
        private final Set<V> vertices;

        /**
         * Constructs a new natural loop defined by the back edge from the specified tail vertex to the specified header
         * vertex.
         *
         * @param tail   the source of the back edge that defines the loop being constructed
         * @param header the target of the back edge that defines the loop being constructed
         */
        NaturalLoop(V tail, V header) {
            this.tails = new HashSet<>();
            tails.add(tail);
            this.header = header;
            this.vertices = new HashSet<>();
        }

        /**
         * Constructs a new natural loop defined by the back edges from the specified tail vertices to the specified
         * header vertex.
         *
         * @param tails  the source vertices of the back edges that defines the loop being constructed
         * @param header the target of the back edge that defines the loop being constructed
         */
        NaturalLoop(Set<V> tails, V header) {
            this.tails = new HashSet<>(tails);
            this.header = header;
            this.vertices = new HashSet<>();
        }

        /**
         * @return single point of entry into this loop i.e., the target of the back edge that defines this loop
         */
        public V getHeader() {
            return header;
        }

        /**
         * @return an unmodifiable set containing the source vertices of the back edges that define this loop
         */
        public Set<V> getTails() {
            return Collections.unmodifiableSet(tails);
        }

        /**
         * @return an unmodifiable set containing all of the vertices that considered to be part of this loop including
         * this loop's tail and header
         */
        public Set<V> getVertices() {
            return Collections.unmodifiableSet(vertices);
        }

        /**
         * @param vertex the vertex whose membership in this loop is being tested
         * @return true if this loop contains the specified vertex
         */
        public boolean contains(V vertex) {
            return vertices.contains(vertex);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof NaturalLoop)) {
                return false;
            }
            NaturalLoop<?> that = (NaturalLoop<?>) o;
            if(header != null ? !header.equals(that.header) : that.header != null) {
                return false;
            }
            return tails.equals(that.tails);
        }

        @Override
        public int hashCode() {
            int result = header != null ? header.hashCode() : 0;
            result = 31 * result + tails.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s: %s", tails, header, vertices);
        }
    }
}
