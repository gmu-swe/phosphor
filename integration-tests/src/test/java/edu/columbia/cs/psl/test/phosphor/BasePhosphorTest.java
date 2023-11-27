package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.Phosphor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class BasePhosphorTest {
    @Before
    public void clearErrorFlag() {
        Phosphor.INSTRUMENTATION_EXCEPTION_OCCURRED = false;
    }

    @After
    public void checkForError() {
		if(Phosphor.INSTRUMENTATION_EXCEPTION_OCCURRED) {
			Assert.fail("Instrumentation error occurred");
		}
    }
}
