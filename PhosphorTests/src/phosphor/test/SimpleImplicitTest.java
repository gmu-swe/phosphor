package phosphor.test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class SimpleImplicitTest {
	public static void main(String[] args) {
		int i = Tainter.taintedInt(1, 4);
		int k;
		
		if (i > 0)//control-tag-add-i
			k = 5;
		else//control-tag-add-i
			k = 6;
		//control-tag-remove-i
		System.out.println(k + " - taint is " + Tainter.getTaint(k));
	}
	static int foo(int in)
	{
		if(in > 5)
			return 10;
		return 12;
	}
}
