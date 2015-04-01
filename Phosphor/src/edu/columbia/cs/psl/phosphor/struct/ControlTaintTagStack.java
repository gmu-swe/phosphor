package edu.columbia.cs.psl.phosphor.struct;

import java.nio.channels.UnsupportedAddressTypeException;

import edu.columbia.cs.psl.phosphor.runtime.SimpleMultiTaintHandler;

public class ControlTaintTagStack {
	ControlTaintTagStack parent;
	Node children; //On child nodes, this points to the child node for us

	public final boolean isEmpty() {
		if (parent != null)
			return parent.isEmpty();
		return top == null || top.tag == 0;
	}

	public ControlTaintTagStack() {
		this.parent = null;
		this.children = null;
	}
	public ControlTaintTagStack(ControlTaintTagStack prevStack, boolean isFromNewMethod) {

		this.parent = null;
		this.children= null;
		if(prevStack.top != null)
		{
			this.top = new Node();
			this.top.tag = prevStack.top.tag;
		}
	}

	public ControlTaintTagStack(ControlTaintTagStack parent) {
		this.parent = parent;
		Node oldChildren = this.parent.children;
		this.parent.children = new Node();
		this.parent.children.prev = oldChildren;
		this.parent.children.tag = 0;
		this.children = this.parent.children;
	}

	public void recalculate() {
		if (top == null)
			top = new Node();
		top.tag = 0;
		Node t = children;
		while (t != null) {
			if (t.tag > 0) {
				top.tag = SimpleMultiTaintHandler.combineTags(top.tag, t.tag);
			}
			t = t.prev;
		}
	}

	public void appendTag(Object ob) {
		if(ob instanceof Tainted)
		{
			if(ob instanceof TaintedWithIntTag)
				appendTag(((TaintedWithIntTag) ob).getPHOSPHOR_TAG());
		}
	}

	public void appendTag(int tag) {
		if (parent != null)
			parent.appendTag(tag);
		if (top != null) {
			int t = SimpleMultiTaintHandler.combineTags(top.tag, tag);
			if (t != top.tag) {
				Node n = new Node();
				n.tag = t;
				n.prev = top;
				top = n;
				return;
			}
			return;
		} else {
			top = new Node();
			top.tag = tag;
			return;
		}
	}

	public void appendTag(int tag1, int tag2) {
		if (tag1 != 0)
			appendTag(tag1);
		if (tag2 == 0)
			appendTag(tag2);
	}

	public void pop() {
		top = null;
		children.tag = 0;
		parent.recalculate();
	}

	Node top;

	class Node implements Cloneable{
		int tag;
		Node prev;
	}

	public int getTag() {
		if (parent != null)
			return parent.getTag();
		return top.tag;
	}
}
