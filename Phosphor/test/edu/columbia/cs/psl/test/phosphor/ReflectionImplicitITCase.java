package edu.columbia.cs.psl.test.phosphor;

import static edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass.assertNonNullTaint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.PreMain;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.ReflectionObjTagITCase.MethodHolder;

public class ReflectionImplicitITCase extends BasePhosphorTest {

	@Test
	public void testIntStreamsDontCrash() throws Exception {
		int sum = IntStream.of(1,2,3,4,5).sum(); //creates a bunch of lambdas
	}

	int[][] multiDArray;
	@Test
	public void testMultiDArrayAssignableFrom() throws Exception {
		Field f = ReflectionImplicitITCase.class.getDeclaredField("multiDArray");
		Object z = new int[5][6];
		assertTrue(f.getType().isAssignableFrom(z.getClass()));
	}
	@Test
	public void testReflectionDoesntCrash() {
		try {
			for (Class<?> c : PreMain.getInstrumentation().getAllLoadedClasses()) {
				Set<Field> allFields = new HashSet<Field>();
				try {
					Field[] declaredFields = c.getDeclaredFields();
					Field[] fields = c.getFields();
					allFields.addAll(Arrays.asList(declaredFields));
					allFields.addAll(Arrays.asList(fields));
				} catch (NoClassDefFoundError e) {
					continue;
				}

				for (Field f : allFields) {
					if ((Modifier.isStatic(f.getModifiers())) && !((Modifier.isFinal(f.getModifiers())) && (f.getType().isPrimitive()))) {
						Object ret = f.get(null);
						if (!f.getType().isArray() && !f.getType().isPrimitive() && ret != null) {
							visit(f, ret);
						}
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	private void visit(Field _f, Object o) throws IllegalArgumentException, IllegalAccessException {
		Object z = _f.get(o);
		if (z == null)
			return;
		Class c = z.getClass();
		Set<Field> allFields = new HashSet<Field>();
		try {
			Field[] declaredFields = c.getDeclaredFields();
			Field[] fields = c.getFields();
			allFields.addAll(Arrays.asList(declaredFields));
			allFields.addAll(Arrays.asList(fields));
		} catch (NoClassDefFoundError e) {
			return;
		}

		for (Field f : allFields) {
			if ((Modifier.isStatic(f.getModifiers())) && !((Modifier.isFinal(f.getModifiers())) && (f.getType().isPrimitive()))) {
				Object ret = f.get(null);
			}
		}
	}

	static class FieldHolder{
		int i;
		long j;
		boolean z;
		short s;
		double d;
		byte b;
		char c;

		int[] ia = new int[4];
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
		ReflectionObjTagITCase.MethodHolder holder = new MethodHolder(false);
		Method method = ReflectionObjTagITCase.MethodHolder.class.getMethod("primitiveParamMethod", Boolean.TYPE);
		boolean z = true;
		Object result = method.invoke(holder, z);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(2, i);
	}

	@Test
	public void testInvokeMethodTaintedPrimitiveArg() throws Exception {
		ReflectionObjTagITCase.MethodHolder holder = new MethodHolder(true);
		Method method = ReflectionObjTagITCase.MethodHolder.class.getMethod("primitiveParamMethod", Boolean.TYPE);
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		Object result = method.invoke(holder, z);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(2, i);
	}

	@Test
	public void testInvokeMethodPrimitiveArrArg() throws Exception {
		ReflectionObjTagITCase.MethodHolder holder = new MethodHolder(false);
		Method method = ReflectionObjTagITCase.MethodHolder.class.getMethod("primitiveArrParamMethod", Class.forName("[Z"));
		boolean[] arr = new boolean[] {true, true};
		Object result = method.invoke(holder, arr);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(arr.length, i);
	}

	@Test
	public void testInvokeMethodTaintedPrimitiveArrArg() throws Exception {
		ReflectionObjTagITCase.MethodHolder holder = new MethodHolder(true);
		Method method = ReflectionObjTagITCase.MethodHolder.class.getMethod("primitiveArrParamMethod", Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		boolean[] arr = new boolean[] {z, z};
		Object result = method.invoke(holder, arr);
		assertTrue("Expected integer return from reflected method.", result instanceof Integer);
		int i = (Integer)result;
		assertEquals(arr.length, i);
	}

	@Test
	public void testNewInstanceConstructorPrimitiveArg() throws Exception {
		Constructor<ReflectionObjTagITCase.ConstructorHolder> cons = ReflectionObjTagITCase.ConstructorHolder.class.getConstructor(Boolean.TYPE);
		boolean z = true;
		ReflectionObjTagITCase.ConstructorHolder instance = cons.newInstance(z);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bool);
	}

	@Test
	public void testNewInstanceConstructorTaintedPrimitiveArg() throws Exception {
		Constructor<ReflectionObjTagITCase.ConstructorHolder> cons = ReflectionObjTagITCase.ConstructorHolder.class.getConstructor(Boolean.TYPE);
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		ReflectionObjTagITCase.ConstructorHolder instance = cons.newInstance(z);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bool);
		assertNonNullTaint(MultiTainter.getTaint(instance.bool));
	}

	@Test
	public void testNewInstanceConstructorPrimitiveArrArg() throws Exception {
		Constructor<ReflectionObjTagITCase.ConstructorHolder> cons = ReflectionObjTagITCase.ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean[] arr = new boolean[] {true, true};
		ReflectionObjTagITCase.ConstructorHolder instance = cons.newInstance((Object)arr);
		assertNotNull(instance.bools);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bools[0]);
	}

	@Test
	public void testNewInstanceConstructorTaintedPrimitiveArrArg() throws Exception {
		Constructor<ReflectionObjTagITCase.ConstructorHolder> cons = ReflectionObjTagITCase.ConstructorHolder.class.getConstructor(Class.forName("[Z"));
		boolean z = MultiTainter.taintedBoolean(true, new Taint<>("PrimArgLabel"));
		boolean[] arr = new boolean[] {z, z};
		ReflectionObjTagITCase.ConstructorHolder instance = cons.newInstance((Object)arr);
		assertNotNull(instance.bools);
		assertTrue("Expected new instance from reflected constructor to have its field set.", instance.bools[0]);
		assertNonNullTaint(MultiTainter.getTaint(instance.bools[0]));
	}
}
