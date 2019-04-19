package edu.columbia.cs.psl.phosphor.bench;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

public class SimpleHashSetBenchmark extends SimpleBenchmark {
	private static final int SIZE = 300;
	private static int nContains;
	SimpleHashSet<String> sh1;
	SimpleHashSet<String> sh2;

	public static void main(String[] args) {
		Runner.main(SimpleHashSetBenchmark.class, args);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sh1 = new SimpleHashSet<>(64);
		for (int j = 0; j < SIZE; j++) {
			sh1.add("SimpleString" + j);
		}

		sh2 = new SimpleHashSet<>(64);
		for (int j = 0; j < SIZE; j++) {
			sh2.add("SimpleString" + j);
		}
	}

	public void timeEqualsInvocation(int reps) {
		nContains = 0;

		for (int i = 0; i < reps; i++) {
			if (sh1.equals(sh2)) {
				nContains++;
			}
		}
	}

	public void timeAddToBusySet(int reps) {
		nContains = 0;

		for (int i = 0; i < reps; i++) {
			sh1.add("newString" + i);
		}
	}
}
