package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Arrays;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.junit.Test;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LoopAwareControlStackTest {

    @Test
    public void testCopyTagEmptyStack() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        assertNullOrEmpty(ctrl.copyTagConstant());
        assertNullOrEmpty(ctrl.copyTagVariant(3));
    }

    @Test
    public void testCopyTagDifferentLevels() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 3);
        ctrl.pushVariant(Taint.withLabel(1), 1, 3, 1);
        ctrl.pushConstant(Taint.withLabel(2), 2, 3);
        assertContainsLabels(ctrl.copyTagConstant(), 0, 2);
        assertContainsLabels(ctrl.copyTagVariant(1), 0, 1, 2);
    }

    @Test
    public void testExitLoop() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 3);
        ctrl.pushVariant(Taint.withLabel(1), 1, 3, 1);
        ctrl.exitLoopLevel(1);
        assertContainsLabels(ctrl.copyTagVariant(1), 0);
    }

    @Test
    public void testPushFramePushTag() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(1), 0, 1);
        assertContainsLabels(ctrl.copyTagConstant(), 0, 1);
    }

    @Test
    public void testPushFramePushPopTag() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(1), 0, 1);
        ctrl.pop(0);
        assertContainsLabels(ctrl.copyTagConstant(), 0);
    }

    @Test
    public void testPushFrameOffset() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.pushVariant(Taint.withLabel(1), 0, 1, 0);
        assertContainsLabels(ctrl.copyTagConstant(), 0);
        assertContainsLabels(ctrl.copyTagVariant(1), 0, 1);
    }

    @Test
    public void testPopFrame() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(1), 0, 1);
        ctrl.popFrame();
        assertContainsLabels(ctrl.copyTagConstant(), 0);
    }

    @Test
    public void testPopToFrame() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ControlStack.ControlFrame<Object> restorePoint = ctrl.getRestorePoint();
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(1), 0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(2), 0, 1);
        ctrl.popToFrame(restorePoint);
        assertContainsLabels(ctrl.copyTagConstant(), 0);
    }

    @Test
    public void testReset() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.pushConstant(Taint.withLabel(0), 0, 1);
        ctrl.startFrame(1, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(1), 0, 1);
        ctrl.startFrame(0, 0).pushFrame();
        ctrl.pushConstant(Taint.withLabel(2), 0, 1);
        ctrl.reset();
        assertNullOrEmpty(ctrl.copyTagConstant());
        ctrl.popFrame();
        assertNullOrEmpty(ctrl.copyTagConstant());
        ctrl.popFrame();
        assertNullOrEmpty(ctrl.copyTagConstant());
    }

    @Test
    public void testDependentCopy() {
        LoopAwareControlStack<Object> ctrl = new LoopAwareControlStack<>();
        ctrl.startFrame(2, 2).setNextFrameArgConstant().setNextFrameArgVariant(1).pushFrame();
    }

    public static void assertContainsLabels(Taint<Object> tag, Object... labels) {
        assertNotNull(tag);
        Set<Object> expected = new HashSet<>(Arrays.asList(labels));
        Set<Object> actual = new HashSet<>(Arrays.asList(tag.getLabels()));
        assertEquals(expected, actual);
    }
}
