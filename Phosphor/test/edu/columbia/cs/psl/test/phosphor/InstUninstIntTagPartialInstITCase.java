package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class InstUninstIntTagPartialInstITCase extends BasePhosphorTest {

	@Test
	public void testInstWorks() throws Exception {
		instrumented();
		unInstrumented();
	}
	@Test
	public void testReflectionInstrumented() throws Exception {
		Class c = PartiallyInstrumentedClass.class;
		assertEquals(5, c.getDeclaredMethods().length);
		assertEquals(12, c.getMethods().length); //wait, getClass, etc
		Method m = c.getDeclaredMethod("uninst");
		m.invoke(null);
		Method m2 = c.getDeclaredMethod("inst",Integer.TYPE);
		m2.invoke(null, 5);

		int[] a = (int[]) ((Object[])ar1)[0];
		assertEquals(3, a.length);
		a = (int[]) ((Object[])ar2)[0];
		assertEquals(3, a.length);
		
		char[] d = ar3[0];
		assertEquals(20, d.length);
		d = ar4[0];
		assertEquals(20, d.length);
		System.out.println(Arrays.toString(c.getDeclaredConstructors()));
		System.out.println(Arrays.toString(c.getDeclaredConstructors()));

	}
	
	private void inst(Object o, int k)
	{
		char[] ar = (char[]) o;
	}
	@Test
	public void testReflectionUnInstrumented() throws Exception {
		Class c = PartiallyInstrumentedClass.class;
		assertEquals(5, c.getDeclaredMethods().length);
		assertEquals(12, c.getMethods().length);
		Method m = c.getDeclaredMethod("uninst");
		m.invoke(null);
		Method m2 = c.getDeclaredMethod("inst",Integer.TYPE);
		m2.invoke(null, 5);
		System.out.println(Arrays.toString(c.getDeclaredConstructors()));
		System.out.println(Arrays.toString(c.getConstructors()));

		c = InstUninstIntTagPartialInstITCase.class;
		m = c.getDeclaredMethod("unInstrumented");
		m.invoke(this);
		m = c.getDeclaredMethod("uninst2", char[][].class);
		m.invoke(this, new Object[]{ar3});
		int[] a = (int[]) ((Object[])ar1)[0];
		assertEquals(3, a.length);
		a = (int[]) ((Object[])ar2)[0];
		assertEquals(3, a.length);
		
		char[] d = ar3[0];
		assertEquals(20, d.length);
		d = ar4[0];
		assertEquals(20, d.length);
		System.out.println(new PartiallyInstrumentedClass().hashCode());
		uninst2(ar4);
	}
	
	private static void beforeUninst()
	{
		ar1 = new Object[]{new int[3],"abcd"};
		ar3 = new char[4][5];
		ar3[0] = new char[20];
		ar4[0] = new char[20];

	}
	private static void beforeInst()
	{
		ar2 = new Object[]{new int[3],"abcd"};
		ar4 = new char[4][5];
	}
	@BeforeClass
	public static void beforeClass()
	{
		beforeInst();
		beforeUninst();
	}
	private static Object ar1;
	private static Object ar2;
	private static char[][] ar3;
	private static char[][] ar4;
	private void instrumented() throws Exception
	{
		int i = Tainter.taintedInt(5, 4);
		assertEquals(4, Tainter.getTaint(i));
	}
	private void uninst2(char[][] in)
	{
		ar3 = in;
	}
	private void unInstrumented() throws Exception
	{
		System.out.println("Running uninstrumented");
		int i = Tainter.taintedInt(5, 4);
		assertEquals(0, Tainter.getTaint(i));
		Object o = new String();
		if(i == 5)
			o = new char[4];
		inst(o,5);
	}
}
