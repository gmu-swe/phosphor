package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.After;
import org.junit.Test;

public class ExceptionalFlowImplicitITCase extends BaseMultiTaintClass{
	@After
	public void resetState() {
		MultiTainter.getControlFlow().reset();
	}

	public void checkAndThrow(int in){
		if(in < 0)
			throw new IllegalStateException();
	}
//	@Test
	public void testUnthrownExceptions(){
		resetState();
		int x = MultiTainter.taintedInt(10,"testUnthrownExceptions.X");
		int y = 10;
		int z = 5;
		try{
			checkAndThrow(x);
		}
		catch(Throwable t){
			y = 1000;
		}
		z = 5;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y),"testUnthrownExceptions.X");
		assertNullOrEmpty(MultiTainter.getTaint(z));

	}
//	@Test
	public void testUnthrownExceptionsReturn(){
		resetState();
		int x = MultiTainter.taintedInt(10,"testUnthrownExceptionsReturn.X");
		int y = 10;
		int z = 5;
		try{
			checkAndThrow(x);
		}
		catch(Throwable t){
			y = 1000;
			return;
		}
		z = 5;
		assertTaintHasOnlyLabel(MultiTainter.getTaint(y),"testUnthrownExceptionsReturn.X");
		assertNullOrEmpty(MultiTainter.getTaint(z));
	}
//	@Test
	public void testUnthrownExceptionsNested(){
		resetState();
		int x = MultiTainter.taintedInt(10,"testUnthrownExceptionsNested.X");
		int y = 10;
		int z = MultiTainter.taintedInt(5, "testUnthrownExceptionsNested.Y");
		try{
			checkAndThrow(x);
			try {
				checkAndThrow(z);
			}
			finally {
				z = 100;
			}
		}
		catch(Throwable t){
			y = 1000;
		}
		assertTaintHasOnlyLabels(MultiTainter.getTaint(y),"testUnthrownExceptionsNested.X","testUnthrownExceptionsNested.Y");
		assertTaintHasOnlyLabels(MultiTainter.getTaint(z),"testUnthrownExceptionsNested.Y","testUnthrownExceptionsNested.X");

	}
}
