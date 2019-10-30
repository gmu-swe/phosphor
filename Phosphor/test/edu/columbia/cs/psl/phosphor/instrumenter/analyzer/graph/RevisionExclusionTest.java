package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.objectweb.asm.Opcodes.*;

public class RevisionExclusionTest {

    private static final int[] POSSIBLE_EXCLUDED_OPCODES = new int[]{ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3,
            ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH,
            LDC, IINC, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
    };

    private static final int[] ALWAYS_EXCLUDED_OPCODES = new int[]{ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3,
            ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH,
            LDC, IINC
    };

    @Test
    public void testAllLocalAssignmentsConstant() throws NoSuchMethodException, IOException {
        String owner = Type.getInternalName(RevisionExclusionTestMethods.class);
        MethodNode mn = RevisionExclusionTestMethods.getMethodNode("allLocalAssignmentsConstant");
        Set<AbstractInsnNode> exclusions = RevisionExclusionInterpreter.identifyRevisionExcludedInstructions(owner, mn);
        assertEquals(instructionsWithOpcodes(mn, POSSIBLE_EXCLUDED_OPCODES), exclusions);
    }

    @Test
    public void testAllLocalAssignmentsConstant2() throws NoSuchMethodException, IOException {
        String owner = Type.getInternalName(RevisionExclusionTestMethods.class);
        MethodNode mn = RevisionExclusionTestMethods.getMethodNode("allLocalAssignmentsConstant2");
        Set<AbstractInsnNode> exclusions = RevisionExclusionInterpreter.identifyRevisionExcludedInstructions(owner, mn);
        assertEquals(instructionsWithOpcodes(mn, POSSIBLE_EXCLUDED_OPCODES), exclusions);
    }

    @Test
    public void testAllLocalAssignmentsExcluded() throws NoSuchMethodException, IOException {
        String owner = Type.getInternalName(RevisionExclusionTestMethods.class);
        MethodNode mn = RevisionExclusionTestMethods.getMethodNode("allLocalAssignmentsExcluded");
        Set<AbstractInsnNode> exclusions = RevisionExclusionInterpreter.identifyRevisionExcludedInstructions(owner, mn);
        assertEquals(instructionsWithOpcodes(mn, POSSIBLE_EXCLUDED_OPCODES), exclusions);
    }

    @Test
    public void testNoLocalAssignmentsExcluded() throws NoSuchMethodException, IOException {
        String owner = Type.getInternalName(RevisionExclusionTestMethods.class);
        MethodNode mn = RevisionExclusionTestMethods.getMethodNode("noLocalAssignmentsExcluded");
        Set<AbstractInsnNode> exclusions = RevisionExclusionInterpreter.identifyRevisionExcludedInstructions(owner, mn);
        assertEquals(instructionsWithOpcodes(mn, ALWAYS_EXCLUDED_OPCODES), exclusions);
    }

    @Test
    public void testUnableToMergeConstants() throws NoSuchMethodException, IOException {
        String owner = Type.getInternalName(RevisionExclusionTestMethods.class);
        MethodNode mn = RevisionExclusionTestMethods.getMethodNode("unableToMergeConstants");
        Set<AbstractInsnNode> exclusions = RevisionExclusionInterpreter.identifyRevisionExcludedInstructions(owner, mn);
        List<AbstractInsnNode> stores = getStoresInOrder(mn);
        boolean[] expected = new boolean[]{true, true, true, true, false, true, false};
        for(int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], exclusions.contains(stores.get(i)));
        }
    }

    private static Set<AbstractInsnNode> instructionsWithOpcodes(MethodNode mn, int... opcodes) {
        Set<Integer> targetedOpcodes = new HashSet<>();
        for(int opcode : opcodes) {
            targetedOpcodes.add(opcode);
        }
        Set<AbstractInsnNode> instructions = new HashSet<>();
        for(AbstractInsnNode insn : mn.instructions.toArray()) {
            if(targetedOpcodes.contains(insn.getOpcode())) {
                instructions.add(insn);
            }
        }
        return instructions;
    }

    private static List<AbstractInsnNode> getStoresInOrder(MethodNode mn) {
        List<AbstractInsnNode> instructions = new ArrayList<>();
        for(AbstractInsnNode insn : mn.instructions.toArray()) {
            switch(insn.getOpcode()) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                    instructions.add(insn);
            }
        }
        return instructions;
    }
}