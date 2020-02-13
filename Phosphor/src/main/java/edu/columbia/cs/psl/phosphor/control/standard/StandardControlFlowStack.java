package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;
import edu.columbia.cs.psl.phosphor.struct.ExceptionalTaintData;
import edu.columbia.cs.psl.phosphor.struct.MaybeThrownException;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;

import java.util.Iterator;

public class StandardControlFlowStack<E> extends ControlFlowStack {

    @SuppressWarnings("rawtypes")
    private static final StandardControlFlowStack disabledInstance = new StandardControlFlowStack(true);
    public static final int PUSHED = 1;
    public static final int NOT_PUSHED = 0;

    private final SinglyLinkedList<Taint<E>> taintHistory = new SinglyLinkedList<>();
    private SinglyLinkedList<MaybeThrownException<E>> unthrownExceptions = null;
    private SinglyLinkedList<MaybeThrownException<E>> influenceExceptions = null;

    public StandardControlFlowStack(boolean disabled) {
        super(disabled);
        taintHistory.push(Taint.emptyTaint()); // Starting taint is null/empty
    }

    private StandardControlFlowStack(StandardControlFlowStack<E> other) {
        super(other.isDisabled());
        unthrownExceptions = other.unthrownExceptions == null ? null : other.unthrownExceptions.copy();
        influenceExceptions = other.influenceExceptions == null ? null : other.influenceExceptions.copy();
        taintHistory.push(other.taintHistory.peek());
    }

    @Override
    public StandardControlFlowStack<E> copyTop() {
        return new StandardControlFlowStack<>(this);
    }

    @Override
    public void reset() {
        int size = taintHistory.size();
        taintHistory.clear();
        for(int i = 0; i < size; i++) {
            taintHistory.push(Taint.emptyTaint());
        }
        if(influenceExceptions != null) {
            influenceExceptions.clear();
        }
        if(unthrownExceptions != null) {
            unthrownExceptions.clear();
        }
    }

    @Override
    public void pushFrame() {

    }

    @Override
    public void popFrame() {

    }

    /**
     * Called once at the start of each exception handler. Should inspect the taint tag on the
     * exception, and if there is one, we'll need to add it to the current stack and
     * return a pointer so it can later be removed
     */
    public final EnqueuedTaint exceptionHandlerStart(Throwable exceptionCaught, Taint<E> exceptionTaint, EnqueuedTaint enqueuedTaint) {
        if(exceptionTaint == null || exceptionTaint.isEmpty() || exceptionTaint == taintHistory.peek() || isDisabled()) {
            return null;
        }
        if(enqueuedTaint == null) {
            enqueuedTaint = new EnqueuedTaint();
        }
        enqueuedTaint.activeCount++;
        taintHistory.push(exceptionTaint.union(taintHistory.peek()));
        return enqueuedTaint;
    }

    /**
     * At the start of an exception handler, this method is called once for each exception type handled by the handler.
     * Removes elements from the unthrown exception list whose exception type either is or is a subtype of the
     * specified type.
     */
    public final void exceptionHandlerStart(Class<? extends Throwable> handledExceptionType) {
        tryBlockEnd(handledExceptionType);
    }

    /**
     * Called once at the end of each handler to remove an exception from influencing the control state.
     * Passed the same MaybeThrownException from the start method.
     */
    public void exceptionHandlerEnd(EnqueuedTaint enqueuedTaint) {
        if(enqueuedTaint != null) {
            pop(enqueuedTaint);
        }
    }

    /**
     * Called once per handled exception type at the end of each try block to clear unthrown exceptions.
     */
    public void tryBlockEnd(Class<? extends Throwable> handledExceptionType) {
        if(influenceExceptions == null) {
            return;
        }
        Iterator<MaybeThrownException<E>> itr = influenceExceptions.iterator();
        while(itr.hasNext()) {
            MaybeThrownException<E> mte = itr.next();
            if(handledExceptionType.isAssignableFrom(mte.getClazz())) {
                itr.remove();
            }
        }
    }

