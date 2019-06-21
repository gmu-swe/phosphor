package edu.columbia.cs.psl.test.phosphor;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;
import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.columbia.cs.psl.phosphor.struct.LazyCharArrayObjTags;
import org.junit.ClassRule;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.rules.ExternalResource;
import sun.misc.Unsafe;

public class ReflectionObjTagITCase extends BasePhosphorTest {

	private static Unsafe unsafe;

	@ClassRule
	public static ExternalResource resource= new ExternalResource() {
		@Override
		protected void before() throws Throwable {
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			unsafe  = (Unsafe)unsafeField.get(null);
		}
	};

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
	public void testArraysSet() {
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
		// Access the holder instance's primitive fields via Field.get and check that the resulting object is tainted
		Field[] primitiveFields = getFieldHolderPrimitiveFields();
		for(Field primitiveField : primitiveFields) {
			assertNonNullTaint(MultiTainter.getTaint(primitiveField.get(holder)));
		}
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

	/* Checks that that using Unsafe.put to update the value of a primitive field also updates the taint tag
	 * for the field. */
	@Test
	public void testUnsafePutTaintedPrimitiveField() throws Exception {
		// Create the holder and tainted values
		FieldHolder holder = new FieldHolder();
		Field[] primitiveFields = getFieldHolderPrimitiveFields();
		int i = MultiTainter.taintedInt(5, "int-field");
		long j  = MultiTainter.taintedLong(44, "long-field");
		boolean z = MultiTainter.taintedBoolean(true, "bool-field");
		short s = MultiTainter.taintedShort((short)5, "short-field");
		double d = MultiTainter.taintedDouble(4.5, "double-field");
		byte b = MultiTainter.taintedByte((byte)4, "byte-field");
		char c = MultiTainter.taintedChar('w', "char-field");
		float f = MultiTainter.taintedFloat(3.3f, "float-field");
		// Set the primitive fields using unsafe
		unsafe.putInt(holder, unsafe.objectFieldOffset(primitiveFields[0]), i);
		unsafe.putLong(holder, unsafe.objectFieldOffset(primitiveFields[1]), j);
		unsafe.putBoolean(holder, unsafe.objectFieldOffset(primitiveFields[2]), z);
		unsafe.putShort(holder, unsafe.objectFieldOffset(primitiveFields[3]), s);
		unsafe.putDouble(holder, unsafe.objectFieldOffset(primitiveFields[4]), d);
		unsafe.putByte(holder, unsafe.objectFieldOffset(primitiveFields[5]), b);
		unsafe.putChar(holder, unsafe.objectFieldOffset(primitiveFields[6]), c);
		unsafe.putFloat(holder, unsafe.objectFieldOffset(primitiveFields[7]), f);
		// Check that the primitive fields of the holder instance are tainted
		assertNonNullTaint(MultiTainter.getTaint(holder.i));
		assertNonNullTaint(MultiTainter.getTaint(holder.j));
		assertNonNullTaint(MultiTainter.getTaint(holder.z));
		assertNonNullTaint(MultiTainter.getTaint(holder.s));
		assertNonNullTaint(MultiTainter.getTaint(holder.d));
		assertNonNullTaint(MultiTainter.getTaint(holder.b));
		assertNonNullTaint(MultiTainter.getTaint(holder.c));
		assertNonNullTaint(MultiTainter.getTaint(holder.f));
	}

	/* Checks that that using Unsafe.putObject to update the value of a primitive array field also updates the taint tags
	 * for the field. */
	@Test
	public void testUnsafePutTaintedPrimitiveArrayField() throws Exception {
		// Create the holder and tainted values
		FieldHolder holder = new FieldHolder();
		Field[] primitiveArrFields = getFieldHolderPrimitiveArrayFields();
		int[] ia = new int[]{MultiTainter.taintedInt(5, "int-field")};
		long[] ja  = new long[]{MultiTainter.taintedLong(44, "long-field")};
		boolean[] za = new boolean[]{MultiTainter.taintedBoolean(true, "bool-field")};
		short[] sa = new short[]{MultiTainter.taintedShort((short)5, "short-field")};
		double[] da = new double[]{MultiTainter.taintedDouble(4.5, "double-field")};
		byte[] ba = new byte[]{MultiTainter.taintedByte((byte)4, "byte-field")};
		char[] ca = new char[]{MultiTainter.taintedChar('w', "char-field")};
		float[] fa = new float[]{MultiTainter.taintedFloat(3.3f, "float-field")};
		// Set the primitive array fields using unsafe
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[0]), ia);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[1]), ja);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[2]), za);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[3]), sa);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[4]), da);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[5]), ba);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[6]), ca);
		unsafe.putObject(holder, unsafe.objectFieldOffset(primitiveArrFields[7]), fa);
		// Check that the primitive array fields' first element is tainted
		assertNonNullTaint(MultiTainter.getTaint(holder.ia[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.ja[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.za[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.sa[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.da[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.ba[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.ca[0]));
		assertNonNullTaint(MultiTainter.getTaint(holder.fa[0]));
	}

	/* Checks that that using Unsafe.getto get the value of a tainted primitive field returns a tainted primitive. */
	@Test
	public void testUnsafeGetTaintedPrimitiveField() throws Exception {
		// Create the holder and tainted values
		FieldHolder holder = new FieldHolder();
		holder.i = MultiTainter.taintedInt(5, "int-field");
		holder.j = MultiTainter.taintedLong(44, "long-field");
		holder.z = MultiTainter.taintedBoolean(true, "bool-field");
		holder.s = MultiTainter.taintedShort((short)5, "short-field");
		holder.d = MultiTainter.taintedDouble(4.5, "double-field");
		holder.b = MultiTainter.taintedByte((byte)4, "byte-field");
		holder.c = MultiTainter.taintedChar('w', "char-field");
		holder.f = MultiTainter.taintedFloat(3.3f, "float-field");
		// Get the primitive fields using unsafe
		Field[] primitiveFields = getFieldHolderPrimitiveFields();
		int i = unsafe.getInt(holder, unsafe.objectFieldOffset(primitiveFields[0]));
		long j = unsafe.getLong(holder, unsafe.objectFieldOffset(primitiveFields[1]));
		boolean z = unsafe.getBoolean(holder, unsafe.objectFieldOffset(primitiveFields[2]));
		short s = unsafe.getShort(holder, unsafe.objectFieldOffset(primitiveFields[3]));
		double d = unsafe.getDouble(holder, unsafe.objectFieldOffset(primitiveFields[4]));
		byte b = unsafe.getByte(holder, unsafe.objectFieldOffset(primitiveFields[5]));
		char c = unsafe.getChar(holder, unsafe.objectFieldOffset(primitiveFields[6]));
		float f = unsafe.getFloat(holder, unsafe.objectFieldOffset(primitiveFields[7]));
		// Check that the primitives are tainted
		assertNonNullTaint(MultiTainter.getTaint(i));
		assertNonNullTaint(MultiTainter.getTaint(j));
		assertNonNullTaint(MultiTainter.getTaint(z));
		assertNonNullTaint(MultiTainter.getTaint(s));
		assertNonNullTaint(MultiTainter.getTaint(d));
		assertNonNullTaint(MultiTainter.getTaint(b));
		assertNonNullTaint(MultiTainter.getTaint(c));
		assertNonNullTaint(MultiTainter.getTaint(f));
	}

	/* Checks that that using Unsafe.getObject to update the value of a primitive array field also updates the taint tags
	 * for the field. */
	@Test
	public void testUnsafeGetTaintedPrimitiveArrayField() throws Exception {
		// Create the holder and tainted values
		FieldHolder holder = new FieldHolder();
		Field[] primitiveArrFields = getFieldHolderPrimitiveArrayFields();
		holder.ia = new int[]{MultiTainter.taintedInt(5, "int-field")};
		holder.ja  = new long[]{MultiTainter.taintedLong(44, "long-field")};
		holder.za = new boolean[]{MultiTainter.taintedBoolean(true, "bool-field")};
		holder.sa = new short[]{MultiTainter.taintedShort((short)5, "short-field")};
		holder.da = new double[]{MultiTainter.taintedDouble(4.5, "double-field")};
		holder.ba = new byte[]{MultiTainter.taintedByte((byte)4, "byte-field")};
		holder.ca = new char[]{MultiTainter.taintedChar('w', "char-field")};
		holder.fa = new float[]{MultiTainter.taintedFloat(3.3f, "float-field")};
		// Get the primitive array fields using unsafe and check that they are tainted
		for(Field field : primitiveArrFields) {
			assertNonNullTaint(MultiTainter.getTaint(Array.get(unsafe.getObject(holder, unsafe.objectFieldOffset(field)), 0)));
		}
	}

	/* Returns the primitive fields for the class FieldHolder. */
	private static Field[] getFieldHolderPrimitiveFields() throws NoSuchFieldException {
		return new Field[] {
				FieldHolder.class.getDeclaredField("i"),
				FieldHolder.class.getDeclaredField("j"),
				FieldHolder.class.getDeclaredField("z"),
				FieldHolder.class.getDeclaredField("s"),
				FieldHolder.class.getDeclaredField("d"),
				FieldHolder.class.getDeclaredField("b"),
				FieldHolder.class.getDeclaredField("c"),
				FieldHolder.class.getDeclaredField("f")
		};
	}

	/* Returns the primitive array fields for the class FieldHolder. */
	private static Field[] getFieldHolderPrimitiveArrayFields() throws NoSuchFieldException {
		return new Field[] {
				FieldHolder.class.getDeclaredField("ia"),
				FieldHolder.class.getDeclaredField("ja"),
				FieldHolder.class.getDeclaredField("za"),
				FieldHolder.class.getDeclaredField("sa"),
				FieldHolder.class.getDeclaredField("da"),
				FieldHolder.class.getDeclaredField("ba"),
				FieldHolder.class.getDeclaredField("ca"),
				FieldHolder.class.getDeclaredField("fa")
		};
	}
}
