package edu.columbia.cs.psl.test.phosphor;

import static java.security.AccessController.doPrivileged;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.junit.Test;

public class LambdaObjTagITCase {
	@Test
	public void testEmptyLambda() throws Exception {
		Runnable r = () -> {
		};
	}
	
	@Test
	public void testLambdaIntArg() throws Exception {
		intArg(new int[10]);
	}
	
	void initStreams(int[] arg)
	{
	}
	void intArg(int[] arg)
	{
		 try {
	            doPrivileged((PrivilegedExceptionAction<Void>) () -> {
	                initStreams(arg);
	                return null;
	            });
	        } catch (PrivilegedActionException ex) {
	        }
	}
}
