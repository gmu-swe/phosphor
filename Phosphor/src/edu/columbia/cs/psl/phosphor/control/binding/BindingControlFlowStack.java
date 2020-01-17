package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;

public final class BindingControlFlowStack<E> extends ControlFlowStack {

    @SuppressWarnings("rawtypes")
    private static final BindingControlFlowStack disabledInstance = new BindingControlFlowStack(true);
    private static final int NOT_PUSHED = -1;
    private ControlFrame<E> stackTop;
    private ControlFrameBuilder<E> frameBuilder;
    private Taint<E> nextBranchTag;

    public BindingControlFlowStack() {
        this(false);
    }

    public BindingControlFlowStack(boolean disabled) {
        super(disabled);
        stackTop = new ControlFrame<>(0, null, null);
        frameBuilder = new ControlFrameBuilder<>();
        nextBranchTag = Taint.emptyTaint();
    }

    private BindingControlFlowStack(BindingControlFlowStack<E> stack) {
        super(stack.isDisabled());
        stackTop = new ControlFrame<>(stack.stackTop.invocationLevel, stack.stackTop.argumentConstancyLevels, null);
        stackTop.levelStackMap.putAll(stack.stackTop.levelStackMap);
        frameBuilder = stack.frameBuilder.copy();
        nextBranchTag = Taint.emptyTaint();
    }

    @Override
    public BindingControlFlowStack<E> copyTop() {
        return new BindingControlFlowStack<>(this);
    }

    public BindingControlFlowStack<E> startFrame(int invocationLevel, int numArguments) {
        frameBuilder.start(invocationLevel, numArguments);
        return this;
    }

    public BindingControlFlowStack<E> setNextFrameArgConstant() {
        frameBuilder.setNextArgLevel(0);
        return this;
    }

    public BindingControlFlowStack<E> setNextFrameArgDependent(int[] dependencies) {
        frameBuilder.setNextArgLevel(getLevel(dependencies));
        return this;
    }

    public BindingControlFlowStack<E> setNextFrameArgVariant(int levelOffset) {
        frameBuilder.setNextArgLevel(getLevel(levelOffset));
        return this;
    }

    public void pushFrame() {
        stackTop = frameBuilder.build(stackTop);
    }

    public int getLevel(int levelOffset) {
        return stackTop.getLevel(levelOffset);
    }

    public int getLevel(int[] dependencies) {
        return stackTop.getLevel(dependencies);
    }

    public void popFrame() {
        stackTop = stackTop.next;
    }

    public Taint<E> copyTagConstant() {
        return isDisabled() ? Taint.emptyTaint() : stackTop.copyTag(0);
    }

    public Taint<E> copyTagDependent(int[] dependencies) {
        return isDisabled() ? Taint.emptyTaint() : stackTop.copyTag(getLevel(dependencies));
    }

    public Taint<E> copyTagVariant(int levelOffset) {
        return isDisabled() ? Taint.emptyTaint() : stackTop.copyTag(getLevel(levelOffset));
    }

    public void pushConstant(int branchID, int branchesSize) {
        if(!isDisabled()) {
            stackTop.push(nextBranchTag, branchID, branchesSize, 0);
        }
    }

    public void pushDependent(int branchID, int branchesSize, int[] dependencies) {
        if(!isDisabled()) {
            stackTop.push(nextBranchTag, branchID, branchesSize, getLevel(dependencies));
        }
    }

    public void pushVariant(int branchID, int branchesSize, int levelOffset) {
        if(!isDisabled()) {
            stackTop.push(nextBranchTag, branchID, branchesSize, getLevel(levelOffset));
        }
    }

    public void pop(int branchID) {
        stackTop.pop(branchID);
    }

    public void reset() {
        stackTop.reset();
        nextBranchTag = Taint.emptyTaint();
    }

    public void exitLoopLevel(int levelOffset) {
        stackTop.exitLoopLevel(levelOffset);
    }

    public void setNextBranchTag(Taint<E> nextBranchTag) {
        this.nextBranchTag = nextBranchTag;
    }

    @Override
    public Taint<E> copyTag() {
        return isDisabled() ? Taint.emptyTaint() : stackTop.copyTag(getLevel(Integer.MAX_VALUE));
    }

    private static final class ControlFrame<E> {

