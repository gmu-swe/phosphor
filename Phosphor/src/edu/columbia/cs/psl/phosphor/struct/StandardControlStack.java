package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;


public final class StandardControlStack<E> extends ControlStack<E> {

    StandardControlFrame<E> stackTop = new StandardControlFrame<>(null);

    @Override
    public StandardControlStack<E> pushFrame() {
        stackTop = new StandardControlFrame<>(stackTop);
        return this;
    }

    @Override
    public StandardControlFrame<E> getRestorePoint() {
        return stackTop;
    }

    @Override
    public void popFrame() {
        stackTop = (StandardControlFrame<E>) stackTop.getNext();
    }

    @Override
    public void popToFrame(ControlFrame<E> restorePoint) {
        stackTop = (StandardControlFrame<E>) restorePoint;
    }

    @Override
    public Taint<E> copyTag() {
        return stackTop.copyTag();
    }

    @Override
    public boolean push(Taint<E> tag, int branchID, int branchesSize) {
        return stackTop.push(tag, branchID, branchesSize);
    }

    @Override
    public boolean pop(int branchID) {
        return stackTop.pop(branchID);
    }

    @Override
    public void pop() {
        stackTop.pop();
    }

    @Override
    public void push(Taint<E> tag) {
        stackTop.push(tag);
    }

    @Override
    public void reset() {
        stackTop.reset();
    }

    private static final class StandardControlFrame<E> extends ControlStack.ControlFrame<E> {

        private boolean[] branchLevels;
        private ControlStack.Node<E> top;

        private StandardControlFrame(StandardControlFrame<E> next) {
            super(next);
            this.top = next == null ? ControlStack.Node.emptyNode() : next.top;
        }

        Taint<E> copyTag() {
            return top.tag;
        }

        boolean push(Taint<E> tag, int branchID, int branchesSize) {
            if(branchLevels == null) {
                branchLevels = new boolean[branchesSize];
            }
            Taint<E> t = Taint.combineTags(this.top.tag, tag);
            if(branchLevels[branchID]) {
                top.tag = t;
                return false;
            } else {
                branchLevels[branchID] = true;
                top = new ControlStack.Node<>(t, top);
                return true;
            }
        }

        boolean pop(int branchID) {
            if(branchLevels != null && branchLevels[branchID]) {
                branchLevels[branchID] = false;
                top = top.next;
                return true;
            } else {
                return false;
            }
        }

        void reset() {
            branchLevels = null;
            if(getNext() instanceof StandardControlFrame) {
                StandardControlFrame<E> n = (StandardControlFrame<E>) getNext();
                n.reset();
                top = n.top;
            } else {
                top = ControlStack.Node.emptyNode();
            }
        }

        void push(Taint<E> tag) {
            Taint<E> t = Taint.combineTags(this.top.tag, tag);
            top = new ControlStack.Node<>(t, top);
        }

        void pop() {
            top = top.next;
        }
    }
}


