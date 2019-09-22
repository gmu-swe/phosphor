package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public final class ControlTaintTagStack {

	private static ControlTaintTagStack instance = new ControlTaintTagStack(true);

	private boolean isDisabled = false;
	private Taint taint = null;
	private LinkedList<MaybeThrownException> unthrownExceptionStack = null;
	private LinkedList<MaybeThrownException> influenceExceptions = null;
	private LinkedList<Taint> prevTaints = new LinkedList<>();

	public ControlTaintTagStack(int zz) {
		this();
	}

	public ControlTaintTagStack() {
	}

	private ControlTaintTagStack(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	@SuppressWarnings("unused")
	public static ControlTaintTagStack factory() {
		return instance;
	}

	/**
	 * Called ONCE at the start of each exception handler. Should inspect the taint tag on the
	 * exception, and if there is one, we'll need to add it to the current ControlTaintTagStack and
	 * return a pointer so it can later be removed
	 */
	public final EnqueuedTaint exceptionHandlerStart(Throwable exceptionCaught, EnqueuedTaint eq) {
		if(exceptionCaught instanceof TaintedWithObjTag) {
			Taint<?> t = (Taint) ((TaintedWithObjTag) exceptionCaught).getPHOSPHOR_TAG();
			if(t != null) {
				return push(t, eq);
			}
		}
		return null;
	}

	/**
	 * Called N times at the start of each exception handler to clear unthrown exceptions, one time
	 * for each handled exception type
	 */
	public final void exceptionHandlerStart(Class<? extends Throwable> exTypeHandled) {
		tryBlockEnd(exTypeHandled);
	}

	/**
	 * Called ONCE at the end of each handler to remove an exception from influencing the control state
	 * Passed the same MaybeThrownException from the start method
	 */
	public final void exceptionHandlerEnd(EnqueuedTaint ex) {
		if(ex != null) {
			pop(ex);
		}
	}

	/**
	 * Called N times at the end of each try block to clear unthrown exceptions, one time for each handled exception type
	 */
	public final void tryBlockEnd(Class<? extends Throwable> exTypeHandled) {
		if(influenceExceptions == null) {
			return;
		}
		LinkedList.Node<MaybeThrownException> n = influenceExceptions.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null) {
			if(exTypeHandled.isAssignableFrom(n.entry.clazz)) {
				if(prev == null) {
					influenceExceptions.pop();
				} else {
					prev.next = n.next;
				}
			} else {
				prev = n;
			}
			n = n.next;
		}
	}

	/**
	 * If there is some maybeThrownException (e.g. from something we are returning to), and we are now in code that
	 * follows that code in a "try" block, then that unthrown exception
	 * is currently effecting the current flow (at least until the end of the catch block)
	 */
	public final void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> t) {
		if(unthrownExceptionStack == null) {
			return;
		}
		LinkedList.Node<MaybeThrownException> n = unthrownExceptionStack.getFirst();
		LinkedList.Node<MaybeThrownException> prev = null;
		while(n != null) {
			if(t.isAssignableFrom(n.entry.clazz)) {
				if(prev == null) {
					unthrownExceptionStack.pop();
				} else {
					prev.next = n.next;
				}
				if(influenceExceptions == null) {
					influenceExceptions = new LinkedList<>();
				}
				influenceExceptions.addFast(n.entry);
			} else {
				prev = n;
			}
			n = n.next;
		}
	}

	/**
	 * 	The "lazy" approach to handling exceptions:
	 * 		Based on the simple static analysis that we do: if we are at a return statement, and we COULD have thrown
	 * 		an exception if we went down some branch differently, we note that, along with whatever taints
	 * 		were applied in this method only
	 */
	public final void addUnthrownException(ExceptionalTaintData taints, Class<? extends Throwable> t) {
		if(taints != null && taints.taint != null) {
			if(unthrownExceptionStack == null) {
				unthrownExceptionStack = new LinkedList<>();
			}
			LinkedList.Node<MaybeThrownException> i = unthrownExceptionStack.getFirst();
			boolean found = false;
			while(i != null) {
				if(i.entry != null && i.entry.clazz == t) {
					found = true;
					i.entry.tag.addDependency(taints.taint);
					break;
				}
				i = i.next;
			}
			if(!found) {
				MaybeThrownException ex = new MaybeThrownException(t, taints.taint.copy());
				unthrownExceptionStack.addFast(ex);
			}
		}
	}

	public final int[] push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData curMethod) {
		if(tag == null) //|| tag == taint) TODO: is this safe to optimize (commented out)?
			return invocationCountPerBranch;
		return _push(tag, invocationCountPerBranch, indexOfBranchInMethod, maxSize, curMethod);
	}

	public final int[] push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize) {
		return push(tag, invocationCountPerBranch, indexOfBranchInMethod, maxSize, null);
	}

	public final int[] push(Object obj, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData curMethod) {
		Taint tag = (obj instanceof TaintedWithObjTag) ? (Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG() : null;
		return push(tag, invocationCountPerBranch, indexOfBranchInMethod, maxSize, curMethod);
	}

	public final int[] push(Object obj, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize) {
		return push(obj, invocationCountPerBranch, indexOfBranchInMethod, maxSize, null);
	}

	public final int[] _push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData exceptionData) {
		if(isDisabled) {
			return invocationCountPerBranch;
		}
		//Try a deeper check
//		if(this.taint != null && (tag.lbl == null || tag.lbl == this.taint.lbl || this.taint.dependencies.contains(tag.lbl))) {
//			boolean ok = true;
//			for(Object lbl : tag.dependencies) {
//				if(!this.taint.dependencies.contains(lbl))
//					ok = false;
//			}
//			if(ok)
//				return prev;
//		}
		if(invocationCountPerBranch == null) {
			invocationCountPerBranch = new int[maxSize];
		}
		invocationCountPerBranch[indexOfBranchInMethod]++;
		if(invocationCountPerBranch[indexOfBranchInMethod] == 1) {
			prevTaints.addFast(this.taint);
			if(exceptionData != null) {
				exceptionData.push(tag);
			}
			if(this.taint == null) {
				this.taint = new Taint<>(tag);
			} else {
				Taint prevTaint = this.taint;
				this.taint = prevTaint.copy();
				this.taint.addDependency(tag);
			}
		}
		return invocationCountPerBranch;
	}

	public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
		if(tag == null || tag == taint || isDisabled) {
			return null;
		}

		EnqueuedTaint ret = prev == null ? new EnqueuedTaint() : prev;

		ret.activeCount++;
		prevTaints.addFast(this.taint);
		if(this.taint == null) {
			this.taint = new Taint(tag);
		} else {
			Taint prevTaint = this.taint;
			this.taint = prevTaint.copy();
			this.taint.addDependency(tag);

		}
		return ret;
	}

	public final void pop(int[] enq, int i, ExceptionalTaintData curMethod) {
		if(enq != null && enq[i] != 0) {
			curMethod.pop(enq[i]);
			pop(enq, i);
		}
	}

	public final void pop(int[] enq, int i) {
		if(enq != null && enq[i] != 0) {
			_pop(enq, i);
		}
	}

	private void _pop(int[] enq, int i) {
		if(enq[i] > 0) {
			this.taint = prevTaints.pop();
		}
		while(enq[i] > 0) {
			enq[i]--;
		}
	}

	public final void pop(int[] enq, ExceptionalTaintData curMethod) {
		if(enq != null) {
			for(int i = 0; i < enq.length; i++) {
				if(enq[i] != 0) {
					curMethod.pop(enq[i]);
					while(enq[i]> 0) {
						this.taint = prevTaints.pop();
						enq[i]--;
					}
				}
			}
		}
	}

	private void _pop(int[] enq) {
		for(int i = 0; i < enq.length; i++) {
			if(enq[i] > 0) {
				this.taint = prevTaints.pop();
				while(enq[i] > 0) {
					enq[i]--;
				}
			}
		}
	}

	public final void pop(int[] enq) {
		if (enq != null) {
			_pop(enq);
		}
	}

	public final void pop(EnqueuedTaint enq) {
		if(enq != null) {
			while(enq.activeCount > 0) {
				this.taint = prevTaints.pop();
				enq.activeCount--;
			}
		}
	}

	public Taint copyTagExceptions() {
		if((taint == null || taint.isEmpty()) && !hasInfluenceExceptions()) {
			return null;
		}
		Taint ret = taint == null ? new Taint() : taint.copy();
		if(!hasInfluenceExceptions()) {
			return ret;
		}
		LinkedList.Node<MaybeThrownException> n = influenceExceptions.getFirst();
		while(n != null) {
			if(n.entry != null && n.entry.tag != null) {
				ret.addDependency(n.entry.tag);
			}
			n = n.next;
		}
		return ret;
	}

    public Taint copyTag() {
        return isEmpty() ? null : taint.copy();
    }

	public Taint getTag() {
		return isEmpty() ? null : taint;
	}

    public final boolean isEmpty() {
        return this.isDisabled || taint == null || taint.isEmpty();
    }

	public void reset() {
		prevTaints = new LinkedList<>();
		taint = null;
		if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
			unthrownExceptionStack = new LinkedList<>();
			influenceExceptions = new LinkedList<>();
		}
	}

	public boolean hasInfluenceExceptions() {
		return influenceExceptions != null && !influenceExceptions.isEmpty();
	}

	@SuppressWarnings("unused")
	public void enable() {
		this.isDisabled = false;
	}

	@SuppressWarnings("unused")
	public void disable() {
		this.isDisabled = true;
	}
}
