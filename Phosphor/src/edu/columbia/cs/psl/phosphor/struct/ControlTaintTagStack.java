package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {
	public Taint taint;
	boolean invalidated;
	LinkedList<MaybeThrownException> unThrownExceptionStack = new LinkedList<>();

	public LinkedList<MaybeThrownException> influenceExceptions = new LinkedList<>();
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

	static ControlTaintTagStack instance = new ControlTaintTagStack();
	public static ControlTaintTagStack factory(){
		return instance;
	}


	/**
	 * Called ONCE at the start of each exception handler. Should inspect the taint tag on the
	 * exception, and if there is one, we'll need to add it to the current controltatinttagstack and
	 * return a pointer so it can later be removed
	 * @param exceptionCaught
	 * @return
	 */
	public final EnqueuedTaint exceptionHandlerStart(Throwable exceptionCaught, EnqueuedTaint eq)
	{
		if(exceptionCaught instanceof TaintedWithObjTag){
			Taint<?> t = (Taint) ((TaintedWithObjTag) exceptionCaught).getPHOSPHOR_TAG();
			if(t != null) {
				EnqueuedTaint ret =  push(t, eq);
				return ret;
			}
		}
		return null;
	}

	/**
	 * Called N times at the start of each exception handler to clear unthrown exceptions, one time
	 * for each handled exception type
	 * @param exTypeHandled
	 */
	public final void exceptionHandlerStart(Class<? extends Throwable> exTypeHandled)
	{
		tryBlockEnd(exTypeHandled);
	}

	/**
	 * Called ONCE at the end of each handler to remove an exception from influencing the control state
	 * Passed the same MaybeThrownException from the start method
	 */
	public final void exceptionHandlerEnd(EnqueuedTaint ex)
	{
		if(ex != null)
			pop(ex);
	}


	/**
	 * Called N times at the end of each try block to clear unthrown exceptions, one time for each handled exception type
	 * @param exTypeHandled
	 */
	public final void tryBlockEnd(Class<? extends Throwable> exTypeHandled){
		LinkedList.Node<MaybeThrownException> n = influenceExceptions.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null){
			if(exTypeHandled.isAssignableFrom(n.entry.clazz)){
				if(prev == null)
					influenceExceptions.pop();
				else
					prev.next = n.next;
				return;
			}
			prev = n;
			n = n.next;
		}

	}

	/*
	If there is some maybeThrownException (e.g. from something we are returning to),
	and we are now in code that follows that code in a "try" block, then that unthrown exception
	is currently effecting the current flow (at least until the end of the catch block)
	 */
	public final void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> t)
	{
		LinkedList.Node<MaybeThrownException> n = unThrownExceptionStack.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null){
			if(t.isAssignableFrom(n.entry.clazz)){
				if(prev == null)
					unThrownExceptionStack.pop();
				else
					prev.next = n.next;
				influenceExceptions.addFast(n.entry);
				return;
			}
			prev = n;
			n = n.next;
		}
	}

	/*
	The "lazy" approach to handling exceptions:
		Based on the simple static analysis that we do: if we are at a return statement, and we COULD have thrown
		an exception if we went down some branch differently, we note that, along with whatever taints
		were applied *in this method only*
	 */
	public final void addUnthrownException(ExceptionalTaintData taints, Class<? extends Throwable> t) {
		if (taints != null && taints.taint != null) {
			MaybeThrownException ex = new MaybeThrownException(t, taints.taint.copy());
			unThrownExceptionStack.add(ex);
		}
	}

	public LinkedList<Taint> prevTaints = new LinkedList<>();

	public final void push(Taint tag, EnqueuedTaint prev[], int i, ExceptionalTaintData curMethod) {
		if(tag != null)
			curMethod.push(tag);
		push(tag, prev, i);
	}
	public final void push(Taint tag, EnqueuedTaint[] prev, int i) {
		if (tag == null || tag == taint)
			return ;

		if(prev[i] == null)
			prev[i] = new EnqueuedTaint();
		prev[i].activeCount++;
		prevTaints.addFast(this.taint);
		if (this.taint == null)
		{
			this.taint = new Taint(tag);
		}
		else {
			Taint prevTaint = this.taint;
			this.taint = prevTaint.copy();
			this.taint.addDependency(tag);

		}
		return;
	}
	public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
		if (tag == null || tag == taint)
			return null;

		EnqueuedTaint ret = (prev == null ? new EnqueuedTaint() : prev);

		ret.activeCount++;
		prevTaints.addFast(this.taint);
		if (this.taint == null)
		{
			this.taint = new Taint(tag);
		}
		else {
			Taint prevTaint = this.taint;
			this.taint = prevTaint.copy();
			this.taint.addDependency(tag);

		}
		return ret;
	}
	public final void pop(EnqueuedTaint enq[], int i, ExceptionalTaintData curMethod) {
		if(enq[i] == null)
			return;
		curMethod.pop(enq[i].activeCount);
		pop(enq, i);

	}
	public final void pop(EnqueuedTaint[] enq, int i) {
		if (enq[i] == null)
			return;
		while (enq[i].activeCount > 0) {
			this.taint = prevTaints.pop();
			enq[i].activeCount--;
		}
	}
	public final void pop(EnqueuedTaint enq[], ExceptionalTaintData curMethod) {
		for(EnqueuedTaint e : enq) {
			if(e != null) {
				curMethod.pop(e.activeCount);
				while (e.activeCount > 0) {
					this.taint = prevTaints.pop();
					e.activeCount--;
				}
			}
		}

	}
	public final void pop(EnqueuedTaint[] enq) {
		for(EnqueuedTaint e : enq)
		{
			if(e != null)
				while (e.activeCount > 0) {
					this.taint = prevTaints.pop();
					e.activeCount--;
				}
		}
	}
	public final void pop(EnqueuedTaint enq) {
		if (enq == null)
			return;
		while (enq.activeCount > 0) {
			this.taint = prevTaints.pop();
			enq.activeCount--;
		}
	}

	public Taint copyTagExceptions(){
		if(
				(taint == null || (taint.hasNoDependencies() && taint.lbl == null))
				&& influenceExceptions.isEmpty()
		){
			return null;
		}
		Taint ret = taint;
		if(influenceExceptions.isEmpty())
			return ret.copy();
		if(ret == null)
			ret = new Taint();
		else
			ret = ret.copy();
		LinkedList.Node<MaybeThrownException> n = influenceExceptions.getFirst();
		while(n != null){
			if(n.entry != null && n.entry.tag != null)
				ret.addDependency(n.entry.tag);
			n=n.next;
		}
		return ret;
	}
	public Taint getTag() {
		if(taint == null || (taint.hasNoDependencies() && taint.lbl == null))
			return null;
		return taint;
	}
	public void reset() {
		unThrownExceptionStack = new LinkedList<>();
		prevTaints = new LinkedList<>();
		taint = null;
	}
}
