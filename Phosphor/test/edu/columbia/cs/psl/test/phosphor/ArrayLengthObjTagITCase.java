package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ArrayLengthObjTagITCase extends BasePhosphorTest {

	@Test
	public void testArrayLengthTaintedPrimArray() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);
		int[] ar = new int[i];
		Taint r = MultiTainter.getTaint(ar.length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}

	@Test
	public void testArrayLengthTaintedObjArray() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);
		String[] ar = new String[i];
		Taint r = MultiTainter.getTaint(ar.length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}

	@Test
	public void testArrayLengthTainted2DPrimArray() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);
		int[][] ar = new int[i][i];
		Taint r = MultiTainter.getTaint(ar.length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
		r = MultiTainter.getTaint(ar[0].length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}

	@Test
	public void testArrayLengthTainted2DObjArray() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);
		String[][] ar = new String[i][i];
		Taint r = MultiTainter.getTaint(ar.length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
		r = MultiTainter.getTaint(ar[0].length);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}

	@Test
	public void testTaintedIndexLoad() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);

		int[] ar = new int[10];
		int j = ar[i];
		Taint r = MultiTainter.getTaint(ar[i]);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
		String[] s = new String[10];
		for (j = 0; j < s.length; j++)
			s[j] = "b";
		r = (Taint) MultiTainter.getTaint(s[i]);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}

	@Test
	public void testTaintedIndexStore() throws Exception {
		int i = MultiTainter.taintedInt(5, "foo");
		Taint t = MultiTainter.getTaint(i);

		int[] ar = new int[10];
		ar[i] = 10;
		Taint r = MultiTainter.getTaint(ar[5]);

		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);

		String[] s = new String[10];
		s[i] = "bar";
		r = (Taint) MultiTainter.getTaint(s[5]);
		if (Configuration.ARRAY_LENGTH_TRACKING)
			assertEquals(t.lbl, r.lbl);
		else
			assertNull(r);
	}
	
	@Test
	public void testArrayListGet() throws Exception {
		ArrayList<String> al = new ArrayList<String>();
		al.add("foo");
		int i = MultiTainter.taintedInt(0, "foo");
		System.out.println(MultiTainter.getTaint(al.get(i)));
	}
}
