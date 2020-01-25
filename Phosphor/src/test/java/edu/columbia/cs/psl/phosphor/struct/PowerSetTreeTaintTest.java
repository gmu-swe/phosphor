package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class PowerSetTreeTaintTest extends TaintTest {

    /* Checks that resetting the PowerSetTree singleton instance, makes all existing Taints' label sets be considered to be empty. */
    @Test
    public void testResetTree() {
        Taint t1 = Taint.withLabel(5);
        Taint t2 = Taint.withLabel(99);
        t1 = t1.union(Taint.withLabel(4)).union(t2);
        assertTrue(t1.containsOnlyLabels(new Object[]{4, 5, 99}));
        assertTrue(t2.containsOnlyLabels(new Object[]{99}));
        // Reset the tree
        PowerSetTree.getInstance().reset();
        assertTrue(t1.isEmpty());
        assertTrue(t2.isEmpty());
        t1 = t1.union(Taint.withLabel(77));
        assertTrue(t1.containsOnlyLabels(new Object[]{77}));
        Taint t3 = Taint.combineTags(t2, t1);
        assertTrue(t3.containsOnlyLabels(new Object[]{77}));
        t2 = t2.union(Taint.withLabel(4));
        assertTrue(t2.containsOnlyLabels(new Object[]{4}));
    }
}
