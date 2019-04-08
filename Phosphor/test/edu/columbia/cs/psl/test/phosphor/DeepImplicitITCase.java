package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Assert;
import org.junit.Test;

public class DeepImplicitITCase extends BasePhosphorTest {
	int x;
	public void setX(int x){
		this.x=x;
	}
	public class Holder{
		int z;

		public void setZ(int z) {
			this.z = z;
		}
		public void otherSetZ(int z){
			this.z = z;
		}
		public void checkAndThrow(int z){
			if(z > 10)
				throw new IllegalArgumentException();
		}
	}
	static Holder oh;
	Holder h;


	@Test
	public void testSimpleLV(){
		int l = 0;
		int y = MultiTainter.taintedInt(10,"testSimpleLV");
		if(y >100)
			l = 10;
		Assert.assertNotNull(MultiTainter.getTaint(l));
	}
	@Test
	public void testMethodsOfFields(){
//		int x = 10;
		h = new Holder();
		oh = new Holder();
		int y = MultiTainter.taintedInt(10,"testMethodsOfFields");
		if(y > 100)
			h.setZ(40);
		if(y > 1000)
			oh.otherSetZ(455);
		Assert.assertNotNull(MultiTainter.getTaint(h.z));
		Assert.assertNotNull(MultiTainter.getTaint(oh.z));
	}
	@Test
	public void testFieldsOfThis(){
		x = 0;
		int y = MultiTainter.taintedInt(10,"testFieldsOfThis");
		if(y > 100)
			this.x =10;
		Assert.assertNotNull(MultiTainter.getTaint(this.x));
	}

	@Test
	public void testCallsMethodThatThrowsExceptionWrappedInTry(){
		x = 0;
		int y = MultiTainter.taintedInt(10, "testCallsMethodThatThrowsException");
		Holder h = new Holder();
		try {
			if (y > 100)
				h.checkAndThrow(y);
		} catch (IllegalArgumentException ex) {
			x = 100;
		}
		Assert.assertNotNull(MultiTainter.getTaint(this.x));
	}

	@Test
	public void testCallsMethodThatThrowsExceptionNoTry() {
		x = 0;
		int y = MultiTainter.taintedInt(10, "testCallsMethodThatThrowsException");
		Holder h = new Holder();
		if (y > 100)
			h.checkAndThrow(y);
		x = 100;
		Assert.assertNotNull(MultiTainter.getTaint(this.x));
	}
}
