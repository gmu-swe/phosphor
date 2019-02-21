package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class EnumIntTagITCase extends BasePhosphorTest {
	@Test
	public void testEnumFlow() throws Exception {
		String s = "abcd";
		Tainter.taintedObject(s, 5);
		assertEquals(5, Tainter.getTaint(s));
		Dummy x = Dummy.a;
		Dummy y = Dummy.valueOf(s);
		assertEquals(5, Tainter.getTaint(y));
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
