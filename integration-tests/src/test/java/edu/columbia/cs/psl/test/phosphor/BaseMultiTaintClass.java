package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BaseMultiTaintClass extends BasePhosphorTest {

    public static void assertNullOrEmpty(Taint taint) {
        if(taint != null && !taint.isEmpty()) {
            fail("Expected null taint. Got: " + taint);
        }
    }

    public static void assertNoTaint(String obj) {
        Taint taint = MultiTainter.getTaint(obj.toCharArray()[0]);
        if(taint != null && !taint.isEmpty()) {
            fail("Expected null taint. Got: " + taint);
        }
    }

    public static void assertNonNullTaint(Object obj) {
        Taint t = MultiTainter.getTaint(obj);
        assertNotNull(obj);
		if(t == null || t.isEmpty()) {
			fail("Expected non-null taint - got: " + t);
		}
    }

    public static void assertNonNullTaint(Taint obj) {
        assertNotNull(obj);
		if(obj.isEmpty()) {
			fail("Expected non-null taint - got: " + obj);
		}
    }

    public static void assertTaintHasLabel(Taint obj, Object lbl) {
        assertNotNull(obj);
        if(!obj.containsLabel(lbl)) {
            fail("Expected taint contained " + lbl + ", has " + obj);
        }
    }

    public static void assertTaintHasOnlyLabel(Taint obj, Object lbl) {
        assertNotNull(obj);
        if(!obj.containsOnlyLabels(new Object[]{lbl})) {
            fail("Expected taint contained ONLY " + lbl + ", found " + obj);
        }

    }

    public static void assertTaintHasOnlyLabels(Taint obj, Object... lbl) {
        assertNotNull(obj);
        if(!obj.containsOnlyLabels(lbl)) {
            fail("Expected taint contained ONLY " + Arrays.toString(lbl) + ", found " + obj);
        }
    }
}
