package edu.columbia.cs.psl.test.phosphor.util;


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
