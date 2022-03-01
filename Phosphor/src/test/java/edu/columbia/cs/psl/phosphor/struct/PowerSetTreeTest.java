package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PowerSetTreeTest {
    @Before
    public void reset() {
        PowerSetTree.getInstance().reset();
    }

    @Test(expected = NullPointerException.class)
    public void testFilterEmptySetNullPredicate() {
        PowerSetTree.SetNode set = PowerSetTree.getInstance().emptySet();
        set.filter(null);
    }

    @Test
    public void testFilterEmptySet() {
        PowerSetTree.SetNode set = PowerSetTree.getInstance().emptySet();
        PowerSetTree.SetNode result = set.filter((o) -> o instanceof Integer);
        Assert.assertTrue(result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testFilterNullPredicate() {
        PowerSetTree.SetNode set = PowerSetTree.getInstance().makeSingletonSet(5);
        set.filter(null);
    }

    @Test
    public void testFilterByInstanceOf() {
        Set<Integer> expected = new HashSet<>(Arrays.asList(0, 10, 50, -4, 60));
        PowerSetTree.SetNode set = PowerSetTree.getInstance().makeSingletonSet(new Object())
                .add(10).add("Hello").add('l').add(50).add(-4).add(60).add("world").add(0);
        Set<Object> result = new HashSet<>(Arrays.asList(set.filter((o) -> o instanceof Integer).getLabels()));
        Assert.assertEquals(expected, result);
    }
}