        private final int invocationLevel;
        private final int[] argumentConstancyLevels;
        private final Map<Integer, Node<E>> levelStackMap;
        private int[] branchLevels;
        private ControlFrame<E> next;

        private ControlFrame(int invocationLevel, int[] argumentConstancyLevels, ControlFrame<E> next) {
            this.next = next;
            this.invocationLevel = invocationLevel;
            this.argumentConstancyLevels = argumentConstancyLevels;
            if(next == null) {
                levelStackMap = new HashMap<>();
            } else {
                levelStackMap = new HashMap<>(next.levelStackMap);
            }
        }

        int getLevel(int levelOffset) {
            return invocationLevel + levelOffset;
        }

        int getLevel(int[] dependencies) {
            if(argumentConstancyLevels == null) {
                return 0;
            } else {
                int max = 0;
                for(int dependency : dependencies) {
                    int value = argumentConstancyLevels[dependency];
                    if(value > max) {
                        max = value;
                    }
                }
                return max;
            }
        }

        Taint<E> copyTag(int level) {
            Taint<E> tag = null;
            for(Integer key : levelStackMap.keySet()) {
                if(key <= level) {
                    tag = Taint.combineTags(tag, levelStackMap.get(key).tag);
                }
            }
            return tag;
        }

        void push(Taint<E> tag, int branchID, int branchesSize, int level) {
            if(tag != null && !tag.isEmpty()) {
                if(branchLevels == null) {
                    branchLevels = new int[branchesSize];
                    Arrays.fill(branchLevels, NOT_PUSHED);
                }
                if(!levelStackMap.containsKey(level)) {
                    levelStackMap.put(level, Node.emptyNode());
                }
                if(branchLevels[branchID] == NOT_PUSHED) {
                    branchLevels[branchID] = level;
                    Taint<E> combined = Taint.combineTags(tag, levelStackMap.get(level).tag);
                    levelStackMap.put(level, new Node<>(combined, levelStackMap.get(level)));
                } else {
                    Node<E> r = levelStackMap.get(level);
                    r.tag = r.tag.union(tag);
                }
            }
        }

        void pop(int branchID) {
            if(branchLevels != null && branchLevels[branchID] != NOT_PUSHED) {
                levelStackMap.put(branchLevels[branchID], levelStackMap.get(branchLevels[branchID]).next);
                branchLevels[branchID] = NOT_PUSHED;
            }
        }

        void reset() {
            if(next != null) {
                next.reset();
            }
            branchLevels = null;
            levelStackMap.clear();
        }

        void exitLoopLevel(int levelOffset) {
            if(branchLevels != null) {
                int level = getLevel(levelOffset);
                levelStackMap.put(level, Node.emptyNode());
                for(int i = 0; i < branchLevels.length; i++) {
                    if(branchLevels[i] == level) {
                        branchLevels[i] = NOT_PUSHED;
                    }
                }
            }
        }
    }

    private static final class ControlFrameBuilder<E> {

        private int invocationLevel = 0;
        private int[] argumentConstancyLevels = null;
        private int currentArg = 0;

        ControlFrameBuilder<E> copy() {
            ControlFrameBuilder<E> copy = new ControlFrameBuilder<>();
            copy.invocationLevel = invocationLevel;
            copy.argumentConstancyLevels = argumentConstancyLevels == null ? null : argumentConstancyLevels.clone();
            copy.currentArg = currentArg;
            return copy;
        }

        void start(int invocationLevel, int numArguments) {
            this.invocationLevel = invocationLevel;
            argumentConstancyLevels = new int[numArguments];
            currentArg = 0;
        }

        void setNextArgLevel(int level) {
            argumentConstancyLevels[currentArg++] = level;
        }

        ControlFrame<E> build(ControlFrame<E> next) {
            ControlFrame<E> frame = new ControlFrame<>(invocationLevel, argumentConstancyLevels, next);
            invocationLevel = 0;
            argumentConstancyLevels = null;
            currentArg = 0;
            return frame;
        }
    }

    private static final class Node<E> {
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

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[");
            for(Node<E> cur = this; cur != null; cur = cur.next) {
                builder.append(cur.tag);
                if(cur.next != null) {
                    builder.append(", ");
                }
            }
            return builder.append("]").toString();
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> BindingControlFlowStack<E> factory(boolean disabled) {
        if(disabled) {
            return disabledInstance;
        } else {
            return new BindingControlFlowStack<>(false);
        }
    }
}
