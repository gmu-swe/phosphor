package edu.columbia.cs.psl.phosphor.control.binding.trace;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.control.binding.LoopLevel;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

import java.util.Iterator;

public class LoopAwareConstancyInfo implements PhosphorInstructionInfo {

    private final int invocationLevel;
    private final SinglyLinkedList<LoopLevel> argumentLevels = new SinglyLinkedList<>();

    LoopAwareConstancyInfo(int invocationLevel) {
        this.invocationLevel = invocationLevel;
    }

    public int getInvocationLevel() {
        return invocationLevel;
    }

    public int getNumArguments() {
        return argumentLevels.size();
    }

    void pushArgumentLevel(LoopLevel level) {
        argumentLevels.push(level);
    }

    public Iterator<LoopLevel> getLevelIterator() {
        return argumentLevels.iterator();
    }
}
