package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public abstract class TaintTest {

    /* Checks that a Taint instance created via the zero-argument constructor is considered to be empty. */
    @Test
    public void testConstructEmptyTaint() {
        Taint t = new Taint();
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance created with an initial int label contains only that label. */
    @Test
    public void testConstructTaintFromIntLabel() {
        int label = 5;
        Taint t = new Taint(label);
        assertTrue(t.containsOnlyLabels(new Object[]{label}));
    }

    /* Checks that a Taint instance created with an initial Integer label contains only that label. */
    @Test
    public void testConstructTaintFromObjectLabel() {
        Integer label = 5;
        Taint t = new Taint(label);
        assertTrue(t.containsOnlyLabels(new Object[]{label}));
    }

    /* Checks that a Taint instance created with an initial null label is considered to be empty. */
    @Test
    public void testConstructTaintFromNullLabel() {
        Integer label = null;
        Taint t = new Taint(label);
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance create from a non-empty Taint is equal to the original Taint and that changes made
     * to the newly constructed Taint instance do not impact the original. */
    @Test
    public void testConstructTaintFromTaint() {
        Taint other = new Taint(5);
        Taint t = new Taint(other);
        assertEquals(other, t);
        t.addDependency(new Taint(6));
        assertFalse(other.containsLabel(6));
        assertTrue(t.containsLabel(6));
    }

    /* Checks that a Taint instance created from an empty Taint is considered to be empty. */
    @Test
    public void testConstructTaintFromEmptyTaint() {
        Taint other = new Taint();
        Taint t = new Taint(other);
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance created from a null Taint is considered to be empty. */
    @Test
    public void testConstructTaintFromNullTaint() {
       Taint other = null;
        Taint t = new Taint(other);
        assertTrue(t.isEmpty());
    }

    /* Checks that a Taint instance create from two non-empty Taints contains all of the labels of both original Taint
     * instances. Also checks that changes made to the newly constructed Taint instance do not impact the originals Taints. */
    @Test
    public void testConstructTaintFromTwoTaints() {
        Taint orig1 = new Taint(5);
        Taint orig2 = new Taint(99);
        Taint t = new Taint(orig1, orig2);
        assertTrue(t.contains(orig1));
        assertTrue(t.contains(orig2));
        t.addDependency(new Taint(6));
        assertFalse(orig1.containsLabel(6));
        assertFalse(orig2.containsLabel(6));
        assertTrue(t.containsLabel(6));
    }

    /* Checks that a Taint instance create from a non-empty Taint and a null Taint equals the non-empty Taint. Also checks
     * that changes made to the newly constructed Taint instance do not impact the original non-empty Taint. */
    @Test
    public void testConstructTaintFromTaintAndNull() {
        // Check when null Taint is the second argument
        Taint orig1 = new Taint(5);
        Taint orig2 = null;
        Taint t = new Taint(orig1, orig2);
        assertEquals(t, orig1);
        t.addDependency(new Taint(6));
        assertFalse(orig1.containsLabel(6));
        assertTrue(t.containsLabel(6));
        // Check when null Taint is the first argument
        Taint t2 = new Taint(orig2, orig1);
        assertEquals(t2, orig1);
        t2.addDependency(new Taint(7));
        assertFalse(orig1.containsLabel(7));
        assertTrue(t2.containsLabel(7));
    }

    /* Checks that a Taint instance create from a non-empty Taint and an empty Taint equals the non-empty Taint. Also checks
     * that changes made to the newly constructed Taint instance do not impact the original Taints. */
    @Test
    public void testConstructTaintFromTaintAndEmptyTaint() {
        // Check when empty Taint is the second argument
        Taint orig1 = new Taint(5);
        Taint orig2 = new Taint();
        Taint t = new Taint(orig1, orig2);
        assertEquals(t, orig1);
        t.addDependency(new Taint(6));
        assertFalse(orig1.containsLabel(6));
        assertFalse(orig2.containsLabel(6));
        assertTrue(t.containsLabel(6));
        // Check when empty Taint is the first argument
        Taint t2 = new Taint(orig2, orig1);
        assertEquals(t2, orig1);
        t2.addDependency(new Taint(7));
        assertFalse(orig1.containsLabel(7));
        assertFalse(orig2.containsLabel(7));
        assertTrue(t2.containsLabel(7));
    }

    /* Checks that a Taint instance create from a null Taint and an empty Taint is empty. Also checks that changes made to
     * the newly constructed Taint instance do not impact the original empty Taint. */
    @Test
    public void testConstructTaintFromNullTaintAndEmptyTaint() {
        // Check when empty Taint is the second argument
        Taint orig1 = new Taint();
        Taint orig2 = null;
        Taint t = new Taint(orig1, orig2);
        assertTrue(t.isEmpty());
        t.addDependency(new Taint(6));
        assertFalse(orig1.containsLabel(6));
        assertTrue(t.containsLabel(6));
        // Check when empty Taint is the first argument
        Taint t2 = new Taint(orig2, orig1);
        assertTrue(t2.isEmpty());
        t2.addDependency(new Taint(7));
        assertFalse(orig1.containsLabel(7));
        assertTrue(t2.containsLabel(7));
    }
    /* Checks that a Taint instance contains the same labels before and after an empty  Taint is added to it as a dependency. */
    @Test
    public void testAddEmptyDependency() {
        Taint t = new Taint(5);
        Taint t2 = new Taint();
        t.addDependency(t2);
        assertTrue(t.containsOnlyLabels(new Object[]{5}));
    }

    /* Checks that a Taint instance contains the same labels before and after a null Taint is added to it as a dependency. */
    @Test
    public void testAddNullDependency() {
        Taint t = new Taint(5);
        t.addDependency(null);
        assertTrue(t.containsOnlyLabels(new Object[]{5}));
    }

    /* Checks that a Taint instance contains the labels in a Taint dependency added to it. */
    @Test
    public void testAddDependency() {
        Taint t = new Taint(5);
        Taint t2 = new Taint(7);
        t.addDependency(t2);
        assertTrue(t.containsOnlyLabels(new Object[]{5, 7}));
    }

    /* Checks that an empty Taint instance is considered to contain a null taint instance. */
    @Test
    public void testEmptyTaintContainsNullTaint() {
        Taint t = new Taint();
        assertTrue(t.contains(null));
    }

    /* Checks that an empty Taint instance is considered to contain a different empty taint instance. */
    @Test
    public void testEmptyTaintContainsEmptyTaint() {
        Taint t = new Taint();
        Taint t2 = new Taint();
        assertTrue(t.contains(t2));
    }

    /* Checks that a non-empty Taint instance is considered to contains an empty Taint instance and that the empty taint
     * instance is not considered to contain the non-empty Taint. */
    @Test
    public void testTaintContainsEmptyTaint() {
        Taint t = new Taint(2);
        Taint t2 = new Taint();
        assertTrue(t.contains(t2));
        assertFalse(t2.contains(t));
    }

    /* Checks that a Taint that is a superset of another taint contains that subset taint and that the subset taint does not
     * contain the superset taint. */
    @Test
    public void testTaintContainsTaint() {
        Taint t = new Taint(2);
        t.addDependency(new Taint(4));
        Taint t2 = new Taint(4);
        assertTrue(t.contains(t2));
        assertFalse(t2.contains(t));
    }
}
