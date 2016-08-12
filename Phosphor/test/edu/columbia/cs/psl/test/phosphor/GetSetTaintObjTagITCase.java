package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class GetSetTaintObjTagITCase extends BaseMultiTaintClass{
	
	@Test
	public void testReferenceType() throws Exception {
		String s = "def";
		HashMap<Object, Object> m =  new HashMap<Object, Object>();
		MultiTainter.taintedObject(s, new Taint(5));;
		MultiTainter.taintedObject(m, new Taint(5));
		
		int[] x = new int[10];
		//In its default mode, Phosphor tracks ONLY for the array ELEMENTS - not for the reference
		//can do -withArrayLengthTags to track a tag for the length of the array
		x = MultiTainter.taintedIntArray(x, new Taint(3));
		
		assertTrue(((TaintedWithObjTag)m).getPHOSPHOR_TAG() != null);
		assertTrue(MultiTainter.getTaint(m) != null);
		assertTrue(MultiTainter.getTaint(s) != null);
		assertTrue(MultiTainter.getTaint(x[0]) != null);
	}
	@Test
	public void testBoxing()
	{
		Boolean z = MultiTainter.taintedBoolean(false, new Taint(5));
		Byte b = MultiTainter.taintedByte((byte) 4, new Taint(5));
		Character c = MultiTainter.taintedChar('a', new Taint(5));
		Integer i = MultiTainter.taintedInt(4, new Taint(5));
		Short s = MultiTainter.taintedShort((short)5, new Taint(5));
		Long l = MultiTainter.taintedLong((long) 5, new Taint(5));
		Float f = MultiTainter.taintedFloat(4f, new Taint(5));
		Double d = MultiTainter.taintedDouble(4d, new Taint(5));
		

		assertTrue(MultiTainter.getTaint(z.booleanValue()) != null);
		assertTrue(MultiTainter.getTaint(b.byteValue()) != null);
		assertTrue(MultiTainter.getTaint(c.charValue()) != null);
		assertTrue(MultiTainter.getTaint(i.intValue()) != null);
		assertTrue(MultiTainter.getTaint(s.shortValue()) != null);
		assertTrue(MultiTainter.getTaint(f.floatValue()) != null);
		assertTrue(MultiTainter.getTaint(l.longValue()) != null);
		assertTrue(MultiTainter.getTaint(d.doubleValue()) != null);
	}
	@Test
	public void testNotBoxing()
	{
		boolean z = MultiTainter.taintedBoolean(false, new Taint(5));
		byte b = MultiTainter.taintedByte((byte) 4, new Taint(5));
		char c = MultiTainter.taintedChar('a', new Taint(5));
		int i = MultiTainter.taintedInt(4, new Taint(5));
		short s = MultiTainter.taintedShort((short)5, new Taint(5));
		long l = MultiTainter.taintedLong((long) 5, new Taint(5));
		float f = MultiTainter.taintedFloat(4f, new Taint(5));
		double d = MultiTainter.taintedDouble(4d, new Taint(5));
		

		assertTrue(MultiTainter.getTaint(z) != null);
		assertTrue(MultiTainter.getTaint(b) != null);
		assertTrue(MultiTainter.getTaint(c) != null);
		assertTrue(MultiTainter.getTaint(i) != null);
		assertTrue(MultiTainter.getTaint(s) != null);
		assertTrue(MultiTainter.getTaint(l) != null);
		assertTrue(MultiTainter.getTaint(f) != null);
		assertTrue(MultiTainter.getTaint(d) != null);
	}
	
	@Test
	public void testToString()
	{
		boolean z = MultiTainter.taintedBoolean(false, new Taint(5));
		byte b = MultiTainter.taintedByte((byte) 4, new Taint(5));
		char c = MultiTainter.taintedChar('a', new Taint(5));
		int i = MultiTainter.taintedInt(4, new Taint(5));
		short s = MultiTainter.taintedShort((short)5, new Taint(5));
		long l = MultiTainter.taintedLong((long) 5, new Taint(5));
		float f = MultiTainter.taintedFloat(4f, new Taint(5));
		double d = MultiTainter.taintedDouble(4d, new Taint(5));
		assertNonNullTaint(Boolean.toString(z));
		assertNonNullTaint(Byte.toString(b));
		assertNonNullTaint(Character.toString(c));
		assertNonNullTaint(Short.toString(s));
		assertNonNullTaint(Long.toString(l));
		assertNonNullTaint(Float.toString(f));
		assertNonNullTaint(Double.toString(d));
		assertNonNullTaint(Integer.toString(i));
	}	
	
}
