package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import junit.framework.AssertionFailedError;
import org.junit.Test;

public class ExceptionImplicitITCase extends BaseMultiTaintClass{

	@Test
	public void testUnthrownExceptionTaintsNextLineOfControl(){

		int x = MultiTainter.taintedInt(10,"testUnthrownExceptionTaintsNextLineOfControl");
		if(x == 1)
			throw new AssertionFailedError();
		int y = 10; // x's taint should propagate into y
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y),"testUnthrownExceptionTaintsNextLineOfControl");
	}

	@Test
	public void testUnthrownExceptionStopsTaintingAtTry(){
		try {
			int x = MultiTainter.taintedInt(10, "testUnthrownExceptionStopsTaintingAtTry");
			if (x == 1)
				throw new AssertionFailedError();
		}catch(Throwable t){
			//nop
		}
		int y = 10; // x's taint should NOT propagate into y
		assertNullOrEmpty(MultiTainter.getTaint(y));
	}

	@Test
	public void testCatchBlockNotTaken(){
		int y = 10; // x's taint should NOT propagate into y
		try {
			int x = MultiTainter.taintedInt(10, "testCatchBlockNotTaken");
			if (x == 1)
				throw new AssertionFailedError();
		}catch(Throwable t){
			//nop
			y = 5;
		}
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y),"testCatchBlockNotTaken");
	}
}
