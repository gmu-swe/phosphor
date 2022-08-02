package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

public class LocalVariableInstCase {
    @Test
    public void testLocalVariableVerifySuccessWhenTaintDisabled() {
        // We only need to load this class to trigger JVM verification.
        for (ClassWithLargeMethod value : ClassWithLargeMethod.values()) {
            // NOOP
        }
    }
}
