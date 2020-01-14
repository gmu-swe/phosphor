package edu.columbia.cs.psl.phosphor.control;

public abstract class ControlFlowStack {

    private int disabled;

    public ControlFlowStack(boolean disabled) {
        this.disabled = disabled ? 1 : 0;
    }

    public void enable() {
        disabled--;
    }

    public void disable() {
        disabled++;
    }

    public abstract ControlFlowStack copyTop();

    public abstract void reset();

    public abstract void pushFrame();

    public abstract void popFrame();
}
