package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public final class ControlTaintTagStack {

    public static final int PUSHED = 1;
    public static final int PUSHED_REVISABLE = -1;
    public static final int NOT_PUSHED = 0;
    private static ControlTaintTagStack instance = new ControlTaintTagStack(true);

    private final SinglyLinkedList<Taint> taintHistory = new SinglyLinkedList<>();
    private final SinglyLinkedList<Taint> revisionExcludedTaintHistory = new SinglyLinkedList<>();
    private final LoopAwareControlStack loopAwareControlStack;
    private int disabled;
    private SinglyLinkedList<MaybeThrownException> unthrownExceptions;
    private SinglyLinkedList<MaybeThrownException> influenceExceptions;

    public ControlTaintTagStack() {
        disabled = 0;
        unthrownExceptions = null;
        influenceExceptions = null;
        taintHistory.push(Taint.emptyTaint()); // starting taint is null/empty
        revisionExcludedTaintHistory.push(Taint.emptyTaint()); // starting taint is null/empty
        loopAwareControlStack = new LoopAwareControlStack();
    }

    private ControlTaintTagStack(boolean disabled) {
        this();
        if(disabled) {
            this.disabled = 1;
        }
    }

    private ControlTaintTagStack(ControlTaintTagStack other) {
        disabled = other.disabled;
        unthrownExceptions = other.unthrownExceptions == null ? null : other.unthrownExceptions.copy();
        influenceExceptions = other.influenceExceptions == null ? null : other.influenceExceptions.copy();
        taintHistory.push(other.taintHistory.peek());
        revisionExcludedTaintHistory.push(other.revisionExcludedTaintHistory.peek());
        loopAwareControlStack = new LoopAwareControlStack(other.loopAwareControlStack);
    }

    /**
     * Called ONCE at the start of each exception handler. Should inspect the taint tag on the
     * exception, and if there is one, we'll need to add it to the current ControlTaintTagStack and
     * return a pointer so it can later be removed
     */
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START)
    public final EnqueuedTaint exceptionHandlerStart(Throwable exceptionCaught, Taint exceptionTaint, EnqueuedTaint eq) {
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
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START_VOID)
    public final void exceptionHandlerStart(Class<? extends Throwable> exTypeHandled) {
        tryBlockEnd(exTypeHandled);
    }

    /**
     * Called ONCE at the end of each handler to remove an exception from influencing the control state
     * Passed the same MaybeThrownException from the start method
     */
    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_END)
    public final void exceptionHandlerEnd(EnqueuedTaint ex) {
        if(ex != null) {
            pop(ex);
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TOP)
    public ControlTaintTagStack copyTop() {
        return new ControlTaintTagStack(this);
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
            if(exTypeHandled.isAssignableFrom(mte.getClazz())) {
                itr.remove();
            }
        }
    }

    /**
     * If there is some maybeThrownException (e.g. from something we are returning to), and we are now in code that
     * follows that code in a "try" block, then that unthrown exception
     * is currently affecting the current flow (at least until the end of the catch block)
     */
    @InvokedViaInstrumentation(record = CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION)
    public final void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> t) {
        if(unthrownExceptions == null) {
            return;
        }
        Iterator<MaybeThrownException> itr = unthrownExceptions.iterator();
        while(itr.hasNext()) {
            MaybeThrownException mte = itr.next();
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
    @InvokedViaInstrumentation(record = CONTROL_STACK_ADD_UNTHROWN_EXCEPTION)
    public final void addUnthrownException(ExceptionalTaintData taints, Class<? extends Throwable> t) {
        if(taints != null && taints.getCurrentTaint() != null) {
            if(unthrownExceptions == null) {
                unthrownExceptions = new SinglyLinkedList<>();
            }
            boolean found = false;
            for(MaybeThrownException mte : unthrownExceptions) {
                if(mte != null && mte.getClazz() == t) {
                    found = true;
                    mte.unionTag(taints.getCurrentTaint());
                    break;
                }
            }
            if(!found) {
                MaybeThrownException ex = new MaybeThrownException(t, taints.getCurrentTaint());
                unthrownExceptions.push(ex);
            }
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG)
    public final int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, boolean revisable) {
        return push(tag, branchTags, branchID, maxSize, null, revisable);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG_EXCEPTION)
    public final int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, ExceptionalTaintData curMethod, boolean revisable) {
        if(disabled != 0 || tag == null || tag.isEmpty()) {
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
            if(!revisable) {
                revisionExcludedTaintHistory.push(tag.union(revisionExcludedTaintHistory.peek()));
            }
        } else {
            Taint r = taintHistory.peek();
            if(r != tag && !r.isSuperset(tag)) {
                taintHistory.push(taintHistory.pop().union(tag));
            }
            if(curMethod != null) {
                r = curMethod.getCurrentTaint();
                if(r != tag && !r.isSuperset(tag)) {
                    curMethod.push(curMethod.pop().union(tag));
                }
            }
            if(!revisable) {
                r = revisionExcludedTaintHistory.peek();
                if(r != tag && !r.isSuperset(tag)) {
                    revisionExcludedTaintHistory.push(revisionExcludedTaintHistory.pop().union(tag));
                }
            }
        }
        branchTags[branchID] = revisable ? PUSHED_REVISABLE : PUSHED;
        return branchTags;
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

    public final EnqueuedTaint push(Taint tag, EnqueuedTaint prev) {
        if(tag == null || tag.isEmpty() || tag == taintHistory.peek() || disabled != 0) {
            return null;
        }
        EnqueuedTaint ret = prev == null ? new EnqueuedTaint() : prev;
        ret.activeCount++;
        taintHistory.push(tag.union(taintHistory.peek()));
        return ret;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_EXCEPTIONS)
    public Taint copyTagExceptions() {
        if(isEmpty() && lacksInfluenceExceptions()) {
            return Taint.emptyTaint();
        }
        Taint ret = taintHistory.peek();
        if(lacksInfluenceExceptions()) {
            return ret;
        }
        for(MaybeThrownException mte : influenceExceptions) {
            if(mte != null && mte.getTag() != null) {
                ret = ret.union(mte.getTag());
            }
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG)
    public Taint copyTag() {
        return disabled != 0 ? Taint.emptyTaint() : taintHistory.peek();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_REVISION_EXCLUDED_TAG)
    public Taint copyRevisionExcludedTag() {
        return disabled != 0 ? Taint.emptyTaint() : revisionExcludedTaintHistory.peek();
    }

    public final boolean isEmpty() {
        return disabled != 0 || taintHistory.peek().isEmpty();
    }

    public boolean lacksInfluenceExceptions() {
        return influenceExceptions == null || influenceExceptions.isEmpty();
    }

    public void reset() {
        int size = taintHistory.size();
        taintHistory.clear();
        for(int i = 0; i < size; i++) {
            taintHistory.push(Taint.emptyTaint());
        }
        size = revisionExcludedTaintHistory.size();
        revisionExcludedTaintHistory.clear();
        for(int i = 0; i < size; i++) {
            revisionExcludedTaintHistory.push(Taint.emptyTaint());
        }
        loopAwareControlStack.reset();
        if(influenceExceptions != null) {
            influenceExceptions.clear();
        }
        if(unthrownExceptions != null) {
            unthrownExceptions.clear();
        }
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_ENABLE)
    public void enable() {
        disabled--;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_DISABLE)
    public void disable() {
        disabled++;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_FRAME)
    public void popFrame(int[] branchTags) {
        if(branchTags != null) {
            pop(branchTags);
        }
        loopAwareControlStack.popFrame();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_FRAME_EXCEPTION)
    public void popFrame(int[] branchTags, ExceptionalTaintData curMethod) {
        if(branchTags != null) {
            pop(branchTags, curMethod);
        }
        loopAwareControlStack.popFrame();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_FRAME)
    public void pushFrame() {
        loopAwareControlStack.pushFrame();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_START_FRAME)
    public ControlTaintTagStack startFrame(int invocationLevel, int numArguments) {
        loopAwareControlStack.startFrame(invocationLevel, numArguments);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_CONSTANT)
    public ControlTaintTagStack setNextFrameArgConstant() {
        loopAwareControlStack.setNextFrameArgConstant();
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_DEPENDENT)
    public ControlTaintTagStack setNextFrameArgDependent(int[] dependencies) {
        loopAwareControlStack.setNextFrameArgDependent(dependencies);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_VARIANT)
    public ControlTaintTagStack setNextFrameArgVariant(int levelOffset) {
        loopAwareControlStack.setNextFrameArgVariant(levelOffset);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_CONSTANT)
    public Taint copyTagConstant() {
        return loopAwareControlStack.copyTagConstant();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_DEPENDENT)
    public Taint copyTagDependent(int[] dependencies) {
        return loopAwareControlStack.copyTagDependent(dependencies);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_VARIANT)
    public Taint copyTagVariant(int levelOffset) {
        return loopAwareControlStack.copyTagVariant(levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_CONSTANT)
    public void pushConstant(Taint tag, int branchID, int branchesSize) {
        loopAwareControlStack.pushConstant(tag, branchID, branchesSize);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_DEPENDENT)
    public void pushDependent(Taint tag, int branchID, int branchesSize, int[] dependencies) {
        loopAwareControlStack.pushDependent(tag, branchID, branchesSize, dependencies);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_VARIANT)
    public void pushVariant(Taint tag, int branchID, int branchesSize, int levelOffset) {
        loopAwareControlStack.pushVariant(tag, branchID, branchesSize, levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_LOOP_AWARE_POP)
    public void loopAwarePop(int branchID) {
        loopAwareControlStack.pop(branchID);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_EXIT_LOOP_LEVEL)
    public void exitLoopLevel(int levelOffset) {
        loopAwareControlStack.exitLoopLevel(levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_FACTORY)
    public static ControlTaintTagStack factory() {
        return instance;
    }
}
