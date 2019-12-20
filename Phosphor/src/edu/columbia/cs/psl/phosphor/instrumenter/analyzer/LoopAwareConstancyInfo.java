package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

import java.util.Iterator;

public class LoopAwareConstancyInfo implements PhosphorInstructionInfo {

    private final int invocationLevel;
    private final SinglyLinkedList<FrameArgumentLevel> argumentLevels = new SinglyLinkedList<>();

    LoopAwareConstancyInfo(int invocationLevel) {
        this.invocationLevel = invocationLevel;
    }

    public int getInvocationLevel() {
        return invocationLevel;
    }

    public int getNumArguments() {
        return argumentLevels.size();
    }

    void pushArgumentLevel(FrameArgumentLevel level) {
        argumentLevels.push(level);
    }

    public Iterator<FrameArgumentLevel> getLevelIterator() {
        return argumentLevels.iterator();
    }

    public enum ConstantArg implements FrameArgumentLevel {
    }

    public interface FrameArgumentLevel {
    }

    public static final class DependentArg implements FrameArgumentLevel {
        private final int[] dependencies;

        DependentArg(int[] dependencies) {
            this.dependencies = dependencies;
        }

        public int[] getDependencies() {
            return dependencies;
        }
    }

    public static final class VariantArg implements FrameArgumentLevel {

        private final int levelOffset;

        VariantArg(int levelOffset) {
            this.levelOffset = levelOffset;
        }

        public int getLevelOffset() {
            return levelOffset;
        }
    }

}
