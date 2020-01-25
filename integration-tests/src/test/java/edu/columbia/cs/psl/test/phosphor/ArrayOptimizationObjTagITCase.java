package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Assert;
import org.junit.Ignore;
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
		b[0] = MultiTainter.taintedByte((byte)1,"Foo");
		b[1] = MultiTainter.taintedByte((byte)1,"Bar");
		int taggedHashCode = b.hashCode();

		Taint taint = MultiTainter.getTaint(taggedHashCode);
		assertNullOrEmpty(taint);
	}

	@Test
	public void testEqualsResultGetsTag() {

		byte[] b = new byte[10];
		b[0] = MultiTainter.taintedByte((byte)1,"Foo");
		b[1] = MultiTainter.taintedByte((byte)1,"Bar");
		boolean taggedEquals = b.equals(null);

		Taint taint = MultiTainter.getTaint(taggedEquals);
		assertNullOrEmpty(taint);
	}

	@Test
	public void testReferenceReassignmentClearsTaint() {

		byte[] b = new byte[10];
		b[0] = MultiTainter.taintedByte((byte)1,"Foo");
		b[1] = MultiTainter.taintedByte((byte)1,"Bar");
		b = new byte[10];
		boolean taggedEquals = b.equals(null);

		Taint taint = MultiTainter.getTaint(taggedEquals);
		assertNullOrEmpty(taint);
	}
}
