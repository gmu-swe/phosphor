package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;
import static org.junit.Assert.*;


import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class PrimitiveTaintObjTagITCase {
	
	@Test
	public void testPrimitiveTaint() {
		int i = MultiTainter.taintedInt(1, "test1");
		Taint t = MultiTainter.getTaint(i);
		System.out.println(t);
		assertEquals("test1", t.getLabel());
	}
	
	@Test
	public void testPrimitiveArrayTaint() {
		int[] arr = {1, 2, 3};
		arr = MultiTainter.taintedIntArray(arr, "test2");
		Taint t = MultiTainter.getTaint(arr[0]);
		System.out.println(t);
		assertEquals("test2", t.getLabel());
	}
	
	public static void main(String[] args) {
		int i = MultiTainter.taintedInt(1, "test1");
		Taint t = MultiTainter.getTaint(i);
		System.out.println(t);
	}
	
}
