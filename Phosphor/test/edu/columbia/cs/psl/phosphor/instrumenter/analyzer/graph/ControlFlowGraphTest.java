package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.junit.Test;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.ControlFlowGraphTestMethods.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphTest {

    @Test
    public void testBasicTableSwitch() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
        for(int i = 1; i <= 6; i++) {
            expected.put(i, new HashSet<>(Arrays.asList(7)));
        }
        expected.put(7, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("basicTableSwitch");
        assertEquals(expected, successors);
    }

    @Test
    public void testBasicLookupSwitch() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
        for(int i = 1; i <= 6; i++) {
            expected.put(i, new HashSet<>(Arrays.asList(7)));
        }
        expected.put(7, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("basicLookupSwitch");
        assertEquals(expected, successors);
    }

    @Test
    public void testTryCatchWithIf() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(3)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 3)));
        expected.put(2, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("tryCatchWithIf");
        assertEquals(expected, successors);
    }

    @Test
    public void testMultipleReturnLoop() throws Exception {
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
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("multipleReturnLoop");
        assertEquals(expected, successors);
    }

    @Test
    public void testIfElseIntoWhileLoop() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1, 2)));
        expected.put(1, new HashSet<>(Arrays.asList(3)));
        expected.put(2, new HashSet<>(Arrays.asList(3)));
        expected.put(3, new HashSet<>(Arrays.asList(4, 5)));
        expected.put(4, new HashSet<>(Arrays.asList(3)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("ifElseIntoWhileLoop");
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithReturn() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 5)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(4, new HashSet<>(Arrays.asList(1)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("forLoopWithReturn");
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithBreak() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 5)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(5)));
        expected.put(4, new HashSet<>(Arrays.asList(1)));
        expected.put(5, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("forLoopWithBreak");
        assertEquals(expected, successors);
    }

    @Test
    public void testForLoopWithOr() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 3)));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, new HashSet<>(Arrays.asList(1)));
        expected.put(4, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("forLoopWithOr");
        assertEquals(expected, successors);
    }

    @Test
    public void testWhileTrue() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(0, new HashSet<>(Arrays.asList(1)));
        expected.put(1, new HashSet<>(Arrays.asList(2, 4)));
        expected.put(2, new HashSet<>(Arrays.asList(1)));
        expected.put(3, new HashSet<>(Arrays.asList(EXIT_NODE_ID)));
        expected.put(4, new HashSet<>(Arrays.asList(3)));
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("whileTrue");
        assertEquals(expected, successors);
    }

    @Test
    public void testNestedLoopsMultipleExits() throws Exception {
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
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("nestedLoopsMultipleExits");
        assertEquals(expected, successors);
    }

    @Test
    public void multipleTryBlocks() throws Exception {
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
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("multipleTryBlocks");
        assertEquals(expected, successors);
    }

    @Test
    public void labeledBreak() throws Exception {
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
        Map<Integer, Set<Integer>> successors = createNumberedSuccessorsMap("labeledBreak");
        assertEquals(expected, successors);
    }
}
