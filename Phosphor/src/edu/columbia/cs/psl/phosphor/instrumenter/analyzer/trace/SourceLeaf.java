package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

import static org.objectweb.asm.Opcodes.*;

public abstract class SourceLeaf {

    static SourceLeaf newLeaf(AbstractInsnNode insn, InstructionEffect effect, Map<TracedValue, ParamValue> paramMap) {
        if(effect == null) {
            return new UnknownSource(insn);
        } else if(effect.product instanceof ConstantTracedValue) {
            return new ConstantLeaf(insn);
        } else if(effect.sources.length == 1 && paramMap.containsKey(effect.sources[0])) {
            int paramNumber = paramMap.get(effect.sources[0]).getLocal();
            return new ParameterLeaf(insn, paramNumber);
        } else {
            switch(insn.getOpcode()) {
                case IALOAD:
                case LALOAD:
                case FALOAD:
                case DALOAD:
                case AALOAD:
                case BALOAD:
                case CALOAD:
                case SALOAD:
                    return new ArrayAccessLeaf(insn);
                case GETSTATIC:
                case GETFIELD:
                    return new FieldAccessLeaf(insn);
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEINTERFACE:
                case INVOKEDYNAMIC:
                    return new MethodResultLeaf(insn);
                case NEW:
                case NEWARRAY:
                case ANEWARRAY:
                case MULTIANEWARRAY:
                    return new NewValueLeaf(insn);
                default:
                    return null;
            }
        }
    }

    static class FieldAccessLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;

        FieldAccessLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    static class ArrayAccessLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;

        ArrayAccessLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    static class MethodResultLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;

        MethodResultLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    public static class NewValueLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;

        NewValueLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    public static class ParameterLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;
        private final int paramNumber;

        ParameterLeaf(AbstractInsnNode insn, int paramNumber) {
            this.insn = insn;
            this.paramNumber = paramNumber;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }

        public int getParamNumber() {
            return paramNumber;
        }
    }

    public static class ConstantLeaf extends SourceLeaf {

        private final AbstractInsnNode insn;

        ConstantLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    public static class UnknownSource extends SourceLeaf {

        private final AbstractInsnNode insn;

        UnknownSource(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getInsn() {
            return insn;
        }
    }

    public static class SplitSource extends SourceLeaf {

        private final Set<AbstractInsnNode> possibleInstructionSources;

        SplitSource(Set<AbstractInsnNode> possibleInstructionSources) {
            this.possibleInstructionSources = Collections.unmodifiableSet(possibleInstructionSources);
        }

        public Set<AbstractInsnNode> getPossibleInstructionSources() {
            return possibleInstructionSources;
        }
    }
}
