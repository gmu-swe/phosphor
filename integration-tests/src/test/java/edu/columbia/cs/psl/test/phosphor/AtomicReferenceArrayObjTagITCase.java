package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicReferenceArrayObjTagITCase extends BaseMultiTaintClass {
    static class Holder {
        int x;
    }

    @Test
    public void testGetAndSetReference() {
        AtomicReferenceArray<Holder> array = new AtomicReferenceArray<Holder>(10);
        array.set(0, MultiTainter.taintedReference(new Holder(), "tag"));
        assertTaintHasLabel(MultiTainter.getTaint(array.get(0)), "tag");
        array.set(0, new Holder());
        assertNullOrEmpty(MultiTainter.getTaint(array.get(0)));
    }
}
