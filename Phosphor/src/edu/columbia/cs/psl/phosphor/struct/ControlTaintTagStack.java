package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.control.binding.BindingControlFlowStack;
import edu.columbia.cs.psl.phosphor.control.standard.StandardControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class ControlTaintTagStack {

    private static ControlTaintTagStack disabledInstance = new ControlTaintTagStack(true);

    private final StandardControlFlowStack standardControlFlowStack;
    private final BindingControlFlowStack bindingControlFlowStack;

    private ControlTaintTagStack(boolean disabled) {
        standardControlFlowStack = new StandardControlFlowStack<>();
        bindingControlFlowStack = new BindingControlFlowStack<>();
        if(disabled) {
            disable();
        }
    }

    private ControlTaintTagStack(ControlTaintTagStack other) {
        standardControlFlowStack = other.standardControlFlowStack.copyTop();
        bindingControlFlowStack = other.bindingControlFlowStack.copyTop();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START)
    public EnqueuedTaint exceptionHandlerStart(Throwable exceptionCaught, Taint exceptionTaint, EnqueuedTaint enqueuedTaint) {
        return standardControlFlowStack.exceptionHandlerStart(exceptionCaught, exceptionTaint, enqueuedTaint);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_START_TYPES)
    public void exceptionHandlerStart(Class<? extends Throwable> handledExceptionType) {
        standardControlFlowStack.exceptionHandlerStart(handledExceptionType);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_EXCEPTION_HANDLER_END)
    public void exceptionHandlerEnd(EnqueuedTaint enqueuedTaint) {
        standardControlFlowStack.exceptionHandlerEnd(enqueuedTaint);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TOP)
    public ControlTaintTagStack copyTop() {
        return new ControlTaintTagStack(this);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_TRY_BLOCK_END)
    public void tryBlockEnd(Class<? extends Throwable> handledExceptionType) {
        standardControlFlowStack.tryBlockEnd(handledExceptionType);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION)
    public void applyPossiblyUnthrownExceptionToTaint(Class<? extends Throwable> type) {
        standardControlFlowStack.applyPossiblyUnthrownExceptionToTaint(type);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_ADD_UNTHROWN_EXCEPTION)
    public void addUnthrownException(ExceptionalTaintData taints, Class<? extends Throwable> type) {
        standardControlFlowStack.addUnthrownException(taints, type);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG)
    public int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize) {
        return standardControlFlowStack.push(tag, branchTags, branchID, maxSize, null);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_TAG_EXCEPTION)
    public int[] push(Taint<?> tag, int[] branchTags, int branchID, int maxSize, ExceptionalTaintData curMethod) {
        return standardControlFlowStack.push(tag, branchTags, branchID, maxSize, curMethod);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_EXCEPTION)
    public void pop(int[] branchTags, int branchID, ExceptionalTaintData curMethod) {
        standardControlFlowStack.pop(branchTags, branchID, curMethod);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP)
    public void pop(int[] branchTags, int branchID) {
        standardControlFlowStack.pop(branchTags, branchID);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL_EXCEPTION)
    public void pop(int[] branchTags, ExceptionalTaintData curMethod) {
        standardControlFlowStack.pop(branchTags, curMethod);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_ALL)
    public void pop(int[] branchTags) {
        standardControlFlowStack.pop(branchTags);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG)
    public Taint<?> copyTag() {
        return standardControlFlowStack.copyTag();
    }

    public void reset() {
        bindingControlFlowStack.reset();
        standardControlFlowStack.reset();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_ENABLE)
    public void enable() {
        standardControlFlowStack.enable();
        bindingControlFlowStack.enable();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_DISABLE)
    public void disable() {
        standardControlFlowStack.disable();
        bindingControlFlowStack.disable();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_FRAME)
    public void popFrame() {
        standardControlFlowStack.popFrame();
        bindingControlFlowStack.popFrame();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_FRAME)
    public void pushFrame() {
        standardControlFlowStack.pushFrame();
        bindingControlFlowStack.pushFrame();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_START_FRAME)
    public ControlTaintTagStack startFrame(int invocationLevel, int numArguments) {
        bindingControlFlowStack.startFrame(invocationLevel, numArguments);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_CONSTANT)
    public ControlTaintTagStack setNextFrameArgConstant() {
        bindingControlFlowStack.setNextFrameArgConstant();
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_DEPENDENT)
    public ControlTaintTagStack setNextFrameArgDependent(int[] dependencies) {
        bindingControlFlowStack.setNextFrameArgDependent(dependencies);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_ARG_VARIANT)
    public ControlTaintTagStack setNextFrameArgVariant(int levelOffset) {
        bindingControlFlowStack.setNextFrameArgVariant(levelOffset);
        return this;
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_CONSTANT)
    public Taint copyTagConstant() {
        return bindingControlFlowStack.copyTagConstant();
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_DEPENDENT)
    public Taint copyTagDependent(int[] dependencies) {
        return bindingControlFlowStack.copyTagDependent(dependencies);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_COPY_TAG_VARIANT)
    public Taint copyTagVariant(int levelOffset) {
        return bindingControlFlowStack.copyTagVariant(levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_SET_NEXT_BRANCH_TAG)
    public void setNextBranchTag(Taint tag) {
        bindingControlFlowStack.setNextBranchTag(tag);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_CONSTANT)
    public void pushConstant(int branchID, int branchesSize) {
        bindingControlFlowStack.pushConstant(branchID, branchesSize);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_DEPENDENT)
    public void pushDependent(int branchID, int branchesSize, int[] dependencies) {
        bindingControlFlowStack.pushDependent(branchID, branchesSize, dependencies);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_PUSH_VARIANT)
    public void pushVariant(int branchID, int branchesSize, int levelOffset) {
        bindingControlFlowStack.pushVariant(branchID, branchesSize, levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_EXIT_LOOP_LEVEL)
    public void exitLoopLevel(int levelOffset) {
        bindingControlFlowStack.exitLoopLevel(levelOffset);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_POP_BINDING)
    public void pop(int branchID) {
        bindingControlFlowStack.pop(branchID);
    }

    @InvokedViaInstrumentation(record = CONTROL_STACK_FACTORY)
    public static ControlTaintTagStack factory(boolean disabled) {
        if(disabled) {
            return disabledInstance;
        } else {
            return new ControlTaintTagStack(false);
        }
    }
}
