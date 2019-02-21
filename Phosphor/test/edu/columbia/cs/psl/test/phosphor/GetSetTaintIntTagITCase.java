package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithIntTag;

public class GetSetTaintIntTagITCase extends BasePhosphorTest {
	
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
	
	@Test
	public void testValueOf()
	{
		String hundred = new String(new char[]{'1','0','0'});
		String TRUE = new String(new char[]{'t','r','u','e'});
		((TaintedWithIntTag)((Object)hundred)).setPHOSPHOR_TAG(5);
		((TaintedWithIntTag)((Object)TRUE)).setPHOSPHOR_TAG(5);

		boolean z = Boolean.parseBoolean(TRUE);
		byte b = Byte.valueOf(hundred);
		byte b2 = Byte.parseByte(hundred);
		byte b3 = Byte.parseByte(hundred, 10);
		int i = Integer.valueOf(hundred);
		int i2 = Integer.valueOf(hundred, 10);
		int i3 = Integer.parseInt(hundred);
		int i4 = Integer.parseInt(hundred,10);
		int i5 = Integer.parseUnsignedInt(hundred);
		int i6 = Integer.parseUnsignedInt(hundred, 10);
		short s = Short.parseShort(hundred);
		short s2 = Short.parseShort(hundred, 10);
		short s3 = Short.valueOf(hundred);
		short s4 = Short.valueOf(hundred, 10);
		long l = Long.valueOf(hundred);
		long l2 = Long.valueOf(hundred, 10);
		long l3 = Long.parseLong(hundred);
		long l4 = Long.parseLong(hundred, 10);
		long l5 = Long.parseUnsignedLong(hundred);
		long l6 = Long.parseUnsignedLong(hundred, 10);
		float f = Float.parseFloat(hundred);
		float f2 = Float.valueOf(hundred);
		double d = Double.parseDouble(hundred);
		double d2 = Double.valueOf(hundred);
		assertTrue(Tainter.getTaint(z) == 5);
		assertTrue(Tainter.getTaint(b) == 5);
		assertTrue(Tainter.getTaint(b2) == 5);
		assertTrue(Tainter.getTaint(b3) == 5);
		assertTrue(Tainter.getTaint(i) == 5);
		assertTrue(Tainter.getTaint(i2) == 5);
		assertTrue(Tainter.getTaint(i3) == 5);
		assertTrue(Tainter.getTaint(i4) == 5);
		assertTrue(Tainter.getTaint(i5) == 5);
		assertTrue(Tainter.getTaint(i6) == 5);
		assertTrue(Tainter.getTaint(s) == 5);
		assertTrue(Tainter.getTaint(s2) == 5);
		assertTrue(Tainter.getTaint(s3) == 5);
		assertTrue(Tainter.getTaint(s4) == 5);
		assertTrue(Tainter.getTaint(l) == 5);
		assertTrue(Tainter.getTaint(l2) == 5);
		assertTrue(Tainter.getTaint(l3) == 5);
		assertTrue(Tainter.getTaint(l4) == 5);
		assertTrue(Tainter.getTaint(l5) == 5);
		assertTrue(Tainter.getTaint(l6) == 5);
		assertTrue(Tainter.getTaint(f) == 5);
		assertTrue(Tainter.getTaint(f2) == 5);
		assertTrue(Tainter.getTaint(d) == 5);
		assertTrue(Tainter.getTaint(d2) == 5);
	}
	
}
