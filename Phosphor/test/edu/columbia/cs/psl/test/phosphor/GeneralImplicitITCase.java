package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class GeneralImplicitITCase extends BaseMultiTaintClass {
	String labelA = "a";
	String labelFoo = "Foo";
	@Test
	public void testSimpleIf() throws Exception {
		resetState();
		int i = MultiTainter.taintedInt(1, labelA);

		int k;

		if (i > 0)//control-tag-add-i
		{
			k = 5;
			assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelA);
		}
		else
			//control-tag-add-i
			k = 6;
		int f = MultiTainter.taintedInt(4, labelFoo);
//		System.out.println("F: " + f); //somehow causes f's taint to get stuck on the control flow tags
//		System.out.println(MultiTainter.getTaint(f));
		//control-tag-remove-i
		int r = 54;
		assertNull(MultiTainter.getTaint(r));

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
			assertNull(MultiTainter.getTaint(r));

			foo(r);
			r = 111;
//			System.out.println(MultiTainter.getTaint(r));
			assertTaintHasOnlyLabel(MultiTainter.getTaint(r), labelFoo);
		}
		assertTaintHasOnlyLabel(MultiTainter.getTaint(i), labelA);
		assertTaintHasOnlyLabel(MultiTainter.getTaint(f), labelFoo);
		assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelA);
		assertTaintHasOnlyLabel(MultiTainter.getTaint(r), labelFoo);
	}
	int foo(int in) {
		int k = 5;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(k), labelFoo);
		assertNull(MultiTainter.getTaint(in));
		if (in > 5)
			return 10;
		return 12;
	}
	
	public static void testA(int x, int y, boolean b) {  
       
    }
	@Test
	public void testRelationalConditional() throws Exception {
		int x = -1;
        int y = -1;
        boolean b = true;

        Object labelX = "x";
        Object labelY = "y";
        Object labelB = "b";
        int xt = MultiTainter.taintedInt(x, labelX);
        int yt = MultiTainter.taintedInt(y, labelY);
        boolean bt = MultiTainter.taintedBoolean(b, labelB);

        int e = xt + 1;
        int f = yt + 2;
        assertTaintHasOnlyLabel(MultiTainter.getTaint(e), labelX);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(f), labelY);

        boolean b1 = e > 0;
        boolean b2 = f > -1;
        boolean b3 = e > -1 && bt;
        assertTaintHasOnlyLabel(MultiTainter.getTaint(b1), labelX);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(b2), labelY);
        
        assertTaintHasOnlyLabels(MultiTainter.getTaint(b3), labelX, labelB);
        
        testA(xt, yt, bt);
	}

	@After
	public void resetState() {
		MultiTainter.getControlFlow().reset();
	}
}
