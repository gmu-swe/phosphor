package phosphor.test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class SourceSinkTest {
	static void sink(Object in) {
		throw new IllegalStateException("Sink method executed succesfully");
	}

	static void sink2(int[] in) {
		throw new IllegalStateException("Sink method executed succesfully");
	}

	static void sink(int in) {
		throw new IllegalStateException("Sink method executed succesfully");
	}

	static void sourceArgs(Object in) {
		System.out.println("Source 1");
	}

	static void sourceArgs2(int[] in) {
		System.out.println("Source 2");
	}

	static int sourceRet1() {
		System.out.println("Source 1");
		return 0;
	}

	static int[] sourceRet2() {
		System.out.println("Source 2");
		return new int[5];
	}

	static Object sourceRet3() {
		System.out.println("Source 3");
		return new long[5];
	}

	static int[] sourceRet4() {
		System.out.println("Source 4");
		return null;
	}

	public static void main(String[] args) {
		int[] t1 = new int[5];
		sourceArgs(t1);
		int[] t2 = new int[6];
		sourceArgs2(t2);
		int[] t3 = sourceRet2();
		long[] t4 = (long[]) sourceRet3();
		int[] t5 = sourceRet4();
		int t6 = sourceRet1();

		try {
			sink(t1);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink(t2);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink(t3);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink(t4);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink(t5);
		} catch (IllegalStateException e) {
			//OK to let "null" go through
		}
		try {
			sink2(t1);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink2(t2);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink2(t3);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}
		try {
			sink2(t5);
		} catch (IllegalStateException e) {
			//OK to let "null" go through
		}
		try {
			sink(t6);
		} catch (IllegalAccessError e) {
			System.out.println("OK: " + e.getMessage());
		}

	}
}
