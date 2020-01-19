package edu.columbia.cs.psl.phosphor;

import org.objectweb.asm.Type;

public interface LocalVariablePhosphorInstructionInfo extends PhosphorInstructionInfo {
    int getLocalVariableIndex();

    LocalVariablePhosphorInstructionInfo setLocalVariableIndex(int index);

    Type getType();
}
