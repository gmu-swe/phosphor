package edu.columbia.cs.psl.test.phosphor;

import static java.security.AccessController.doPrivileged;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class LambdaObjTagITCase {

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
}
