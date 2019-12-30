package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;

public interface LoopLevel {
    enum ConstantLoopLevel implements LoopLevel {
        CONSTANT_LOOP_LEVEL;

        @Override
        public String toString() {
            return "ConstantLoopLevel";
        }
    }

    final class DependentLoopLevel implements LoopLevel {

        private final int[] dependencies;

        public DependentLoopLevel(int[] dependencies) {
            Arrays.sort(dependencies);
            this.dependencies = dependencies;
        }

        public int[] getDependencies() {
            return dependencies;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof DependentLoopLevel)) {
                return false;
            }
            DependentLoopLevel that = (DependentLoopLevel) o;
            return Arrays.equals(dependencies, that.dependencies);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(dependencies);
        }

        @Override
        public String toString() {
            return "DependentLoopLevel" + Arrays.toString(dependencies);
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

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof VariantLoopLevel)) {
                return false;
            }
            VariantLoopLevel that = (VariantLoopLevel) o;
            return levelOffset == that.levelOffset;
        }

        @Override
        public int hashCode() {
            return levelOffset;
        }

        @Override
        public String toString() {
            return "VariantLoopLevel(" + "+" + levelOffset + ')';
        }
    }
}
