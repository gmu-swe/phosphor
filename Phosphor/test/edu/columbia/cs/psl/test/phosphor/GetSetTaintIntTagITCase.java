package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;

public class GetSetTaintIntTagITCase {
	
	@Test
	public void testReferenceType() throws Exception {
		String s = "def";
		HashMap<Object, Object> m =  new HashMap<Object, Object>();
		Tainter.taintedObject(s, 5);
		Tainter.taintedObject(m, 5);
		
		int[] x = new int[10];
		//In its default mode, Phosphor tracks ONLY for the array ELEMENTS - not for the reference
		//can do -withArrayLengthTags to track a tag for the length of the array
		x = Tainter.taintedIntArray(x, 3);
		
		assertTrue(((TaintedWithIntTag)m).getPHOSPHOR_TAG() != 0);
		assertTrue(Tainter.getTaint(m) != 0);
		assertTrue(Tainter.getTaint(s) != 0);
		assertTrue(Tainter.getTaint(x[0]) != 0);
	}
	@Test
	public void testBoxing()
	{
		Boolean z = Tainter.taintedBoolean(false, 4);
		Byte b = Tainter.taintedByte((byte) 4, 4);
		Character c = Tainter.taintedChar('a', 4);
		Integer i = Tainter.taintedInt(4, 4);
		Short s = Tainter.taintedShort((short)5, 5);
		Long l = Tainter.taintedLong((long) 5, 5);
		Float f = Tainter.taintedFloat(4f, 4);
		Double d = Tainter.taintedDouble(4d, 4);
		

		assertTrue(Tainter.getTaint(z.booleanValue()) != 0);
		assertTrue(Tainter.getTaint(b.byteValue()) != 0);
		assertTrue(Tainter.getTaint(c.charValue()) != 0);
		assertTrue(Tainter.getTaint(i.intValue()) != 0);
		assertTrue(Tainter.getTaint(s.shortValue()) != 0);
		assertTrue(Tainter.getTaint(f.floatValue()) != 0);
		assertTrue(Tainter.getTaint(l.longValue()) != 0);
		assertTrue(Tainter.getTaint(d.doubleValue()) != 0);
	}
	@Test
	public void testNotBoxing()
	{
		boolean z = Tainter.taintedBoolean(false, 4);
		byte b = Tainter.taintedByte((byte) 4, 4);
		char c = Tainter.taintedChar('a', 4);
		int i = Tainter.taintedInt(4, 4);
		short s = Tainter.taintedShort((short)5, 5);
		long l = Tainter.taintedLong((long) 5, 5);
		float f = Tainter.taintedFloat(4f, 4);
		double d = Tainter.taintedDouble(4d, 4);
		

		assertTrue(Tainter.getTaint(z) != 0);
		assertTrue(Tainter.getTaint(b) != 0);
		assertTrue(Tainter.getTaint(c) != 0);
		assertTrue(Tainter.getTaint(i) != 0);
		assertTrue(Tainter.getTaint(s) != 0);
		assertTrue(Tainter.getTaint(l) != 0);
		assertTrue(Tainter.getTaint(f) != 0);
		assertTrue(Tainter.getTaint(d) != 0);
	}
	
	@Test
	public void testToString()
	{
		boolean z = Tainter.taintedBoolean(false, 5);
		byte b = Tainter.taintedByte((byte) 4, 5);
		char c = Tainter.taintedChar('a', 5);
		int i = Tainter.taintedInt(4, 5);
		short s = Tainter.taintedShort((short)5, 5);
		long l = Tainter.taintedLong((long) 5, 5);
		float f = Tainter.taintedFloat(4f, 5);
		double d = Tainter.taintedDouble(4d, 5);
		assertTrue(Tainter.getTaint(Boolean.toString(z)) != 0);
		assertTrue(Tainter.getTaint(Byte.toString(b)) != 0);
		assertTrue(Tainter.getTaint(Character.toString(c)) != 0);
		assertTrue(Tainter.getTaint(Short.toString(s)) != 0);
		assertTrue(Tainter.getTaint(Long.toString(l)) != 0);
		assertTrue(Tainter.getTaint(Float.toString(f)) != 0);
		assertTrue(Tainter.getTaint(Double.toString(d)) != 0);
		assertTrue(Tainter.getTaint(Integer.toString(i)) != 0);
	}	
	
}
