package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg.ControlFlowGraphTestMethods.getMethodNode;
import static junit.framework.TestCase.assertTrue;

public class BindingControlFlowAnalyzerTest {

    @Test
    public void testMultipleReturnLoopDominators() throws Exception {
        MethodNode mn = getMethodNode("multipleReturnLoop");
        AbstractInsnNode[] originalInsns = mn.instructions.toArray();
        BindingControlFlowAnalyzer.analyze(mn);
        AbstractInsnNode[] modifiedInsns = mn.instructions.toArray();
        int i = 0;
        for(AbstractInsnNode originalInsn : originalInsns) {
            SinglyLinkedList<AbstractInsnNode> additions = new SinglyLinkedList<>();
            for(AbstractInsnNode modifiedInsn = modifiedInsns[i++]; modifiedInsn != originalInsn; modifiedInsn = modifiedInsns[i++]) {
                additions.enqueue(modifiedInsn);
            }
            if(ControlFlowGraph.isExitInstruction(originalInsn)) {
                // Check that a pop-all was added before exit instructions
                boolean foundPopAll = false;
                for(AbstractInsnNode node : additions) {
                    if(node.getOpcode() == TaintUtils.BRANCH_END && node instanceof VarInsnNode && ((VarInsnNode) node).var == -1) {
                        foundPopAll = true;
                    }
                }
                assertTrue(foundPopAll);
            }
        }
    }
}
