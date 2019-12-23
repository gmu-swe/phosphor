package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.LoopLevel;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace.TracingInterpreterTestMethods.calculateLoopLevelMap;
import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace.TracingInterpreterTestMethods.getMethodNode;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.*;

@Ignore
public class TracingInterpreterTest {

    @Test
    public void testAllLocalAssignmentsConstant() throws Exception {
        checkAllLocalStoresAtLevel("allLocalAssignmentsConstant", LoopLevel.ConstantLoopLevel.class);
    }

    @Test
    public void testAllLocalAssignmentsConstant2() throws Exception {
        checkAllLocalStoresAtLevel("allLocalAssignmentsConstant2", LoopLevel.ConstantLoopLevel.class);

    }

    @Test
    public void testAllLocalAssignmentsConstant3() throws Exception {
        checkAllLocalStoresAtLevel("allLocalAssignmentsConstant3", LoopLevel.ConstantLoopLevel.class);

    }

    @Test
    public void testArgDependentAssignment() throws Exception {
        MethodNode mn = getMethodNode("argDependentAssignment");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
    }

    @Test
    public void testArgDependentBranching() throws Exception {
        MethodNode mn = getMethodNode("argDependentBranching");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
    }

    @Test
    public void testArrayIncrement() throws Exception {
        MethodNode mn = getMethodNode("arrayIncrement");
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
    }

    private void checkAllLocalStoresAtLevel(String methodName, Class<? extends LoopLevel> targetLevel) throws Exception {
        MethodNode mn = getMethodNode(methodName);
        Map<AbstractInsnNode, LoopLevel> loopLevelMap = calculateLoopLevelMap(mn);
        for(AbstractInsnNode insn : loopLevelMap.keySet()) {
            switch((insn.getOpcode())) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                    assertTrue(targetLevel.isInstance(loopLevelMap.get(insn)));
            }
        }
    }
}