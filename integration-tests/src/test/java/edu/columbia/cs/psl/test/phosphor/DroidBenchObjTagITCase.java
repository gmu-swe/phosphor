package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

public class DroidBenchObjTagITCase extends BasePhosphorTest {

	public static void assertNoTaint(String obj)
	{
		Taint taint = MultiTainter.getTaint(obj.toCharArray()[0]);
		if(taint == null)
		{
			return;
		}
		if(taint.isEmpty())
			return;
		fail("Expected null taint. Got: " + taint);
	}
	public static int getTaint(String description) {
		Taint taint = MultiTainter.getTaint(description.toCharArray()[0]);
		return (taint == null || taint.isEmpty()) ? 0 : 1;
	}

	static int i = 0;

	public static String taintedString(String string) {
		Object r = new String(string.toCharArray());
		((TaintedWithObjTag) r).setPHOSPHOR_TAG(Taint.withLabel("Some tainted data " + (++i)));
		return (String) r;
	}

	public static String taintedString() {
		return taintedString("abcdefghi");
	}

	static class TestFieldSensitivity1 {
		Datacontainer d1;

		void setTaint(Datacontainer data) {
			data.setDescription("abcdtestfieldsens1");
			data.setSecret(taintedString("abcdefg"));
		}

		void sendTaint() {
			assertNoTaint(d1.getDescription());
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
		d1.setDescription("abcdfs2");
		d1.setSecret(taintedString("abcdefg"));
		assertNoTaint(d1.getDescription());
	}

	@Test
	public void testFieldSensitivity3() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcdts3");
		d1.setSecret(taintedString("abcdefg"));
		assertTrue(getTaint(d1.getSecret()) != 0);
	}

