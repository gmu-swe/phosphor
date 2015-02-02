package phosphor.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class ArraysTest {
	
	static void testOneDArrays()
	{
		int[] a1 = new int[4];
		a1[0] = Tainter.taintedInt(2, 5);
		byte[] a2 = new byte[4];
		a2[0] = Tainter.taintedByte((byte)2, 6);
		assert(Tainter.getTaint(a1[0]) == 5);
		assert(Tainter.getTaint(a2[0]) == 6);
		
		Object b = a1;
		Object c = a2;
		assert(Tainter.getTaint(((int[])b)[0]) == 5);
		assert(Tainter.getTaint(((byte[])c)[0]) == 6);
	}
	static void testTwoDArrays()
	{
		int[][] a1 = new int[4][6];
		a1[0][0] = Tainter.taintedInt(2, 5);
		byte[][] a2 = new byte[4][5];
		a2[0][0] = Tainter.taintedByte((byte)2, 6);
		assert(Tainter.getTaint(a1[0][0]) == 5);
		assert(Tainter.getTaint(a2[0][0]) == 6);
		
		Object b = a1;
		Object c = a2;
		assert(Tainter.getTaint(((int[][])b)[0][0]) == 5);
		assert(Tainter.getTaint(((byte[][])c)[0][0]) == 6);
		
	}
	
	public static void main(String[] args) {
		for (Method m : ArraysTest.class.getDeclaredMethods()) {
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
