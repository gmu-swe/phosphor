package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.junit.Test;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphCreator.getBasicBlocks;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphTestMethods.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ControlFlowGraphCreatorTest {

    private void checkBasicSwitchSuccessors(FlowGraph<ControlFlowNode> cfg) {
        Iterable<BasicBlock> basicBlocks = getBasicBlocks(cfg);
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(basicBlocks);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        for(int i = 1; i <= 6; i++) {
            expected.add(idBlockMap.get(i));
        }
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(0))); // Block 0 should be succeeded by all of the case blocks
        expected.clear();
        expected.add(idBlockMap.get(7));
        for(int i = 1; i <= 6; i++) {
            assertEquals(expected, cfg.getSuccessors(idBlockMap.get(i))); // Each case block is succeeded by block 7
        }
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(7))); // Block 7 should be succeeded only by the exit
    }

    private void checkBasicSwitchImmediateDominators(FlowGraph<ControlFlowNode> cfg) {
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(getBasicBlocks(cfg));
        Map<ControlFlowNode, ControlFlowNode> immediateDominators = cfg.getImmediateDominators();
        assertEquals(cfg.getEntryPoint(), immediateDominators.get(idBlockMap.get(0)));
        for(int i = 1; i <= 7; i++) {
            assertEquals(idBlockMap.get(0), immediateDominators.get(idBlockMap.get(i)));
        }
    }
    
    private void checkBasicSwitchDominanceFrontiers(FlowGraph<ControlFlowNode> cfg) {
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(getBasicBlocks(cfg));
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.getDominanceFrontiers();
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
        checkBasicSwitchSuccessors(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchSuccessors() throws Exception {
        checkBasicSwitchSuccessors(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testBasicTableSwitchImmediateDominators() throws Exception {
        checkBasicSwitchImmediateDominators(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchImmediateDominators() throws Exception {
        checkBasicSwitchImmediateDominators(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicLookupSwitch")));
    }
    
    @Test
    public void testBasicTableSwitchDominanceFrontiers() throws Exception {
        checkBasicSwitchDominanceFrontiers(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicTableSwitch")));
    }

    @Test
    public void testBasicLookupSwitchDominanceFrontiers() throws Exception {
        checkBasicSwitchDominanceFrontiers(ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("basicLookupSwitch")));
    }

    @Test
    public void testTryCatchWithIfSuccessors() throws Exception {
        FlowGraph<ControlFlowNode> cfg = ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("tryCatchWithIf"));
        Iterable<BasicBlock> basicBlocks = getBasicBlocks(cfg);
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(basicBlocks);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        expected.add(idBlockMap.get(3));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(0)));
        //
        expected.clear();
        expected.add(idBlockMap.get(2));
        expected.add(idBlockMap.get(3));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(1)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(2)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(3)));
    }

    @Test
    public void testMultipleReturnLoopSuccessors() throws Exception {
        FlowGraph<ControlFlowNode> cfg = ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(0)));
        //
        expected.clear();
        expected.add(idBlockMap.get(2));
        expected.add(idBlockMap.get(8));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(1)));
        //
        expected.clear();
        expected.add(idBlockMap.get(3));
        expected.add(idBlockMap.get(6));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(2)));
        //
        expected.clear();
        expected.add(idBlockMap.get(4));
        expected.add(idBlockMap.get(5));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(3)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(4)));
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(5)));
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(6)));
        //
        expected.clear();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(7)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, cfg.getSuccessors(idBlockMap.get(8)));
    }

    @Test
    public void testMultipleReturnLoopDominators() throws Exception {
        FlowGraph<ControlFlowNode> cfg = ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        Map<ControlFlowNode, ControlFlowNode> immediateDominators = cfg.getImmediateDominators();
        assertEquals(cfg.getEntryPoint(), immediateDominators.get(idBlockMap.get(0)));
        assertEquals(idBlockMap.get(0), immediateDominators.get(idBlockMap.get(1)));
        assertEquals(idBlockMap.get(1), immediateDominators.get(idBlockMap.get(2)));
        assertEquals(idBlockMap.get(2), immediateDominators.get(idBlockMap.get(3)));
        assertEquals(idBlockMap.get(3), immediateDominators.get(idBlockMap.get(4)));
        assertEquals(idBlockMap.get(3), immediateDominators.get(idBlockMap.get(5)));
        assertEquals(idBlockMap.get(2), immediateDominators.get(idBlockMap.get(6)));
        assertEquals(idBlockMap.get(2), immediateDominators.get(idBlockMap.get(7)));
        assertEquals(idBlockMap.get(1), immediateDominators.get(idBlockMap.get(8)));
    }
    
    @Test
    public void testMultipleReturnLoopDominanceFrontiers() throws Exception {
        FlowGraph<ControlFlowNode> cfg = ControlFlowGraphCreator.createControlFlowGraph(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.getDominanceFrontiers();
        Set<ControlFlowNode> expected = new HashSet<>();
        assertTrue(dominanceFrontiers.get(idBlockMap.get(0)).isEmpty());
        //
        expected.clear();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(1)));
        //
        expected.clear();
        expected.add(idBlockMap.get(1));
        expected.add(cfg.getExitPoint());
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(2)));
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        expected.add(cfg.getExitPoint());
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(3)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(4)));
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(5)));
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(6)));
        //
        expected.clear();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(7)));
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, dominanceFrontiers.get(idBlockMap.get(8)));
    }
}
