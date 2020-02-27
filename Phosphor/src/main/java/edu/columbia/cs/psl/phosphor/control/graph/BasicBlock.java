package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.Iterator;

public interface BasicBlock {

    AbstractInsnNode getFirstInsn();

    AbstractInsnNode getLastInsn();

    String toDotString(Map<Label, String> labelNames);

    static Map<Label, String> getNumericLabelNames(InsnList instructions) {
        Map<Label, String> labelMap = new HashMap<>();
        int i = 0;
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(insn instanceof LabelNode) {
                Label label = ((LabelNode) insn).getLabel();
                if(!labelMap.containsKey(label)) {
                    labelMap.put(label, String.format("L%d", i++));
                }
            }
        }
        return labelMap;
    }
}
