package edu.columbia.cs.psl.test.phosphor.runtime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.PreMain;

public class ReflectionImplicitITCase extends ReflectionObjTagITCase{

	@Test
	public void testIntStreamsDontCrash() throws Exception {
		int sum = IntStream.of(1, 2, 3, 4, 5).sum(); //creates a bunch of lambdas
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
}
