package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;
import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNullOrEmpty;

@SuppressWarnings("unused")
public class MethodHolder {

    public static final int RET_VALUE = 2;

    // Whether methods' arguments are expected to be tainted
    private final boolean checkArgsTainted;

    MethodHolder(boolean checkArgsTainted) {
        this.checkArgsTainted = checkArgsTainted;
    }

    public int example(boolean taintedBool) {
        if(checkArgsTainted) {
            assertNonNullTaint(MultiTainter.getTaint(taintedBool));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(taintedBool));
        }
        return RET_VALUE;
    }

    public int example(boolean[] b) {
        if(checkArgsTainted) {
            assertNonNullTaint(MultiTainter.getTaint(b[0]));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(b[0]));
        }
        return RET_VALUE;
    }
}
