package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;

public final class LoopAwareControlStack<E> extends ControlStack<E> {

    private static final int NOT_PUSHED = -1;
    private LoopAwareControlFrame<E> stackTop = new LoopAwareControlFrame<>(0, null, null);
    private LoopAwareControlFrameBuilder<E> frameBuilder = new LoopAwareControlFrameBuilder<>();

    @Override
    public LoopAwareControlStack<E> startFrame(int invocationLevel, int numArguments) {
        frameBuilder.start(invocationLevel, numArguments);
        return this;
    }

    @Override
    public LoopAwareControlStack<E> setNextFrameArgConstant() {
        frameBuilder.setNextArgLevel(0);
        return this;
    }

    @Override
    public LoopAwareControlStack<E> setNextFrameArgDependent(int[] dependencies) {
        frameBuilder.setNextArgLevel(getLevel(dependencies));
        return this;
    }

    @Override
    public LoopAwareControlStack<E> setNextFrameArgVariant(int levelOffset) {
        frameBuilder.setNextArgLevel(getLevel(levelOffset));
        return this;
    }

    @Override
    public LoopAwareControlStack<E> pushFrame() {
        stackTop = frameBuilder.build(stackTop);
        return this;
    }

    @Override
    public ControlFrame<E> getRestorePoint() {
        return stackTop;
    }

    @Override
    public int getLevel(int levelOffset) {
        return stackTop.getLevel(levelOffset);
    }

    @Override
    public int getLevel(int[] dependencies) {
        return stackTop.getLevel(dependencies);
    }

    @Override
    public void popFrame() {
        stackTop = (LoopAwareControlFrame<E>) stackTop.getNext();
    }

    @Override
    public void popToFrame(ControlFrame<E> restorePoint) {
        stackTop = (LoopAwareControlFrame<E>) restorePoint;
    }

    @Override
    public Taint<E> copyTag() {
        return stackTop.copyTag();
    }

    @Override
    public Taint<E> copyTagConstant() {
        return stackTop.copyTag(0);
    }

    @Override
    public Taint<E> copyTagArgDependent(int[] dependencies) {
        return stackTop.copyTag(getLevel(dependencies));
    }

    @Override
    public Taint<E> copyTagVariant(int levelOffset) {
        return stackTop.copyTag(getLevel(levelOffset));
    }

    @Override
    public void pushConstant(Taint<E> tag, int branchID, int branchesSize) {
        stackTop.push(tag, branchID, branchesSize, 0);
    }

    @Override
    public void pushDependent(Taint<E> tag, int branchID, int branchesSize, int[] dependencies) {
        stackTop.push(tag, branchID, branchesSize, getLevel(dependencies));
    }

    @Override
    public void pushVariant(Taint<E> tag, int branchID, int branchesSize, int levelOffset) {
        stackTop.push(tag, branchID, branchesSize, getLevel(levelOffset));
    }

    @Override
    public boolean pop(int branchID) {
        return stackTop.pop(branchID);
    }

    @Override
    public void reset() {
        stackTop.reset();
    }

    @Override
    public void exitLoopLevel(int levelOffset) {
        stackTop.exitLoopLevel(levelOffset);
    }

    private static final class LoopAwareControlFrame<E> extends ControlStack.ControlFrame<E> {

        private final int invocationLevel;
        private final int[] argumentConstancyLevels;
        private final Map<Integer, ControlStack.Node<E>> levelStackMap;
        private int[] branchLevels;

        private LoopAwareControlFrame(int invocationLevel, int[] argumentConstancyLevels, LoopAwareControlFrame<E> next) {
            super(next);
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

        Taint<E> copyTag() {
            Taint<E> tag = null;
            for(Integer key : levelStackMap.keySet()) {
                tag = Taint.combineTags(tag, levelStackMap.get(key).tag);
            }
            return tag;
        }

        void push(Taint<E> tag, int branchID, int branchesSize, int level) {
            if(branchLevels == null) {
                branchLevels = new int[branchesSize];
                Arrays.fill(branchLevels, NOT_PUSHED);
            }
            if(!levelStackMap.containsKey(level)) {
                levelStackMap.put(level, ControlStack.Node.emptyNode());
            }
            if(branchLevels[branchID] == NOT_PUSHED) {
                branchLevels[branchID] = level;
                levelStackMap.put(level, new ControlStack.Node<>(Taint.combineTags(tag, levelStackMap.get(level).tag), levelStackMap.get(level)));
            } else {
                Node<E> r = levelStackMap.get(level);
                r.tag = r.tag.union(tag);
            }
        }

        boolean pop(int branchID) {
            if(branchLevels != null && branchLevels[branchID] != NOT_PUSHED) {
                levelStackMap.put(branchLevels[branchID], levelStackMap.get(branchLevels[branchID]).next);
                branchLevels[branchID] = NOT_PUSHED;
                return true;
            } else {
                return false;
            }
        }

        void reset() {
            if(getNext() instanceof LoopAwareControlFrame) {
                ((LoopAwareControlFrame<Object>) getNext()).reset();
            }
            branchLevels = null;
            levelStackMap.clear();
        }

        void exitLoopLevel(int levelOffset) {
            if(branchLevels != null) {
                int level = getLevel(levelOffset);
                levelStackMap.put(level, ControlStack.Node.emptyNode());
                for(int i = 0; i < branchLevels.length; i++) {
                    if(branchLevels[i] == level) {
                        branchLevels[i] = NOT_PUSHED;
                    }
                }
            }
        }
    }

    private static final class LoopAwareControlFrameBuilder<E> {

        private int invocationLevel = 0;
        private int[] argumentConstancyLevels = null;
        private int currentArg = 0;

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
}
