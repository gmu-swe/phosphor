package edu.columbia.cs.psl.test.phosphor;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;
import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ReflectionObjTagITCase extends BasePhosphorTest {

	public static class FieldHolder {

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

	public static class MethodHolder {
		// Whether methods should assert that their arguments are tainted
		boolean checkArgsTainted;

		MethodHolder(boolean checkArgsTainted) {
			this.checkArgsTainted = checkArgsTainted;
		}

		public int primitiveParamMethod(boolean taintedBool) {
			if(checkArgsTainted) {
				assertNonNullTaint(MultiTainter.getTaint(taintedBool));
			}
			return taintedBool ? 2 : 0;
		}

		public int primitiveArrParamMethod(boolean[] b) {
			if(checkArgsTainted) {
				assertNonNullTaint(MultiTainter.getTaint(b[0]));
			}
			return b == null ? 0 : b.length;
		}
	}

	public static class ConstructorHolder {
		public boolean bool;
		public boolean[] bools;
		public ConstructorHolder(boolean bool) {
			this.bool = bool;
		}

		public ConstructorHolder(boolean[] bools) {
			this.bools = bools;
		}
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

	@Test
	public void testInvokeMethodPrimitiveArg() throws Exception {
		MethodHolder holder = new MethodHolder(false);
		Method method = MethodHolder.class.getMethod("primitiveParamMethod", Boolean.TYPE);
		boolean z = true;
		Object result = method.invoke(holder, z);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(2, i);
	}

	@Test
	public void testInvokeMethodTaintedPrimitiveArg() throws Exception {
		MethodHolder holder = new MethodHolder(true);
		Method method = MethodHolder.class.getMethod("primitiveParamMethod", Boolean.TYPE);
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		Object result = method.invoke(holder, z);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(2, i);
	}

	@Test
	public void testInvokeMethodPrimitiveArrArg() throws Exception {
		MethodHolder holder = new MethodHolder(false);
		Method method = MethodHolder.class.getMethod("primitiveArrParamMethod", Class.forName("[Z"));
		boolean[] arr = new boolean[] {true, true};
		Object result = method.invoke(holder, arr);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(arr.length, i);
	}

	@Test
	public void testInvokeMethodTaintedPrimitiveArrArg() throws Exception {
		MethodHolder holder = new MethodHolder(true);
		Method method = MethodHolder.class.getMethod("primitiveArrParamMethod", Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		boolean[] arr = new boolean[] {z, z};
		Object result = method.invoke(holder, arr);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(arr.length, i);
	}

	@Test
	public void testNewInstanceConstructorPrimitiveArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Boolean.TYPE);
		boolean z = true;
		ConstructorHolder instance = cons.newInstance(z);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bool);
	}

	@Test
	public void testNewInstanceConstructorTaintedPrimitiveArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Boolean.TYPE);
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		ConstructorHolder instance = cons.newInstance(z);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bool);
		assertNonNullTaint(MultiTainter.getTaint(instance.bool));
	}

	@Test
	public void testNewInstanceConstructorPrimitiveArrArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean[] arr = new boolean[] {true, true};
		ConstructorHolder instance = cons.newInstance((Object)arr);
		assertNotNull(instance.bools);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bools[0]);
	}

	@Test
	public void testNewInstanceConstructorTaintedPrimitiveArrArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		boolean[] arr = new boolean[] {z, z};
		ConstructorHolder instance = cons.newInstance((Object)arr);
		assertNotNull(instance.bools);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bools[0]);
		assertNonNullTaint(MultiTainter.getTaint(instance.bools[0]));
	}
}
