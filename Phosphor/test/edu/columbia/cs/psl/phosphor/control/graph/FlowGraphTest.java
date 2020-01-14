package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.control.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowGraphTest {

    // A graph that contains only a source and sink node and a single edge between them
    private static FlowGraph<Integer> emptyGraph;

    // A graph containing a lot of potential loops based on figure 9.38 in Compilers: Principles, Techniques, and Tools
    // (2nd Edition) by Alfred V. Aho, Monica S. Lam, Ravi Sethi, and Jeffrey D. Ullman in section 9.6.1
    private static FlowGraph<Integer> loopingGraph;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            initializeGraphs();
        }
    };

    @Test
    public void testGetAllSuccessorsEmptyGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>());
        assertEquals(expected, emptyGraph.getSuccessors());
    }

    @Test
    public void testGetAllPredecessorsEmptyGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>());
        expected.put(1, new HashSet<>(Arrays.asList(0)));
        assertEquals(expected, emptyGraph.getPredecessors());
    }

    @Test
    public void testGetSuccessors() {
        assertEquals(new HashSet<>(Arrays.asList(1)), emptyGraph.getSuccessors(0));
        assertEquals(new HashSet<>(), emptyGraph.getSuccessors(1));
    }

    @Test
    public void testGetPredecessors() {
        assertEquals(new HashSet<>(), emptyGraph.getPredecessors(0));
        assertEquals(new HashSet<>(Arrays.asList(0)), emptyGraph.getPredecessors(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSuccessorsVertexNotInGraph() {
        emptyGraph.getSuccessors(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPredecessorsVertexNotInGraph() {
        emptyGraph.getPredecessors(2);
    }

    @Test
    public void testGetVerticesEmptyGraph() {
        assertEquals(new HashSet<>(Arrays.asList(0, 1)), emptyGraph.getVertices());
    }

    @Test
    public void testGetTransverseGraphEmptyGraph() {
        FlowGraph<Integer> expected = new FlowGraphBuilder<Integer>()
                .addEntryPoint(1)
                .addExitPoint(0)
                .addEdge(1, 0)
                .build();
        assertEquals(expected, emptyGraph.getTransverseGraph());
    }

    @Test
    public void testGetImmediateDominatorsEmptyGraph() {
        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(0, null);
        expected.put(1, 0);
        assertEquals(expected, emptyGraph.getImmediateDominators());
    }

    @Test
    public void testGetDominatorSetsEmptyGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(0)));
        expected.put(1, new HashSet<>(Arrays.asList(0, 1)));
        assertEquals(expected, emptyGraph.getDominatorSets());
    }

    @Test
    public void testGetDominanceFrontiersEmptyGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>());
        expected.put(1, new HashSet<>());
        assertEquals(expected, emptyGraph.getDominanceFrontiers());
    }

    @Test
    public void testGetNaturalLoopsEmptyGraph() {
        assertTrue(emptyGraph.getNaturalLoops().isEmpty());
    }

    @Test
    public void testGetAllSuccessorsLoopingGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 3)));
        expected.put(2, new HashSet<>(Arrays.asList(3)));
        expected.put(3, new HashSet<>(Arrays.asList(4)));
        expected.put(4, new HashSet<>(Arrays.asList(3, 5, 6)));
        expected.put(5, new HashSet<>(Arrays.asList(7)));
        expected.put(6, new HashSet<>(Arrays.asList(7)));
        expected.put(7, new HashSet<>(Arrays.asList(4, 8)));
        expected.put(8, new HashSet<>(Arrays.asList(3, 9, 10)));
        expected.put(9, new HashSet<>(Arrays.asList(1)));
        expected.put(10, new HashSet<>(Arrays.asList(7, 11)));
        expected.put(11, new HashSet<>());
        assertEquals(expected, loopingGraph.getSuccessors());
    }

    @Test
    public void testGetAllPredecessorsLoopingGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>());
        expected.put(1, new HashSet<>(Arrays.asList(0, 9)));
        expected.put(2, new HashSet<>(Arrays.asList(1)));
        expected.put(3, new HashSet<>(Arrays.asList(1, 2, 4, 8)));
        expected.put(4, new HashSet<>(Arrays.asList(3, 7)));
        expected.put(5, new HashSet<>(Arrays.asList(4)));
        expected.put(6, new HashSet<>(Arrays.asList(4)));
        expected.put(7, new HashSet<>(Arrays.asList(5, 6, 10)));
        expected.put(8, new HashSet<>(Arrays.asList(7)));
        expected.put(9, new HashSet<>(Arrays.asList(8)));
        expected.put(10, new HashSet<>(Arrays.asList(8)));
        expected.put(11, new HashSet<>(Arrays.asList(10)));
        assertEquals(expected, loopingGraph.getPredecessors());
    }

    @Test
    public void testGetVerticesLoopingGraph() {
        Set<Integer> expected = new HashSet<>();
        for(int i = 0; i <= 11; i++) {
            expected.add(i);
        }
        assertEquals(expected, loopingGraph.getVertices());
    }

    @Test
    public void testGetTransverseGraphLoopingGraph() {
        FlowGraph<Integer> expected = new FlowGraphBuilder<Integer>()
                .addEntryPoint(11)
                .addExitPoint(0)
                .addEdge(1, 0)
                .addEdge(2, 1)
                .addEdge(3, 1)
                .addEdge(3, 2)
                .addEdge(4, 3)
                .addEdge(3, 4)
                .addEdge(5, 4)
                .addEdge(6, 4)
                .addEdge(7, 5)
                .addEdge(7, 6)
                .addEdge(4, 7)
                .addEdge(8, 7)
                .addEdge(3, 8)
                .addEdge(9, 8)
                .addEdge(10, 8)
                .addEdge(1, 9)
                .addEdge(7, 10)
                .addEdge(11, 10)
                .build();
        assertEquals(expected, loopingGraph.getTransverseGraph());
    }

    @Test
    public void testGetImmediateDominatorsLoopingGraph() {
        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(0, null);
        expected.put(1, 0);
        expected.put(2, 1);
        expected.put(3, 1);
        expected.put(4, 3);
        expected.put(5, 4);
        expected.put(6, 4);
        expected.put(7, 4);
        expected.put(8, 7);
        expected.put(9, 8);
        expected.put(10, 8);
        expected.put(11, 10);
        assertEquals(expected, loopingGraph.getImmediateDominators());
    }

    @Test
    public void testGetDominatorSetsLoopingGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(0)));
        expected.put(1, new HashSet<>(Arrays.asList(0, 1)));
        expected.put(2, new HashSet<>(Arrays.asList(0, 1, 2)));
        expected.put(3, new HashSet<>(Arrays.asList(0, 1, 3)));
        expected.put(4, new HashSet<>(Arrays.asList(0, 1, 3, 4)));
        expected.put(5, new HashSet<>(Arrays.asList(0, 1, 3, 4, 5)));
        expected.put(6, new HashSet<>(Arrays.asList(0, 1, 3, 4, 6)));
        expected.put(7, new HashSet<>(Arrays.asList(0, 1, 3, 4, 7)));
        expected.put(8, new HashSet<>(Arrays.asList(0, 1, 3, 4, 7, 8)));
        expected.put(9, new HashSet<>(Arrays.asList(0, 1, 3, 4, 7, 8, 9)));
        expected.put(10, new HashSet<>(Arrays.asList(0, 1, 3, 4, 7, 8, 10)));
        expected.put(11, new HashSet<>(Arrays.asList(0, 1, 3, 4, 7, 8, 10, 11)));
        assertEquals(expected, loopingGraph.getDominatorSets());
    }

    @Test
    public void testGetDominanceFrontiersLoopingGraph() {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>());
        expected.put(1, new HashSet<>(Arrays.asList(1)));
        expected.put(2, new HashSet<>(Arrays.asList(3)));
        expected.put(3, new HashSet<>(Arrays.asList(1, 3)));
        expected.put(4, new HashSet<>(Arrays.asList(1, 3, 4)));
        expected.put(5, new HashSet<>(Arrays.asList(7)));
        expected.put(6, new HashSet<>(Arrays.asList(7)));
        expected.put(7, new HashSet<>(Arrays.asList(1, 3, 4, 7)));
        expected.put(8, new HashSet<>(Arrays.asList(1, 3, 7)));
        expected.put(9, new HashSet<>(Arrays.asList(1)));
        expected.put(10, new HashSet<>(Arrays.asList(7)));
        expected.put(11, new HashSet<>());
        assertEquals(expected, loopingGraph.getDominanceFrontiers());
    }

    @Test
    public void testGetNaturalLoopsLoopingGraph() {
        Map<NaturalLoop<Integer>, Set<Integer>> expected = new HashMap<>();
        expected.put(new NaturalLoop<>(10, 7), new HashSet<>(Arrays.asList(7, 8, 10)));
        expected.put(new NaturalLoop<>(7, 4), new HashSet<>(Arrays.asList(4, 5, 6, 7, 8, 10)));
        expected.put(new NaturalLoop<>(new HashSet<>(Arrays.asList(4, 8)), 3), new HashSet<>(Arrays.asList(3, 4, 5, 6, 7, 8, 10)));
        expected.put(new NaturalLoop<>(9, 1), new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
        Set<NaturalLoop<Integer>> loops = loopingGraph.getNaturalLoops();
        assertEquals(expected.keySet(), loops);
        for(NaturalLoop<Integer> loop : loops) {
            assertEquals(expected.get(loop), loop.getVertices());
        }
    }

    private static void initializeGraphs() {
        emptyGraph = new FlowGraphBuilder<Integer>()
                .addEntryPoint(0)
                .addExitPoint(1)
                .addEdge(0, 1)
                .build();
        loopingGraph = new FlowGraphBuilder<Integer>()
                .addEntryPoint(0)
                .addExitPoint(11)
                .addEdge(0, 1)
                .addEdge(1, 2)
                .addEdge(1, 3)
                .addEdge(2, 3)
                .addEdge(3, 4)
                .addEdge(4, 3)
                .addEdge(4, 5)
                .addEdge(4, 6)
                .addEdge(5, 7)
                .addEdge(6, 7)
                .addEdge(7, 4)
                .addEdge(7, 8)
                .addEdge(8, 3)
                .addEdge(8, 9)
                .addEdge(8, 10)
                .addEdge(9, 1)
                .addEdge(10, 7)
                .addEdge(10, 11)
                .build();
    }
}