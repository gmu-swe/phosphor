package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public final class ControlTaintTagStack {

    private static ControlTaintTagStack instance = new ControlTaintTagStack(true);
    private final SinglyLinkedList<Taint> taintHistory;
    private boolean isDisabled = false;
    private SinglyLinkedList<MaybeThrownException> unthrownExceptions = null;
    private SinglyLinkedList<MaybeThrownException> influenceExceptions = null;

    public ControlTaintTagStack() {
        this.taintHistory = new SinglyLinkedList<>();
        taintHistory.push(null); // starting taint is null/empty

    }

    private ControlTaintTagStack(boolean isDisabled) {
        this();
        this.isDisabled = isDisabled;
    }

    /**
     * Called ONCE at the start of each exception handler. Should inspect the taint tag on the
     * exception, and if there is one, we'll need to add it to the current ControlTaintTagStack and
     * return a pointer so it can later be removed
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START)
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
     * At the start of an exception handler, this method is called once for each exception type handled by the handler.
     * Removes elements from the unthrown exception list whose exception type either is or is a subtype of the specified type.
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START_VOID)
    public final void exceptionHandlerStart(Class<? extends Throwable> exTypeHandled) {
        tryBlockEnd(exTypeHandled);
    }

    /**
     * Called ONCE at the end of each handler to remove an exception from influencing the control state
     * Passed the same MaybeThrownException from the start method
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_END)
    public final void exceptionHandlerEnd(EnqueuedTaint ex) {
        if(ex != null) {
            pop(ex);
        }
    }

    /**
     * Called N times at the end of each try block to clear unthrown exceptions, one time for each handled exception type
     */
    @InvokedViaInstrumentation(record = CONTROL_STACK_TRY_BLOCK_END)
    public final void tryBlockEnd(Class<? extends Throwable> exTypeHandled) {
        if(influenceExceptions == null) {
            return;
        }
        Iterator<MaybeThrownException> itr = influenceExceptions.iterator();
        while(itr.hasNext()) {
            MaybeThrownException mte = itr.next();
            if(exTypeHandled.isAssignableFrom(mte.clazz)) {
                itr.remove();
            }
        }
    }

    /**
     * If there is some maybeThrownException (e.g. from something we are returning to), and we are now in code that
     * follows that code in a "try" block, then that unthrown exception
     * is currently affecting the current flow (at least until the end of the catch block)
     */
    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION)
    public final void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> t) {
        if(unthrownExceptions == null) {
            return;
        }
        Iterator<MaybeThrownException> itr = unthrownExceptions.iterator();
        while(itr.hasNext()) {
            MaybeThrownException mte = itr.next();
            if(t.isAssignableFrom(mte.clazz)) {
                itr.remove();
                if(influenceExceptions == null) {
                    influenceExceptions = new SinglyLinkedList<>();
                }
                influenceExceptions.push(mte);
            }
        }
    }

    /**
     * The "lazy" approach to handling exceptions:
     * Based on the simple static analysis that we do: if we are at a return statement, and we COULD have thrown
     * an exception if we went down some branch differently, we note that, along with whatever taints
     * were applied in this method only
     */
    @SuppressWarnings("unused")

    @InvokedViaInstrumentation(record = CONTROL_STACK_ADD_UNTHROWN_EXCEPTION)
    public final void addUnthrownException(ExceptionalTaintData taints, Class<? extends Throwable> t) {
        if(taints != null && taints.getCurrentTaint() != null) {
            if(unthrownExceptions == null) {
                unthrownExceptions = new SinglyLinkedList<>();
            }
            boolean found = false;
            for(MaybeThrownException mte : unthrownExceptions) {
                if(mte != null && mte.clazz == t) {
                    found = true;
                    mte.tag = Taint.combineTags(mte.tag, taints.getCurrentTaint().copy());
                    break;
                }
            }
            if(!found) {
                MaybeThrownException ex = new MaybeThrownException(t, taints.getCurrentTaint().copy());
                unthrownExceptions.push(ex);
            }
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG_EXCEPTION)
    public final int[] push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData curMethod) {
        if(tag == null) {
            return invocationCountPerBranch;
        }
        return _push(tag, invocationCountPerBranch, indexOfBranchInMethod, maxSize, curMethod);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG)
    public final int[] push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize) {
        return push(tag, invocationCountPerBranch, indexOfBranchInMethod, maxSize, null);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_OBJECT_EXCEPTION)
    public final int[] push(Object obj, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData curMethod) {
        if(obj instanceof TaintedWithObjTag) {
            return push((Taint) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), invocationCountPerBranch, indexOfBranchInMethod, maxSize, curMethod);
        } else {
            return invocationCountPerBranch;
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_OBJECT)
    public final int[] push(Object obj, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize) {
        return push(obj, invocationCountPerBranch, indexOfBranchInMethod, maxSize, null);
    }

    @SuppressWarnings("unchecked")
    private int[] _push(Taint tag, int[] invocationCountPerBranch, int indexOfBranchInMethod, int maxSize, ExceptionalTaintData exceptionData) {
        if(isDisabled || tag == null || tag.isEmpty()) {
            return invocationCountPerBranch;
        }
        if(invocationCountPerBranch == null) {
            invocationCountPerBranch = new int[maxSize];
        }
        invocationCountPerBranch[indexOfBranchInMethod]++;
        if(invocationCountPerBranch[indexOfBranchInMethod] == 1) {
            // Adding a label for this branch for the first time
            taintHistory.push(new Taint(tag, taintHistory.peek()));
            if(exceptionData != null) {
                exceptionData.push(tag);
            }
        } else {
            taintHistory.peek().addDependency(tag);
            if(exceptionData != null) {
                exceptionData.getCurrentTaint().addDependency(tag);
            }
        }
        return invocationCountPerBranch;
    }

    @SuppressWarnings("unchecked")
    public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
        if(tag == null || tag.isEmpty() || tag == taintHistory.peek() || isDisabled) {
            return null;
        }
        EnqueuedTaint ret = prev == null ? new EnqueuedTaint() : prev;
        ret.activeCount++;
        taintHistory.push(new Taint(tag, taintHistory.peek()));
        return ret;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_EXCEPTION)
    public final void pop(int[] invocationCountPerBranch, int indexOfBranchInMethod, ExceptionalTaintData curMethod) {
        if(invocationCountPerBranch != null && invocationCountPerBranch[indexOfBranchInMethod] > 0) {
            curMethod.pop();
            taintHistory.pop();
            invocationCountPerBranch[indexOfBranchInMethod] = 0;
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP)
    public final void pop(int[] invocationCountPerBranch, int indexOfBranchInMethod) {
        if(invocationCountPerBranch != null) {
            if(invocationCountPerBranch[indexOfBranchInMethod] > 0) {
                taintHistory.pop();
            }
            invocationCountPerBranch[indexOfBranchInMethod] = 0;
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL_EXCEPTION)
    public final void pop(int[] invocationCountPerBranch, ExceptionalTaintData curMethod) {
        if(invocationCountPerBranch != null) {
            pop(invocationCountPerBranch);
            if(curMethod != null) {
                curMethod.reset();
            }
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL)
    public final void pop(int[] invocationCountPerBranch) {
        if(invocationCountPerBranch != null) {
            for(int i = 0; i < invocationCountPerBranch.length; i++) {
                if(invocationCountPerBranch[i] > 0) {
                    taintHistory.pop();
                    invocationCountPerBranch[i] = 0;
                }
            }
        }
    }

    public final void pop(EnqueuedTaint enq) {
        if(enq != null) {
            while(enq.activeCount > 0) {
                taintHistory.pop();
                enq.activeCount--;
            }
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_EXCEPTIONS)
    public Taint copyTagExceptions() {
        if(isEmpty() && lacksInfluenceExceptions()) {
            return null;
        }
        Taint ret = taintHistory.peek() == null ? new Taint() : taintHistory.peek().copy();
        if(lacksInfluenceExceptions()) {
            return ret;
        }

        for(MaybeThrownException mte : influenceExceptions) {
            if(mte != null && mte.tag != null) {
                ret.addDependency(mte.tag);
            }
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG)
    public Taint copyTag() {
        return isEmpty() ? null : taintHistory.peek().copy();
    }

    public Taint getTag() {
        return isEmpty() ? null : taintHistory.peek();
    }

    public final boolean isEmpty() {
        return isDisabled || taintHistory.peek() == null || taintHistory.peek().isEmpty();
    }

    public void reset() {
        taintHistory.clear();
        taintHistory.push(null);
        if(influenceExceptions != null) {
            influenceExceptions.clear();
        }
        if(unthrownExceptions != null) {
            unthrownExceptions.clear();
        }
    }

    public boolean lacksInfluenceExceptions() {
        return influenceExceptions == null || influenceExceptions.isEmpty();
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_ENABLE)
    public void enable() {
        this.isDisabled = false;
    }

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_DISABLE)
    public void disable() {
        this.isDisabled = true;
    }

    @SuppressWarnings("unused")
    public static ControlTaintTagStack factory() {
        return instance;
    }
}
