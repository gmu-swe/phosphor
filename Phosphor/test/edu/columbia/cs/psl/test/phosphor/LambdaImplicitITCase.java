package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.security.AccessController.doPrivileged;

public class LambdaImplicitITCase {

	@Test
	public void testCollectors() throws Exception{
		List<String> givenList = Arrays.asList("a", "bb", "ccc", "dd");
		givenList.stream().collect(Collectors.toList());
	}

	@Test
	public void testIntStreamsDontCrash() throws Exception {
		int sum = IntStream.of(1, 2, 3, 4, 5).sum(); //creates a bunch of lambdas
	}

	@Test
	public void testEmptyLambda() throws Exception {
		Runnable r = () -> {
		};
	}

	@Test
	public void testLambdaIntArg() throws Exception {
		intArg(new int[10]);
	}

	void initStreams(int[] arg) {
	}

	void intArg(int[] arg) {
		try {
			doPrivileged((PrivilegedExceptionAction<Void>) () -> {
				initStreams(arg);
				return null;
			});
		} catch (PrivilegedActionException ex) {
		}
	}
	@Test
	public void testSupplier() throws Exception {
		Supplier<double[]> supplier = () -> new double[3];
		double[] d = supplier.get();
	}
}
