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
		arr = MultiTainter.taintedIntArray(arr, lbls);
		//System.out.println("arr[0] taint: " + MultiTainter.getTaint(arr[0]));
		assertEquals(lbls, MultiTainter.getTaint(arr[0]));
		
		ArrayList<String> intTaints = new ArrayList<String>();
		intTaints.add("intTaint");
		int i = MultiTainter.taintedInt(5, intTaints);
		//System.out.println("i taint: " + MultiTainter.getTaint(i));
		assertEquals(intTaints, MultiTainter.getTaint(i));
		
		arr[0] = i + 9;
		//System.out.println("arr[0] taint again: " + MultiTainter.getTaint(arr[0]).getDependencies().toString());
		assertEquals("[intTaint]", MultiTainter.getTaint(arr[0]).getDependencies().toString());
	}
	
	public static void main(String[] args) {
		testIncrementArray();
	}

}
