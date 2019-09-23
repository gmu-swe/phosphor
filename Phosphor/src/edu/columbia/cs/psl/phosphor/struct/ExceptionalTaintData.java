package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ExceptionalTaintData {

	private final SinglyLinkedList<Taint> taintHistory;

	public ExceptionalTaintData() {
		this.taintHistory = new SinglyLinkedList<>();
		this.taintHistory.push(null); // starting taint is null/empty
	}

	public Taint getCurrentTaint() {
		return taintHistory.peek();
	}

	public void reset() {
		this.taintHistory.clear();
	}

	@SuppressWarnings("unchecked")
	public void push(Taint tag) {
		this.taintHistory.push(new Taint(tag, taintHistory.peek()));
	}

	public Taint pop() {
		return this.taintHistory.pop();
	}
}
