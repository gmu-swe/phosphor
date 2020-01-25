package edu.columbia.cs.psl.phosphor.instrumenter.asm;

import org.objectweb.asm.Label;

public class OffsetPreservingLabel extends Label {
    private int originalPosition;

    public OffsetPreservingLabel(int originalPosition) {
        this.originalPosition = originalPosition;
    }

    public int getOriginalPosition() {
        return originalPosition;
    }
}
