package phosphor.test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class SimpleImplicitTest {
	public SimpleImplicitTest() {

	}

	public SimpleImplicitTest(String s) {

	}

	public SimpleImplicitTest(int i) {

	}


	static native int foo(int in, double in2);

	public static void main(String[] args) {
		int i = MultiTainter.taintedInt(1, "a");
		int m = MultiTainter.taintedInt(1, "a");

		System.out.println(i + " i - taint is " + MultiTainter.getTaint(i));

		int k;

		if (i > 0)//control-tag-add-i
		{
			k = 5;
			System.out.println(MultiTainter.getTaint(k));
			if(m > 0)// add m
				k++;
			//remove m
		}
		else
		{
			//control-tag-add-i
			k = 6;
			if(m > 0)// add m
				k++;
			//remove m
		}
		int f = MultiTainter.taintedInt(4, "Foo");
		//control-tag-remove-i
		int r = 54;
		switch (f) {
		case 0:
			r = 5;
			break;
		case 1:
			r = 6;
			break;
		case 2:
			r = 7;
			break;
		default:
			foo(r);
			r = 111;
		}
		System.out.println(i + " i - taint is " + MultiTainter.getTaint(i));
		System.out.println(f + " f - taint is " + MultiTainter.getTaint(f));
		System.out.println(k + " k - taint is " + MultiTainter.getTaint(k));
		System.out.println(r + " r - taint is " + MultiTainter.getTaint(r));
	}

	static int foo(int in) {
		int k = 5;
		System.out.println("In foo, taint is "  + MultiTainter.getTaint(k));
		if (in > 5)
			return 10;
		return 12;
	}
}
