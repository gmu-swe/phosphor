package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {
	public Taint taint;
	boolean invalidated;
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
//		if(tag != null && tag.taint != null){
//			MaybeThrownException ex = new MaybeThrownException(t,tag.taint);
//			exceptionChildren.add(ex);
//			invalidated = true;
//			recalculate();
//		}
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
//				recalculate();
				break;
			}
			n = n.next;
		}
	}

	public LinkedList<Taint> prevTaints = new LinkedList<>();

	public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
		if (tag == null || tag == taint)
			return null;

		EnqueuedTaint ret = (prev == null ? new EnqueuedTaint() : prev);
		ret.activeCount++;
		prevTaints.addFast(this.taint);
		if (this.taint == null)
			this.taint = new Taint(tag);
		else {
			Taint prevTaint = this.taint;
			this.taint = prevTaint.copy();
			this.taint.addDependency(tag);
		}
		return ret;
	}

	public final void pop(EnqueuedTaint enq) {
		if (enq == null)
			return;
		while (enq.activeCount > 0) {
			this.taint = prevTaints.pop();
			enq.activeCount--;
		}
	}

	public Taint getTag() {
		if(taint == null || (taint.hasNoDependencies() && taint.lbl == null))
			return null;
		return taint;
	}
	public void reset() {
		exceptionChildren = new DoubleLinkedList<>();
		prevTaints = new LinkedList<>();
		taint = null;
	}
}
