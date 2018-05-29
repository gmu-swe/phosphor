package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Test;

public class ArrayOptimizationObjTagITCase extends BaseMultiTaintClass{
	@Test
	public void testNewArrayConstantClearsTaint(){

		byte[] b = new byte[10];
		b[0] = MultiTainter.taintedByte((byte)1,"Foo");
		b[0] = 10;
		assertNullOrEmpty(MultiTainter.getTaint(b[0]));


	}
}
