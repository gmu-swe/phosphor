package edu.columbia.cs.psl.test.phosphor.runtime;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.columbia.cs.psl.phosphor.struct.TaggedCharArray;
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

		public int last;

		public ConstructorHolder(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9,
								 int a10, int a11, int a12, int a13, int a14, int a15, int a16, int a17, int a18,
								 int a19, int a20, int a21, int a22, int a23, int a24, int a25, int a26, int a27,
								 int a28, int a29, int a30, int a31, int a32, int a33, int a34, int a35, int a36,
								 int a37, int a38, int a39) {
			last = a39;
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
			assertTrue(objTaint.isSuperset(valTaint));
			assertTrue(valTaint.isSuperset(objTaint));
		}
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
		boolean z = MultiTainter.taintedBoolean(true, Taint.withLabel("PrimArgLabel"));
		ConstructorHolder instance = cons.newInstance(z);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bool);
		assertNonNullTaint(MultiTainter.getTaint(instance.bool));
	}

	@Test
	public void testNewInstanceConstructorPrimitiveArrArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean[] arr = new boolean[] {true, true};
		ConstructorHolder instance = cons.newInstance(arr);
		assertNotNull(instance.bools);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bools[0]);
	}

	@Test
	public void testNewInstanceConstructorTaintedPrimitiveArrArg() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, Taint.withLabel("PrimArgLabel"));
		boolean[] arr = new boolean[] {z, z};
		ConstructorHolder instance = cons.newInstance(arr);
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

	/* Checks that Phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter. */
	@Test
	public void testPrimitiveConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{Boolean.TYPE}, cons.getParameterTypes());
	}

	/* Checks that Phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter and a primitive array parameter. */
	@Test
	public void testPrimitiveArrayConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(boolean[].class, Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{boolean[].class, Boolean.TYPE}, cons.getParameterTypes());
	}

	/* Checks that Phosphor parameters are correctly remapped when calling Constructor.getParameterTypes for a Constructor
	 * with a primitive parameter, a primitive array parameter, and primitive 2D array parameter. */
	@Test
	public void testPrimitive2DArrayConstructorGetParamTypes() throws Exception {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(boolean[][].class, boolean[].class, Boolean.TYPE);
		assertArrayEquals(new Class<?>[]{boolean[][].class, boolean[].class, Boolean.TYPE}, cons.getParameterTypes());
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
		Constructor<TaggedCharArray> constructor = TaggedCharArray.class.getDeclaredConstructor(Taint.class, char[].class);
		Object result = constructor.newInstance(null, "testPrimitiveArrayIgnoredConstructor".toCharArray());
		assertNotNull(result);
	}

	@Test
	public void testReflectionConstructorWithoutTaint() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Constructor<ConstructorHolder> constructor = ConstructorHolder.class.getConstructor(int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class);
		ConstructorHolder obj2 = constructor.newInstance(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
				15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39);
		assertNotNull(obj2);
	}

	@Test
	public void testReflectionConstructorTaintPropagation() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Constructor<ConstructorHolder> constructor = ConstructorHolder.class.getConstructor(int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
				int.class, int.class, int.class);
		int i39 = MultiTainter.taintedInt(39, "tainted");
		ConstructorHolder obj2 = constructor.newInstance(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
				15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, i39);
		assertTaintHasOnlyLabel(MultiTainter.getTaint(obj2.last), "tainted");
	}

	public static void main(String[] args) throws Throwable {
		Constructor<ConstructorHolder> cons = ConstructorHolder.class.getConstructor(Boolean.TYPE);
		boolean z = MultiTainter.taintedBoolean(true, Taint.withLabel("PrimArgLabel"));
		ConstructorHolder instance = cons.newInstance(z);
		System.out.println(MultiTainter.getTaint(instance.bool));
	}
}
