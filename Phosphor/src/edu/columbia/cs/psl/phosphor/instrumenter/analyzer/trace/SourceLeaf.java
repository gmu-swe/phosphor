package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

import static org.objectweb.asm.Opcodes.*;

abstract class SourceLeaf {

    static SourceLeaf newLeaf(AbstractInsnNode insn, InstructionEffect effect, Map<TracedValue, ParamValue> paramMap) {
        if(effect == null) {
            return new UnknownSource(insn);
        } else if(effect.product instanceof ConstantTracedValue) {
            return new ConstantLeaf(insn);
        } else if(effect.sources.length == 1 && paramMap.containsKey(effect.sources[0])) {
            return new ParameterLeaf(insn);
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

        AbstractInsnNode insn;

        FieldAccessLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class ArrayAccessLeaf extends SourceLeaf {

        AbstractInsnNode insn;

        ArrayAccessLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class MethodResultLeaf extends SourceLeaf {

        AbstractInsnNode insn;

        MethodResultLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class NewValueLeaf extends SourceLeaf {

        AbstractInsnNode insn;

        NewValueLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class ParameterLeaf extends SourceLeaf {

        AbstractInsnNode insn;

        ParameterLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class ConstantLeaf extends SourceLeaf {

        AbstractInsnNode insn;

        ConstantLeaf(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class UnknownSource extends SourceLeaf {

        AbstractInsnNode insn;

        UnknownSource(AbstractInsnNode insn) {
            this.insn = insn;
        }
    }

    static class SplitSource extends SourceLeaf {

        Set<AbstractInsnNode> possibleInstructionSources;

        SplitSource(Set<AbstractInsnNode> possibleInstructionSources) {
            this.possibleInstructionSources = possibleInstructionSources;
        }
    }
}
