package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import junit.framework.TestCase;

public class PrimitiveTaintIT extends TestCase {
	
	@Test
	public void testPrimitiveTaint() {
		int i = MultiTainter.taintedInt(1, "test1");
		Taint t = MultiTainter.getTaint(i);
		assertEquals(t.getLabel(), "test1");
	}
	
	@Test
	public void testPrimitiveArrayTaint() {
		int[] arr = {1, 2, 3};
		arr = MultiTainter.taintedIntArray(arr, "test2");
		Taint t = MultiTainter.getTaint(arr[0]);
		assertEquals(t.getLabel(), "test2");
	}

}
