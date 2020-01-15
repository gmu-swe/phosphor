package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;

public class UnthrownExceptionCheck implements PhosphorInstructionInfo {
    private final String exceptionType;

    public UnthrownExceptionCheck(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof UnthrownExceptionCheck)) {
            return false;
        }
        UnthrownExceptionCheck that = (UnthrownExceptionCheck) o;
        return exceptionType != null ? exceptionType.equals(that.exceptionType) : that.exceptionType == null;
    }

    @Override
    public int hashCode() {
        return exceptionType != null ? exceptionType.hashCode() : 0;
    }
}

