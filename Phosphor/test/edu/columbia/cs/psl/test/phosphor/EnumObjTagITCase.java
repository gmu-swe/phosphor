package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class EnumObjTagITCase extends BasePhosphorTest {
	@Test
	public void testEnumFlow() throws Exception {
		String s = "abcd";
		MultiTainter.taintedObject(s, new Taint("foo"));
		assertEquals("foo", MultiTainter.getTaint(s).lbl);
		Dummy x = Dummy.a;
		Dummy y = Dummy.valueOf(s);
		assertEquals("foo", MultiTainter.getTaint(y).lbl);
		assertSame(y, Dummy.abcd);
	}
	@Test
	public void testArrayCmpEq() throws Exception{
		int[] a = new int[5];
		Object f = a;
		assertSame(f, a);
	}
	enum Dummy {
		a("A"),b("B"),c("C"),d("D"),abcd("ABCD");
		String val;
		Dummy(String s)
		{
			this.val = s;
		}
	}
}
