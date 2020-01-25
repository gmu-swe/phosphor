package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;
import org.junit.After;
import org.junit.Test;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;

public class AutoTaintConstructorsImplicitITCase extends AutoTaintConstructorsObjTagITCase {
    /**
     * All tests are inherited from the obj tag test.
     */

    @After
    public void resetState() {
        MultiTainter.getControlFlow().reset();
    }
}

