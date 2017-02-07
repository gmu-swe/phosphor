package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {
	public Taint taint;
	boolean invalidated;
	DoubleLinkedList<EnqueuedTaint> children = new  DoubleLinkedList<EnqueuedTaint>();

	public final boolean isEmpty() {
		return taint == null || (taint.lbl == null && taint.hasNoDependencies());
	}
	public ControlTaintTagStack(int zz) {
		this();
	}
	public ControlTaintTagStack() {
	}
	public Taint copyTag()
	{
		if(taint ==null || getTag() == null)
			return null;
		return taint.copy();
	}

	public final void recalculate() {
		if(!invalidated)
			return;
		invalidated = false;

		if (children != null) {
			taint = new Taint();

			DoubleLinkedList.Node<EnqueuedTaint> n = children.getFirst();
			while(n != null) {
				Taint t = n.entry.taint;
				if (t != null) {
					if (t.lbl != null)
						taint.addDependency(t);
					else if (!(t.hasNoDependencies())) {
						taint.dependencies.addAll(t.dependencies);
					}
				}
				n = n.next;
			}
		}
		else
			taint = null;
	}
	
	public final EnqueuedTaint push(Object ob,EnqueuedTaint prev) {
		if (ob instanceof Tainted && ob instanceof TaintedWithObjTag) {
		    return push((Taint) (((TaintedWithObjTag) ob).getPHOSPHOR_TAG()), prev);
		}
		return prev;
	}
	public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
		if (tag == null || tag == taint)
			return prev;
		//Ensure not already enqueued in this thread's control flow
		if(tag.enqueuedInControlFlow == null)
			tag.enqueuedInControlFlow = new LinkedList<EnqueuedTaint>();
		LinkedList.Node<EnqueuedTaint> e = tag.enqueuedInControlFlow.getFirst();
		while (e != null) {
			if (e.entry != null && e.entry.controlTag == this)
				return prev; //already have this taint tag in this controlflowtainttagstack
			e = e.next;
		}
		EnqueuedTaint entry = new EnqueuedTaint(tag, this);

		entry.prev = prev;
		entry.place = children.add(entry);
		tag.enqueuedInControlFlow.addFast(entry);
		if(taint == null)
			taint = new Taint();
		taint.addDependency(tag);
		invalidated = true;
		return entry;
	}

	private boolean _pop(EnqueuedTaint enq)
	{
		if(enq == null || enq.taint == null)
		{
			return false;
		}
		invalidated = true;

		Taint tag = enq.taint;
		boolean recalc = tag.lbl != null || !tag.hasNoDependencies();
		LinkedList.Node<EnqueuedTaint> e = tag.enqueuedInControlFlow.getFirst();
		LinkedList.Node<EnqueuedTaint> p = null;
		while(e != null)
		{
			if(e.entry.controlTag == this)
			{
				//Remove from controltag's list
				DoubleLinkedList.Node<EnqueuedTaint> pl = e.entry.place;
				if(pl.prev != null)
					pl.prev.next = pl.next;
				if(pl.next != null)
					pl.next.prev  = pl.prev;
				//Remove from tag's list
				if(p != null)
					p.next = e.next;
				else
					tag.enqueuedInControlFlow.clear();
			}
			p = e;
			e = e.next;
		}
		return recalc;
	}
	public final void pop(EnqueuedTaint enq) {

		boolean recalc = _pop(enq);
		if (enq != null) {
			EnqueuedTaint prev = enq.prev;
			while (prev != null) {
				_pop(prev);
				prev = prev.prev;
			}
		}
		if (recalc)
		{
			recalculate();
		}
	}

	public Taint getTag() {
		if(taint == null || (taint.hasNoDependencies() && taint.lbl == null))
			return null;
		return taint;
	}
	public void reset() {
		children = new DoubleLinkedList<EnqueuedTaint>();
		taint = null;
	}
}
