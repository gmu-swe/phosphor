package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import java.util.stream.IntStream;

public class LambdaIntTagITCase {
	@Test
	public void testEmptyLambda() throws Exception {
		Runnable r = () -> {
		};
	}

	@Test
	public void testIntStreamsDontCrash() throws Exception {
		int sum = IntStream.of(1, 2, 3, 4, 5).sum(); //creates a bunch of lambdas
	}
}
