package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

public interface LoopLevel {
    enum ConstantLoopLevel implements LoopLevel {
        CONSTANT_LOOP_LEVEL
    }

    final class DependentLoopLevel implements LoopLevel {
        private final int[] dependencies;

        public DependentLoopLevel(int[] dependencies) {
            this.dependencies = dependencies;
        }

        public int[] getDependencies() {
            return dependencies;
        }
    }

    final class VariantLoopLevel implements LoopLevel {

        private final int levelOffset;

        public VariantLoopLevel(int levelOffset) {
            this.levelOffset = levelOffset;
        }

        public int getLevelOffset() {
            return levelOffset;
        }
    }
}
