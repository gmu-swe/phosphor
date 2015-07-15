package phosphor.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class PrimitiveTest {
	
	static void testBoxing()
	{
		Boolean z = Tainter.taintedBoolean(false, 4);
		Byte b = Tainter.taintedByte((byte) 4, 4);
		Character c = Tainter.taintedChar('a', 4);
		Integer i = Tainter.taintedInt(4, 4);
		Short s = Tainter.taintedShort((short)5, 5);
		Long l = Tainter.taintedLong((long) 5, 5);
		Float f = Tainter.taintedFloat(4f, 4);
		Double d = Tainter.taintedDouble(4d, 4);
		

		assert(Tainter.getTaint(z.booleanValue()) != 0);
		assert(Tainter.getTaint(b.byteValue()) != 0);
		assert(Tainter.getTaint(c.charValue()) != 0);
		assert(Tainter.getTaint(i.intValue()) != 0);
		assert(Tainter.getTaint(s.shortValue()) != 0);
		assert(Tainter.getTaint(f.floatValue()) != 0);
		assert(Tainter.getTaint(l.longValue()) != 0);
		assert(Tainter.getTaint(d.doubleValue()) != 0);
	}
	static void testNotBoxing()
	{
		boolean z = Tainter.taintedBoolean(false, 4);
		byte b = Tainter.taintedByte((byte) 4, 4);
		char c = Tainter.taintedChar('a', 4);
		int i = Tainter.taintedInt(4, 4);
		short s = Tainter.taintedShort((short)5, 5);
		long l = Tainter.taintedLong((long) 5, 5);
		float f = Tainter.taintedFloat(4f, 4);
		double d = Tainter.taintedDouble(4d, 4);
		

		assert(Tainter.getTaint(z) != 0);
		assert(Tainter.getTaint(b) != 0);
		assert(Tainter.getTaint(c) != 0);
		assert(Tainter.getTaint(i) != 0);
		assert(Tainter.getTaint(s) != 0);
		assert(Tainter.getTaint(l) != 0);
		assert(Tainter.getTaint(f) != 0);
		assert(Tainter.getTaint(d) != 0);
	}
	
	public static void main(String[] args) {
		for (Method m : PrimitiveTest.class.getDeclaredMethods()) {
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
