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

    public int example(Object b) {
        return example((boolean[]) b);
    }

    public int getLast(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10,
                       int a11, int a12, int a13, int a14, int a15, int a16, int a17, int a18, int a19, int a20,
                       int a21, int a22, int a23, int a24, int a25, int a26, int a27, int a28, int a29, int a30,
                       int a31, int a32, int a33, int a34, int a35, int a36, int a37, int a38, int a39) {
        if (checkArgsTainted) {
            assertNonNullTaint(MultiTainter.getTaint(a39));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(a39));
        }
        return a39;
    }
}
