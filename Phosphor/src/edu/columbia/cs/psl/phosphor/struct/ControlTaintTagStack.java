package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public final class ControlTaintTagStack {

    public static final int PUSHED = 1;
    public static final int PUSHED_REVISABLE = -1;
    public static final int NOT_PUSHED = 0;

    private static ControlTaintTagStack instance = new ControlTaintTagStack(true);
    private final SinglyLinkedList<Taint> taintHistory = new SinglyLinkedList<>();
    private final SinglyLinkedList<Taint> revisionExcludedTaintHistory = new SinglyLinkedList<>();
    private boolean isDisabled = false;
    private SinglyLinkedList<MaybeThrownException> unthrownExceptions = null;
    private SinglyLinkedList<MaybeThrownException> influenceExceptions = null;

    public ControlTaintTagStack() {
        taintHistory.push(null); // starting taint is null/empty
        revisionExcludedTaintHistory.push(null); // starting taint is null/empty
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
    public final int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, ExceptionalTaintData curMethod, boolean revisable) {
        if(tag == null) {
            return branchTags;
        } else {
            return _push(tag, branchTags, branchID, maxSize, curMethod, revisable);
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG)
    public final int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, boolean revisable) {
        return push(tag, branchTags, branchID, maxSize, null, revisable);
    }

    @SuppressWarnings("unchecked")
    private int[] _push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, ExceptionalTaintData exceptionData, boolean revisable) {
        if(isDisabled || tag == null || tag.isEmpty()) {
            return branchTags;
        }
        if(branchTags == null) {
            branchTags = new int[maxSize];
        }
        if(branchTags[branchID] == NOT_PUSHED) {
            // Adding a label for this branch for the first time
            taintHistory.push(new Taint(tag, taintHistory.peek()));
            if(exceptionData != null) {
                exceptionData.push(new Taint(tag, taintHistory.peek()));
            }
            if(!revisable) {
                revisionExcludedTaintHistory.push(new Taint(tag, revisionExcludedTaintHistory.peek()));
            }
        } else {
            taintHistory.peek().addDependency(tag);
            if(exceptionData != null) {
                exceptionData.getCurrentTaint().addDependency(tag);
            }
            if(!revisable) {
                revisionExcludedTaintHistory.peek().addDependency(tag);
            }
        }
        branchTags[branchID] = revisable ? PUSHED_REVISABLE : PUSHED;
        return branchTags;
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
    public final void pop(int[] branchTags, int branchID, ExceptionalTaintData curMethod) {
        if(branchTags != null && branchTags[branchID] != NOT_PUSHED) {
            curMethod.pop();
            taintHistory.pop();
            if(branchTags[branchID] == PUSHED) {
                revisionExcludedTaintHistory.pop();
            }
            branchTags[branchID] = NOT_PUSHED;
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP)
    public final void pop(int[] branchTags, int branchID) {
        if(branchTags != null) {
            if(branchTags[branchID] != NOT_PUSHED) {
                taintHistory.pop();
            }
            if(branchTags[branchID] == PUSHED) {
                revisionExcludedTaintHistory.pop();
            }
            branchTags[branchID] = NOT_PUSHED;
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL_EXCEPTION)
    public final void pop(int[] branchTags, ExceptionalTaintData curMethod) {
        if(branchTags != null) {
            pop(branchTags);
            if(curMethod != null) {
                curMethod.reset();
            }
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL)
    public final void pop(int[] branchTags) {
        if(branchTags != null) {
            for(int i = 0; i < branchTags.length; i++) {
                if(branchTags[i] != NOT_PUSHED) {
                    taintHistory.pop();
                    if(branchTags[i] == PUSHED) {
                        revisionExcludedTaintHistory.pop();
                    }
                    branchTags[i] = NOT_PUSHED;
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

    @SuppressWarnings("unused")
    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_REVISION_EXCLUDED_TAG)
    public Taint copyRevisionExcludedTag() {
        return isEmpty() ? null : revisionExcludedTaintHistory.peek();
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
        revisionExcludedTaintHistory.clear();
        revisionExcludedTaintHistory.push(null);
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
    @InvokedViaInstrumentation(record = CONTROL_STACK_FACTORY)
    public static ControlTaintTagStack factory() {
        return instance;
    }
}
