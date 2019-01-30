package edu.columbia.cs.psl.test.phosphor.util;


public class TaintThroughExample {

	public void taintBackToArgs(int[] input)
	{
		try {
			input[0] = 1;
			int x = input[0];
		}finally{
			//taint input
		}
	}
}
