package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public class ReferenceArrayTarget implements PhosphorInstructionInfo {
    private String originalArrayType;

    public ReferenceArrayTarget(String originalArrayType) {
        this.originalArrayType = originalArrayType;
    }

    public String getOriginalArrayType() {
        return originalArrayType;
    }

    @Override
    public String toString() {
        return "ReferenceArrayTarget{" +
                "originalArrayType='" + originalArrayType + '\'' +
                '}';
    }
}
