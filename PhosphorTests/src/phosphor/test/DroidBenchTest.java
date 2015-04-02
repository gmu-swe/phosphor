package phosphor.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import de.ecspride.BaseClass;
import de.ecspride.BaseClass2;
import de.ecspride.DataStore;
import de.ecspride.Datacontainer;
import de.ecspride.General;
import de.ecspride.VarA;
import de.ecspride.VarB;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class DroidBenchTest {

	public static int getTaint(String description) {
		return MultiTainter.getTaint(description.toCharArray()[0]) == null ? 0 : 1;
	}

	public static String taintedString(String string) {
		char[] c = MultiTainter.taintedCharArray(string.toCharArray(), 5);
		String r = new String(c);
		return r;
	}

	public static String taintedString() {
		char[] c = MultiTainter.taintedCharArray("abcdefghi".toCharArray(), 5);
		String r = new String(c);
		return r;
	}

	static class TestFieldSensitivity1 {
		Datacontainer d1;

		void setTaint(Datacontainer data) {
			data.setDescription("abcd");
			data.setSecret(taintedString("abcdefg"));
		}

		void sendTaint() {
			assert (getTaint(d1.getDescription()) == 0);
		}
	}

	static void testFieldSensitivity1() {
		TestFieldSensitivity1 t = new TestFieldSensitivity1();
		t.d1 = new Datacontainer();
		t.setTaint(t.d1);
		t.sendTaint();
	}

	static void testFieldSensitivity2() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setSecret(taintedString("abcdefg"));
		assert (getTaint(d1.getDescription()) == 0);
	}

	static void testFieldSensitivity3() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setSecret(taintedString("abcdefg"));
		assert (getTaint(d1.getSecret()) != 0);
	}

	static void testFieldSensitivity4() {
		Datacontainer d1 = new Datacontainer();
		d1.setDescription("abcd");
		d1.setDescription(taintedString("abcdefg"));
		assert (getTaint(d1.getDescription()) != 0);
	}

	static void testInheritedObjects1() {
		int a = 46 + 1;
		General g;
		if (a == 47)
			g = new VarA();
		else
			g = new VarB();
		assert (getTaint(g.getInfo()) != 0);
	}

	static void testObjectSensitivity1() {
		LinkedList<String> list1 = new LinkedList<String>();
		LinkedList<String> list2 = new LinkedList<String>();
		list1.add(taintedString("abcd")); //source
		list2.add("123");
		assert (getTaint(list2.getFirst()) == 0);
	}

	static void testObjectSensitivity2() {
		String var;
		DataStore ds = new DataStore();

		String taintedString = taintedString("abcd");

		var = taintedString;
		ds.field = taintedString;

		var = "abc";
		ds.field = "def";

		assert (getTaint(var) == 0);
		assert (getTaint(ds.field) == 0);
	}

	static void testExceptions1() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			throw new RuntimeException();
		} catch (RuntimeException ex) {
			assert (getTaint(imei) != 0);
		}
	}

	static void testExceptions2() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			int[] arr = new int[(int) Math.sqrt(49)];
			if (arr[32] > 0)
				imei = "";
		} catch (RuntimeException ex) {
			assert (getTaint(imei) != 0);
		}
	}

	static void testExceptions3() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			int[] arr = new int[42];
			if (arr[32] > 0)
				imei = "";
		} catch (RuntimeException ex) {
			assert (getTaint(imei) != 0);
		}
	}

	static void testExceptions4() {
		String imei = "";
		try {
			imei = taintedString("abcd");
			throw new RuntimeException(imei);
		} catch (RuntimeException ex) {
			assert (getTaint(ex.getMessage()) != 0);
		}
	}

	static void testLoopExample1() {
		String imei = taintedString("abcd");

		String obfuscated = "";
		for (char c : imei.toCharArray())
			obfuscated += c + "_";
		assert (getTaint(obfuscated) != 0);
	}

	static void testLoopExample2() {
		String imei = taintedString("abcd");

		String obfuscated = "";
		for (int i = 0; i < 10; i++)
			if (i == 9)
				for (char c : imei.toCharArray())
					obfuscated += c + "_";
		assert (getTaint(obfuscated) != 0);
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
				assert (getTaint(message) != 0);
			}
		}

	}

	static void testSourceCodeSpecific1() {
		new SourceCodeSpecific1().doTest();
	}

	public static String im;

	static void testStaticInitialization1() {
		im = taintedString();
		new StaticInitClass1();
	}

	public static class StaticInitClass1 {
		static {
			assert (getTaint(im) != 0);
		}
	}

	public static String im2;

	static void testStaticInitialization2() {
		new StaticInitClass2();
		assert (getTaint(im2) != 0);
	}

	public static class StaticInitClass2 {
		static {
			im2 = taintedString();
		}
	}

	static void testUnreachableCode() {
		int i = 46 + 1;
		if (i < 47) {
			String s = taintedString();
			assert (getTaint(s) != 0);
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
			assert (getTaint(message) != 0);
		}
	}

	static void testImplicitFlow1() {
		new ImplicitFlow1().doTest();
	}

	static boolean passwordCorrect;

	static void testImplicitFlow2() {
		String userInputPassword = taintedString("superSecure");
		if (userInputPassword.equals("superSecure"))
			passwordCorrect = true;

		if (passwordCorrect)
			assert (getTaint("foo") != 0);
		else
			assert (getTaint("foo") == 0);
	}

	static void testImplicitFlow3() {
		assert (false); //TODO to implement this - we know it will fail!!
	}

	static void testImplicitFlow4() {
		assert (false); //TODO to implement this - we know it will fail!!
	}

	static void testArrayAccess1() {
		String[] arrayData = new String[3];
		arrayData[0] = "abcd";
		arrayData[1] = taintedString();
		arrayData[2] = "abcd";
		assert (getTaint(arrayData[2]) == 0);
	}

	static void testArrayAccess2() {
		String[] arrayData = new String[10];
		arrayData[0] = "abcd";
		arrayData[4] = "abcd";
		arrayData[5] = taintedString();
		arrayData[2] = "abcd";
		assert (getTaint(arrayData[calculateIndex()]) == 0);
	}

	private static int calculateIndex() {
		int index = 1;
		index++;
		index *= 5;
		index = index % 10;
		index += 4;
		return index;
	}

	static void testHashMapAccess1() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("tainted", taintedString());
		map.put("untainted", "abcd");
		assert (getTaint(map.get("untainted")) == 0);
		assert (getTaint(map.get("tainted")) != 0);
	}

	static void testListAccess1() {
		LinkedList<String> list = new LinkedList<String>();
		list.add("b");
		list.add(taintedString());
		list.add("c");
		list.add("d");
		assert (getTaint(list.getFirst()) == 0);
		assert (getTaint(list.get(0)) == 0);
		assert (getTaint(list.get(1)) != 0);
	}

	static void testReflectionTest1() {
		try {
			BaseClass bc = (BaseClass) Class.forName("de.ecspride.ConcreteClass").newInstance();
			bc.imei = taintedString();
			assert (getTaint(bc.imei) != 0);
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

	static void testReflectionTest2() {
		try {
			BaseClass bc = (BaseClass) Class.forName("de.ecspride.ConcreteClass").newInstance();
			bc.imei = taintedString();
			assert (getTaint(bc.foo()) != 0);
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

	static void testReflectionTest3() {
		try {
			String imei = taintedString();

			Class c = Class.forName("de.ecspride.ReflectiveClass");
			Object o = c.newInstance();
			Method m = c.getMethod("setIme" + "i", String.class);
			m.invoke(o, imei);

			Method m2 = c.getMethod("getImei");
			String s = (String) m2.invoke(o);

			assert (getTaint(s) != 0);
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
	static void testReflectionTest4()
	{

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
	public static void main(String[] args) {
		for (Method m : DroidBenchTest.class.getDeclaredMethods()) {
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
