package clone.test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class PrimitiveTaint {
	
	public static void main(String[] args) {
		int i = MultiTainter.taintedInt(1, "test1");
		System.out.println("Taint of i: " + MultiTainter.getTaint(i)); //null lable, but dep contains test1, looks strange
		
		int[] arr = {1, 2, 3};
		arr = MultiTainter.taintedIntArray(arr, "test2");
		System.out.println("Taint of arr[0]: " + MultiTainter.getTaint(arr[0])); //label is test2, dep null, looks good
	}

}
