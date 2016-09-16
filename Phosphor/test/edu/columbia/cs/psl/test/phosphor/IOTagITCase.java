package edu.columbia.cs.psl.test.phosphor;

import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class IOTagITCase {
	
	@Test
	public static void testIncrementArray() {
		int[] arr = {1, 2, 3};
		ArrayList<String> lbls = new ArrayList<String>();
		lbls.add("arrTaint");
		arr = MultiTainter.taintedIntArray(arr, lbls, lbls);
		//System.out.println("arr taint: " + MultiTainter.getTaint(arr));
		//System.out.println("arr[0] taint: " + MultiTainter.getTaint(arr[0]));
		assertEquals("[arrTaint]", MultiTainter.getTaint(arr).getLabel().toString());
		assertEquals("[arrTaint]", MultiTainter.getTaint(arr[0]).getLabel().toString());
		
		ArrayList<String> intTaints = new ArrayList<String>();
		intTaints.add("intTaint");
		int i = MultiTainter.taintedInt(5, intTaints);
		
		ArrayList<String> intTaints2 = new ArrayList<String>();
		intTaints2.add("intTaint2");
		int j = MultiTainter.taintedInt(7, intTaints2);
		
		arr[0] = i + 9;
		//System.out.println("arr[0] taint by i: " + MultiTainter.getTaint(arr[0]));
		assertEquals("[intTaint]", MultiTainter.getTaint(arr[0]).getLabel().toString());
		assertEquals("[]", MultiTainter.getTaint(arr[0]).getDependencies().toString());
		
		arr[0] = i + j;
		//System.out.println("arr[0] taint again: " + MultiTainter.getTaint(arr[0]));
		assertNull(MultiTainter.getTaint(arr[0]).getLabel());
		assertEquals("[[intTaint] [intTaint2] ]", MultiTainter.getTaint(arr[0]).getDependencies().toString());
	}
	
	public static void main(String[] args) {
		testIncrementArray();
	}

}
