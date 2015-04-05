package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ControlTaintTagStack {
	public Taint taint;
	boolean invalidated;
	public Taint parentTaint;
	ControlTaintTagStack[] children;

	public final boolean isEmpty() {
		return taint.lbl == null && taint.hasNoDependencies();
	}

	public ControlTaintTagStack() {
		this.taint = new Taint();
	}

	public ControlTaintTagStack(int nJump) {
		this.children = new ControlTaintTagStack[nJump];
		this.taint = new Taint();
	}

	public ControlTaintTagStack(int nJump, ControlTaintTagStack parent) {
		this.taint = new Taint();
		this.parentTaint = parent.taint;
		this.children = new ControlTaintTagStack[nJump];
	}

	public final void recalculate() {
		taint = new Taint();
		taint.addDependency(parentTaint);
		for (ControlTaintTagStack t : children) {
			if (t != null)
				taint.addDependency(t.taint);
		}
	}

	public final void appendTag(Object ob, int child) {
		if (ob instanceof Tainted) {
			if (ob instanceof TaintedWithObjTag)
				appendTag((Taint) (((TaintedWithObjTag) ob).getPHOSPHOR_TAG()), child);
		}
	}

	public final void appendTag(Taint tag, int child) {
		if (tag == null)
			return;
		if (tag.lbl != null) {
			if (children[child - 1] == null)
				children[child - 1] = new ControlTaintTagStack();
			if (children[child - 1].taint.addDependency(tag))
				taint.addDependency(tag);
		} else if (!tag.hasNoDependencies()) //addign a tag with a bunch of deps
		{
			if (children[child - 1] == null)
				children[child - 1] = new ControlTaintTagStack();
			edu.columbia.cs.psl.phosphor.struct.LinkedList.Node<Taint> n = tag.dependencies.getFirst();
			while (n != null) {
				if (n.entry != null && n.entry.lbl != null)
					if (children[child - 1].taint.addDependency(n.entry))
						taint.addDependency(n.entry);
				n = n.next;
			}
		}
	}

	public final void appendTag(Taint tag, Taint tag2, int child) {
		appendTag(tag, child);
		appendTag(tag2, child);
	}

	public final void pop(int child) {
		boolean recalc = children[child - 1] != null;
		children[child - 1] = null;
		if (recalc)
			recalculate();
	}

	public Taint getTag() {
		return taint;
	}
}
