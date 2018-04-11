package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {
	public Taint taint;
	boolean invalidated;
	DoubleLinkedList<EnqueuedTaint> children = new  DoubleLinkedList<EnqueuedTaint>();
	DoubleLinkedList<MaybeThrownException> exceptionChildren = new DoubleLinkedList<>();

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

	public final void addPossibleException(EnqueuedTaint tag, Class<? extends Throwable> t){
		if(tag != null && tag.taint != null){
			MaybeThrownException ex = new MaybeThrownException(t,tag.taint);
			exceptionChildren.add(ex);
			invalidated = true;
			recalculate();
		}
	}
	public final void exceptionResolved(Class<? extends Throwable> t){
		DoubleLinkedList.Node<MaybeThrownException> n = exceptionChildren.getFirst();
		while(n != null && n.entry != null){
			if(n.entry.clazz == null || (t == null && n.entry.clazz == null) || (t != null && t.isAssignableFrom(n.entry.clazz))){
				//Found
				DoubleLinkedList.Node<MaybeThrownException> prev = n.prev;
//				if(n.next != null) {
					prev.next = n.next;
					if(n.next != null)
						n.next.prev = prev;
//				}
//				else{
//					this was the last
//					exceptionChildren.popLast();
//				}
				invalidated = true;
				recalculate();
				break;
			}
			n = n.next;
		}
	}
	public final void recalculate() {
		if (!invalidated)
			return;
		invalidated = false;

		taint = new Taint();

		DoubleLinkedList.Node<EnqueuedTaint> n = children.getFirst();
		while (n != null) {
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

		DoubleLinkedList.Node<MaybeThrownException> n2 = exceptionChildren.getFirst();
		while (n2 != null) {
			Taint t = n2.entry.tag;
			if (t != null) {
				if (t.lbl != null)
					taint.addDependency(t);
				else if (!(t.hasNoDependencies())) {
					taint.dependencies.addAll(t.dependencies);
				}
			}
			n2 = n2.next;
		}
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
			taint = new Taint(tag);
		else if (taint.lbl == null && taint.hasNoDependencies())
		{
			taint.lbl = tag.lbl;
			if(tag.dependencies != null)
				taint.dependencies.addAll(tag.dependencies);
		}
		else {
			taint.addDependency(tag);
		}
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
		if (enq == null)
			return;
		boolean recalc = _pop(enq);
		EnqueuedTaint prev = enq.prev;
		while (prev != null) {
			_pop(prev);
			prev = prev.prev;
		}
		if (recalc) {
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
		exceptionChildren = new DoubleLinkedList<>();
		taint = null;
	}
}
