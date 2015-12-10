package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class EnqueuedTaint {
	public Taint taint;
	public ControlTaintTagStack controlTag;
	public DoubleLinkedList.Node<EnqueuedTaint> place;
	public EnqueuedTaint(Taint taint, ControlTaintTagStack controlTag) {
		this.taint = taint;
		this.controlTag = controlTag;
	}
	public EnqueuedTaint prev;
}
