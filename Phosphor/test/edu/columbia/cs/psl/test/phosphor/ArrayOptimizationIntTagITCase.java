package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ArrayOptimizationIntTagITCase  extends BaseMultiTaintClass {

    @Test
    public void testNewArrayConstantClearsTaint(){

        byte[] b = new byte[10];
        b[0] = Tainter.taintedByte(b[0], 42);
        b[0] = 10;

        Assert.assertEquals(0, Tainter.getTaint(b[0]));
    }

    @Ignore
    @Test
    public void testHashCodeGetsTaint() {

        byte[] b = new byte[10];
        MultiTainter.taintedObject(b, new Taint("foo"));
        int taggedHashCode = b.hashCode();

        assertNonNullTaint(taggedHashCode);
    }

    @Ignore
    @Test
    public void testEqualsResultGetsTag() {

        byte[] b = new byte[10];
        MultiTainter.taintedObject(b, new Taint("foo"));
        boolean taggedEquals = b.equals(null);

        assertNonNullTaint(MultiTainter.getTaint(taggedEquals));
    }
}