    /**
     * If there is some maybeThrownException (e.g., from something we are returning to), and we are now in code that
     * follows that code in a "try" block, then that unthrown exception
     * is currently affecting the current flow (at least until the end of the catch block)
     */
    public final void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> t) {
        if(unthrownExceptions == null) {
            return;
        }
        Iterator<MaybeThrownException<E>> itr = unthrownExceptions.iterator();
        while(itr.hasNext()) {
            MaybeThrownException<E> mte = itr.next();
            if(t.isAssignableFrom(mte.getClazz())) {
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
    public final void addUnthrownException(ExceptionalTaintData<E> taints, Class<? extends Throwable> t) {
        if(taints != null && taints.getCurrentTaint() != null) {
            if(unthrownExceptions == null) {
                unthrownExceptions = new SinglyLinkedList<>();
            }
            boolean found = false;
            for(MaybeThrownException<E> mte : unthrownExceptions) {
                if(mte != null && mte.getClazz() == t) {
                    found = true;
                    mte.unionTag(taints.getCurrentTaint());
                    break;
                }
            }
            if(!found) {
                MaybeThrownException<E> ex = new MaybeThrownException<>(t, taints.getCurrentTaint());
                unthrownExceptions.push(ex);
            }
        }
    }

    public final int[] push(Taint<E> tag, int[] branchTags, int branchID, int maxSize) {
        return push(tag, branchTags, branchID, maxSize, null);
    }

    public final int[] push(Taint<E> tag, int[] branchTags, int branchID, int maxSize, ExceptionalTaintData<E> curMethod) {
        if(isDisabled() || tag == null || tag.isEmpty()) {
            return branchTags;
        }
        if(branchTags == null) {
            branchTags = new int[maxSize];
        }
        if(branchTags[branchID] == NOT_PUSHED) {
            // Adding a label for this branch for the first time
            taintHistory.push(tag.union(taintHistory.peek()));
            if(curMethod != null) {
                curMethod.push(tag.union(taintHistory.peek()));
            }
        } else {
            Taint<E> r = taintHistory.peek();
            if(r != tag && !r.isSuperset(tag)) {
                taintHistory.push(taintHistory.pop().union(tag));
            }
            if(curMethod != null) {
                r = curMethod.getCurrentTaint();
                if(r != tag && !r.isSuperset(tag)) {
                    curMethod.push(curMethod.pop().union(tag));
                }
            }
        }
        branchTags[branchID] = PUSHED;
        return branchTags;
    }

    public final void pop(int[] branchTags, int branchID, ExceptionalTaintData<E> exceptionalTaintData) {
        if(branchTags != null && branchTags[branchID] == PUSHED) {
            exceptionalTaintData.pop();
            taintHistory.pop();
            branchTags[branchID] = NOT_PUSHED;
        }
    }

    public final void pop(int[] branchTags, int branchID) {
        if(branchTags != null) {
            if(branchTags[branchID] == PUSHED) {
                taintHistory.pop();
            }
            branchTags[branchID] = NOT_PUSHED;
        }
    }

    public final void pop(int[] branchTags, ExceptionalTaintData<E> exceptionalTaintData) {
        if(branchTags != null) {
            pop(branchTags);
            if(exceptionalTaintData != null) {
                exceptionalTaintData.reset();
            }
        }
    }

    public final void pop(int[] branchTags) {
        if(branchTags != null) {
            for(int i = 0; i < branchTags.length; i++) {
                if(branchTags[i] == PUSHED) {
                    taintHistory.pop();
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

    private Taint<E> _copyTagExceptions(Taint<E> ret){
        for(MaybeThrownException<E> mte : influenceExceptions) {
            if(mte != null && mte.getTag() != null) {
                ret = ret.union(mte.getTag());
            }
        }
        return ret;
    }
    @Override
    public Taint<E> copyTag() {
        if(isDisabled()) {
            return Taint.emptyTaint();
        }
        Taint<E> ret = taintHistory.peek();
        if (influenceExceptions != null) {
            ret = _copyTagExceptions(ret);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <E> StandardControlFlowStack<E> factory(boolean disabled) {
        if(disabled) {
            return disabledInstance;
        } else {
            return new StandardControlFlowStack<>(false);
        }
    }
}
