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
		// MultiTainter.taintedObject(s, Taint.withLabel("foo"));
		s = MultiTainter.taintedReference(s, "foo");
		assertEquals(Taint.withLabel("foo"), MultiTainter.getTaint(s));
		Dummy x = Dummy.a;
		Dummy y = Dummy.valueOf(s);
		assertEquals(Taint.withLabel("foo"), MultiTainter.getTaint(y));
		assertSame(y, Dummy.abcd);
	}
	@Test
	public void testArrayCmpEq() throws Exception {
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
