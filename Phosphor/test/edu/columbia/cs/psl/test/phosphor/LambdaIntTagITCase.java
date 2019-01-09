package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LambdaIntTagITCase {

	// This is known to not work correctly on int tags. It may never be possible to make it work completely on int tags, because
	// we need to add a fake parameter to every modified constructor to make sure that it is unique. But, we can't do that
	// for cases where INVOKEDYNAMIC binds a constructor to a non-constructor call (although maybe we can fix this, not enough
	// time to look into it now)
//	@Test
//	public void testCollectors() throws Exception{
//		List<String> givenList = Arrays.asList("a", "bb", "ccc", "dd");
//		givenList.stream().collect(Collectors.toList());
//
//	}
	@Test
	public void testEmptyLambda() throws Exception {
		Runnable r = () -> {
		};
	}

	@Test
	public void testIntStreamsDontCrash() throws Exception {
		int sum = IntStream.of(1, 2, 3, 4, 5).sum(); //creates a bunch of lambdas
	}

	@Test
	public void testSupplier() throws Exception {
		Supplier<double[]> supplier = () -> new double[3];
		double[] d = supplier.get();
	}
}
