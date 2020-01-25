package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.After;
import org.junit.Test;

public class ExceptionalFlowImplicitITCase extends BaseMultiTaintClass {
	@After
	public void resetState() {
		MultiTainter.getControlFlow().reset();
	}

	public void checkAndThrow(int in) {
		if(in < 0) {
			throw new IllegalStateException();
		}
	}

	@Test
	public void testThrownException() {
		resetState();
		int x = MultiTainter.taintedInt(5, "testThrownExceptions.X");
		int y = 10;
		int z = 10;
		try {
			checkAndThrow(x);
		} catch(IllegalStateException ex) {
			y = 4;
		}
		z = 45;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y), "testThrownExceptions.X");
		assertNullOrEmpty(MultiTainter.getTaint(z));
	}

	@Test
	public void testThrownExceptionNested() {
		resetState();
		int x = MultiTainter.taintedInt(5, "testThrownExceptionsNested.X");
		int x2 = MultiTainter.taintedInt(5, "testThrownExceptionsNested.X2");
		int y = 10;
		int z = 10;
		int q = 40;
		try {
			try {
				checkAndThrow(x);
				if(x2 > 5) {
					throw new NullPointerException();
				}
			} catch(NullPointerException npe) {
				y = 45;
			}
		} catch(IllegalStateException ex) {
			z = 4;
		}
		q = 56;
		assertTaintHasOnlyLabels(MultiTainter.getTaint(y), "testThrownExceptionsNested.X", "testThrownExceptionsNested.X2");
		assertTaintHasOnlyLabel(MultiTainter.getTaint(z), "testThrownExceptionsNested.X");
		assertNullOrEmpty(MultiTainter.getTaint(q));
	}

	@Test
	public void testUnthrownExceptions() {
		resetState();
		int x = MultiTainter.taintedInt(10, "testUnthrownExceptions.X");
		int y = 10;
		int z = 5;
		try {
			checkAndThrow(x);
		} catch(Throwable t) {
			y = 1000;
		}
		z = 5;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y), "testUnthrownExceptions.X");
		assertNullOrEmpty(MultiTainter.getTaint(z));

	}

	@Test
	public void testUnthrownExceptionsReturn() {
		resetState();
		int x = MultiTainter.taintedInt(10, "testUnthrownExceptionsReturn.X");
		int y = 10;
		int z = 5;
		try {
			checkAndThrow(x);
		} catch(Throwable t) {
			y = 1000;
			return;
		}
		z = 5;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y),"testUnthrownExceptionsReturn.X");
		assertNullOrEmpty(MultiTainter.getTaint(z));
	}
}
