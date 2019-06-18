package edu.columbia.cs.psl.test.phosphor;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;
import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		float f;

		int[] ia = new int[4];
		long[] ja = new long[4];
		boolean[] za = new boolean[4];
		short[] sa = new short[4];
		double[] da = new double[4];
		byte[] ba = new byte[4];
		char[] ca = new char[4];
		float[] fa = new float[4];
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
		public boolean[][] boolsArr;

		@SuppressWarnings("unused")
		public ConstructorHolder(boolean bool) {
			this.bool = bool;
		}

		@SuppressWarnings("unused")
		public ConstructorHolder(boolean[] bools) {
			this.bools = bools;
		}

		@SuppressWarnings("unused")
		public ConstructorHolder(boolean[] bools, boolean bool) {
			this.bool = bool;
			this.bools = bools;
		}

		@SuppressWarnings("unused")
		public ConstructorHolder(boolean[][] boolsArr, boolean[] bools, boolean bool) {
			this.bool = bool;
			this.bools = bools;
			this.boolsArr = boolsArr;
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

		assertEquals(MultiTainter.getTaint(arr[0]).getLabels()[0], MultiTainter.getTaint(ret).getLabels()[0]);
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
		boolean z = MultiTainter.taintedBoolean(true, "PrimArgLabel");
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
		Object result = method.invoke(holder, (Object) arr);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(arr.length, i);
	}

	@Test
	public void testInvokeMethodTaintedPrimitiveArrArg() throws Exception {
		MethodHolder holder = new MethodHolder(true);
		Method method = MethodHolder.class.getMethod("primitiveArrParamMethod", Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, "PrimArgLabel");
		boolean[] arr = new boolean[] {z, z};
		Object result = method.invoke(holder, (Object) arr);
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

	@Test
	public void testGetFieldsHidesPhosphorFields() {
		Field[] fields = FieldHolder.class.getFields();
		assertEquals("Expected FieldHolder to have no public fields.", 0, fields.length);
	}

	@Test
	public void testGetDeclaredFieldsHidesPhosphorFields() {
		Field[] fields = FieldHolder.class.getDeclaredFields();
		List<String> expectedNames = Arrays.asList("i", "j", "z", "s", "d", "b", "c", "f", "ia", "ja", "za", "sa", "da", "ba", "ca", "fa");
		List<String> actualNames = new ArrayList<>();
		for(Field field : fields) {
			actualNames.add(field.getName());
		}
		assertTrue(String.format("Expected FieldHolder to have declared fields %s, but got %s", expectedNames, actualNames),
				expectedNames.size() == actualNames.size() && expectedNames.containsAll(actualNames) && actualNames.containsAll(expectedNames));
	}

	@Test
	public void testGetTaintedPrimitiveField() throws Exception {
		// Get the primitive Field objects
		Field intField = FieldHolder.class.getDeclaredField("i");
		Field longField = FieldHolder.class.getDeclaredField("j");
		Field booleanField = FieldHolder.class.getDeclaredField("z");
		Field shortField = FieldHolder.class.getDeclaredField("s");
		Field doubleField = FieldHolder.class.getDeclaredField("d");
		Field byteField = FieldHolder.class.getDeclaredField("b");
		Field charField = FieldHolder.class.getDeclaredField("c");
		Field floatField = FieldHolder.class.getDeclaredField("f");
		// Create the holder and set its primitive fields to tainted values
		FieldHolder holder = new FieldHolder();
		holder.i = MultiTainter.taintedInt(5, "int-field");
		holder.j = MultiTainter.taintedLong(44, "long-field");
		holder.z = MultiTainter.taintedBoolean(true, "bool-field");
		holder.s = MultiTainter.taintedShort((short)5, "short-field");
		holder.d = MultiTainter.taintedDouble(4.5, "double-field");
		holder.b = MultiTainter.taintedByte((byte)4, "byte-field");
		holder.c = MultiTainter.taintedChar('w', "char-field");
		holder.f = MultiTainter.taintedFloat(3.3f, "float-field");
		// Access the primitive fields via Field.get and check that the resulting object is tainted
		assertNonNullTaint(MultiTainter.getTaint(intField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(longField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(booleanField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(shortField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(doubleField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(byteField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(charField.get(holder)));
		assertNonNullTaint(MultiTainter.getTaint(floatField.get(holder)));
	}

	/* Checks that phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter. */
	@Test
	public void testPrimitiveConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{Boolean.TYPE}, cons.getParameterTypes());
	}

	/* Checks that phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter and a primitive array parameter. */
	@Test
	public void testPrimitiveArrayConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(boolean[].class, Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{boolean[].class, Boolean.TYPE}, cons.getParameterTypes());
	}

	/* Checks that phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter, a primitive array parameter, and primitive 2D array parameter. */
	@Test
	public void testPrimitive2DArrayConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(boolean[][].class, boolean[].class, Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{boolean[][].class, boolean[].class, Boolean.TYPE}, cons.getParameterTypes());
	}

	/* Checks that phosphor added equals and hashcode methods are hidden from Class.getDeclaredMethods. */
	@Test
	public void testHashCodeAndEqualsHiddenFromGetDeclaredMethods() {
		String[] methodNames = new String[]{"primitiveParamMethod", "primitiveArrParamMethod"};
		Class<?>[] returnTypes = new Class<?>[]{Integer.TYPE, Integer.TYPE};
		Class<?>[][] paramTypes = new Class<?>[][]{
				new Class<?>[]{Boolean.TYPE},
				new Class<?>[]{boolean[].class},
		};
		Method[] methods = MethodHolder.class.getDeclaredMethods();
		assertEquals(2, methods.length);
		for(Method method : methods) {
			boolean methodMatchesExpected = false;
			for(int i = 0; i < methodNames.length; i++) {
				if(method.getName().equals(methodNames[i]) && method.getReturnType().equals(returnTypes[i])
					&& Arrays.equals(method.getParameterTypes(), paramTypes[i])) {
					methodMatchesExpected = true;
					break;
				}
			}
			assertTrue(methodMatchesExpected);
		}
	}
}
