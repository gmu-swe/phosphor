package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

public class BitSetTaintTest extends TaintTest {

    public static final int maxUniqueElements = 200;
    private static int originalCapacity = -1;

    @ClassRule
    public static final ExternalResource rule  = new ExternalResource() {
        @Override
        protected void before() {
            // Set BIT_SET_CAPACITY
            originalCapacity = Taint.BIT_SET_CAPACITY;
            Taint.BIT_SET_CAPACITY = maxUniqueElements;
        }
        @Override
        protected void after() {
            // Restore BIT_SET_CAPACITY
            Taint.BIT_SET_CAPACITY = originalCapacity;
        }
    };
}