	@Test
	public void testFieldSensitivity4() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcdts4");
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
		assertTrue(getTaint(g.getInfoMultiTaint()) != 0);
	}

	@Test
	public void testObjectSensitivity1() {
		LinkedList<String> list1 = new LinkedList<String>();
		LinkedList<String> list2 = new LinkedList<String>();
		list1.add(taintedString("abcdts1")); //source
		list2.add("123");
		assertTrue(getTaint(list2.getFirst()) == 0);
	}

	@Test
	public void testObjectSensitivity2() {
		String var;
		DataStore ds = new DataStore();

		String taintedString = taintedString("abcdts2");

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
			imei = taintedString("abcde1");
			throw new RuntimeException();
		} catch (RuntimeException ex) {
			assertTrue(getTaint(imei) != 0);
		}
	}

	@Test
	public void testExceptions2() {
		String imei = "";
		try {
			imei = taintedString("abcde2");
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
			imei = taintedString("abcde3");
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
			imei = taintedString("abcde4");
			throw new RuntimeException(imei);
		} catch (RuntimeException ex) {
			assertTrue(getTaint(ex.getMessage()) != 0);
		}
	}

	@Test
	public void testLoopExample1() {
		String imei = taintedString("abcdex1");

		String obfuscated = "";
		for (char c : imei.toCharArray())
			obfuscated += c + "_";
		assertTrue(getTaint(obfuscated) != 0);
	}

	@Test
	public void testLoopExample2() {
		String imei = taintedString("abcdex2");

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

			String message = (a == b) ? "no taint" : taintedString("abcdsc1"); //source

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

	static class ImplicitFlow4 {
		public void doTest() {
			String password = taintedString();
			String username = taintedString("hanns");
			try {
				boolean passwordCorrect = lookup(username, password);
				assertTrue(MultiTainter.getTaint(passwordCorrect) != null && !MultiTainter.getTaint(passwordCorrect).isEmpty());
			} catch (Exception ex) {
				//should be a sink here
				ex.printStackTrace();
			}
			//should be a sink here
			assertTrue(false); //We have no concept of exceptional control flow tainting yet
		}

		private boolean lookup(String username, String password) throws Exception {
			if (!username.equals("hanns"))
				throw new Exception("username not available");
			else return !username.equals("hanns") || password.equals("superSecure");
		}
	}

	static class ImplicitFlow3 {
		public void doTest() {
			ArrayList arrayList = new ArrayList();
			LinkedList linkedList = new LinkedList();
			((TaintedWithObjTag) arrayList).setPHOSPHOR_TAG(Taint.withLabel("arraylist tag"));
			((TaintedWithObjTag) linkedList).setPHOSPHOR_TAG(Taint.withLabel("arraylist tag"));

			leakInformationBit(linkedList);
			leakInformationBit(arrayList);
			leakInformationBit(linkedList);
		}

		private void leakInformationBit(List list) {
			if (list instanceof ArrayList) {
				boolean labeledWithCurrentTag = false;
				assertTrue(MultiTainter.getTaint(labeledWithCurrentTag) != null
						&& !MultiTainter.getTaint(labeledWithCurrentTag).isEmpty());
			} else if (list instanceof LinkedList) {
				boolean labeledWithCurrentTag = false;
				assertTrue(MultiTainter.getTaint(labeledWithCurrentTag) != null
						&& !MultiTainter.getTaint(labeledWithCurrentTag).isEmpty());
			}
		}
	}

	static class ImplicitFlow1 {
		public void doTest() {
			String imei = taintedString("0123456789");
			String obfuscatedIMEI = obfuscateIMEI(imei);
			writeToLog(obfuscatedIMEI);

			//hard to detect
			obfuscatedIMEI = reallyHardObfuscatedIMEI(imei);
			writeToLog(obfuscatedIMEI);

		}

		private String obfuscateIMEI(String imei) {
			String result = "";

			for (char c : imei.toCharArray()) {
				switch (c) {
				case '0':
					result += 'a';
					break;
				case '1':
					result += 'b';
					break;
				case '2':
					result += 'c';
					break;
				case '3':
					result += 'd';
					break;
				case '4':
					result += 'e';
					break;
				case '5':
					result += 'f';
					break;
				case '6':
					result += 'g';
					break;
				case '7':
					result += 'h';
					break;
				case '8':
					result += 'i';
					break;
				case '9':
					result += 'j';
					break;
				default:
					System.err.println("Problem in obfuscateIMEI for character: " + c);
				}
			}
			return result;
		}

		private String reallyHardObfuscatedIMEI(String imei) {
			//ASCII values for integer: 48-57
			Integer[] numbers = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
					40, 4142, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58 };

			char[] imeiAsChar = imei.toCharArray();

			char[] newOldIMEI = new char[imeiAsChar.length];

			for (int i = 0; i < imeiAsChar.length; i++) {
				newOldIMEI[i] = Character.forDigit(numbers[(int) imeiAsChar[i]], 10);
			}

			return newOldIMEI.toString();
		}

		private void writeToLog(String message) {
			assertTrue(getTaint(message) != 0);
		}
	}

	@Test(expected=java.lang.AssertionError.class)
	public void testImplicitFlow1() {
		new ImplicitFlow1().doTest();
	}

	static boolean passwordCorrect;

	@Test(expected=java.lang.AssertionError.class)
	public void testImplicitFlow2() {
		String userInputPassword = taintedString("superSecure");
		if (userInputPassword.equals("superSecure"))
			passwordCorrect = true;
		Taint taint = MultiTainter.getTaint(passwordCorrect);
		assertTrue(taint != null && !taint.isEmpty());
	}

	@Test(expected=java.lang.AssertionError.class)
	public void testImplicitFlow3() {
		new ImplicitFlow3().doTest();
	}

	@Test(expected=java.lang.AssertionError.class)
	public void testImplicitFlow4() {
		new ImplicitFlow4().doTest();
	}

	@Test
	public void testArrayAccess1() {
		String[] arrayData = new String[3];
		arrayData[0] = "abcdaa1";
		arrayData[1] = taintedString();
		arrayData[2] = "abcde";
		assertNoTaint(arrayData[2]);
	}

	@Test
	public void testArrayAccess2() {
		String[] arrayData = new String[10];
		arrayData[0] = "abcdaa2";
		arrayData[4] = "abcde";
		arrayData[5] = taintedString();
		arrayData[2] = "abcdef";
		assertNoTaint(arrayData[calculateIndex()]);
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
		map.put("untainted", "abcdzzzzzzzzhma1");
		assertNoTaint(map.get("untainted"));
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
			String s = bc.fooMultiTaint();
			bc.barMultiTaint(s);
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

	public static void main(String[] args) {
		for (Method m : DroidBenchObjTagITCase.class.getDeclaredMethods()) {
			if (m.getName().startsWith("test")) {
				System.out.println(m.getName());
				try {
					m.invoke(null);
				} catch (IllegalAccessException e) {
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
		}
	}
}
