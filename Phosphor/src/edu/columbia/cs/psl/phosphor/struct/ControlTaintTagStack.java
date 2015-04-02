package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.SimpleMultiTaintHandler;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ControlTaintTagStack {
	ControlTaintTagStack parent;
	public Taint taint;
	Node children; //On child nodes, this points to the child node for us
	boolean invalidated;
	
	public final boolean isEmpty() {
		if (parent != null)
			return parent.isEmpty();
		return taint.hasNoDependencies();
	}

	public ControlTaintTagStack() {
		this.parent = null;
		this.children = null;
		this.taint = new Taint();
	}
	public ControlTaintTagStack(ControlTaintTagStack prevStack, boolean isFromNewMethod) {

		this.parent = null;
		this.taint = new Taint(prevStack.taint);
		this.children= null;
	}

	public ControlTaintTagStack(ControlTaintTagStack parent) {
		this.parent = parent;
		this.taint = new Taint();
		Node oldChildren = this.parent.children;
		this.parent.children = new Node();
		this.parent.children.prev = oldChildren;
		this.parent.children.tag = null;
		this.children = this.parent.children;
	}

	public void recalculate() {
		taint = new Taint();
		Node t = children;
		while (t != null) {
			if (t.tag != null && !t.tag.hasNoDependencies()) {
				taint.addDependency(t.tag);
			}
			t = t.prev;
		}
	}

	public void appendTag(Object ob) {
		if(ob instanceof Tainted)
		{
			if(ob instanceof TaintedWithObjTag)
				appendTag((Taint) (((TaintedWithObjTag) ob).getPHOSPHOR_TAG()));
		}
	}
	public void appendTag(Taint tag) {
		if(tag == null)
			return;
		if(tag.lbl != null)
		{
			if(taint.addDependency(tag))
				parent.taint.addDependency(tag);
		}
		else if(!tag.hasNoDependencies())
		{
			edu.columbia.cs.psl.phosphor.struct.LinkedList.Node<Taint> n = tag.dependencies.getFirst();
			while(n != null)
			{
				if(n.entry != null && n.entry.lbl != null)
					if(taint.addDependency(n.entry))
						parent.taint.addDependency(n.entry);
				n =n.next;
			}
		}
	}
	public void appendTag(Taint tag, Taint tag2) {
		appendTag(tag);
		appendTag(tag2);
	}
	
	public void pop() {
		boolean recalc = !taint.hasNoDependencies();
		taint = new Taint();
		if(recalc)
			parent.recalculate();
	}

	Node top;

	class Node implements Cloneable{
		Taint tag;
		Node prev;
	}

	public Taint getTag() {
		if (parent != null)
			return parent.getTag();
		return taint;
	}
}
