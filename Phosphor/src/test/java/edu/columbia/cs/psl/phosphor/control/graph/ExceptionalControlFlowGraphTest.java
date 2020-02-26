package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.junit.Test;

import static edu.columbia.cs.psl.phosphor.control.graph.ControlFlowGraphTestUtil.*;
import static junit.framework.TestCase.assertEquals;

public class ExceptionalControlFlowGraphTest {
    @Test
    public void testImplicitNPE() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(ENTRY_NODE_ID, Collections.singleton(0));
        expected.put(0, new HashSet<>(Arrays.asList(1, 2)));
        expected.put(1, Collections.singleton(3));
        expected.put(2, Collections.singleton(3));
        expected.put(3, Collections.singleton(EXIT_NODE_ID));
        expected.put(EXIT_NODE_ID, new HashSet<>());
        Map<Integer, Set<Integer>> successors = calculateSuccessors(ExceptionalControlFlowTestMethods.class,
                "implicitNPE", true);
        assertEquals(expected, successors);
    }

    @Test
    public void testExplicitIOException() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(ENTRY_NODE_ID, Collections.singleton(0));
        expected.put(0, new HashSet<>(Arrays.asList(1, 2)));
        expected.put(1, new HashSet<>(Arrays.asList(3, EXIT_NODE_ID)));
        expected.put(2, Collections.singleton(4));
        expected.put(3, Collections.singleton(4));
        expected.put(4, Collections.singleton(EXIT_NODE_ID));
        expected.put(EXIT_NODE_ID, new HashSet<>());
        Map<Integer, Set<Integer>> successors = calculateSuccessors(ExceptionalControlFlowTestMethods.class,
                "explicitIOException", true);
        assertEquals(expected, successors);
    }

    @Test
    public void testCallsExceptionThrowingMethod() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(ENTRY_NODE_ID, Collections.singleton(0));
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 3)));
        expected.put(1, Collections.singleton(4));
        expected.put(2, Collections.singleton(4));
        expected.put(3, Collections.singleton(4));
        expected.put(4, Collections.singleton(EXIT_NODE_ID));
        expected.put(EXIT_NODE_ID, new HashSet<>());
        Map<Integer, Set<Integer>> successors = calculateSuccessors(ExceptionalControlFlowTestMethods.class,
                "callsExceptionThrowingMethod", true);
        assertEquals(expected, successors);
    }
    
    @Test
    public void testNestedHandlers() throws Exception {
        Map<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(ENTRY_NODE_ID, Collections.singleton(0));
        expected.put(0, new HashSet<>(Arrays.asList(1, 2, 4)));
        expected.put(1, Collections.singleton(3));
        expected.put(2, new HashSet<>(Arrays.asList(3, 4)));
        expected.put(3, Collections.singleton(5));
        expected.put(4, Collections.singleton(5));
        expected.put(5, Collections.singleton(EXIT_NODE_ID));
        expected.put(EXIT_NODE_ID, new HashSet<>());
        Map<Integer, Set<Integer>> successors = calculateSuccessors(ExceptionalControlFlowTestMethods.class,
                "nestedHandlers", true);
        assertEquals(expected, successors);
    }
}
