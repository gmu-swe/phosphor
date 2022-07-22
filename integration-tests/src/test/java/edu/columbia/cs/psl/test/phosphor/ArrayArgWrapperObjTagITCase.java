package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNull;

public class ArrayArgWrapperObjTagITCase {

    @Test
    public void testArgWrapperTaintNotLeaked() {
        byte[] b = new byte[]{1, 2, 3};
        Arrays.copyOf(b, 3); // `copyof` calls System.arraycopy which is not instrumented.
        checkNull(null);
    }

    @Test
    public void testNoUnwrapForNull() {
        checkNull(null);
    }
    
    public void checkNull(byte[] input) {
        assertNull(input);
    }
}
