package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ExceptionalTaintData<E> {

    private final SinglyLinkedList<Taint<E>> taintHistory;

    public ExceptionalTaintData() {
        taintHistory = new SinglyLinkedList<>();
        taintHistory.push(Taint.emptyTaint()); // starting taint is null/empty
    }

    public Taint<E> getCurrentTaint() {
        return taintHistory.peek();
    }

    public void reset() {
        taintHistory.clear();
        taintHistory.push(Taint.emptyTaint()); // starting taint is null/empty
    }

    public void push(Taint<E> tag) {
        taintHistory.push(tag.union(taintHistory.peek()));
    }

    public Taint<E> pop() {
        return taintHistory.pop();
    }
}
