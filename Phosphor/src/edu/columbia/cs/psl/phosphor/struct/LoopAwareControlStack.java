package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;

@SuppressWarnings("unused")
public final class LoopAwareControlStack<E> {

    private static final int NOT_PUSHED = -1;
    private LoopAwareControlFrame<E> stackTop = new LoopAwareControlFrame<>();
    private LoopAwareControlFrameBuilder<E> frameBuilder = new LoopAwareControlFrameBuilder<>();
    private boolean disabled;

    public LoopAwareControlStack() {
        disabled = false;
    }

    public LoopAwareControlStack(boolean disabled) {
        this.disabled = disabled;
    }

    public LoopAwareControlStack<E> startFrame(int levelOffset, int numArguments) {
        frameBuilder.start(getLevel(levelOffset), numArguments);
        return this;
    }

    public LoopAwareControlStack<E> setNextFrameArgConstant() {
        frameBuilder.setNextArgLevel(0);
        return this;
    }

    public LoopAwareControlStack<E> setNextFrameArgDependent(int[] dependencies) {
        frameBuilder.setNextArgLevel(getLevel(dependencies));
        return this;
    }

    public LoopAwareControlStack<E> setNextFrameArgVariant(int levelOffset) {
        frameBuilder.setNextArgLevel(getLevel(levelOffset));
        return this;
    }

    public LoopAwareControlStack<E> pushFrame() {
        stackTop = frameBuilder.build(stackTop);
        return this;
    }

    public LoopAwareControlFrame<E> pushFrameAndGetRestorePoint() {
        LoopAwareControlFrame<E> ret = stackTop;
        stackTop = frameBuilder.build(stackTop);
        return ret;
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

    public void popToFrame(LoopAwareControlFrame<E> restorePoint) {
        stackTop = restorePoint;
    }

    public Taint<E> copyTagConstant() {
        return disabled ? null : stackTop.copyTagConstant();
    }

    public Taint<E> copyTagArgDependent(int[] dependencies) {
        return disabled ? null : stackTop.copyTagDependent(dependencies);
    }

    public Taint<E> copyTagVariant(int levelOffset) {
        return disabled ? null : stackTop.copyTagVariant(levelOffset);
    }

    public void pushConstant(int branchID, Taint<E> tag, int branchesSize) {
        if(!disabled && tag != null && !tag.isEmpty()) {
            stackTop.pushConstant(branchID, tag, branchesSize);
        }
    }

    public void pushDependent(int branchID, Taint<E> tag, int branchesSize, int[] dependencies) {
        if(!disabled && tag != null && !tag.isEmpty()) {
            stackTop.pushArgDependent(branchID, tag, branchesSize, dependencies);
        }
    }

    public void pushVariant(int branchID, Taint<E> tag, int branchesSize, int levelOffset) {
        if(!disabled && tag != null && !tag.isEmpty()) {
            stackTop.pushVariant(branchID, tag, branchesSize, levelOffset);
        }
    }

    public void pop(int branchID) {
        stackTop.pop(branchID);
    }

    public void disable() {
        disabled = true;
    }

    public void enable() {
        disabled = false;
    }

    public void reset() {
        for(LoopAwareControlFrame<E> frame = stackTop; frame != null; frame = frame.next) {
            frame.reset();
        }
    }

    public void exitLoopLevel(int levelOffset) {
        stackTop.exitLoopLevel(levelOffset);
    }

    public static final class LoopAwareControlFrame<E> {

        private final int invocationLevel;
        private final int[] argumentConstancyLevels;
        private final Map<Integer, Node<E>> levelStackMap;
        private LoopAwareControlFrame<E> next;
        private int[] branchLevels;

        private LoopAwareControlFrame() {
            this(0, null, null);
        }

        private LoopAwareControlFrame(int invocationLevel, int[] argumentConstancyLevels, LoopAwareControlFrame<E> next) {
            this.invocationLevel = invocationLevel;
            this.argumentConstancyLevels = argumentConstancyLevels;
            this.next = next;
            if(next == null) {
                levelStackMap = new HashMap<>();
            } else {
                levelStackMap = new HashMap<>(next.levelStackMap);
            }
        }

        private int getLevel(int levelOffset) {
            return invocationLevel + levelOffset;
        }

        private int getLevel(int[] dependencies) {
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

        private Taint<E> copyTagConstant() {
            return copyTag(0);
        }

        private Taint<E> copyTagDependent(int[] dependencies) {
            return copyTag(getLevel(dependencies));
        }

        private Taint<E> copyTagVariant(int levelOffset) {
            return copyTag(getLevel(levelOffset));
        }

        private void pushConstant(int branchID, Taint<E> tag, int branchesSize) {
            push(branchID, tag, branchesSize, 0);
        }

        private void pushArgDependent(int branchID, Taint<E> tag, int branchesSize, int[] dependencies) {
            push(branchID, tag, branchesSize, getLevel(dependencies));
        }

        private void pushVariant(int branchID, Taint<E> tag, int branchesSize, int levelOffset) {
            push(branchID, tag, branchesSize, getLevel(levelOffset));
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

        void push(int branchID, Taint<E> tag, int branchesSize, int level) {
            if(branchLevels == null) {
                branchLevels = new int[branchesSize];
                Arrays.fill(branchLevels, NOT_PUSHED);
            }
            if(!levelStackMap.containsKey(level)) {
                levelStackMap.put(level, new Node<>(null, null));
            }
            if(branchLevels[branchID] == NOT_PUSHED) {
                branchLevels[branchID] = level;
                levelStackMap.put(level, new Node<>(Taint.combineTags(tag, levelStackMap.get(level).tag).copy(), levelStackMap.get(level)));
            } else {
                levelStackMap.get(level).tag.addDependency(tag);
            }
        }

        void pop(int branchID) {
            if(branchLevels != null && branchLevels[branchID] != NOT_PUSHED) {
                levelStackMap.put(branchLevels[branchID], levelStackMap.get(branchLevels[branchID]).next);
                branchLevels[branchID] = NOT_PUSHED;
            }
        }

        void reset() {
            branchLevels = null;
            levelStackMap.clear();
        }

        void exitLoopLevel(int levelOffset) {
            if(branchLevels != null) {
                int level = getLevel(levelOffset);
                levelStackMap.put(level, new Node<>(null, null));
                for(int i = 0; i < branchLevels.length; i++) {
                    if(branchLevels[i] == level) {
                        branchLevels[i] = NOT_PUSHED;
                    }
                }
            }
        }
    }

    private static final class LoopAwareControlFrameBuilder<E> {

        private int invocationLevel;
        private int[] argumentConstancyLevels;
        private int currentArg;

        void start(int invocationLevel, int numArguments) {
            this.invocationLevel = invocationLevel;
            argumentConstancyLevels = new int[numArguments];
            currentArg = 0;
        }

        void setNextArgLevel(int level) {
            argumentConstancyLevels[currentArg++] = level;
        }

        LoopAwareControlFrame<E> build(LoopAwareControlFrame<E> next) {
            LoopAwareControlFrame<E> frame = new LoopAwareControlFrame<>(invocationLevel, argumentConstancyLevels, next);
            argumentConstancyLevels = null;
            return frame;
        }
    }

    private static final class Node<E> {

        Taint<E> tag;
        Node<E> next;

        Node(Taint<E> tag, Node<E> next) {
            this.tag = tag;
            this.next = next;
        }
    }
}
