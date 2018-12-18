package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ReflectionObjTagITCase {
	
	static class FieldHolder{
		int i;
		long j;
		boolean z;
		short s;
		double d;
		byte b;
		char c;
		
		int[] ia = new int[4];
		long[] ja = new long[4];
		boolean[] za = new boolean[4];
		short[] sa = new short[4];
		double[] da = new double[4];
		byte[] ba = new byte[4];
		char[] ca = new char[4];

	}
	
	@Test
	public void testArraysSet() throws Exception {
		boolean[] b = { false };
		if (b.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(b); i++) {
				Array.get(b, i);
			}
		}
	}
	@Test
	public void testReflectionSetField() throws Exception {
		FieldHolder fh = new FieldHolder();
		for(Field f : fh.getClass().getDeclaredFields())
		{
			if(f.getType().isPrimitive() && !Modifier.isFinal(f.getModifiers()))
			{
				if(f.getType() == int.class)
				{
					f.setInt(fh, MultiTainter.taintedInt(f.getInt(fh), "tainted_"+f.getName()));
				} else if (f.getType() == long.class) {
					f.setLong(fh, MultiTainter.taintedLong(f.getLong(fh), "tainted_" + f.getName()));
				} else if (f.getType() == boolean.class) {
					f.setBoolean(fh, MultiTainter.taintedBoolean(f.getBoolean(fh), "tainted_" + f.getName()));
				} else if (f.getType() == short.class) {
					f.setShort(fh, MultiTainter.taintedShort(f.getShort(fh), "tainted_" + f.getName()));
				} else if (f.getType() == double.class) {
					f.setDouble(fh, MultiTainter.taintedDouble(f.getDouble(fh), "tainted_" + f.getName()));
				} else if (f.getType() == byte.class) {
					f.setByte(fh, MultiTainter.taintedByte(f.getByte(fh), "tainted_" + f.getName()));
				} else if (f.getType() == char.class) {
					f.setChar(fh, MultiTainter.taintedChar(f.getChar(fh), "tainted_" + f.getName()));
				}
			} else if (f.getType().isArray()) {
				if (f.getType().getComponentType() == Integer.TYPE) {
					f.set(fh, MultiTainter.taintedIntArray((int[]) f.get(fh), "tainted_" + f.getName()));
				}
			}
		}
		assertNotNull(MultiTainter.getTaint(fh.ia[0]));

		assertNotNull(MultiTainter.getTaint(fh.i));
		assertNotNull(MultiTainter.getTaint(fh.b));
		assertNotNull(MultiTainter.getTaint(fh.c));
		assertNotNull(MultiTainter.getTaint(fh.d));
		assertNotNull(MultiTainter.getTaint(fh.j));
		assertNotNull(MultiTainter.getTaint(fh.s));
		assertNotNull(MultiTainter.getTaint(fh.z));

	}
	@Test
	public void testRefArraySet() {
		int[] arr = { 18 };
		arr = MultiTainter.taintedIntArray(arr, "arr");

		Object obj = (Object) arr;
		int ret = Array.getInt(obj, 0);
		// Exception in thread "main" java.lang.ClassCastException:
		// edu.columbia.cs.psl.phosphor.runtime.LazyArrayObjTags cannot be cast
		// to edu.columbia.cs.psl.phosphor.runtime.Taint

		assertEquals(MultiTainter.getTaint(arr[0]).lbl, MultiTainter.getTaint(ret).lbl);
	}

	@Test
	public void testBoxing() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < 5; i++) {
			i = MultiTainter.taintedInt(i, "collection");
			list.add(i);
		}

		int val = 18;

		val = MultiTainter.taintedInt(val, "int");
		int newVal = list.get(0) + val;
		list.add(newVal);

		for (int i = 0; i < list.size(); i++) {
			Integer obj = list.get(i);
			int objVal = list.get(i);

			Taint objTaint = MultiTainter.getTaint(obj); 
			Taint valTaint = MultiTainter.getTaint(objVal);
			assertTrue(objTaint.contains(valTaint));
			assertTrue(valTaint.contains(objTaint));
		}
	}
}
