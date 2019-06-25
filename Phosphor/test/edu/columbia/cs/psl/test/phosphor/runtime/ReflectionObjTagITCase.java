package edu.columbia.cs.psl.test.phosphor.runtime;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ReflectionObjTagITCase extends BaseMultiTaintClass {

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
	public void testBoxing() {
		ArrayList<Integer> list = new ArrayList<>();
		for(int i = 0; i < 5; i++) {
			i = MultiTainter.taintedInt(i, "collection");
			list.add(i);
		}
		int val = 18;
		val = MultiTainter.taintedInt(val, "int");
		int newVal = list.get(0) + val;
		list.add(newVal);
		for(int i = 0; i < list.size(); i++) {
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
		assertTrue(String.format("Expected declared fields %s, but got %s", expectedNames, actualNames),
				expectedNames.size() == actualNames.size() && expectedNames.containsAll(actualNames) && actualNames.containsAll(expectedNames));
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

	/* Checks that phosphor added equals and hashcode methods are replaced by Object.equals and Object.hashCode for
	 * Class.getMethods. */
	@Test
	public void testHashCodeAndEqualsReplacedInGetMethods() throws NoSuchMethodException {
		HashSet<Method> expected = new HashSet<>();
		expected.add(MethodHolder.class.getDeclaredMethod("primitiveParamMethod", Boolean.TYPE));
		expected.add(MethodHolder.class.getDeclaredMethod("primitiveArrParamMethod", boolean[].class));
		expected.addAll(Arrays.asList(Object.class.getMethods()));
		Method[] methods = MethodHolder.class.getMethods();
		HashSet<Method> actual = new HashSet<>(Arrays.asList(methods));
		assertEquals(expected, actual);
	}

	/* Checks that parameters passed to getDeclaredConstructor and newInstance for ignored classes like Boolean are not
	 * remapped. */
	@Test
	public void testBooleanIgnoredConstructor() throws Exception {
		Constructor<Boolean> constructor = Boolean.class.getDeclaredConstructor(boolean.class);
		Boolean result = constructor.newInstance(false);
		assertFalse(result);
	}

	/* Checks that primitive array parameters passed to getDeclaredConstructor and newInstance for ignored classes
	 * like LazyCharArrayObjTags are not remapped. */
	@Test
	public void testPrimitiveArrayIgnoredConstructor() throws Exception {
		Constructor<LazyCharArrayObjTags> constructor = LazyCharArrayObjTags.class.getDeclaredConstructor(Taint.class, char[].class);
		Object result = constructor.newInstance(null, "testPrimitiveArrayIgnoredConstructor".toCharArray());
		assertNotNull(result);
	}

	/* Checks that parameters passed to getDeclaredMethod and invoke for ignored classes like Boolean are not remapped. */
	@Test
	public void testBooleanIgnoredMethod() throws Exception {
		Method method = Boolean.class.getDeclaredMethod("toString", boolean.class);
		String result = (String)method.invoke(null, false);
		assertNotNull(result);
	}
}
