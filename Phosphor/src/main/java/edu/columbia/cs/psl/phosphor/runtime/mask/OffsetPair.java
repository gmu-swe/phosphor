package edu.columbia.cs.psl.phosphor.runtime.mask;

@SuppressWarnings("unused")

public class OffsetPair {

    public final long origFieldOffset;
    public final long wrappedFieldOffset;
    public final long tagFieldOffset;
    public final boolean isStatic;

    public OffsetPair(boolean isStatic, long origFieldOffset, long wrappedFieldOffset, long tagFieldOffset) {
        this.isStatic = isStatic;
        this.origFieldOffset = origFieldOffset;
        this.tagFieldOffset = tagFieldOffset;
        this.wrappedFieldOffset = wrappedFieldOffset;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OffsetPair && this.origFieldOffset == ((OffsetPair) other).origFieldOffset && this.isStatic == ((OffsetPair) other).isStatic;
    }

    @Override
    public int hashCode() {
        return (int) (origFieldOffset ^ (origFieldOffset >>> 32));
    }

    @Override
    public String toString() {
        return String.format("{field @ %d -> tag @ %d, wrapper @ %d}", origFieldOffset, tagFieldOffset, wrappedFieldOffset);
    }
}
