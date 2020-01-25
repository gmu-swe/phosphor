package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.util.TaintThroughExample;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.AutoTaintLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSinkError;

import java.net.URI;
import java.nio.channels.SocketChannel;

public class AutoTaintObjTagITCase extends BaseMultiTaintClass {

	public String source() {
		return "Foo";
	}

	public int iSource() {
		return 10;
	}
	
	public void sink(int i) {
		System.out.println("Sout still Sink'ed: " + i);
	}
	
	public void sink(String i) {
		System.out.println("Sink'ed: " + i);
	}

	public void source(int[] a) {
		a[0] = 2;
	}

	public int exceptionCatchingSink(int i) {
		try {
			int[] arr = new int[1];
			int x = arr[2];
			return x;
		} catch(Exception e) {
			return 7;
		}
	}

	public void sourceWithDoubleCharArrParam(char[][] arr) {
		arr[0][0] = 'b';
	}

	public void sourceWithStringArrParam(String[] arr) {
		arr[0] = "hello";
	}

	/* Tests that calling a taintThrough method for an untainted object doesn't clear existing taint tags of that method's
	 * primitive return value.
	 */
	@Test
	public void testNestedTaintThroughKeepsExistingTaintPrimitive() throws Exception {
		TaintThroughExample untaintedObj = new TaintThroughExample();
		int taintedInt = iSource();
		taintedInt = untaintedObj.passIntTaintThrough(taintedInt);
		assertNonNullTaint(taintedInt);
	}


	/* Tests that calling a taintThrough method for an untainted object doesn't clear existing taint tags of that method's
	 * object return value.
	 */
	@Test
	public void testNestedTaintThroughKeepsExistingTaintObject() throws Exception {
		TaintThroughExample untaintedObj = new TaintThroughExample();
		String taintedString = source();
		taintedString = untaintedObj.passStringTaintThrough(taintedString);
		assertNonNullTaint(taintedString);
	}

	@Test
	public void testTaintThroughHandlesUntaintedObject() throws Exception {
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
	public void testTaintThroughAppliesToArgsAtEndOfMethod() throws Exception {
		TaintThroughExample ex = new TaintThroughExample();
		ex = MultiTainter.taintedReference(ex, Taint.withLabel("Test"));

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
		assertTrue(MultiTainter.getTaint(i).getLabels()[0] instanceof AutoTaintLabel);
		assertTrue(MultiTainter.getTaint(ar[1]).getLabels()[0]  instanceof AutoTaintLabel);
	}

	/* Asserts that calling a tainted object's taintThrough method taints that method's primitive return value. */
	@Test
	public void testPrimitiveTaintThrough() throws Exception {
		TaintThroughExample taintedObj = new TaintThroughExample();
		taintedObj = MultiTainter.taintedReference(taintedObj, Taint.withLabel("Test"));
		int ret = taintedObj.taintThroughInt();
		assertNonNullTaint(ret);
	}

	@Test
	public void testStaticThroughMethod() throws Exception {
		String s = "test";
		s = MultiTainter.taintedReference(s, Taint.withLabel("Test"));
		String ret = TaintThroughExample.staticMethod(s);
		assertNonNullTaint(ret);
	}

	/* Asserts that sink's try-finally blocks don't disrupt methods normal exception handling. */
	@Test
	public void testExceptionHandlingSink() throws Exception {
		int result = exceptionCatchingSink(5);
		assertEquals(7, result);
	}

	/* Checks that sources properly taint multi-dimensional primitive array arguments passed to them. */
	@Test
	public void testSourceTaints2DCharArrArg() throws Exception {
		char[][] chars = new char[1][1];
		sourceWithDoubleCharArrParam(chars);
		assertNonNullTaint(chars[0][0]);
	}

	/* Checks that sources properly taint object array arguments passed to them. */
	@Test
	public void testSourceTaintsStringArg() throws Exception {
		String[] arr = new String[]{null, "Test2"};
		sourceWithStringArrParam(arr);
		assertNonNullTaint(arr[0]);
	}

	public static int trap(int in){
		return in;
	}

	@Test(expected = TaintSinkError.class)
	public void testIntSink() throws Exception {
		trap(5);
		System.out.println("Hi!");
		sink(iSource());
	}
	
	@Test(expected = TaintSinkError.class)
	public void testStringSink() throws Exception {
		sink(source());
	}

	@Test
	public void testURITaintThrough() throws Exception {
		URI uri = new URI("http://java.sun.com/j2se/1.3/");
		uri = MultiTainter.taintedReference(uri, Taint.withLabel("testURITaintThrough"));
		String path = uri.getPath();
		assertNonNullTaint(path);
	}

	@Test
	public void testSocketChannelTaintThrough() throws Exception {
		SocketChannel channel = SocketChannel.open();
		channel = MultiTainter.taintedReference(channel, Taint.withLabel("testSocketChannelTaintThrough"));
		boolean connected = channel.isConnected();
		assertNonNullTaint(MultiTainter.getTaint(connected));
	}
}
