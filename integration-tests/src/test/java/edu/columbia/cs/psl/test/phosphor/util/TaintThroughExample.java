package edu.columbia.cs.psl.test.phosphor.util;


import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class TaintThroughExample {

	public void taintBackToArgs(int[] input) {
		try {
			input[0] = 1;
			int x = input[0];
		} finally {
			//taint input
		}
	}

	public int passIntTaintThrough(int i) {
		System.out.println(MultiTainter.getTaint(i));
		return i;
	}

	public String passStringTaintThrough(String s) {
		return s;
	}

	public int taintThroughInt() {
		return 7;
	}

	public static String staticMethod(String s) {
		return s;
	}


}
