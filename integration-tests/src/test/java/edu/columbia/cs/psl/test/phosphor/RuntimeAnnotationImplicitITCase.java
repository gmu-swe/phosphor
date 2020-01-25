package edu.columbia.cs.psl.test.phosphor;

import org.junit.Test;

public class RuntimeAnnotationImplicitITCase extends BasePhosphorTest {

	
	@Test
	public void testSimpleIf()  {
		// this will crash if the test fails.
		@RuntimeAnnotationExample("EXAMPLE") int a = 100;
		
		
		a = a + 100;
		
		int b = 10;
		
		a = b + 1;
		
		
	}
}
