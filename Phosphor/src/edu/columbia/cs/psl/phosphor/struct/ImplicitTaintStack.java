package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.SimpleMultiTaintHandler;

public class ImplicitTaintStack {
	public int push(int tag) {
		//		synchronized (this) 
		{
			/*
			 * Is this tag already represented in the stack markings?
			 */
			if (top != null) {
				int t = SimpleMultiTaintHandler.combineTags(top.tag, tag);
				if (t != top.tag) {
					Node n = new Node();
					n.tag = t;
					n.prev = top;
					top = n;
					return t;
				}
				return t;
			} else {
				top = new Node();
				top.tag = tag;
				return tag;
			}
		}
	}

	public int pop() {
		//		synchronized (this) 
		{
			top = top.prev;
			if (top != null)
				return top.tag;
			return 0;
		}
	}

	Node top;

	class Node {
		int tag;
		Node prev;
	}
}
