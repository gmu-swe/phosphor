package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ExceptionalTaintData {

    private final SinglyLinkedList<Taint> taintHistory;

    public ExceptionalTaintData() {
        taintHistory = new SinglyLinkedList<>();
        taintHistory.push(null); // starting taint is null/empty
    }

    public Taint getCurrentTaint() {
        return taintHistory.peek();
    }

    public void reset() {
        taintHistory.clear();
        taintHistory.push(null); // starting taint is null/empty
    }

    @SuppressWarnings("unchecked")
    public void push(Taint tag) {
        taintHistory.push(tag.union(taintHistory.peek()));
    }

    public Taint pop() {
        return taintHistory.pop();
    }

    public void unionWithCurrentTaint(Taint tag) {
        Taint top = getCurrentTaint();
        if (top != null && top.isSuperset(tag)) {
            return;
        }
        push(tag.union(pop()));
    }
}
