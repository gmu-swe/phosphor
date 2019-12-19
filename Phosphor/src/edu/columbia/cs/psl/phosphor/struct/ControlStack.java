package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public abstract class ControlStack<E> {

    public abstract ControlStack<E> pushFrame();

    public abstract ControlFrame<E> getRestorePoint();

    public abstract void popFrame();

    public abstract void popToFrame(ControlFrame<E> restorePoint);

    public abstract Taint<E> copyTag();

    public abstract boolean pop(int branchID);

    public abstract void reset();

    public ControlStack<E> setNextFrameArgConstant() {
        throw new UnsupportedOperationException();
    }

    public ControlStack<E> setNextFrameArgDependent(int[] dependencies) {
        throw new UnsupportedOperationException();
    }

    public ControlStack<E> setNextFrameArgVariant(int levelOffset) {
        throw new UnsupportedOperationException();
    }

    public ControlStack<E> startFrame(int invocationLevel, int numArguments) {
        throw new UnsupportedOperationException();
    }

    public int getLevel(int levelOffset) {
        throw new UnsupportedOperationException();
    }

    public int getLevel(int[] dependencies) {
        throw new UnsupportedOperationException();
    }

    public Taint<E> copyTagConstant() {
        throw new UnsupportedOperationException();
    }

    public Taint<E> copyTagArgDependent(int[] dependencies) {
        throw new UnsupportedOperationException();
    }

    public Taint<E> copyTagVariant(int levelOffset) {
        throw new UnsupportedOperationException();
    }

    public boolean push(Taint<E> tag, int branchID, int branchesSize) {
        throw new UnsupportedOperationException();
    }

    public void pushConstant(Taint<E> tag, int branchID, int branchesSize) {
        throw new UnsupportedOperationException();
    }

    public void pushDependent(Taint<E> tag, int branchID, int branchesSize, int[] dependencies) {
        throw new UnsupportedOperationException();
    }

    public void pushVariant(Taint<E> tag, int branchID, int branchesSize, int levelOffset) {
        throw new UnsupportedOperationException();
    }

    public void pop() {
        throw new UnsupportedOperationException();
    }

    public void push(Taint<E> tag) {
        throw new UnsupportedOperationException();
    }

    public void exitLoopLevel(int levelOffset) {
        throw new UnsupportedOperationException();
    }

    static final class Node<E> {
        @SuppressWarnings("rawtypes")
        private static final Node EMPTY_NODE = new Node<>(Taint.emptyTaint(), null);
        Taint<E> tag;
        Node<E> next;

        Node(Taint<E> tag, Node<E> next) {
            this.tag = tag;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        static <E> Node<E> emptyNode() {
            return (Node<E>) EMPTY_NODE;
        }
    }

    public abstract static class ControlFrame<E> {

        private ControlFrame<E> next;

        ControlFrame(ControlFrame<E> next) {
            this.next = next;
        }

        ControlFrame<E> getNext() {
            return next;
        }
    }
}
