package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

public class LambdaIntTagITCase {
	@Test
	public void testEmptyLambda() throws Exception {
		Runnable r = () -> {
		};
	}
}
