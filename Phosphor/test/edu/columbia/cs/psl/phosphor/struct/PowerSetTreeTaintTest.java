package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

public class PowerSetTreeTaintTest extends TaintTest {

    private static int originalCapacity = -1;

    @ClassRule
    public static final ExternalResource rule  = new ExternalResource() {
        @Override
        protected void before() {
            // Set BIT_SET_CAPACITY
            originalCapacity = Taint.BIT_SET_CAPACITY;
            Taint.BIT_SET_CAPACITY = -1;
        }
        @Override
        protected void after() {
            // Restore BIT_SET_CAPACITY
            Taint.BIT_SET_CAPACITY = originalCapacity;
        }
    };
}
