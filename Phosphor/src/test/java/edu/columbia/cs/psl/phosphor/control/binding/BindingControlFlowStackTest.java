package edu.columbia.cs.psl.phosphor.control.binding;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class BindingControlFlowStackTest {

    @Test
    public void testCopyTagEmptyStack() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        assertNullOrEmpty(ctrl.copyTagConstant());
        assertNullOrEmpty(ctrl.copyTagVariant(3));
    }

    @Test
    public void testCopyTagDifferentLevels() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 3);
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushVariant(1, 3, 1);
        ctrl.setNextBranchTag(Taint.withLabel(2));
        ctrl.pushConstant(2, 3);
        assertContainsLabels(ctrl.copyTagConstant(), 0, 2);
        assertContainsLabels(ctrl.copyTagVariant(1), 0, 1, 2);
    }

    @Test
    public void testExitLoop() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 3);
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushVariant(1, 3, 1);
        ctrl.exitLoopLevel(1);
        assertContainsLabels(ctrl.copyTagVariant(1), 0);
    }

    @Test
    public void testPushFramePushTag() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushConstant(0, 1);
        assertContainsLabels(ctrl.copyTagConstant(), 0, 1);
    }

    @Test
    public void testPushFramePushPopTag() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushConstant(0, 1);
        ctrl.pop(0);
        assertContainsLabels(ctrl.copyTagConstant(), 0);
    }

    @Test
    public void testPushFrameOffset() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushVariant(0, 1, 0);
        assertContainsLabels(ctrl.copyTagConstant(), 0);
        assertContainsLabels(ctrl.copyTagVariant(1), 0, 1);
    }

    @Test
    public void testPopFrame() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushConstant(0, 1);
        ctrl.popFrame();
        assertContainsLabels(ctrl.copyTagConstant(), 0);
    }

    @Test
    public void testReset() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.setNextBranchTag(Taint.withLabel(0));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(1));
        ctrl.pushConstant(0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.setNextBranchTag(Taint.withLabel(2));
        ctrl.pushConstant(0, 1);
        ctrl.reset();
        assertNullOrEmpty(ctrl.copyTagConstant());
        ctrl.popFrame();
        assertNullOrEmpty(ctrl.copyTagConstant());
        ctrl.popFrame();
        assertNullOrEmpty(ctrl.copyTagConstant());
    }

    @Test
    public void testDependentCopy() {
        BindingControlFlowStack<Object> ctrl = new BindingControlFlowStack<>();
        ctrl.startFrame(2, 2).setNextFrameArgConstant().setNextFrameArgVariant(1).pushFrame();
    }

    public static void assertContainsLabels(Taint<Object> tag, Object... labels) {
        assertNotNull(tag);
        Set<Object> expected = new HashSet<>(Arrays.asList(labels));
        Set<Object> actual = new HashSet<>(Arrays.asList(tag.getLabels()));
        assertEquals(expected, actual);
    }

    public static void assertNullOrEmpty(Taint<?> taint) {
        if(taint != null && !taint.isEmpty()) {
            fail("Expected null taint. Got: " + taint);
        }
    }
}
