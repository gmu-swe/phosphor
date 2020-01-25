package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.LocalVariablePhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.struct.Field;
import org.objectweb.asm.Type;

public interface ForceControlStore extends PhosphorInstructionInfo {

    final class ForceControlStoreLocal implements ForceControlStore, LocalVariablePhosphorInstructionInfo {

        private final int index;
        private final Type type;

        public ForceControlStoreLocal(int index, Type type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public int getLocalVariableIndex() {
            return index;
        }

        @Override
        public ForceControlStoreLocal setLocalVariableIndex(int index) {
            return new ForceControlStoreLocal(index, type);
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof ForceControlStoreLocal)) {
                return false;
            }
            ForceControlStoreLocal that = (ForceControlStoreLocal) o;
            return index == that.index;
        }

        @Override
        public int hashCode() {
            return index;
        }
    }

    final class ForceControlStoreField implements ForceControlStore {

        private final Field field;

        public ForceControlStoreField(Field field) {
            if(field == null) {
                throw new NullPointerException();
            }
            this.field = field;
        }

        public Field getField() {
            return field;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            } else if(!(o instanceof ForceControlStoreField)) {
                return false;
            }
            ForceControlStoreField that = (ForceControlStoreField) o;
            return field.equals(that.field);
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }
    }
}
