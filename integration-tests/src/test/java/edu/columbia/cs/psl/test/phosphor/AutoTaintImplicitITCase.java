package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.After;

public class AutoTaintImplicitITCase extends AutoTaintObjTagITCase {

    /**
     * All tests are inherited from the obj tag test.
     */
    @After
    public void resetState() {
        MultiTainter.getControlFlow().reset();
    }
}
