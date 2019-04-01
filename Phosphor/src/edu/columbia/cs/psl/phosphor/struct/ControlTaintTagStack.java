package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {

	public boolean isDisabled;
	public Taint taint;
	LinkedList<MaybeThrownException> unThrownExceptionStack;// = new LinkedList<>();

	public LinkedList<MaybeThrownException> influenceExceptions;// = new LinkedList<>();
	public final boolean isEmpty() {
		return taint == null || this.isDisabled || (taint.lbl == null && taint.hasNoDependencies());
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

	private ControlTaintTagStack(boolean isDisabled){
		this.isDisabled = isDisabled;
	}
	static ControlTaintTagStack instance = new ControlTaintTagStack(true);

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
		if(influenceExceptions == null)
			return;
		LinkedList.Node<MaybeThrownException> n = influenceExceptions.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null){
			if(exTypeHandled.isAssignableFrom(n.entry.clazz)){
				if(prev == null)
					influenceExceptions.pop();
				else {
					prev.next = n.next;
				}
			}
			else
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
		if(unThrownExceptionStack == null)
			return;
		LinkedList.Node<MaybeThrownException> n = unThrownExceptionStack.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null){
			if(t.isAssignableFrom(n.entry.clazz)){
				if(prev == null)
					unThrownExceptionStack.pop();
				else {
					prev.next = n.next;
				}
				if (influenceExceptions == null)
					influenceExceptions = new LinkedList<>();
				influenceExceptions.addFast(n.entry);
			}
			else
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
			if(unThrownExceptionStack == null)
				unThrownExceptionStack = new LinkedList<>();
			LinkedList.Node<MaybeThrownException> i = unThrownExceptionStack.getFirst();
			boolean found = false;
			while(i != null)
			{
				if(i.entry != null && i.entry.clazz == t)
				{
					found = true;
					if(taints.taint.tags != null)
						i.entry.tag.setBits(taints.taint.tags);
					else
						i.entry.tag.addDependency(taints.taint);
					break;
				}
				i = i.next;
			}
			if(!found) {
				MaybeThrownException ex = new MaybeThrownException(t, taints.taint.copy());
				unThrownExceptionStack.addFast(ex);
			}
		}
	}

	public LinkedList<Taint> prevTaints = new LinkedList<>();

	public final int[] push(Taint tag, int prev[], int i, int maxSize, ExceptionalTaintData curMethod) {
		if (tag == null) //|| tag == taint) TODO: is this safe to optimize (commented out)?
			return prev;
		return _push(tag, prev, i, maxSize, curMethod);
	}
	public final int[] push(Taint tag, int[] prev, int i, int maxSize) {
		if (tag == null || tag == taint)
			return prev;
		return _push(tag, prev, i, maxSize, null);
	}
	public final int[] push(Object obj, int prev[], int i, int maxSize, ExceptionalTaintData curMethod) {
		Taint tag = null;
		if(obj instanceof TaintedWithObjTag)
			tag = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		if(tag == null || tag == taint)
			return prev;
		return _push(tag, prev, i, maxSize, curMethod);
	}
	public final int[] push(Object obj, int[] prev, int i, int maxSize) {
		Taint tag = null;
		if(obj instanceof TaintedWithObjTag)
			tag = (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG();
		if(tag == null || tag == taint)
			return prev;
		return _push(tag, prev, i, maxSize, null);
	}

	public final int[] _push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData exceptionData){
		if(isDisabled)
			return invocationCountPerBranch;
		//Try a deeper check
//		if(this.taint != null && (tag.lbl == null || tag.lbl == this.taint.lbl || this.taint.dependencies.contains(tag.lbl)))
//		{
//			boolean ok = true;
//			for(Object lbl : tag.dependencies)
//
//
//			{
//				if(!this.taint.dependencies.contains(lbl))
//					ok = false;
//			}
//			if(ok)
//				return prev;
//
//		}
		if(invocationCountPerBranch == null)
			invocationCountPerBranch = new int[maxSize];
		invocationCountPerBranch[indexOfBranchInMethod]++;
		if(invocationCountPerBranch[indexOfBranchInMethod] == 1) {
			prevTaints.addFast(this.taint);
			if (exceptionData != null) {
				exceptionData.push(tag);
			}
			if (this.taint == null) {
				this.taint = new Taint(tag);
			} else {
				Taint prevTaint = this.taint;
				this.taint = prevTaint.copy();
				this.taint.addDependency(tag);

			}
		}
		return invocationCountPerBranch;
	}
	public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
		if (tag == null || tag == taint || isDisabled)
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
	public final void pop(int enq[], int i, ExceptionalTaintData curMethod) {
		if(enq == null || enq[i] == 0)
			return;
		curMethod.pop(enq[i]);
		pop(enq, i);

	}
	public final void pop(int[] enq, int i) {
		if (enq == null || enq[i] == 0)
			return;
		_pop(enq, i);
	}
	private final void _pop(int[] enq, int i){
		if(enq[i] > 0)
			this.taint = prevTaints.pop();
		while (enq[i]> 0) {
			enq[i]--;
		}
	}
	public final void pop(int enq[], ExceptionalTaintData curMethod) {
		if(enq == null)
			return;
		for(int i = 0; i < enq.length; i++){
			if(enq[i] != 0) {
				curMethod.pop(enq[i]);
				while (enq[i]> 0) {
					this.taint = prevTaints.pop();
					enq[i]--;
				}
			}
		}

	}
	private final void _pop(int[] enq){
		for (int i = 0; i < enq.length; i++) {
			if (enq[i] != 0) {
				if(enq[i] > 0)
					this.taint = prevTaints.pop();
				while (enq[i] > 0) {
					enq[i]--;
				}
			}
		}
	}
	public final void pop(int[] enq) {
		if(enq == null)
			return;
		_pop(enq);
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
				&& (influenceExceptions == null || influenceExceptions.isEmpty())
		){
			return null;
		}
		Taint ret = taint;
		if(influenceExceptions == null || influenceExceptions.isEmpty())
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
		if(taint == null || isDisabled || (taint.hasNoDependencies() && taint.lbl == null))
			return null;
		return taint;
	}
	public void reset() {
		prevTaints = new LinkedList<>();
		taint = null;
		if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
			unThrownExceptionStack = new LinkedList<>();
			influenceExceptions = new LinkedList<>();
		}
	}
}
