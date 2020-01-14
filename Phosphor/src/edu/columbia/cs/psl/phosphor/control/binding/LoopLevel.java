package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import org.objectweb.asm.MethodVisitor;

import static edu.columbia.cs.psl.phosphor.control.PropagatingControlFlowDelegator.push;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static org.objectweb.asm.Opcodes.*;

public interface LoopLevel {

    // stack_pre = [ControlTaintTagStack]
    // stack_post = [ControlTaintTagStack]
    void setArgument(MethodVisitor mv);

    // stack_pre = [ControlTaintTagStack]
    // stack_post = [Taint]
    void copyTag(MethodVisitor mv);

    // stack_pre = [ControlTaintTagStack, Taint, branchID, numBranches]
    // stack_post = []
    void pushTag(MethodVisitor mv);

    enum ConstantLoopLevel implements LoopLevel {
        CONSTANT_LOOP_LEVEL;

        @Override
        public String toString() {
            return "ConstantLoopLevel";
        }

        @Override
        public void setArgument(MethodVisitor mv) {
            CONTROL_STACK_SET_ARG_CONSTANT.delegateVisit(mv);
        }

        @Override
        public void copyTag(MethodVisitor mv) {
            CONTROL_STACK_COPY_TAG_CONSTANT.delegateVisit(mv);
        }

        @Override
        public void pushTag(MethodVisitor mv) {
            CONTROL_STACK_PUSH_CONSTANT.delegateVisit(mv);
        }
    }

    final class DependentLoopLevel implements LoopLevel {

        private final int[] dependencies;

        public DependentLoopLevel(int[] dependencies) {
            Arrays.sort(dependencies);
            this.dependencies = dependencies;
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

        @Override
        public void setArgument(MethodVisitor mv) {
            pushDependencies(mv);
            CONTROL_STACK_SET_ARG_DEPENDENT.delegateVisit(mv);
        }

        @Override
        public void copyTag(MethodVisitor mv) {
            pushDependencies(mv);
            CONTROL_STACK_COPY_TAG_DEPENDENT.delegateVisit(mv);
        }

        @Override
        public void pushTag(MethodVisitor mv) {
            pushDependencies(mv);
            CONTROL_STACK_PUSH_DEPENDENT.delegateVisit(mv);
        }

        private void pushDependencies(MethodVisitor mv) {
            // Make the dependencies array
            push(mv, dependencies.length);
            mv.visitIntInsn(NEWARRAY, T_INT);
            for(int i = 0; i < dependencies.length; i++) {
                mv.visitInsn(DUP); // Duplicate the array reference
                push(mv, i); // Push the index
                push(mv, dependencies[i]); // Push the dependency value
                mv.visitInsn(IASTORE);
            }
        }
    }

    final class VariantLoopLevel implements LoopLevel {

        private final int levelOffset;

        public VariantLoopLevel(int levelOffset) {
            this.levelOffset = levelOffset;
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

        @Override
        public void setArgument(MethodVisitor mv) {
            push(mv, levelOffset);
            CONTROL_STACK_SET_ARG_VARIANT.delegateVisit(mv);
        }

        @Override
        public void copyTag(MethodVisitor mv) {
            push(mv, levelOffset);
            CONTROL_STACK_COPY_TAG_VARIANT.delegateVisit(mv);
        }

        @Override
        public void pushTag(MethodVisitor mv) {
            push(mv, levelOffset);
            CONTROL_STACK_PUSH_VARIANT.delegateVisit(mv);
        }

        public int getLevelOffset() {
            return levelOffset;
        }
    }
}
