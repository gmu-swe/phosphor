package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.After;
import org.junit.Test;

public class OrImplicitITCase extends BaseMultiTaintClass {

	@After
	public void resetState() {
		MultiTainter.getControlFlow().reset();
	}
	@Test
	public void testOneWayOr() throws Exception {
		resetState();
		int x = MultiTainter.taintedInt(-1, "x");
		int y = MultiTainter.taintedInt(6, "y");
		int result = 0;
		int other = 0;
		if (x > 0 || y > 0) {
			result += 1;
		}
		other+=5;
		assertTaintHasOnlyLabels(MultiTainter.getTaint(result), "x", "y");
		assertNullOrEmpty(MultiTainter.getTaint(other));
	}
	
	@Test
	public void testOneWayOr2() throws Exception {
		resetState();
		int x = MultiTainter.taintedInt(1, "x");
		int y = MultiTainter.taintedInt(6, "y");
		int result = 0;
		int other = 0;
		if (x > 0 || y > 0) {
			result += 1;
		}
		other+=5;
		assertTaintHasOnlyLabels(MultiTainter.getTaint(result), "x");
		assertNullOrEmpty(MultiTainter.getTaint(other));
	}
	
	@Test
	public void testTwoWayOr() throws Exception {
		resetState();
		int x = MultiTainter.taintedInt(-1, "x");
		int y = MultiTainter.taintedInt(6, "y");
		int z = MultiTainter.taintedInt(8, "z");

		int result = 0;
		int other = 0;
		if (x > 0 || y > 0) {
			result += 1;
			if(z > 3)
				result += 2;
		}
		other+=5;
		assertTaintHasOnlyLabels(MultiTainter.getTaint(result), "x", "y","z");
		assertNullOrEmpty(MultiTainter.getTaint(other));
	}
	@Test
	public void testTwoWayOr2() throws Exception {
		resetState();
		int x = MultiTainter.taintedInt(2, "x");
		int y = MultiTainter.taintedInt(6, "y");
		int z = MultiTainter.taintedInt(8, "z");

		int result = 0;
		int other = 0;
		if (x > 0 || y > 0) {
			result += 1;
			if(z > 3)
				result += 2;
		}
		other+=5;
		assertTaintHasOnlyLabels(MultiTainter.getTaint(result), "x","z");
		assertNullOrEmpty(MultiTainter.getTaint(other));
	}
	
}
