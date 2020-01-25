package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public abstract class TaintTest {

    /* Checks that a Taint instance created via the zero-argument constructor is considered to be empty. */
    @Test
    public void testConstructEmptyTaint() {
        Taint t = Taint.emptyTaint();
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance created with an initial int label contains only that label. */
    @Test
    public void testConstructTaintFromIntLabel() {
        int label = 5;
        Taint t = Taint.withLabel(label);
        assertTrue(t.containsOnlyLabels(new Object[]{label}));
    }

    /* Checks that a Taint instance created with an initial Integer label contains only that label. */
    @Test
    public void testConstructTaintFromObjectLabel() {
        Integer label = 5;
        Taint t = Taint.withLabel(label);
        assertTrue(t.containsOnlyLabels(new Object[]{label}));
    }

    /* Checks that a Taint instance created with an initial null label is considered to be empty. */
    @Test
    public void testConstructTaintFromNullLabel() {
        Integer label = null;
        Taint t = Taint.withLabel(label);
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance created from a null Taint is considered to be empty. */
    @Test
    public void testConstructTaintFromNullTaint() {
       Taint other = null;
        Taint t = Taint.withLabel(other);
        assertTrue(t.isEmpty());
    }

    /* Checks that an empty Taint instance is considered to contain a null taint instance. */
    @Test
    public void testEmptyTaintContainsNullTaint() {
        Taint t = Taint.emptyTaint();
        assertTrue(t.containsLabel(null));
    }

    /* Checks that an empty Taint instance is considered to contain a different empty taint instance. */
    @Test
    public void testEmptyTaintContainsEmptyTaint() {
        Taint t = Taint.emptyTaint();
        Taint t2 = Taint.emptyTaint();
        assertTrue(t.isSuperset(t2));
    }

    /* Checks that a non-empty Taint instance is considered to contains an empty Taint instance and that the empty taint
     * instance is not considered to contain the non-empty Taint. */
    @Test
    public void testTaintContainsEmptyTaint() {
        Taint t = Taint.withLabel(2);
        Taint t2 = Taint.emptyTaint();
        assertTrue(t.isSuperset(t2));
        assertFalse(t2.isSuperset(t));
    }

    /* Checks that a Taint that is a superset of another taint contains that subset taint and that the subset taint does not
     * contain the superset taint. */
    @Test
    public void testTaintContainsTaint() {
        Taint t = Taint.withLabel(2);
        t = t.union(Taint.withLabel(4));
        Taint t2 = Taint.withLabel(4);
        assertTrue(t.isSuperset(t2));
        assertFalse(t2.isSuperset(t));
    }
}
