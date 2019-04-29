package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
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

    /* Checks that resetting the PowerSetTree singleton instance, makes all existing Taints' label sets be considered to be empty. */
    @Test
    public void testResetTree() {
        Taint t1 = new Taint(5);
        Taint t2 = new Taint(99);
        t1.addDependency(new Taint(4));
        t1.addDependency(t2);
        assertTrue(t1.containsOnlyLabels(new Object[]{4, 5, 99}));
        assertTrue(t2.containsOnlyLabels(new Object[]{99}));
        // Reset the tree
        PowerSetTree.getInstance().reset();
        assertTrue(t1.isEmpty());
        assertTrue(t2.isEmpty());
        t1.addDependency(new Taint(77));
        assertTrue(t1.containsOnlyLabels(new Object[]{77}));
        Taint t3 = Taint.combineTags(t2, t1);
        assertTrue(t3.containsOnlyLabels(new Object[]{77}));
        t2.addDependency(new Taint(4));
        assertTrue(t2.containsOnlyLabels(new Object[]{4}));
    }
}
