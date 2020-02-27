package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.junit.Test;

import static edu.columbia.cs.psl.phosphor.control.graph.ControlFlowGraphTestUtil.calculateLoops;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphLoopsTest {

    @Test
    public void testBasicTableSwitch() throws Exception {
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "basicTableSwitch", false);
        assertTrue(loops.isEmpty());
    }

    @Test
    public void testBasicLookupSwitch() throws Exception {
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "basicLookupSwitch", false);
        assertTrue(loops.isEmpty());
    }

    @Test
    public void testTryCatchWithIf() throws Exception {
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "tryCatchWithIf", false);
        assertTrue(loops.isEmpty());
    }

    @Test
    public void testMultipleReturnLoop() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 3, 5, 6, 7)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "multipleReturnLoop", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testIfElseIntoWhileLoop() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(3, new HashSet<>(Arrays.asList(3, 4)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "ifElseIntoWhileLoop", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testForLoopWithReturn() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 4)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "forLoopWithReturn", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testForLoopWithBreak() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 4)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "forLoopWithBreak", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testForLoopWithOr() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 3)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "forLoopWithOr", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testWhileTrue() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "whileTrue", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testNestedLoopsMultipleExits() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 4, 5, 6, 7, 9)));
        expected.put(5, new HashSet<>(Arrays.asList(5, 6, 7)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "nestedLoopsMultipleExits", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testMultipleTryBlocks() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "multipleTryBlocks", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testLabeledBreak() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 3, 4, 6, 7)));
        expected.put(3, new HashSet<>(Arrays.asList(3, 4, 6)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "labeledBreak", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testDoWhile() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "doWhile", false);
        assertEquals(expected, loops);
    }

    @Test
    public void testContinueWhile() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, new HashSet<>(Arrays.asList(1, 2, 3, 4)));
        Map<Integer, Set<Integer>> loops = calculateLoops(ControlFlowGraphTestMethods.class, "continueWhile", false);
        assertEquals(expected, loops);
    }
}
