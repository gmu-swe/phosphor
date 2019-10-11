package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.junit.Test;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphTestMethods.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphTest {

    @Test
    public void testBasicTableSwitch() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("basicTableSwitch"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
        for(int i = 1; i <= 6; i++) {
            expected.put(i, new HashSet<>(Arrays.asList(7)));
        }
        expected.put(7, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testBasicLookupSwitch() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("basicLookupSwitch"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
        for(int i = 1; i <= 6; i++) {
            expected.put(i, new HashSet<>(Arrays.asList(7)));
        }
        expected.put(7, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testTryCatchWithIf() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("tryCatchWithIf"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(3)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 3)));
        expected.put(2, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testMultipleReturnLoop() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("multipleReturnLoop"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 8)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 6)));
        expected.put(3, new HashSet<>(Arrays.asList(4, 5)));
        expected.put(4, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(5, new HashSet<>(Arrays.asList(7)));
        expected.put(6, new HashSet<>(Arrays.asList(7)));
        expected.put(7, new HashSet<>(Arrays.asList(1)));
        expected.put(8, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testIfElseIntoWhileLoop() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("ifElseIntoWhileLoop"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2)));
        expected.put(1, new HashSet<>(Arrays.asList(3)));
        expected.put(2, new HashSet<>(Arrays.asList(3)));
        expected.put(3, new HashSet<>(Arrays.asList(4, 5)));
        expected.put(4, new HashSet<>(Arrays.asList(3)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithReturn() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("forLoopWithReturn"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 5)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(4, new HashSet<>(Arrays.asList(1)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithBreak() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("forLoopWithBreak"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 5)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(5)));
        expected.put(4, new HashSet<>(Arrays.asList(1)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithOr() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("forLoopWithOr"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 3)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(1)));
        expected.put(4, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testWhileTrue() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("whileTrue"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 4)));
        expected.put(2, new HashSet<>(Arrays.asList(1)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(4, new HashSet<>(Arrays.asList(3)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void testNestedLoopsMultipleExits() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("nestedLoopsMultipleExits"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 10)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(4, new HashSet<>(Arrays.asList(5)));
        expected.put(5, new HashSet<>(Arrays.asList(6, 9)));
        expected.put(6, new HashSet<>(Arrays.asList(7, 8)));
        expected.put(7, new HashSet<>(Arrays.asList(5)));
        expected.put(8, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(9, new HashSet<>(Arrays.asList(1)));
        expected.put(10, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void multipleTryBlocks() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("multipleTryBlocks"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 6)));
        expected.put(2, new HashSet<>(Arrays.asList(1)));
        expected.put(3, new HashSet<>(Arrays.asList(7)));
        expected.put(4, new HashSet<>(Arrays.asList(5, 7)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(6, new HashSet<>(Arrays.asList(7)));
        expected.put(7, new HashSet<>(Arrays.asList(9)));
        expected.put(8, new HashSet<>(Arrays.asList(9)));
        expected.put(9, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }

    @Test
    public void labeledBreak() throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraph(getMethodNode("labeledBreak"));
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 8)));
        expected.put(2, new HashSet<>(Arrays.asList(3)));
        expected.put(3, new HashSet<>(Arrays.asList(4, 7)));
        expected.put(4, new HashSet<>(Arrays.asList(5, 6)));
        expected.put(5, new HashSet<>(Arrays.asList(8)));
        expected.put(6, new HashSet<>(Arrays.asList(3)));
        expected.put(7, new HashSet<>(Arrays.asList(1)));
        expected.put(8, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap(cfg);
        assertEquals(expected, successors);
    }
}
