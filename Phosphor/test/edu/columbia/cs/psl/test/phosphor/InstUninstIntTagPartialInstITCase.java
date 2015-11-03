package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class InstUninstIntTagPartialInstITCase {

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

	}
	
	private void instrumented() throws Exception
	{
		int i = Tainter.taintedInt(5, 4);
		assertEquals(4, Tainter.getTaint(i));
	}
	private void unInstrumented() throws Exception
	{
		int i = Tainter.taintedInt(5, 4);
		assertEquals(0, Tainter.getTaint(i));
	}
}
