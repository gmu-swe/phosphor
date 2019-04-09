package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Test;

public class ArrayOptimizationObjTagITCase extends BaseMultiTaintClass{
	@Test
	public void testNewArrayConstantClearsTaint(){

		byte[] b = new byte[10];
		b[0] = MultiTainter.taintedByte((byte)1,"Foo");
		b[0] = 10;
		assertNullOrEmpty(MultiTainter.getTaint(b[0]));


	}


	@Test
	public void testHashCodeGetsTaint() {

		byte[] b = new byte[10];
		MultiTainter.taintedObject(b, new Taint("foo"));

		int taggedHashCode = b.hashCode();

		assertNonNullTaint(taggedHashCode);
	}

	@Test
	public void testEqualsResultGetsTag() {

		byte[] b = new byte[10];
		MultiTainter.taintedObject(b, new Taint("foo"));

		boolean taggedEquals = b.equals(null);

		assertNonNullTaint(MultiTainter.getTaint(taggedEquals));
	}


}
