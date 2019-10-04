package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.junit.Test;
import org.objectweb.asm.tree.*;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphTestMethods.getMethodNode;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphTestMethods.makeBlockIDBasicBlockMap;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphTest {

    private void checkBasicSwitchSuccessors(ControlFlowGraph cfg) {
        BasicBlock[] basicBlocks = cfg.getBasicBlocks();
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(basicBlocks);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        for(int i = 1; i <= 6; i++) {
            expected.add(idBlockMap.get(i));
        }
        assertEquals(expected, idBlockMap.get(0).successors); // Block 0 should be succeeded by all of the case blocks
        expected.clear();
        expected.add(idBlockMap.get(7));
        for(int i = 1; i <= 6; i++) {
            assertEquals(expected, idBlockMap.get(i).successors); // Each case block is succeeded by block 7
        }
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, idBlockMap.get(7).successors); // Block 7 should be succeeded only by the exit
    }

    private void checkBasicSwitchImmediateDominators(ControlFlowGraph cfg) {
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(cfg.getBasicBlocks());
        Map<ControlFlowNode, ControlFlowNode> immediateDominators = cfg.calculateImmediateDominators();
        assertEquals(cfg.getEntryPoint(), immediateDominators.get(idBlockMap.get(0)));
        for(int i = 1; i <= 7; i++) {
            assertEquals(idBlockMap.get(0), immediateDominators.get(idBlockMap.get(i)));
        }
    }

    private void checkBasicSwitchImmediatePostDominators(ControlFlowGraph cfg) {
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(cfg.getBasicBlocks());
        Map<ControlFlowNode, ControlFlowNode> immediatePostDominators = cfg.calculateImmediatePostDominators();
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(7)));
        for(int i = 0; i <= 6; i++) {
            assertEquals(idBlockMap.get(7), immediatePostDominators.get(idBlockMap.get(i)));
        }
    }

    private void checkBasicSwitchDominanceFrontiers(ControlFlowGraph cfg) {
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(cfg.getBasicBlocks());
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.calculateDominanceFrontiers();
        Set<ControlFlowNode> expected = new HashSet<>();
        assertTrue(dominanceFrontiers.get(idBlockMap.get(0)).isEmpty());
        expected.add(idBlockMap.get(7));
        for(int i = 1; i <= 6; i++) {
            assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(i)));
        }
        assertTrue(dominanceFrontiers.get(idBlockMap.get(7)).isEmpty());
    }

    @Test
    public void testBasicTableSwitchSuccessors() throws Exception {
        checkBasicSwitchSuccessors(ControlFlowGraph.analyze(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchSuccessors() throws Exception {
        checkBasicSwitchSuccessors(ControlFlowGraph.analyze(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testBasicTableSwitchImmediateDominators() throws Exception {
        checkBasicSwitchImmediateDominators(ControlFlowGraph.analyze(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchImmediateDominators() throws Exception {
        checkBasicSwitchImmediateDominators(ControlFlowGraph.analyze(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testBasicTableSwitchImmediatePostDominators() throws Exception {
        checkBasicSwitchImmediatePostDominators(ControlFlowGraph.analyze(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchImmediatePostDominators() throws Exception {
        checkBasicSwitchImmediatePostDominators(ControlFlowGraph.analyze(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testBasicTableSwitchDominanceFrontiers() throws Exception {
        checkBasicSwitchDominanceFrontiers(ControlFlowGraph.analyze(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchDominanceFrontiers() throws Exception {
        checkBasicSwitchDominanceFrontiers(ControlFlowGraph.analyze(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testTryCatchWithIfSuccessors() throws Exception {
        ControlFlowGraph cfg = ControlFlowGraph.analyze(getMethodNode("tryCatchWithIf"));
        BasicBlock[] basicBlocks = cfg.getBasicBlocks();
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(basicBlocks);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        expected.add(idBlockMap.get(3));
        assertEquals(expected, idBlockMap.get(0).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(2));
        expected.add(idBlockMap.get(3));
        assertEquals(expected, idBlockMap.get(1).successors);
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, idBlockMap.get(2).successors);
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, idBlockMap.get(3).successors);
    }
}
