package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.instrumenter.PhosphorTextifier;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.TraceMethodVisitor;

public class InstructionTextifier extends PhosphorTextifier {

    public static final InstructionTextifier instance = new InstructionTextifier();

    private TraceMethodVisitor tmv = new TraceMethodVisitor(this);
    private Map<Label, String> labelNames;

    private InstructionTextifier() {

    }

    public String convertInstructionToString(AbstractInsnNode insn, Map<Label, String> labelNames) {
        this.labelNames = labelNames;
        insn.accept(tmv);
        String result = stringBuilder.toString();
        stringBuilder.delete(0, stringBuilder.length());
        return result.trim();
    }

    @Override
    protected void appendLabel(final Label label) {
        if(labelNames != null && labelNames.containsKey(label)) {
            stringBuilder.append(labelNames.get(label));
        } else {
            stringBuilder.append(label.toString());
        }
    }

    public static InstructionTextifier getInstance() {
        return instance;
    }
}
