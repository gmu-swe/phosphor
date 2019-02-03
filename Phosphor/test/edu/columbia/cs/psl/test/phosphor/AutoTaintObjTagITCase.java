package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.util.TaintThroughExample;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.AutoTaintLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;

public class AutoTaintObjTagITCase extends BaseMultiTaintClass {

	public String source()
	{
		return "Foo";
	}
	public int iSource()
	{
		return 10;
	}
	
	public void sink(int i)
	{
		System.out.println("Sink'ed: " + i);
	}
	
	public void sink(String i)
	{
		System.out.println("Sink'ed: " + i);
	}
	public void source(int[] a)
	{
		a[0] = 2;
	}


	@Test
	public void testTaintThroughHandlesUntaintedObject() throws Exception{

		try {

			TaintThroughExample ex = new TaintThroughExample();

			int[] ar = new int[10];

			ex.taintBackToArgs(ar);

			assertNullOrEmpty(MultiTainter.getTaint(ar[0]));
			assertNullOrEmpty(MultiTainter.getTaint(ar[1]));
		}
		catch (NullPointerException ex) {
			fail(ex.toString());
		}
	}

	@Test
	public void testTaintThroughAppliesToArgsAtEndOfMethod() throws Exception{
		TaintThroughExample ex = new TaintThroughExample();
		MultiTainter.taintedObject(ex,new Taint("Test"));

		int[] ar = new int[10];

		ex.taintBackToArgs(ar);

		assertNonNullTaint(MultiTainter.getTaint(ar[0]));
		assertNonNullTaint(MultiTainter.getTaint(ar[1]));
	}

	@Test
	public void testAutoTaintTaints() throws Exception {
		String s = source();
		int i = iSource();
		
		int[] ar = new int[10];
		source(ar);
		
		assertNonNullTaint(s);
		assertNonNullTaint(MultiTainter.getTaint(i));
		assertTrue(MultiTainter.getTaint(i).lbl instanceof AutoTaintLabel);
		assertTrue(MultiTainter.getTaint(ar[1]).lbl instanceof AutoTaintLabel);

	}
	
	@Test(expected = TaintSinkError.class)
	public void testIntSink() throws Exception {
		sink(iSource());
	}
	
	@Test(expected = TaintSinkError.class)
	public void testStringSink() throws Exception {
		sink(source());
	}
}
