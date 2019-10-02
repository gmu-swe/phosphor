package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.struct.IntObjectAMT;
import edu.columbia.cs.psl.phosphor.struct.IntSinglyLinkedList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphTest {

    private ClassNode classNode;

    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Exception {
            ClassReader cr = new ClassReader(ControlFlowGraphTest.class.getName());
            classNode = new ClassNode();
            cr.accept(classNode, 0);
        }
    };

    private MethodNode getMethodNode(String methodName) throws NoSuchMethodException {
        for(MethodNode mn : classNode.methods) {
            if(mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }

    @SuppressWarnings("unused")
    public int tableSwitchMethod(int value) {
        int y;
        switch(value) {
            case 1:
                y = 44;
                break;
            case 8:
                y = 88;
                break;
            case 3:
                y = 99;
                break;
            case 4:
                y = -8;
                break;
            case 5:
                y = 220;
                break;
            default:
                y = 0;
        }
        return y * 6;
    }

    @SuppressWarnings("unused")
    public int lookupSwitchMethod(int value) {
        int y;
        switch(value) {
            case 1:
                y = 44;
                break;
            case 11:
                y = 99;
                break;
            case 33333:
                y = -8;
                break;
            case 77:
                y = 220;
                break;
            case -9:
                y = 12;
                break;
            default:
                y = 0;
        }
        return y * 6;
    }

    private void checkSwitchRpoSuccessors(IntObjectAMT<IntSinglyLinkedList> rpoSuccessors) {
        // Check that the start node is succeeded ony by the switch start
        assertEquals(1, rpoSuccessors.get(0).size());
        assertEquals(1, rpoSuccessors.get(0).peek());
        // Check that the switch start is succeeded by all 6 case nodes
        Set<Integer> expected = new HashSet<>();
        for(int i = 2; i <=7; i++) {
            expected.add(i);
        }
        Set<Integer> actual = new HashSet<>();
        for(int i : rpoSuccessors.get(1)) {
            actual.add(i);
        }
        assertEquals(expected, actual);
        // Check that the nodes for the 6 switch case are succeeded only by the (return y * 6;) node
        for(int i = 2; i <= 7; i++) {
            assertEquals(1, rpoSuccessors.get(i).size());
            assertEquals(8, rpoSuccessors.get(i).peek());
        }
        // Check that the node right before the exit (return y * 6;) is only succeeded by the exit
        assertEquals(1, rpoSuccessors.get(8).size());
        assertEquals(9, rpoSuccessors.get(8).peek());
        // Check that the exit node has no successors
        assertTrue(rpoSuccessors.get(9).isEmpty());
    }

    @Test
    public void testAnalyzeTableSwitch() throws Exception {
        MethodNode mn = getMethodNode("tableSwitchMethod");
        ControlFlowGraph cfg = ControlFlowGraph.analyze(mn);
        checkSwitchRpoSuccessors(cfg.getReversePostOrderSuccessorsMap());
    }

    @Test
    public void testAnalyzeLookupSwitch() throws Exception {
        MethodNode mn = getMethodNode("lookupSwitchMethod");
        ControlFlowGraph cfg = ControlFlowGraph.analyze(mn);
        checkSwitchRpoSuccessors(cfg.getReversePostOrderSuccessorsMap());
    }
}
