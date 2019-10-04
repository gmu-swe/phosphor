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


    private Map<Integer, BasicBlock> getBlockIDMapForMultipleReturnLoopCFG(ControlFlowGraph cfg) {
        BasicBlock[] basicBlocks = cfg.getBasicBlocks();
        Map<Integer, BasicBlock> idBlockMap = makeBlockIDBasicBlockMap(basicBlocks);
        for(BasicBlock basicBlock : basicBlocks) {
            if(!idBlockMap.containsValue(basicBlock) && basicBlock.getLastInsn() instanceof JumpInsnNode) {
                idBlockMap.put(1, basicBlock);
                break;
            }
        }
        return idBlockMap;
    }

    @Test
    public void testMultipleReturnLoopSuccessors() throws Exception {
        ControlFlowGraph cfg = ControlFlowGraph.analyze(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        HashSet<ControlFlowNode> expected = new HashSet<>();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, idBlockMap.get(0).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(2));
        expected.add(idBlockMap.get(8));
        assertEquals(expected, idBlockMap.get(1).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(3));
        expected.add(idBlockMap.get(6));
        assertEquals(expected, idBlockMap.get(2).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(4));
        expected.add(idBlockMap.get(5));
        assertEquals(expected, idBlockMap.get(3).successors);
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, idBlockMap.get(4).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, idBlockMap.get(5).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(7));
        assertEquals(expected, idBlockMap.get(6).successors);
        //
        expected.clear();
        expected.add(idBlockMap.get(1));
        assertEquals(expected, idBlockMap.get(7).successors);
        //
        expected.clear();
        expected.add(cfg.getExitPoint());
        assertEquals(expected, idBlockMap.get(8).successors);
    }

    @Test
    public void testMultipleReturnLoopDominators() throws Exception {
        ControlFlowGraph cfg = ControlFlowGraph.analyze(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        Map<ControlFlowNode, ControlFlowNode> immediateDominators = cfg.calculateImmediateDominators();
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
    public void testMultipleReturnLoopImmediatePostDominators() throws Exception {
        ControlFlowGraph cfg = ControlFlowGraph.analyze(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        Map<ControlFlowNode, ControlFlowNode> immediatePostDominators = cfg.calculateImmediatePostDominators();
        assertEquals(idBlockMap.get(1), immediatePostDominators.get(idBlockMap.get(0)));
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(1)));
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(2)));
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(3)));
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(4)));
        assertEquals(idBlockMap.get(7), immediatePostDominators.get(idBlockMap.get(5)));
        assertEquals(idBlockMap.get(7), immediatePostDominators.get(idBlockMap.get(6)));
        assertEquals(idBlockMap.get(1), immediatePostDominators.get(idBlockMap.get(7)));
        assertEquals(cfg.getExitPoint(), immediatePostDominators.get(idBlockMap.get(8)));
    }

    @Test
    public void testMultipleReturnLoopDominanceFrontiers() throws Exception {
        ControlFlowGraph cfg = ControlFlowGraph.analyze(getMethodNode("multipleReturnLoop"));
        Map<Integer, BasicBlock> idBlockMap = getBlockIDMapForMultipleReturnLoopCFG(cfg);
        Map<ControlFlowNode, Set<ControlFlowNode>> dominanceFrontiers = cfg.calculateDominanceFrontiers();
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
