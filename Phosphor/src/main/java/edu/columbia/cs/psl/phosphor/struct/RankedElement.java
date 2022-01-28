package edu.columbia.cs.psl.phosphor.struct;

/**
 * Record type that associates an element with a rank.
 */
final class RankedElement {
    private final Object element;
    private final int rank;

    RankedElement(Object element, int rank) {
        this.element = element;
        this.rank = rank;
    }

    public Object getElement() {
        return element;
    }

    public int getRank() {
        return rank;
    }
}
