package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.DependentLoopLevel;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.VariantLoopLevel;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;
import java.util.function.Predicate;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel.ConstantLoopLevel.CONSTANT_LOOP_LEVEL;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace.TracingInterpreterTestMethods.calculateLoopLevelMap;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace.TracingInterpreterTestMethods.getMethodNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.*;

public class TracingInterpreterTest {

    @Test
    public void testAllLocalAssignmentsConstant() throws Exception {
        checkAllStoresConstant("allLocalAssignmentsConstant");
    }

    @Test
    public void testAllLocalAssignmentsConstant2() throws Exception {
        checkAllStoresConstant("allLocalAssignmentsConstant2");
    }

    @Test
    public void testAllLocalAssignmentsConstant3() throws Exception {
        checkAllStoresConstant("allLocalAssignmentsConstant3");
    }

    @Test
    public void testArgDependentAssignment() throws Exception {
        MethodNode mn = getMethodNode("argDependentAssignment");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                new DependentLoopLevel(new int[]{1}),
                new DependentLoopLevel(new int[]{0}),
                new DependentLoopLevel(new int[]{2}),
                new DependentLoopLevel(new int[]{0, 1})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArgDependentBranching() throws Exception {
        MethodNode mn = getMethodNode("argDependentBranching");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                CONSTANT_LOOP_LEVEL,
                CONSTANT_LOOP_LEVEL,
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(0),
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(0)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testLocalSelfComputation() throws Exception {
        MethodNode mn = getMethodNode("localSelfComputation");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                CONSTANT_LOOP_LEVEL
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArraySelfComputation() throws Exception {
        MethodNode mn = getMethodNode("arraySelfComputation");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new DependentLoopLevel(new int[]{0})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Ignore
    @Test
    public void testMultiArraySelfComputation() throws Exception {
        MethodNode mn = getMethodNode("multiArraySelfComputation");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new DependentLoopLevel(new int[]{0})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testFieldSelfComputation() throws Exception {
        MethodNode mn = getMethodNode("fieldSelfComputation");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new DependentLoopLevel(new int[]{0})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArrayFieldSelfComputation() throws Exception {
        MethodNode mn = getMethodNode("arrayFieldSelfComputation");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new DependentLoopLevel(new int[]{0})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testLocalAssignedVariantValue() throws Exception {
        MethodNode mn = getMethodNode("localAssignedVariantValue");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArrayAssignedVariantValue() throws Exception {
        MethodNode mn = getMethodNode("arrayAssignedVariantValue");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testMultiArrayAssignedVariantValue() throws Exception {
        MethodNode mn = getMethodNode("multiArrayAssignedVariantValue");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testFieldAssignedVariantValue() throws Exception {
        MethodNode mn = getMethodNode("fieldAssignedVariantValue");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArrayFieldAssignedVariantValue() throws Exception {
        MethodNode mn = getMethodNode("arrayFieldAssignedVariantValue");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testVariantArray() throws Exception {
        MethodNode mn = getMethodNode("variantArray");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testVariantArray2() throws Exception {
        MethodNode mn = getMethodNode("variantArray2");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                new VariantLoopLevel(0),
                CONSTANT_LOOP_LEVEL,
                new VariantLoopLevel(1),
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testTwoArrays() throws Exception {
        MethodNode mn = getMethodNode("twoArrays");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                CONSTANT_LOOP_LEVEL,
                new DependentLoopLevel(new int[]{1}),
                new VariantLoopLevel(1)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArrayAliasing() throws Exception {
        MethodNode mn = getMethodNode("arrayAliasing");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                new DependentLoopLevel(new int[]{0}),
                new DependentLoopLevel(new int[]{0})
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    @Test
    public void testArrayAliasingVariant() throws Exception {
        MethodNode mn = getMethodNode("arrayAliasingVariant");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        List<LoopLevel> expected = Arrays.asList(
                new DependentLoopLevel(new int[]{0}),
                new VariantLoopLevel(0)
        );
        assertEquals(expected, getLoopLevels(getStoreInstructions(mn), loopLevelMap));
    }

    private static List<LoopLevel> getLoopLevels(List<AbstractInsnNode> instructions, Map<AbstractInsnNode, LoopLevel> loopLevelMap) {
        List<LoopLevel> levels = new LinkedList<>();
        for(AbstractInsnNode insn : instructions) {
            levels.add(loopLevelMap.get(insn));
        }
        return levels;
    }

    private static List<AbstractInsnNode> getStoreInstructions(MethodNode mn) {
        Predicate<AbstractInsnNode> filter = (insn) -> {
            switch(insn.getOpcode()) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                case IASTORE:
                case LASTORE:
                case FASTORE:
                case DASTORE:
                case AASTORE:
                case BASTORE:
                case CASTORE:
                case SASTORE:
                case PUTSTATIC:
                case PUTFIELD:
                    return true;
                default:
                    return false;
            }
        };
        return filterInstructions(mn, filter);
    }

    private static List<AbstractInsnNode> filterInstructions(MethodNode mn, Predicate<AbstractInsnNode> filter) {
        LinkedList<AbstractInsnNode> stores = new LinkedList<>();
        Iterator<AbstractInsnNode> itr = mn.instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(filter.test(insn)) {
                stores.add(insn);
            }
        }
        return stores;
    }

    private static void checkAllStoresConstant(String methodName) throws Exception {
        MethodNode mn = getMethodNode(methodName);
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        for(AbstractInsnNode insn : getStoreInstructions(mn)) {
            assertTrue("Expected instruction to be at constant loop level:" + insn, loopLevelMap.get(insn) instanceof LoopLevel.ConstantLoopLevel);
        }
    }

}