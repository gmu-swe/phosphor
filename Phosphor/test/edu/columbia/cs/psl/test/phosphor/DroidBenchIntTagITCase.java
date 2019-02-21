package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import de.ecspride.BaseClass;
import de.ecspride.BaseClass2;
import de.ecspride.DataStore;
import de.ecspride.Datacontainer;
import de.ecspride.General;
import de.ecspride.VarA;
import de.ecspride.VarB;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class DroidBenchIntTagITCase extends BasePhosphorTest {

	public static int getTaint(String description) {
		return Tainter.getTaint(description.toCharArray()[0]);
	}

	static int i = 0;

	public static String taintedString(String string) {
		return new String(Tainter.taintedCharArray(string.toCharArray(), 5));
	}

	public static String taintedString() {
		return taintedString(new String("abcdefghi"));
	}

	static class TestFieldSensitivity1 {
		Datacontainer d1;

		void setTaint(Datacontainer data) {
			data.setDescription("abcd");
			data.setSecret(taintedString("abcdefg"));
		}

		void sendTaint() {
			assertTrue(getTaint(d1.getDescription()) == 0);
		}
	}

	@Test
	public void testFieldSensitivity1() {
		TestFieldSensitivity1 t = new TestFieldSensitivity1();
		t.d1 = new Datacontainer();
		t.setTaint(t.d1);
		t.sendTaint();
	}

	@Test
	public void testFieldSensitivity2() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setSecret(taintedString("abcdefg"));
		assertTrue(getTaint(d1.getDescription()) == 0);
	}

	@Test
	public void testFieldSensitivity3() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setSecret(taintedString("abcdefg"));
		assertTrue(getTaint(d1.getSecret()) != 0);
	}

	@Test
	public void testFieldSensitivity4() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setDescription(taintedString("abcdefg"));
		assertTrue(getTaint(d1.getDescription()) != 0);
	}

	@Test
	public void testInheritedObjects1() {
		int a = 46 + 1;
		General g;
		if (a == 47)
			g = new VarA();
		else
			g = new VarB();
		assertTrue(getTaint(g.getInfo()) != 0);
	}

	@Test
	public void testObjectSensitivity1() {
		LinkedList<String> list1 = new LinkedList<String>();
		LinkedList<String> list2 = new LinkedList<String>();
		list1.add(taintedString("abcd")); //source
		list2.add("123");
		assertTrue(getTaint(list2.getFirst()) == 0);
	}

	@Test
	public void testObjectSensitivity2() {
		String var;
		DataStore ds = new DataStore();

		String taintedString = taintedString("abcd");

		var = taintedString;
		ds.field = taintedString;

		var = "abc";
		ds.field = "def";

		assertTrue(getTaint(var) == 0);
		assertTrue(getTaint(ds.field) == 0);
	}

	@Test
	public void testExceptions1() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			throw new RuntimeException();
		} catch (RuntimeException ex) {
			assertTrue(getTaint(imei) != 0);
		}
	}

	@Test
	public void testExceptions2() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			int[] arr = new int[(int) Math.sqrt(49)];
			if (arr[32] > 0)
				imei = "";
		} catch (RuntimeException ex) {
			assertTrue(getTaint(imei) != 0);
		}
	}

	@Test
	public void testExceptions3() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			int[] arr = new int[42];
			if (arr[32] > 0)
				imei = "";
		} catch (RuntimeException ex) {
			assertTrue(getTaint(imei) != 0);
		}
	}

	@Test
	public void testExceptions4() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			throw new RuntimeException(imei);
		} catch (RuntimeException ex) {
			assertTrue(getTaint(ex.getMessage()) != 0);
		}
	}

	@Test
	public void testLoopExample1() {
		String imei = taintedString("abcd");

		String obfuscated = "";
		for (char c : imei.toCharArray())
			obfuscated += c + "_";
		assertTrue(getTaint(obfuscated) != 0);
	}

	@Test
	public void testLoopExample2() {
		String imei = taintedString("abcd");

		String obfuscated = "";
		for (int i = 0; i < 10; i++)
			if (i == 9)
				for (char c : imei.toCharArray())
					obfuscated += c + "_";
		assertTrue(getTaint(obfuscated) != 0);
	}

	static class SourceCodeSpecific1 {
		void doTest() {
			Set<String> phoneNumbers = new HashSet<String>();
			phoneNumbers.add("+49 123456");
			phoneNumbers.add("+49 654321");
			phoneNumbers.add("+49 111111");
			phoneNumbers.add("+49 222222");
			phoneNumbers.add("+49 333333");

			int a = 22 + 11;
			int b = 22 * 2 - 1 + a;

			String message = (a == b) ? "no taint" : taintedString("abcd"); //source

			sendSMS(phoneNumbers, message);
		}

		private void sendSMS(Set<String> numbers, String message) {
			for (String number : numbers) {
				assertTrue(getTaint(message) != 0);
			}
		}

	}

	@Test
	public void testSourceCodeSpecific1() {
		new SourceCodeSpecific1().doTest();
	}

	public static String im;

	@Test
	public void testStaticInitialization1() {
		im = taintedString();
		new StaticInitClass1();
	}

	public static class StaticInitClass1 {
		static {
			assertTrue(getTaint(im) != 0);
		}
	}

	public static String im2;

	@Test
	public void testStaticInitialization2() {
		new StaticInitClass2();
		assertTrue(getTaint(im2) != 0);
	}

	public static class StaticInitClass2 {
		static {
			im2 = taintedString();
		}
	}

	@Test
	public void testUnreachableCode() {
		int i = 46 + 1;
		if (i < 47) {
			String s = taintedString();
			assertTrue(getTaint(s) != 0);
		}
	}

	@Test
	public void testArrayAccess1() {
		String[] arrayData = new String[3];
		arrayData[0] = "abcd";
		arrayData[1] = taintedString();
		arrayData[2] = "abcd";
		assertTrue(getTaint(arrayData[2]) == 0);
	}

	@Test
	public void testArrayAccess2() {
		String[] arrayData = new String[10];
		arrayData[0] = "abcd";
		arrayData[4] = "abcd";
		arrayData[5] = taintedString();
		arrayData[2] = "abcd";
		assertTrue(getTaint(arrayData[calculateIndex()]) == 0);
	}

	private static int calculateIndex() {
		int index = 1;
		index++;
		index *= 5;
		index = index % 10;
		index += 4;
		return index;
	}

	@Test
	public void testHashMapAccess1() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("tainted", taintedString());
		map.put("untainted", "abcd");
		assertTrue(getTaint(map.get("untainted")) == 0);
		assertTrue(getTaint(map.get("tainted")) != 0);
	}

	@Test
	public void testListAccess1() {
		LinkedList<String> list = new LinkedList<String>();
		list.add("b");
		list.add(taintedString());
		list.add("c");
		list.add("d");
		assertTrue(getTaint(list.getFirst()) == 0);
		assertTrue(getTaint(list.get(0)) == 0);
		assertTrue(getTaint(list.get(1)) != 0);
	}

	@Test
	public void testReflectionTest1() {
		try {
			BaseClass bc = (BaseClass) Class.forName("de.ecspride.ConcreteClass").newInstance();
			bc.imei = taintedString();
			assertTrue(getTaint(bc.imei) != 0);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testReflectionTest2() {
		try {
			BaseClass bc = (BaseClass) Class.forName("de.ecspride.ConcreteClass").newInstance();
			bc.imei = taintedString();
			assertTrue(getTaint(bc.foo()) != 0);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testReflectionTest3() {
		try {
			String imei = taintedString();

			Class c = Class.forName("de.ecspride.ReflectiveClass");
			Object o = c.newInstance();
			Method m = c.getMethod("setIme" + "i", String.class);
			m.invoke(o, imei);

			Method m2 = c.getMethod("getImei");
			String s = (String) m2.invoke(o);

			assertTrue(getTaint(s) != 0);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testReflectionTest4() {
		try {
			BaseClass2 bc = (BaseClass2) Class.forName("de.ecspride.ConcreteClass2").newInstance();
			String s = bc.foo();
			bc.bar(s);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
