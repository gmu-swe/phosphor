package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.AutoTaintLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;
import edu.columbia.cs.psl.test.phosphor.util.TaintThroughExample;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AutoTaintImplicitITCase extends AutoTaintObjTagITCase {

    /**
     * All tests are inherited from the obj tag test.
     */

    @After
    public void resetState() {
        MultiTainter.getControlFlow().reset();
    }
}
